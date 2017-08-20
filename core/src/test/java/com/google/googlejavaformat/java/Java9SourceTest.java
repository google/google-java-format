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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for Java 9 syntax issues. */
@RunWith(JUnit4.class)
public class Java9SourceTest {

  @Test
  @Ignore
  public void issue75() throws FormatterException {
    String sourceString = "module empty {}";
    new Formatter().formatSource(sourceString);
  }

  @Test
  @Ignore
  public void issue155() throws FormatterException {
    String sourceString =
        "class EffectivelyFinalVariableUsedInTryWithResourcesStatement {\n"
            + "  void useAndClose(AutoCloseable resource) {\n"
            + "    try (resource) {}\n"
            + "  }\n"
            + "}";
    new Formatter().formatSource(sourceString);
  }

  @Test
  @Ignore
  public void issue176() throws FormatterException {
    String sourceString =
        "interface InterfaceWithPrivateStaticMethod {\n"
                + "  private static void bar() {}\n"
                + "  default void foo() { bar(); }\n"
                + "}";
    new Formatter().formatSource(sourceString);
  }
}
