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
import static java.util.Comparator.comparingInt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.googlejavaformat.java.JavaInput.Tok;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/// Scans the compilation unit for non-whitespace cleanup opportunities, inserting missing
/// control-flow braces.
final class CleanupScanner extends TreePathScanner<@Nullable Void, @Nullable Void> {

  static ImmutableList<Replacement> scan(
      JCCompilationUnit unit, String contents, Collection<Range<Integer>> ranges)
      throws FormatterException {
    RangeSet<Integer> targetRanges = TreeRangeSet.create(ranges);
    ImmutableList<Tok> toks = JavaInput.buildToks(contents, ImmutableSet.of());
    RangeSet<Integer> activeDeletions = TreeRangeSet.create();
    NavigableMap<Integer, StringBuilder> activeInsertions = new TreeMap<>();
    new CleanupScanner(unit, toks, targetRanges, activeDeletions, activeInsertions)
        .scan(unit, null);
    if (activeDeletions.isEmpty() && activeInsertions.isEmpty()) {
      return ImmutableList.of();
    }
    List<Replacement> replacements = new ArrayList<>();
    for (Range<Integer> del : activeDeletions.asRanges()) {
      int dStart = del.lowerEndpoint();
      int dEnd = del.upperEndpoint();
      var subMap =
          activeInsertions.subMap(dStart, /* fromInclusive= */ true, dEnd, /* toInclusive= */ true);
      String rep = String.join("", subMap.values());
      subMap.clear();
      replacements.add(Replacement.create(dStart, dEnd, rep));
    }
    for (Map.Entry<Integer, StringBuilder> entry : activeInsertions.entrySet()) {
      int pos = entry.getKey();
      replacements.add(Replacement.create(pos, pos, entry.getValue().toString()));
    }
    replacements.sort(comparingInt(r -> r.replaceRange().lowerEndpoint()));
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

  private final JCCompilationUnit unit;
  private final ImmutableList<Tok> toks;
  private final RangeSet<Integer> targetRanges;
  private final RangeSet<Integer> activeDeletions;
  private final NavigableMap<Integer, StringBuilder> activeInsertions;

  CleanupScanner(
      JCCompilationUnit unit,
      ImmutableList<Tok> toks,
      RangeSet<Integer> targetRanges,
      RangeSet<Integer> activeDeletions,
      NavigableMap<Integer, StringBuilder> activeInsertions) {
    this.unit = unit;
    this.toks = toks;
    this.targetRanges = targetRanges;
    this.activeDeletions = activeDeletions;
    this.activeInsertions = activeInsertions;
  }

  private boolean inRange(int startPos, int endPos) {
    return targetRanges.intersects(Range.closedOpen(startPos, endPos));
  }

  private void insert(int pos, String text) {
    activeInsertions.computeIfAbsent(pos, k -> new StringBuilder()).append(text);
  }

  @Override
  public @Nullable Void visitIf(IfTree tree, @Nullable Void unused) {
    super.visitIf(tree, null);
    checkBraces(tree, tree.getThenStatement());
    if (!(tree.getElseStatement() instanceof IfTree)) {
      checkBraces(tree, tree.getElseStatement());
    }
    return null;
  }

  @Override
  public @Nullable Void visitForLoop(ForLoopTree tree, @Nullable Void unused) {
    super.visitForLoop(tree, null);
    checkBraces(tree, tree.getStatement());
    return null;
  }

  @Override
  public @Nullable Void visitEnhancedForLoop(EnhancedForLoopTree tree, @Nullable Void unused) {
    super.visitEnhancedForLoop(tree, null);
    checkBraces(tree, tree.getStatement());
    return null;
  }

  @Override
  public @Nullable Void visitWhileLoop(WhileLoopTree tree, @Nullable Void unused) {
    super.visitWhileLoop(tree, null);
    checkBraces(tree, tree.getStatement());
    return null;
  }

  @Override
  public @Nullable Void visitDoWhileLoop(DoWhileLoopTree tree, @Nullable Void unused) {
    super.visitDoWhileLoop(tree, null);
    checkBraces(tree, tree.getStatement());
    return null;
  }

  private void checkBraces(Tree owner, @Nullable StatementTree body) {
    if (body == null || body instanceof BlockTree) {
      return;
    }
    int rangeEnd =
        owner instanceof DoWhileLoopTree ? getEndPosition(owner, unit) : getEndPosition(body, unit);
    if (!inRange(getStartPosition(owner), rangeEnd)) {
      return;
    }
    Tok prev = prevToken(getStartPosition(body));
    if (prev == null) {
      return;
    }
    insert(prev.getPosition() + prev.length(), "{");
    insert(closingBraceInsertionPos(body), "}");
    if (body instanceof EmptyStatementTree) {
      activeDeletions.add(Range.closedOpen(getStartPosition(body), getEndPosition(body, unit)));
    }
  }

  private int closingBraceInsertionPos(StatementTree stmt) {
    int stmtEnd = getEndPosition(stmt, unit);
    if (stmt instanceof EmptyStatementTree) {
      return stmtEnd;
    }
    boolean sawComment = false;
    for (int i = tokIndexAtOrAfter(stmtEnd); i < toks.size(); i++) {
      Tok tok = toks.get(i);
      if (tok.isToken()) {
        return stmtEnd;
      }
      if (tok.isNewline()) {
        return sawComment ? tok.getPosition() + tok.length() : stmtEnd;
      }
      if (tok.isComment()) {
        sawComment = true;
      }
    }
    return stmtEnd;
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

  private @Nullable Tok prevToken(int pos) {
    for (int i = tokIndexAtOrAfter(pos) - 1; i >= 0; i--) {
      Tok tok = toks.get(i);
      if (tok.isToken() && !tok.getText().isEmpty()) {
        return tok;
      }
    }
    return null;
  }
}
