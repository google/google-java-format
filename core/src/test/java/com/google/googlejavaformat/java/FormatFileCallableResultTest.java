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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Path;
import java.nio.file.Paths;

/** {@link FormatFileCallable}Test */
@RunWith(JUnit4.class)
public class FormatFileCallableResultTest {

    @Test
    public void testCreateWithNoChange() {
        Path path = Paths.get("TestFile.java");
        String input = "class T {}";
        String output = "class T {}";

        FormatFileCallable.Result result = FormatFileCallable.Result.create(path, input, output, null);

        assertThat(result.path()).isEqualTo(path);
        assertThat(result.input()).isEqualTo(input);
        assertThat(result.output()).isEqualTo(output);
        assertThat(result.exception()).isNull();
        assertThat(result.changed()).isFalse();
    }

    @Test
    public void testCreateWithChange() {
        Path path = Paths.get("TestFile.java");
        String input = "class  T {}";
        String output = "class T {}";

        FormatFileCallable.Result result = FormatFileCallable.Result.create(path, input, output, null);

        assertThat(result.changed()).isTrue();
    }

    @Test
    public void testCreateWithException() {
        Path path = Paths.get("TestFile.java");
        String input = "bad code";
        FormatterException exception = new FormatterException("Parse error");

        FormatFileCallable.Result result = FormatFileCallable.Result.create(path, input, null, exception);

        assertThat(result.path()).isEqualTo(path);
        assertThat(result.input()).isEqualTo(input);
        assertThat(result.output()).isNull();
        assertThat(result.exception()).isEqualTo(exception);
        assertThat(result.changed()).isTrue(); // input != null output ⇒ changed is true
    }

    @Test
    public void testCreateWithNullPath() {
        String input = "x = 1;";
        String output = "x=1;";

        FormatFileCallable.Result result = FormatFileCallable.Result.create(null, input, output, null);

        assertThat(result.path()).isNull();
        assertThat(result.changed()).isTrue();
    }

    @Test
    public void testChangedReturnsFalseWhenInputAndOutputAreSame() {
        String input = "final int x = 42;";
        FormatFileCallable.Result result = FormatFileCallable.Result.create(null, input, input, null);
        assertThat(result.changed()).isFalse();
    }
}