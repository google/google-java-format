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
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for {@link ImportOrderer}. */
@RunWith(Enclosed.class)
public class ImportOrdererTest {

  /** Tests for import ordering in Google style. */
  @RunWith(Parameterized.class)
  public static class GoogleStyle {

    private final String input;
    private final String reordered;

    public GoogleStyle(String input, String reordered) {
      this.input = input;
      this.reordered = reordered;
    }

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> parameters() {
      // Inputs are provided as three-dimensional arrays. Each element of the outer array is a test
      // case. It consists of two arrays of lines. The first array of lines is the test input, and
      // the second one is the expected output. If the second array has a single element starting
      // with !! then it is expected that ImportOrderer will throw a FormatterException with that
      // message.
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
            "import java.util.Collection;",
            "// BUG: diagnostic contains",
            "import java.util.List;",
            "",
            "class B74235047 {}"
          },
          {
            "import java.util.Collection;",
            "// BUG: diagnostic contains",
            "import java.util.List;",
            "",
            "class B74235047 {}"
          }
        },
        {
          {
            "import java.util.Set;",
            "import java.util.Collection;",
            "// BUG: diagnostic contains",
            "import java.util.List;",
            "",
            "class B74235047 {}"
          },
          {
            "import java.util.Collection;",
            "// BUG: diagnostic contains",
            "import java.util.List;",
            "import java.util.Set;",
            "",
            "class B74235047 {}"
          }
        },
        {
          {
            "import java.util.List;",
            "// BUG: diagnostic contains",
            "import java.util.Set;",
            "import java.util.Collection;",
            "",
            "class B74235047 {}"
          },
          {
            "import java.util.Collection;",
            "import java.util.List;",
            "// BUG: diagnostic contains",
            "import java.util.Set;",
            "",
            "class B74235047 {}"
          }
        },
        {
          {
            "// BEGIN-STRIP",
            "import com.google.testing.testsize.MediumTest;",
            "import com.google.testing.testsize.MediumTestAttribute;",
            "// END-STRIP",
            "",
            "class B74235047 {}"
          },
          {
            "// BEGIN-STRIP",
            "import com.google.testing.testsize.MediumTest;",
            "import com.google.testing.testsize.MediumTestAttribute;",
            "// END-STRIP",
            "",
            "class B74235047 {}"
          }
        },
        {
          {
            "import com.google.testing.testsize.MediumTest;          // Keep this import",
            "import com.google.testing.testsize.MediumTestAttribute; // Keep this import",
            "",
            "class B74235047 {}"
          },
          {
            "import com.google.testing.testsize.MediumTest;          // Keep this import",
            "import com.google.testing.testsize.MediumTestAttribute; // Keep this import",
            "",
            "class B74235047 {}"
          }
        },
        {
          {
            "import java.util.Set;",
            "import java.util.List;",
            "",
            "// This comment doesn't get moved because of the blank line.",
            "",
            "class B74235047 {}"
          },
          {
            "import java.util.List;",
            "import java.util.Set;",
            "",
            "// This comment doesn't get moved because of the blank line.",
            "",
            "class B74235047 {}"
          }
        },
        {
          {
            "import b.B;",
            "// MOE: end_strip",
            "import c.C;",
            "// MOE: begin_strip",
            "import a.A;",
            "",
            "class B74235047 {}"
          },
          {
            "import a.A;",
            "import b.B;",
            "// MOE: end_strip",
            "import c.C;",
            "// MOE: begin_strip",
            "",
            "class B74235047 {}"
          }
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
        {
          {
            "package p;",
            "",
            "import java.lang.Bar;",
            "import java.lang.Baz;",
            ";",
            "import java.lang.Foo;",
            "",
            "interface Test {}",
          },
          {
            "package p;",
            "",
            "import java.lang.Bar;",
            "import java.lang.Baz;",
            "import java.lang.Foo;",
            "",
            "interface Test {}",
          }
        }
      };

      ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
      Arrays.stream(inputsOutputs).forEach(input -> builder.add(createRow(input)));
      return builder.build();
    }

    @Test
    public void reorder() throws FormatterException {
      try {
        String output = ImportOrderer.reorderImports(input, Style.GOOGLE);
        assertWithMessage("Expected exception").that(reordered).doesNotMatch("^!!");
        assertWithMessage(input).that(output).isEqualTo(reordered);
      } catch (FormatterException e) {
        if (!reordered.startsWith("!!")) {
          throw e;
        }
        assertThat(reordered).endsWith("\n");
        assertThat(e)
            .hasMessageThat()
            .isEqualTo("error: " + reordered.substring(2, reordered.length() - 1));
      }
    }
  }

  /** Tests for import ordering in AOSP style. */
  @RunWith(Parameterized.class)
  public static class AospStyle {

    private final String input;
    private final String reordered;

    public AospStyle(String input, String reordered) {
      this.input = input;
      this.reordered = reordered;
    }

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> parameters() {
      // Inputs are provided as three-dimensional arrays. Each element of the outer array is a test
      // case. It consists of two arrays of lines. The first array of lines is the test input, and
      // the second one is the expected output. If the second array has a single element starting
      // with !! then it is expected that ImportOrderer will throw a FormatterException with that
      // message.
      //
      // If a line ends with \ then we remove the \ and don't append a \n. That allows us to check
      // some parsing edge cases.
      String[][][] inputsOutputs = {
        // Capital letter before lowercase
        {
          {
            "package foo;",
            "",
            "import android.abC.Bar;",
            "import android.abc.Bar;",
            "public class Blim {}",
          },
          {
            "package foo;",
            "",
            "import android.abC.Bar;",
            "import android.abc.Bar;",
            "",
            "public class Blim {}",
          }
        },
        // Blank line between "com.android" and "com.anythingelse"
        {
          {
            "package foo;",
            "",
            "import com.android.Bar;",
            "import com.google.Bar;",
            "public class Blim {}",
          },
          {
            "package foo;",
            "",
            "import com.android.Bar;",
            "",
            "import com.google.Bar;",
            "",
            "public class Blim {}",
          }
        },
        // Rough ordering -- statics, android, third party, then java, with blank lines between
        // major groupings
        {
          {
            "package foo;",
            "",
            "import static net.Bar.baz;",
            "import static org.junit.Bar.baz;",
            "import static com.google.Bar.baz;",
            "import static java.lang.Bar.baz;",
            "import static junit.Bar.baz;",
            "import static javax.annotation.Bar.baz;",
            "import static android.Bar.baz;",
            "import net.Bar;",
            "import org.junit.Bar;",
            "import com.google.Bar;",
            "import java.lang.Bar;",
            "import junit.Bar;",
            "import javax.annotation.Bar;",
            "import android.Bar;",
            "public class Blim {}",
          },
          {
            "package foo;",
            "",
            "import static android.Bar.baz;",
            "",
            "import static com.google.Bar.baz;",
            "",
            "import static junit.Bar.baz;",
            "",
            "import static net.Bar.baz;",
            "",
            "import static org.junit.Bar.baz;",
            "",
            "import static java.lang.Bar.baz;",
            "",
            "import static javax.annotation.Bar.baz;",
            "",
            "import android.Bar;",
            "",
            "import com.google.Bar;",
            "",
            "import junit.Bar;",
            "",
            "import net.Bar;",
            "",
            "import org.junit.Bar;",
            "",
            "import java.lang.Bar;",
            "",
            "import javax.annotation.Bar;",
            "",
            "public class Blim {}",
          }
        },
        {
          {
            "package foo;",
            "",
            "import static java.first.Bar.baz;",
            "import static com.second.Bar.baz;",
            "import com.first.Bar;",
            "import static android.second.Bar.baz;",
            "import dalvik.first.Bar;",
            "import static dalvik.first.Bar.baz;",
            "import static androidx.second.Bar.baz;",
            "import java.second.Bar;",
            "import static com.android.second.Bar.baz;",
            "import static net.first.Bar.baz;",
            "import gov.second.Bar;",
            "import junit.second.Bar;",
            "import static libcore.second.Bar.baz;",
            "import static java.second.Bar.baz;",
            "import static net.second.Bar.baz;",
            "import static org.first.Bar.baz;",
            "import static dalvik.second.Bar.baz;",
            "import javax.first.Bar;",
            "import static javax.second.Bar.baz;",
            "import android.first.Bar;",
            "import android.second.Bar;",
            "import static javax.first.Bar.baz;",
            "import androidx.first.Bar;",
            "import static androidx.first.Bar.baz;",
            "import androidx.second.Bar;",
            "import com.android.first.Bar;",
            "import gov.first.Bar;",
            "import com.android.second.Bar;",
            "import dalvik.second.Bar;",
            "import static org.second.Bar.baz;",
            "import net.first.Bar;",
            "import libcore.second.Bar;",
            "import static android.first.Bar.baz;",
            "import com.second.Bar;",
            "import static gov.second.Bar.baz;",
            "import static gov.first.Bar.baz;",
            "import static junit.first.Bar.baz;",
            "import libcore.first.Bar;",
            "import junit.first.Bar;",
            "import javax.second.Bar;",
            "import static libcore.first.Bar.baz;",
            "import net.second.Bar;",
            "import static com.first.Bar.baz;",
            "import org.second.Bar;",
            "import static junit.second.Bar.baz;",
            "import java.first.Bar;",
            "import org.first.Bar;",
            "import static com.android.first.Bar.baz;",
            "public class Blim {}",
          },
          {
            "package foo;", //
            "",
            "import static android.first.Bar.baz;",
            "import static android.second.Bar.baz;",
            "",
            "import static androidx.first.Bar.baz;",
            "import static androidx.second.Bar.baz;",
            "",
            "import static com.android.first.Bar.baz;",
            "import static com.android.second.Bar.baz;",
            "",
            "import static dalvik.first.Bar.baz;",
            "import static dalvik.second.Bar.baz;",
            "",
            "import static libcore.first.Bar.baz;",
            "import static libcore.second.Bar.baz;",
            "",
            "import static com.first.Bar.baz;",
            "import static com.second.Bar.baz;",
            "",
            "import static gov.first.Bar.baz;",
            "import static gov.second.Bar.baz;",
            "",
            "import static junit.first.Bar.baz;",
            "import static junit.second.Bar.baz;",
            "",
            "import static net.first.Bar.baz;",
            "import static net.second.Bar.baz;",
            "",
            "import static org.first.Bar.baz;",
            "import static org.second.Bar.baz;",
            "",
            "import static java.first.Bar.baz;",
            "import static java.second.Bar.baz;",
            "",
            "import static javax.first.Bar.baz;",
            "import static javax.second.Bar.baz;",
            "",
            "import android.first.Bar;",
            "import android.second.Bar;",
            "",
            "import androidx.first.Bar;",
            "import androidx.second.Bar;",
            "",
            "import com.android.first.Bar;",
            "import com.android.second.Bar;",
            "",
            "import dalvik.first.Bar;",
            "import dalvik.second.Bar;",
            "",
            "import libcore.first.Bar;",
            "import libcore.second.Bar;",
            "",
            "import com.first.Bar;",
            "import com.second.Bar;",
            "",
            "import gov.first.Bar;",
            "import gov.second.Bar;",
            "",
            "import junit.first.Bar;",
            "import junit.second.Bar;",
            "",
            "import net.first.Bar;",
            "import net.second.Bar;",
            "",
            "import org.first.Bar;",
            "import org.second.Bar;",
            "",
            "import java.first.Bar;",
            "import java.second.Bar;",
            "",
            "import javax.first.Bar;",
            "import javax.second.Bar;",
            "",
            "public class Blim {}",
          },
        }
      };
      ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
      Arrays.stream(inputsOutputs).forEach(input -> builder.add(createRow(input)));
      return builder.build();
    }

    @Test
    public void reorder() throws FormatterException {
      try {
        String output = ImportOrderer.reorderImports(input, Style.AOSP);
        assertWithMessage("Expected exception").that(reordered).doesNotMatch("^!!");
        assertWithMessage(input).that(output).isEqualTo(reordered);
      } catch (FormatterException e) {
        if (!reordered.startsWith("!!")) {
          throw e;
        }
        assertThat(reordered).endsWith("\n");
        assertThat(e)
            .hasMessageThat()
            .isEqualTo("error: " + reordered.substring(2, reordered.length() - 1));
      }
    }
  }

  private static Object[] createRow(String[][] inputAndOutput) {
    assertThat(inputAndOutput).hasLength(2);
    String[] input = inputAndOutput[0];
    String[] output = inputAndOutput[1];
    if (output.length == 0) {
      output = input;
    }
    Object[] row = {
      Joiner.on('\n').join(input) + '\n', //
      Joiner.on('\n').join(output) + '\n',
    };
    // If a line ends with \ then we remove the \ and don't append a \n. That allows us to check
    // some parsing edge cases.
    row[0] = ((String) row[0]).replace("\\\n", "");
    return row;
  }
}
