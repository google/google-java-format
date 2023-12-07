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
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ServiceLoader;
import javax.tools.Tool;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link GoogleJavaFormatToolProvider}. */
@RunWith(JUnit4.class)
public class GoogleJavaFormatToolTest {

  @Test
  public void testUsageOutputAfterLoadingViaToolName() {
    String name = "google-java-format";

    assertThat(
            ServiceLoader.load(Tool.class).stream()
                .map(ServiceLoader.Provider::get)
                .map(Tool::name))
        .contains(name);

    Tool format =
        ServiceLoader.load(Tool.class).stream()
            .filter(provider -> name.equals(provider.get().name()))
            .findFirst()
            .get()
            .get();

    InputStream in = new ByteArrayInputStream(new byte[0]);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();

    int result = format.run(in, out, err, "--help");

    assertThat(result).isNotEqualTo(0);

    String usage = new String(err.toByteArray(), UTF_8);

    // Check that doc links are included.
    assertThat(usage).containsMatch("http.*/google-java-format");
    assertThat(usage).contains("Usage: google-java-format");
  }
}
