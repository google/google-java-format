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

package com.google.googlejavaformat.java;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration test for GoogleJavaFormatVersion. */
@RunWith(JUnit4.class)
public class GoogleJavaFormatVersionTest {

  @Test
  public void testVersionMatch() throws Exception {
    String pom = new String(Files.readAllBytes(Paths.get("pom.xml")));
    int beginIndex = pom.indexOf("<version>") + 9; // 9 = "<version>".length()
    int endIndex = pom.indexOf("</version>", beginIndex);
    String pomVersion = pom.substring(beginIndex, endIndex).trim();

    assertThat(GoogleJavaFormatVersion.VERSION).isEqualTo(pomVersion);
  }
}
