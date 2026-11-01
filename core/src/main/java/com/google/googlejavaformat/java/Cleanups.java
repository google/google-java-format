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

import static java.lang.Math.max;
import static java.lang.Math.min;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeRangeSet;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.jspecify.annotations.Nullable;

/// Applies non-whitespace cleanups before core formatting.
///
/// Each pass produces replacements relative to the current contents; the requested character ranges
/// are remapped through every pass so formatting still targets the code the caller selected.
final class Cleanups {

  /// The cleaned contents, the requested character ranges remapped onto them (plus any modified
  /// regions), and the javac unit for `contents` if it can be reused for formatting.
  record Result(
      String contents,
      Collection<Range<Integer>> characterRanges,
      @Nullable JCCompilationUnit unit) {}

  private record Parsed(Context context, JCCompilationUnit unit) {}

  /// Returns the enabled passes for formatting or fixing imports.
  static ImmutableSet<CleanupPass> enabledPasses(
      boolean cleanup, boolean removeUnusedImports, boolean reorderModifiers, boolean sortImports) {
    EnumSet<CleanupPass> passes = EnumSet.noneOf(CleanupPass.class);
    if (removeUnusedImports) {
      passes.add(CleanupPass.REMOVE_UNUSED_IMPORTS);
    }
    if (cleanup) {
      passes.add(CleanupPass.AST_CLEANUPS);
    }
    if (reorderModifiers) {
      passes.add(CleanupPass.REORDER_MODIFIERS);
    }
    if (sortImports) {
      passes.add(CleanupPass.SORT_IMPORTS);
    }
    return Sets.immutableEnumSet(passes);
  }

  static Result apply(
      String contents,
      Collection<Range<Integer>> characterRanges,
      Style style,
      ImmutableSet<CleanupPass> passes)
      throws FormatterException {
    JavaInput.validateRanges(characterRanges, contents.length());
    if (passes.isEmpty()) {
      return new Result(contents, characterRanges, null);
    }
    RangeSet<Integer> normalized = TreeRangeSet.create();
    for (Range<Integer> range : characterRanges) {
      normalized.add(
          range.isEmpty() && range.lowerEndpoint() < contents.length()
              ? Range.closedOpen(range.lowerEndpoint(), range.lowerEndpoint() + 1)
              : range);
    }
    Cleanups cleanups = new Cleanups(contents, ImmutableRangeSet.copyOf(normalized), style);
    for (CleanupPass pass : passes) {
      cleanups.applyReplacements(pass.findReplacements(cleanups));
    }
    // An empty range can't be represented in a RangeSet, so restore it if the passes removed all
    // contents (or the input was empty).
    Collection<Range<Integer>> resultRanges =
        cleanups.contents.isEmpty() && !characterRanges.isEmpty()
            ? ImmutableList.of(Range.closedOpen(0, 0))
            : cleanups.ranges.asRanges();
    return new Result(
        cleanups.contents, resultRanges, cleanups.parsed == null ? null : cleanups.parsed.unit());
  }

  private String contents;
  private ImmutableRangeSet<Integer> ranges;
  private @Nullable Parsed parsed;
  private final Style style;

  private Cleanups(String contents, ImmutableRangeSet<Integer> ranges, Style style) {
    this.contents = contents;
    this.ranges = ranges;
    this.style = style;
  }

  String contents() {
    return contents;
  }

  ImmutableSet<Range<Integer>> ranges() {
    return ranges.asRanges();
  }

  Style style() {
    return style;
  }

  private Parsed parsed() throws FormatterException {
    if (parsed != null) {
      return parsed;
    }
    Context ctx = new Context();
    List<Diagnostic<? extends JavaFileObject>> errorDiagnostics = new ArrayList<>();
    JCCompilationUnit cu =
        Trees.parse(ctx, errorDiagnostics, /* allowStringFolding= */ false, contents);
    if (!errorDiagnostics.isEmpty()) {
      throw FormatterException.fromJavacDiagnostics(errorDiagnostics);
    }
    Parsed p = new Parsed(ctx, cu);
    this.parsed = p;
    return p;
  }

  JCCompilationUnit unit() throws FormatterException {
    return parsed().unit();
  }

  Context context() throws FormatterException {
    return parsed().context();
  }

  /// Applies a pass's replacements to the current contents, remapping requested ranges and adding
  /// modified regions.
  private void applyReplacements(List<Replacement> replacements) {
    if (replacements.isEmpty()) {
      return;
    }
    Edit edit = Edit.apply(contents, replacements);
    RangeSet<Integer> newRanges = TreeRangeSet.create(edit.mapRanges(ranges.asRanges()));
    for (Edit.Span span : edit.spans()) {
      Range<Integer> r = reformatRange(span, edit.contents());
      if (!r.isEmpty()) {
        newRanges.add(r);
      }
    }

    this.contents = edit.contents();
    this.ranges = ImmutableRangeSet.copyOf(newRanges);
    this.parsed = null;
  }

  /// Returns the range to reformat for the given span.
  private static Range<Integer> reformatRange(Edit.Span span, String contents) {
    if (span.outStart() == span.outEnd()) {
      return deletionReformatRange(contents, span.outStart());
    }
    return Range.closedOpen(span.outStart(), span.outEnd());
  }

  /// Returns the range to reformat around a deletion at `pos`: the surrounding whitespace plus one
  /// character of each adjacent token, or empty if there is no surrounding whitespace.
  private static Range<Integer> deletionReformatRange(String contents, int pos) {
    int start = pos;
    while (start > 0 && CharMatcher.whitespace().matches(contents.charAt(start - 1))) {
      start--;
    }
    int end = pos;
    while (end < contents.length() && CharMatcher.whitespace().matches(contents.charAt(end))) {
      end++;
    }
    if (start == end) {
      return Range.closedOpen(0, 0);
    }
    return Range.closedOpen(max(0, start - 1), min(contents.length(), end + 1));
  }
}
