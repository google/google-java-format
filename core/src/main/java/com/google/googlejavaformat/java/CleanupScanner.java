/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.googlejavaformat.java;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.googlejavaformat.java.Trees.getEndPosition;
import static com.google.googlejavaformat.java.Trees.getStartPosition;
import static com.sun.tools.javac.code.Flags.ENUM;
import static com.sun.tools.javac.code.Flags.GENERATED_MEMBER;
import static java.lang.Math.max;
import static java.lang.Math.min;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.googlejavaformat.Newlines;
import com.google.googlejavaformat.java.JavaInput.Tok;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Scans the compilation unit for non-whitespace cleanup opportunities, such as removing redundant
/// semicolons.
final class CleanupScanner extends TreePathScanner<@Nullable Void, @Nullable Void> {

  // Replace with Flags.IMPLICIT_CLASS once JDK 25 is the minimum supported version
  private static final int IMPLICIT_CLASS = 1 << 19;

  static ImmutableList<Replacement> scan(
      JCCompilationUnit unit, String contents, Collection<Range<Integer>> ranges)
      throws FormatterException {
    RangeSet<Integer> targetRanges = TreeRangeSet.create(ranges);
    ImmutableList<Tok> toks = JavaInput.buildToks(contents, ImmutableSet.of());
    RangeSet<Integer> activeDeletions = TreeRangeSet.create();
    new CleanupScanner(contents, unit, toks, targetRanges, activeDeletions).scan(unit, null);
    if (activeDeletions.isEmpty()) {
      return ImmutableList.of();
    }
    List<Replacement> replacements = new ArrayList<>();
    for (Range<Integer> del : activeDeletions.asRanges()) {
      replacements.add(Replacement.create(del.lowerEndpoint(), del.upperEndpoint(), ""));
    }
    verifyCommentsPreserved(toks, contents, replacements);
    return ImmutableList.copyOf(replacements);
  }

  private static void verifyCommentsPreserved(
      ImmutableList<Tok> oldToks, String contents, List<Replacement> replacements)
      throws FormatterException {
    StringBuilder sb = new StringBuilder(contents.length());
    int cursor = 0;
    for (Replacement replacement : replacements) {
      Range<Integer> range = replacement.replaceRange();
      sb.append(contents, cursor, range.lowerEndpoint());
      sb.append(replacement.replacementString());
      cursor = range.upperEndpoint();
    }
    sb.append(contents, cursor, contents.length());
    String newContents = sb.toString();

    ImmutableList<String> expectedComments =
        oldToks.stream()
            .filter(Tok::isComment)
            .map(Tok::getOriginalText)
            .collect(toImmutableList());
    ImmutableList<String> actualComments =
        JavaInput.buildToks(newContents, ImmutableSet.of()).stream()
            .filter(Tok::isComment)
            .map(Tok::getOriginalText)
            .collect(toImmutableList());
    if (!expectedComments.equals(actualComments)) {
      throw new FormatterException("error: cleanups modified comments");
    }
  }

  private final String contents;
  private final JCCompilationUnit unit;
  private final ImmutableList<Tok> toks;
  private final RangeSet<Integer> targetRanges;
  private final RangeSet<Integer> activeDeletions;

  CleanupScanner(
      String contents,
      JCCompilationUnit unit,
      ImmutableList<Tok> toks,
      RangeSet<Integer> targetRanges,
      RangeSet<Integer> activeDeletions) {
    this.contents = contents;
    this.unit = unit;
    this.toks = toks;
    this.targetRanges = targetRanges;
    this.activeDeletions = activeDeletions;
  }

  private boolean inRange(int startPos, int endPos) {
    return targetRanges.intersects(Range.closedOpen(startPos, endPos));
  }

  private void deleteRedundantSemicolon(int semiPos) {
    int start = semiPos;
    while (start > 0 && isHorizontalWhitespace(contents.charAt(start - 1))) {
      start--;
    }
    int lineBegin = start;
    while (!Newlines.isLineStart(contents, lineBegin)) {
      lineBegin--;
    }
    int end = semiPos + 1;
    while (end < contents.length() && isHorizontalWhitespace(contents.charAt(end))) {
      end++;
    }
    boolean nothingBeforeOnLine =
        start == lineBegin || activeDeletions.encloses(Range.closedOpen(lineBegin, start));
    if (nothingBeforeOnLine && end < contents.length()) {
      if (contents.charAt(end) == '\r') {
        end++;
        if (end < contents.length() && contents.charAt(end) == '\n') {
          end++;
        }
      } else if (contents.charAt(end) == '\n') {
        end++;
      }
    }
    activeDeletions.add(Range.closedOpen(start, end));
  }

  private static boolean isHorizontalWhitespace(char c) {
    return c == ' ' || c == '\t';
  }

  @Override
  public @Nullable Void visitCompilationUnit(CompilationUnitTree node, @Nullable Void unused) {
    checkTopLevelSemicolons(node);
    return super.visitCompilationUnit(node, null);
  }

  // Top-level semicolons between or after type declarations are represented as EmptyStatementTree
  // nodes in CompilationUnitTree.getTypeDecls(), but javac omits top-level semicolons from the AST
  // in some cases:
  // - In compilation units with no type declarations (e.g. only package and/or import declarations
  //   followed by extra semicolons), JavacParser drops trailing semicolons at EOF without adding
  //   them to getTypeDecls().
  // - In compact source files (implicitly declared classes), JavacParser.constructImplicitClass
  //   strips all JCSkip (EmptyStatementTree) nodes when constructing the synthetic ClassDef.
  // Scan token gaps between top-level declarations so all redundant top-level semicolons are
  // handled uniformly.
  private void checkTopLevelSemicolons(CompilationUnitTree node) {
    RangeSet<Integer> validSpans = TreeRangeSet.create();
    if (node.getPackage() != null) {
      addNodeSpan(validSpans, node.getPackage());
    }
    if (node.getModule() != null) {
      addNodeSpan(validSpans, node.getModule());
    }
    for (ImportTree imp : node.getImports()) {
      addNodeSpan(validSpans, imp);
    }
    for (Tree decl : node.getTypeDecls()) {
      switch (decl) {
        case EmptyStatementTree unused -> {}
        case ClassTree classTree
            when (TreeInfo.flags((JCTree) classTree) & IMPLICIT_CLASS) == IMPLICIT_CLASS -> {
          for (Tree member : classTree.getMembers()) {
            if ((TreeInfo.flags((JCTree) member) & GENERATED_MEMBER) == 0) {
              addNodeSpan(validSpans, member);
            }
          }
        }
        default -> addNodeSpan(validSpans, decl);
      }
    }
    deleteSemicolonsInGaps(0, contents.length(), validSpans);
  }

  private void addNodeSpan(RangeSet<Integer> spans, Tree tree) {
    spans.add(Range.closedOpen(getStartPosition(tree), getEndPosition(tree, unit)));
  }

  @Override
  public @Nullable Void visitClass(ClassTree tree, @Nullable Void unused) {
    checkClassSemicolons(tree);
    return super.visitClass(tree, null);
  }

  private void checkClassSemicolons(ClassTree tree) {
    if ((TreeInfo.flags((JCTree) tree) & IMPLICIT_CLASS) == IMPLICIT_CLASS
        || tree.getMembers() == null) {
      return;
    }
    int classStart = getStartPosition(tree);
    int classEnd = getEndPosition(tree, unit);
    RangeSet<Integer> memberSpans = TreeRangeSet.create();
    // Record the spans of constructor arguments in `new MyClass(arg1, arg2) {...}` and enum
    // constants with class bodies `FOO(arg1, arg2) {...}` (where javac sets the anonymous
    // ClassTree's start position to the enum constant identifier before the argument list), so
    // semicolons inside argument expressions (such as lambdas) do not show up as gaps in the
    // class body.
    if (getCurrentPath().getParentPath().getLeaf() instanceof NewClassTree newClass) {
      for (Tree arg : newClass.getArguments()) {
        addNodeSpan(memberSpans, arg);
      }
    }
    int lastEnumConstantEnd = classStart;
    int firstNonEnumMemberStart = classEnd;
    for (Tree member : tree.getMembers()) {
      long flags = TreeInfo.flags((JCTree) member);
      if ((flags & GENERATED_MEMBER) != 0) {
        continue;
      }
      int mStart = getStartPosition(member);
      int mEnd = getEndPosition(member, unit);
      memberSpans.add(Range.closedOpen(mStart, mEnd));
      if (tree.getKind() == Tree.Kind.ENUM) {
        if (member instanceof VariableTree && (flags & ENUM) != 0) {
          lastEnumConstantEnd = max(lastEnumConstantEnd, mEnd);
        } else {
          firstNonEnumMemberStart = min(firstNonEnumMemberStart, mStart);
        }
      }
    }
    if (tree.getKind() == Tree.Kind.ENUM && !memberSpans.isEmpty()) {
      Tok enumSemi = findTokenAtOrAfter(lastEnumConstantEnd, firstNonEnumMemberStart, ";");
      if (enumSemi != null) {
        memberSpans.add(
            Range.closedOpen(enumSemi.getPosition(), enumSemi.getPosition() + enumSemi.length()));
      }
    }
    deleteSemicolonsInGaps(classStart, classEnd, memberSpans);
  }

  private void deleteSemicolonsInGaps(int start, int end, RangeSet<Integer> validSpans) {
    for (Range<Integer> gap :
        validSpans.complement().subRangeSet(Range.closedOpen(start, end)).asRanges()) {
      for (Tok semi : semicolonTokensInRange(gap.lowerEndpoint(), gap.upperEndpoint())) {
        int pos = semi.getPosition();
        if (inRange(pos, pos + 1)) {
          deleteRedundantSemicolon(pos);
        }
      }
    }
  }

  @Override
  public @Nullable Void visitEmptyStatement(EmptyStatementTree tree, @Nullable Void unused) {
    // Include CaseTree for empty statements in colon-style switch statement groups
    // (e.g. `switch (x) { case FOO: ; case BAR: }`).
    Tree parent = getCurrentPath().getParentPath().getLeaf();
    if (parent instanceof BlockTree || parent instanceof CaseTree) {
      int pos = getStartPosition(tree);
      if (inRange(pos, pos + 1)) {
        deleteRedundantSemicolon(pos);
      }
    }
    return null;
  }

  // Hand-rolled lower-bound binary search to avoid allocating a dummy Tok or wrapper list for
  // Collections.binarySearch.
  private int tokIndexAtOrAfter(int pos) {
    int lo = 0;
    int hi = toks.size();
    while (lo < hi) {
      int mid = (lo + hi) >>> 1;
      if (toks.get(mid).getPosition() < pos) {
        lo = mid + 1;
      } else {
        hi = mid;
      }
    }
    return lo;
  }

  private @Nullable Tok findTokenAtOrAfter(int startPos, int endPos, String text) {
    for (int i = tokIndexAtOrAfter(startPos); i < toks.size(); i++) {
      Tok tok = toks.get(i);
      if (tok.getPosition() >= endPos) {
        break;
      }
      if (tok.isToken() && tok.getText().equals(text)) {
        return tok;
      }
    }
    return null;
  }

  private List<Tok> semicolonTokensInRange(int startPos, int endPos) {
    List<Tok> result = new ArrayList<>();
    for (int i = tokIndexAtOrAfter(startPos); i < toks.size(); i++) {
      Tok tok = toks.get(i);
      if (tok.getPosition() >= endPos) {
        break;
      }
      if (tok.isToken() && tok.getText().equals(";")) {
        result.add(tok);
      }
    }
    return result;
  }
}
