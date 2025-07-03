/*
 * Copyright 2019 Google Inc.
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
import static org.junit.Assert.*;

import com.google.common.collect.Range;
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
    public void testCreateWithNegativeStartPositionThrows() {
        try {
            Replacement.create(-1, 5, "text");
            fail("Expected IllegalArgumentException for negative startPosition");
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageThat().contains("startPosition must be non-negative");
        }
    }

    @Test
    public void testCreateWithStartPositionAfterEndPositionThrows() {
        try {
            Replacement.create(10, 5, "text");
            fail("Expected IllegalArgumentException for startPosition after endPosition");
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageThat().contains("startPosition cannot be after endPosition");
        }
    }

    @Test
    public void testEqualsAndHashCodeWithEqualReplacements() {
        Replacement a = Replacement.create(0, 4, "abc");
        Replacement b = Replacement.create(0, 4, "abc");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    public void testEqualsWithDifferentReplaceRange() {
        Replacement a = Replacement.create(0, 4, "abc");
        Replacement b = Replacement.create(1, 4, "abc");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    public void testEqualsWithDifferentReplacementString() {
        Replacement a = Replacement.create(0, 4, "abc");
        Replacement b = Replacement.create(0, 4, "def");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    public void testEqualsWithNullAndDifferentType() {
        Replacement a = Replacement.create(0, 4, "abc");
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("NotAReplacement");
    }

    @Test
    public void testGetReplaceRangeReturnsCorrectRange() {
        Replacement replacement = Replacement.create(5, 10, "text");
        assertThat(replacement.getReplaceRange()).isEqualTo(Range.closedOpen(5, 10));
    }

    @Test
    public void testGetReplacementStringReturnsCorrectString() {
        Replacement replacement = Replacement.create(5, 10, "text");
        assertThat(replacement.getReplacementString()).isEqualTo("text");
    }

}