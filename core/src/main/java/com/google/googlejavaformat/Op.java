/*
 * Copyright 2015 Google Inc.
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

package com.google.googlejavaformat;

/// An `Op` is a member of the sequence of formatting operations emitted by [OpsBuilder] and
/// transformed by [DocBuilder] into a [Doc]. Leaf subclasses of [Doc] implement `Op`; [Doc.Level]
/// is the only non-leaf, and is represented by paired [OpenOp]-[CloseOp] `Op`s.
public interface Op {
  /// Add an `Op` to a [DocBuilder].
  ///
  /// @param builder the [DocBuilder]
  void add(DocBuilder builder);
}
