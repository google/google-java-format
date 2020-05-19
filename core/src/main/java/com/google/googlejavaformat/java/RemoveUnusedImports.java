/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.googlejavaformat.java;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.CharMatcher;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeMap;
import com.google.common.collect.TreeRangeSet;
import com.google.googlejavaformat.Newlines;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.DCTree;
import com.sun.tools.javac.tree.DCTree.DCReference;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;

/**
 * Removes unused imports from a source file. Imports that are only used in javadoc are also
 * removed, and the references in javadoc are replaced with fully qualified names.
 */
public class RemoveUnusedImports {

  // Visits an AST, recording all simple names that could refer to imported
  // types and also any javadoc references that could refer to imported
  // types (`@link`, `@see`, `@throws`, etc.)
  //
  // No attempt is made to determine whether simple names occur in contexts
  // where they are type names, so there will be false positives. For example,
  // `List` is not identified as unused import below:
  //
  // ```
  // import java.util.List;
  // class List {}
  // ```
  //
  // This is still reasonably effective in practice because type names differ
  // from other kinds of names in casing convention, and simple name
  // clashes between imported and declared types are rare.
  private static class UnusedImportScanner extends TreePathScanner<Void, Void> {

    private final Set<String> usedNames = new LinkedHashSet<>();
    private final Multimap<String, Range<Integer>> usedInJavadoc = HashMultimap.create();
    final JavacTrees trees;
    final DocTreeScanner docTreeSymbolScanner;

    private UnusedImportScanner(JavacTrees trees) {
      this.trees = trees;
      docTreeSymbolScanner = new DocTreeScanner();
    }

    /** Skip the imports themselves when checking for usage. */
    @Override
    public Void visitImport(ImportTree importTree, Void usedSymbols) {
      return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree tree, Void unused) {
      if (tree == null) {
        return null;
      }
      usedNames.add(tree.getName().toString());
      return null;
    }

    @Override
    public Void scan(Tree tree, Void unused) {
      if (tree == null) {
        return null;
      }
      scanJavadoc();
      return super.scan(tree, unused);
    }

    private void scanJavadoc() {
      if (getCurrentPath() == null) {
        return;
      }
      DocCommentTree commentTree = trees.getDocCommentTree(getCurrentPath());
      if (commentTree == null) {
        return;
      }
      docTreeSymbolScanner.scan(new DocTreePath(getCurrentPath(), commentTree), null);
    }

    // scan javadoc comments, checking for references to imported types
    class DocTreeScanner extends DocTreePathScanner<Void, Void> {
      @Override
      public Void visitIdentifier(com.sun.source.doctree.IdentifierTree node, Void aVoid) {
        return null;
      }

      @Override
      public Void visitReference(ReferenceTree referenceTree, Void unused) {
        DCReference reference = (DCReference) referenceTree;
        long basePos =
            reference.getSourcePosition((DCTree.DCDocComment) getCurrentPath().getDocComment());
        // the position of trees inside the reference node aren't stored, but the qualifier's
        // start position is the beginning of the reference node
        if (reference.qualifierExpression != null) {
          new ReferenceScanner(basePos).scan(reference.qualifierExpression, null);
        }
        // Record uses inside method parameters. The javadoc tool doesn't use these, but
        // IntelliJ does.
        if (reference.paramTypes != null) {
          for (JCTree param : reference.paramTypes) {
            // TODO(cushon): get start positions for the parameters
            new ReferenceScanner(-1).scan(param, null);
          }
        }
        return null;
      }

      // scans the qualifier and parameters of a javadoc reference for possible type names
      private class ReferenceScanner extends TreeScanner<Void, Void> {
        private final long basePos;

        public ReferenceScanner(long basePos) {
          this.basePos = basePos;
        }

        @Override
        public Void visitIdentifier(IdentifierTree node, Void aVoid) {
          usedInJavadoc.put(
              node.getName().toString(),
              basePos != -1
                  ? Range.closedOpen((int) basePos, (int) basePos + node.getName().length())
                  : null);
          return super.visitIdentifier(node, aVoid);
        }
      }
    }
  }

  public static String removeUnusedImports(final String contents) throws FormatterException {
    Context context = new Context();
    JCCompilationUnit unit = parse(context, contents);
    if (unit == null) {
      // error handling is done during formatting
      return contents;
    }
    UnusedImportScanner scanner = new UnusedImportScanner(JavacTrees.instance(context));
    scanner.scan(unit, null);
    return applyReplacements(
        contents, buildReplacements(contents, unit, scanner.usedNames, scanner.usedInJavadoc));
  }

  private static JCCompilationUnit parse(Context context, String javaInput)
      throws FormatterException {
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    context.put(DiagnosticListener.class, diagnostics);
    Options.instance(context).put("--enable-preview", "true");
    Options.instance(context).put("allowStringFolding", "false");
    JCCompilationUnit unit;
    JavacFileManager fileManager = new JavacFileManager(context, true, UTF_8);
    try {
      fileManager.setLocation(StandardLocation.PLATFORM_CLASS_PATH, ImmutableList.of());
    } catch (IOException e) {
      // impossible
      throw new IOError(e);
    }
    SimpleJavaFileObject source =
        new SimpleJavaFileObject(URI.create("source"), JavaFileObject.Kind.SOURCE) {
          @Override
          public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return javaInput;
          }
        };
    Log.instance(context).useSource(source);
    ParserFactory parserFactory = ParserFactory.instance(context);
    JavacParser parser =
        parserFactory.newParser(
            javaInput, /*keepDocComments=*/ true, /*keepEndPos=*/ true, /*keepLineMap=*/ true);
    unit = parser.parseCompilationUnit();
    unit.sourcefile = source;
    Iterable<Diagnostic<? extends JavaFileObject>> errorDiagnostics =
        Iterables.filter(diagnostics.getDiagnostics(), Formatter::errorDiagnostic);
    if (!Iterables.isEmpty(errorDiagnostics)) {
      // error handling is done during formatting
      throw FormatterException.fromJavacDiagnostics(errorDiagnostics);
    }
    return unit;
  }

  /** Construct replacements to fix unused imports. */
  private static RangeMap<Integer, String> buildReplacements(
      String contents,
      JCCompilationUnit unit,
      Set<String> usedNames,
      Multimap<String, Range<Integer>> usedInJavadoc) {
    RangeMap<Integer, String> replacements = TreeRangeMap.create();
    for (JCImport importTree : unit.getImports()) {
      String simpleName = getSimpleName(importTree);
      if (!isUnused(unit, usedNames, usedInJavadoc, importTree, simpleName)) {
        continue;
      }
      // delete the import
      int endPosition = importTree.getEndPosition(unit.endPositions);
      endPosition = Math.max(CharMatcher.isNot(' ').indexIn(contents, endPosition), endPosition);
      String sep = Newlines.guessLineSeparator(contents);
      if (endPosition + sep.length() < contents.length()
          && contents.subSequence(endPosition, endPosition + sep.length()).toString().equals(sep)) {
        endPosition += sep.length();
      }
      replacements.put(Range.closedOpen(importTree.getStartPosition(), endPosition), "");
    }
    return replacements;
  }

  private static String getSimpleName(JCImport importTree) {
    return importTree.getQualifiedIdentifier() instanceof JCIdent
        ? ((JCIdent) importTree.getQualifiedIdentifier()).getName().toString()
        : ((JCFieldAccess) importTree.getQualifiedIdentifier()).getIdentifier().toString();
  }

  private static boolean isUnused(
      JCCompilationUnit unit,
      Set<String> usedNames,
      Multimap<String, Range<Integer>> usedInJavadoc,
      JCImport importTree,
      String simpleName) {
    String qualifier =
        ((JCFieldAccess) importTree.getQualifiedIdentifier()).getExpression().toString();
    if (qualifier.equals("java.lang")) {
      return true;
    }
    if (unit.getPackageName() != null && unit.getPackageName().toString().equals(qualifier)) {
      return true;
    }
    if (importTree.getQualifiedIdentifier() instanceof JCFieldAccess
        && ((JCFieldAccess) importTree.getQualifiedIdentifier())
            .getIdentifier()
            .contentEquals("*")) {
      return false;
    }

    if (usedNames.contains(simpleName)) {
      return false;
    }
    if (usedInJavadoc.containsKey(simpleName)) {
      return false;
    }
    return true;
  }

  /** Applies the replacements to the given source, and re-format any edited javadoc. */
  private static String applyReplacements(String source, RangeMap<Integer, String> replacements) {
    // save non-empty fixed ranges for reformatting after fixes are applied
    RangeSet<Integer> fixedRanges = TreeRangeSet.create();

    // Apply the fixes in increasing order, adjusting ranges to account for
    // earlier fixes that change the length of the source. The output ranges are
    // needed so we can reformat fixed regions, otherwise the fixes could just
    // be applied in descending order without adjusting offsets.
    StringBuilder sb = new StringBuilder(source);
    int offset = 0;
    for (Map.Entry<Range<Integer>, String> replacement : replacements.asMapOfRanges().entrySet()) {
      Range<Integer> range = replacement.getKey();
      String replaceWith = replacement.getValue();
      int start = offset + range.lowerEndpoint();
      int end = offset + range.upperEndpoint();
      sb.replace(start, end, replaceWith);
      if (!replaceWith.isEmpty()) {
        fixedRanges.add(Range.closedOpen(start, end));
      }
      offset += replaceWith.length() - (range.upperEndpoint() - range.lowerEndpoint());
    }
    return sb.toString();
  }
}
