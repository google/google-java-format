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

import com.google.common.base.CharMatcher;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeMap;
import com.google.common.collect.TreeRangeSet;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Removes unused imports from a source file. Imports that are only used in javadoc are also
 * removed, and the references in javadoc are replaced with fully qualified names.
 */
public class RemoveUnusedImports {

  /** Configuration for javadoc-only imports. */
  public enum JavadocOnlyImports {
    /** Remove imports that are only used in javadoc, and fully qualify any {@code @link} tags. */
    REMOVE,
    /** Keep imports that are only used in javadoc. */
    KEEP
  }

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
  private static class UnusedImportScanner extends ASTVisitor {

    private final Set<String> usedNames = new LinkedHashSet<>();
    private final Multimap<String, ASTNode> usedInJavadoc = HashMultimap.create();

    @Override
    public boolean visit(Javadoc node) {
      node.accept(
          new ASTVisitor(/*visitDocTags=*/ true) {
            @Override
            public boolean visit(TagElement node) {
              recordTag(node);
              return super.visit(node);
            }
          });
      return super.visit(node);
    }

    private void recordTag(TagElement node) {
      if (node.getTagName() == null) {
        return;
      }
      switch (node.getTagName()) {
        case "@link":
        case "@see":
        case "@throws":
          recordReference(Iterables.<ASTNode>getFirst(node.fragments(), null));
          break;
        default:
          break;
      }
    }

    private void recordReference(ASTNode reference) {
      if (reference instanceof SimpleName) {
        recordSimpleName(reference);
      } else if (reference instanceof MemberRef) {
        recordSimpleName(((MemberRef) reference).getQualifier());
      } else if (reference instanceof MethodRef) {
        recordSimpleName(((MethodRef) reference).getQualifier());
      }
    }

    private void recordSimpleName(ASTNode typeReference) {
      if (typeReference instanceof SimpleName) {
        usedInJavadoc.put(((SimpleName) typeReference).getIdentifier(), typeReference);
      }
    }

    @Override
    public boolean visit(ImportDeclaration node) {
      // skip imports
      return false;
    }

    @Override
    public boolean visit(SimpleName node) {
      usedNames.add(node.getIdentifier().toString());
      return super.visit(node);
    }
  }

  public static String removeUnusedImports(
      final String contents, JavadocOnlyImports javadocOnlyImports) {
    CompilationUnit unit = parse(contents);
    if (unit == null) {
      // error handling is done during formatting
      return contents;
    }
    UnusedImportScanner scanner = new UnusedImportScanner();
    unit.accept(scanner);
    return applyReplacements(
        contents,
        buildReplacements(
            contents, unit, scanner.usedNames, scanner.usedInJavadoc, javadocOnlyImports));
  }

  private static CompilationUnit parse(String source) {
    ASTParser parser = ASTParser.newParser(AST.JLS8);
    parser.setSource(source.toCharArray());
    Map<String, String> options = JavaCore.getOptions();
    JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
    parser.setCompilerOptions(options);
    CompilationUnit unit = (CompilationUnit) parser.createAST(null);
    if (unit.getMessages().length > 0) {
      // error handling is done during formatting
      return null;
    }
    return unit;
  }

  /** Construct replacements to fix unused imports. */
  private static RangeMap<Integer, String> buildReplacements(
      String contents,
      CompilationUnit unit,
      Set<String> usedNames,
      Multimap<String, ASTNode> usedInJavadoc,
      JavadocOnlyImports javadocOnlyImports) {
    RangeMap<Integer, String> replacements = TreeRangeMap.create();
    for (ImportDeclaration importTree : (List<ImportDeclaration>) unit.imports()) {
      String simpleName =
          importTree.getName() instanceof QualifiedName
              ? ((QualifiedName) importTree.getName()).getName().toString()
              : importTree.getName().toString();
      if (usedNames.contains(simpleName)) {
        continue;
      }
      if (usedInJavadoc.containsKey(simpleName) && javadocOnlyImports == JavadocOnlyImports.KEEP) {
        continue;
      }
      // delete the import
      int endPosition = importTree.getStartPosition() + importTree.getLength();
      endPosition = Math.max(CharMatcher.isNot(' ').indexIn(contents, endPosition), endPosition);
      String sep = System.lineSeparator();
      if (endPosition + sep.length() < contents.length()
          && contents.subSequence(endPosition, endPosition + sep.length()).equals(sep)) {
        endPosition += sep.length();
      }
      replacements.put(Range.closedOpen(importTree.getStartPosition(), endPosition), "");
      // fully qualify any javadoc references with the same simple name as a deleted
      // non-static import
      if (!importTree.isStatic()) {
        for (ASTNode doc : usedInJavadoc.get(simpleName)) {
          String replaceWith = importTree.getName().toString();
          Range<Integer> range =
              Range.closedOpen(doc.getStartPosition(), doc.getStartPosition() + doc.getLength());
          replacements.put(range, replaceWith);
        }
      }
    }
    return replacements;
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
    String result = sb.toString();

    // If there were any non-empty replaced ranges (e.g. javadoc), reformat the fixed regions.
    // We could avoid formatting twice in --fix-imports=also mode, but that is not the default
    // and removing imports won't usually affect javadoc.
    if (!fixedRanges.isEmpty()) {
      try {
        result = new Formatter().formatSource(result, fixedRanges.asRanges());
      } catch (FormatterException e) {
        // javadoc reformatting is best-effort
      }
    }
    return result;
  }
}
