/*
 * Copyright 2016 Google Inc.
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
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for {@link ImportOrderer}. */
@RunWith(Parameterized.class)
public class ImportOrdererTest {

  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> parameters() {
    // A three-dimensional array! Each element of the outer array is a test case. It consists of
    // two arrays of lines. The first array of lines is the test input, and the second one is the
    // expected output. If the second array has a single element starting with !! then it is
    // expected that ImportOrderer will throw a FormatterException with that message.
    //
    // If a line ends with \ then we remove the \ and don't append a \n. That allows us to check
    // some parsing edge cases.
    String[][][] inputsOutputs = {
      {
        // Empty input produces empty output.
        {}, //
        {}
      },
      {
        {
          "foo", "bar",
        },
        {
          "foo", "bar",
        },
      },
      {
        {
          "package foo;", //
          "",
          "import com.google.first.Bar;",
          "",
          "public class Blim {}",
        },
        {
          "package foo;", //
          "",
          "import com.google.first.Bar;",
          "",
          "public class Blim {}",
        },
      },
      {
        {
          "package foo;",
          "",
          "import com.google.first.Bar;",
          "import com.google.second.Foo;",
          "",
          "public class Blim {}",
        },
        {
          "package foo;",
          "",
          "import com.google.first.Bar;",
          "import com.google.second.Foo;",
          "",
          "public class Blim {}",
        },
      },
      {
        {
          "package foo;",
          "",
          "import com.google.second.Foo;",
          "import com.google.first.Bar;",
          "",
          "public class Blim {}",
        },
        {
          "package foo;",
          "",
          "import com.google.first.Bar;",
          "import com.google.second.Foo;",
          "",
          "public class Blim {}",
        },
      },
      {
        {
          // Check double semicolons
          "package foo;",
          "",
          "import com.google.second.Foo;;",
          "import com.google.first.Bar;;",
          "",
          "public class Blim {}",
        },
        {
          "package foo;",
          "",
          "import com.google.first.Bar;",
          "import com.google.second.Foo;",
          "",
          "public class Blim {}",
        },
      },
      {
        {
          "package foo;",
          "",
          "import com.google.second.Foo;",
          "import com.google.first.Bar;",
          "import com.google.second.Foo;",
          "import com.google.first.Bar;",
          "",
          "public class Blim {}",
        },
        {
          "package foo;",
          "",
          "import com.google.first.Bar;",
          "import com.google.second.Foo;",
          "",
          "public class Blim {}",
        },
      },
      {
        // Google style frowns on wildcard imports, but we handle them all the same.
        {
          "package foo;",
          "",
          "import com.google.second.*;",
          "import com.google.first.Bar;",
          "import com.google.first.*;",
          "",
          "public class Blim {}",
        },
        {
          "package foo;",
          "",
          "import com.google.first.*;",
          "import com.google.first.Bar;",
          "import com.google.second.*;",
          "",
          "public class Blim {}",
        },
      },
      {
        {
          "package com.google.example;",
          "",
          "import com.google.common.base.Preconditions;",
          "",
          "import org.junit.runner.RunWith;",
          "import org.junit.runners.JUnit4;",
          "",
          "import java.util.List;",
          "",
          "import javax.annotations.Nullable;",
          "",
          "import static org.junit.Assert.fail;",
          "import static com.google.truth.Truth.assertThat;",
          "",
          "@RunWith(JUnit4.class)",
          "public class SomeTest {}",
        },
        {
          "package com.google.example;",
          "",
          "import static com.google.truth.Truth.assertThat;",
          "import static org.junit.Assert.fail;",
          "",
          "import com.google.common.base.Preconditions;",
          "import java.util.List;",
          "import javax.annotations.Nullable;",
          "import org.junit.runner.RunWith;",
          "import org.junit.runners.JUnit4;",
          "",
          "@RunWith(JUnit4.class)",
          "public class SomeTest {}",
        },
      },

      // we unindent imports, if we reorder them
      {
        {
          "  import  com.foo.Second;", //
          "  import com.foo.First;",
          "  public class Foo {}",
        },
        {
          "import com.foo.First;", //
          "import com.foo.Second;",
          "",
          "public class Foo {}",
        }
      },

      // Error cases
      {
        {
          "package com.google.example;", //
          "",
          "import\\", // \\ means there is no newline here.
        },
        {
          "!!Unexpected token after import: ",
        }
      },
      {
        {
          "package com.google.example;", //
          "",
          "import",
        },
        {
          "!!Unexpected token after import: \n",
        }
      },
      {
        {
          "package com.google.example;", //
          "",
          "import foo\\",
        },
        {
          "!!Expected ; after import",
        }
      },
      {
        {
          "package com.google.example;", //
          "",
          "import foo.\\",
        },
        {
          "!!Could not parse imported name, at: ",
        }
      },
      {
        {
          "package com.google.example;",
          "",
          "import com.foo.Second;",
          "import com.foo.First;",
          "// we don't support comments between imports",
          "import com.foo.Third;",
        },
        {
          "!!Imports not contiguous (perhaps a comment separates them?)",
        }
      },
      {
        {
          "import com.foo.Second;",
          "import com.foo.First;",
          "/* we don't support block comments",
          "   between imports either */",
          "import com.foo.Third;",
        },
        {
          "!!Imports not contiguous (perhaps a comment separates them?)",
        }
      },
      {
        {
          "import com.foo.Second; /* no block comments after imports */", //
          "import com.foo.First;",
        },
        {
          "!!Imports not contiguous (perhaps a comment separates them?)",
        }
      },
      {
        {
          "import com.foo.Second;",
          "import com.foo.First;",
          "/* but we're not fooled by comments that look like imports:",
          "import com.foo.Third;",
          "*/",
        },
        {
          "import com.foo.First;",
          "import com.foo.Second;",
          "",
          "/* but we're not fooled by comments that look like imports:",
          "import com.foo.Third;",
          "*/",
        }
      },
      {
        {
          "import com . foo . Second ;", // syntactically valid, but we don't support it
          "import com.foo.First;",
        },
        {
          "!!Expected ; after import",
        }
      },
      {
        {
          "import com.abc.@;", //
          "import com.abc.@@;",
        },
        {
          "!!Could not parse imported name, at: @",
        }
      },
      {
        {
          "import com.abc.3;", // digits not syntactically valid
          "import com.abc.2;",
        },
        {
          // .3 is a single token (a floating-point constant)
          "!!Expected ; after import",
        }
      },
      {
        {
          "import com.foo.Second", // missing semicolon
          "import com.foo.First;",
        },
        {
          "!!Expected ; after import",
        }
      },
      {
        {
          "import com.foo.Second; import com.foo.First;", "class Test {}",
        },
        {
          "import com.foo.First;", //
          "import com.foo.Second;",
          "",
          "class Test {}",
        }
      },
      {
        {
          "import com.foo.Second; import com.foo.First; class Test {}",
        },
        {
          "import com.foo.First;", //
          "import com.foo.Second;",
          "",
          "class Test {}",
        }
      },
      {
        {
          "package p;", //
          "",
          "/** test */",
          "",
          "import a.A;",
          "",
          "/** test */",
          "",
          "class Test {}",
        },
        {
          "package p;", //
          "",
          "/** test */",
          "",
          "import a.A;",
          "",
          "/** test */",
          "",
          "class Test {}",
        }
      },
      {
        {
          "package p; import a.A; class Test {}",
        },
        {
          "package p;", //
          "",
          "import a.A;",
          "",
          "class Test {}",
        }
      },
    };
    ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
    for (String[][] inputAndOutput : inputsOutputs) {
      assertThat(inputAndOutput.length).isEqualTo(2);
      String[] input = inputAndOutput[0];
      String[] output = inputAndOutput[1];
      if (output.length == 0) {
        output = input;
      }
      String[] parameters = {
        Joiner.on('\n').join(input) + '\n', //
        Joiner.on('\n').join(output) + '\n',
      };
      parameters[0] = parameters[0].replace("\\\n", "");
      builder.add(parameters);
    }
    return builder.build();
  }

  private final String input;
  private final String reordered;

  public ImportOrdererTest(String input, String reordered) {
    this.input = input;
    this.reordered = reordered;
  }

  @Test
  public void reorder() throws FormatterException {
    try {
      String output = ImportOrderer.reorderImports(input);
      assertWithMessage("Expected exception").that(reordered).doesNotMatch("^!!");
      assertWithMessage(input).that(output).isEqualTo(reordered);
    } catch (FormatterException e) {
      if (!reordered.startsWith("!!")) {
        throw e;
      }
      assertThat(reordered).endsWith("\n");
      assertThat(e.getMessage())
          .isEqualTo("error: " + reordered.substring(2, reordered.length() - 1));
    }
  }
}
