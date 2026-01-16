/*
 * Copyright 2025 Google Inc.
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.Range;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link Replacement}Test */
@RunWith(JUnit4.class)
public class ReplacementTest {

  @Test
  public void testCreateWithValidInput() {
    Replacement replacement = Replacement.create(3, 7, "replacementText");
    assertThat(replacement.getReplaceRange()).isEqualTo(Range.closedOpen(3, 7));
    assertThat(replacement.getReplacementString()).isEqualTo("replacementText");
  }

  @Test
  public void invalidReplacementRange() {
    assertThrows(IllegalArgumentException.class, () -> Replacement.create(-1, 5, "text"));
    assertThrows(IllegalArgumentException.class, () -> Replacement.create(10, 5, "text"));
  }

  @Test
  public void testEqualsAndHashCode() {
    new EqualsTester()
        .addEqualityGroup(Replacement.create(0, 4, "abc"), Replacement.create(0, 4, "abc"))
        .addEqualityGroup(Replacement.create(1, 4, "abc"))
        .addEqualityGroup(Replacement.create(0, 4, "def"))
        .testEquals();
  }
}
