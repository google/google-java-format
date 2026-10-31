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

package com.google.googlejavaformat.layout;

import com.google.common.collect.ImmutableList;
import com.google.googlejavaformat.Indent;
import com.google.googlejavaformat.Output.BreakTag;
import java.util.Optional;

/// Factory methods for constructing [Doc]s.
///
/// The names follow Wadler's [A prettier
/// printer](https://homepages.inf.ed.ac.uk/wadler/papers/prettier/prettier.pdf) (`text`, `line`,
/// `group`, `fill`) and Leijen's wl-pprint library (`softline`, `hardline`).
public final class Docs {

  private Docs() {}

  public static Doc text(String text) {
    return new Doc.Text(text);
  }

  public static Doc space() {
    return Doc.Space.INSTANCE;
  }

  /// A group with no extra indent.
  public static Doc group(Doc... docs) {
    return new Doc.Group(Indent.Const.ZERO, ImmutableList.copyOf(docs));
  }

  /// A group whose breaks are indented by `plusIndent` relative to the enclosing indent, i.e.
  /// Wadler's `group (nest plusIndent ...)`.
  public static Doc group(int plusIndent, Doc... docs) {
    return new Doc.Group(Indent.Const.make(plusIndent, 1), ImmutableList.copyOf(docs));
  }

  /// A unified break that is written as `flat` when not taken.
  public static Doc line(String flat) {
    return new Doc.Break(Doc.BreakMode.UNIFIED, flat, Indent.Const.ZERO, Optional.empty());
  }

  /// A unified break that is written as a space when not taken, i.e. Wadler's `line`.
  public static Doc line() {
    return line(" ");
  }

  /// A unified break that is written as nothing when not taken.
  public static Doc softline() {
    return line("");
  }

  /// A fill break that is written as `flat` when not taken. A group of fill breaks behaves like
  /// Wadler's `fill` (§4).
  public static Doc fill(String flat) {
    return new Doc.Break(Doc.BreakMode.FILL, flat, Indent.Const.ZERO, Optional.empty());
  }

  /// A break that is always taken.
  public static Doc hardline() {
    return new Doc.Break(Doc.BreakMode.FORCED, "", Indent.Const.ZERO, Optional.empty());
  }

  /// A unified break that records whether it was taken in `tag`.
  public static Doc taggedLine(String flat, BreakTag tag) {
    return new Doc.Break(Doc.BreakMode.UNIFIED, flat, Indent.Const.ZERO, Optional.of(tag));
  }
}
