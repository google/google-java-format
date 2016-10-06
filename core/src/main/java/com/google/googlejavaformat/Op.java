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

/**
 * An {@code Op} is a member of the sequence of formatting operations emitted by {@link OpsBuilder}
 * and transformed by {@link DocBuilder} into a {@link Doc}. Leaf subclasses of {@link Doc}
 * implement {@code Op}; {@link Doc.Level} is the only non-leaf, and is represented by paired {@link
 * OpenOp}-{@link CloseOp} {@code Op}s.
 */
public interface Op {
  /**
   * Add an {@code Op} to a {@link DocBuilder}.
   *
   * @param builder the {@link DocBuilder}
   */
  void add(DocBuilder builder);
}
