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
import static com.google.googlejavaformat.java.RemoveUnusedImports.removeUnusedImports;
import static org.junit.Assume.assumeTrue;

import com.google.common.base.Joiner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests that unused import removal doesn't remove types used in case labels. */
@RunWith(JUnit4.class)
public class RemoveUnusedImportsCaseLabelsTest {
  @Test
  public void preserveTypesInCaseLabels() throws FormatterException {
    assumeTrue(Runtime.version().feature() >= 17);
    String input =
        Joiner.on('\n')
            .join(
                "package example;",
                "import example.model.SealedInterface;",
                "import example.model.TypeA;",
                "import example.model.TypeB;",
                "public class Main {",
                "  public void apply(SealedInterface sealedInterface) {",
                "    switch(sealedInterface) {",
                "      case TypeA a -> System.out.println(\"A!\");",
                "      case TypeB b -> System.out.println(\"B!\");",
                "    }",
                "  }",
                "}");
    assertThat(removeUnusedImports(input)).isEqualTo(input);
  }
}
