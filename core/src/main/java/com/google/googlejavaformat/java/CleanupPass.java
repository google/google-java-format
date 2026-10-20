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

import com.google.common.collect.ImmutableList;

/// Standard cleanup passes applied before formatting.
enum CleanupPass {
  /// Removes unused imports.
  REMOVE_UNUSED_IMPORTS {
    @Override
    ImmutableList<Replacement> findReplacements(Cleanups cleanups) throws FormatterException {
      return RemoveUnusedImports.findUnusedImports(
          cleanups.context(), cleanups.unit(), cleanups.contents());
    }
  },
  /// Reorders modifiers into JLS order.
  REORDER_MODIFIERS {
    @Override
    ImmutableList<Replacement> findReplacements(Cleanups cleanups) throws FormatterException {
      JavaInput javaInput = new JavaInput(cleanups.contents());
      return ModifierOrderer.findModifierReplacements(javaInput, cleanups.ranges());
    }
  },
  /// Reorders imports into canonical style order.
  SORT_IMPORTS {
    @Override
    ImmutableList<Replacement> findReplacements(Cleanups cleanups) throws FormatterException {
      return ImportOrderer.findReplacements(cleanups.contents(), cleanups.style());
    }
  };

  /// Finds replacements to apply to the current pipeline state. The returned replacements must be
  /// relative to `cleanups.contents()`, sorted by start position, and non-overlapping.
  abstract ImmutableList<Replacement> findReplacements(Cleanups cleanups) throws FormatterException;
}
