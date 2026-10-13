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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link JavaFormatterOptions}. */
@RunWith(JUnit4.class)
public final class JavaFormatterOptionsTest {

  @Test
  public void styleValues() {
    assertThat(Style.values()).asList().containsExactly(Style.GOOGLE, Style.AOSP).inOrder();
  }

  @Test
  public void styleValueOf() {
    assertThat(Style.valueOf("GOOGLE")).isSameInstanceAs(Style.GOOGLE);
    assertThat(Style.valueOf("AOSP")).isSameInstanceAs(Style.AOSP);
  }

  @Test
  public void styleValueOf_invalid() {
    assertThrows(IllegalArgumentException.class, () -> Style.valueOf("google"));
    assertThrows(IllegalArgumentException.class, () -> Style.valueOf("PALANTIR"));
    assertThrows(NullPointerException.class, () -> Style.valueOf(null));
  }

  @Test
  public void styleName() {
    for (Style style : Style.values()) {
      assertThat(Style.valueOf(style.name())).isSameInstanceAs(style);
    }
    assertThat(Style.AOSP.toBuilder().maxLineLength(120).useTabs(true).build().name())
        .isEqualTo("AOSP");
  }
}
