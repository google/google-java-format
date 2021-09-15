/*
 * Copyright 2021 Google Inc.
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
import static com.google.common.truth.Truth8.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link GoogleJavaFormatToolProvider}. */
@RunWith(JUnit4.class)
public class GoogleJavaFormatToolProviderTest {

  @Test
  public void testUsageOutputAfterLoadingViaToolName() {
    String name = "google-java-format";

    assertThat(
            ServiceLoader.load(ToolProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .map(ToolProvider::name))
        .contains(name);

    ToolProvider format = ToolProvider.findFirst(name).get();

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    int result = format.run(new PrintWriter(out, true), new PrintWriter(err, true), "--help");

    assertThat(result).isEqualTo(0);

    String usage = err.toString();

    // Check that doc links are included.
    assertThat(usage).containsMatch("http.*/google-java-format");
    assertThat(usage).contains("Usage: google-java-format");
  }
}
