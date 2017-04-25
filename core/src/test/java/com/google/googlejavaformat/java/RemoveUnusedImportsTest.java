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
import static com.google.googlejavaformat.java.RemoveUnusedImports.JavadocOnlyImports.KEEP;
import static com.google.googlejavaformat.java.RemoveUnusedImports.JavadocOnlyImports.REMOVE;
import static com.google.googlejavaformat.java.RemoveUnusedImports.removeUnusedImports;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** {@link RemoveUnusedImports}Test */
@RunWith(Parameterized.class)
public class RemoveUnusedImportsTest {
  @Parameters(name = "{index}: {0}")
  public static Collection<Object[]> parameters() {
    String[][][] inputsOutputs = {
      {
        {
          "import java.util.List;",
          "import java.util.ArrayList;",
          "",
          "class Test {",
          "  /** could be an {@link ArrayList} */",
          "  List<String> xs;",
          "}",
        },
        {
          "import java.util.List;",
          "import java.util.ArrayList;",
          "",
          "class Test {",
          "  /** could be an {@link ArrayList} */",
          "  List<String> xs;",
          "}",
        },
        {
          "import java.util.List;",
          "",
          "class Test {",
          "  /** could be an {@link java.util.ArrayList} */",
          "  List<String> xs;",
          "}",
        },
      },
      {
        {
          "import java.util.ArrayList;", //
          "import java.util.Collection;",
          "/** {@link ArrayList#add} {@link Collection#remove(Object)} */",
          "class Test {}",
        },
        {
          "import java.util.ArrayList;", //
          "import java.util.Collection;",
          "/** {@link ArrayList#add} {@link Collection#remove(Object)} */",
          "class Test {}",
        },
        {
          "/** {@link java.util.ArrayList#add} {@link java.util.Collection#remove(Object)} */",
          "class Test {}",
        }
      },
      {
        {
          "import a.A;",
          "import a.B;",
          "import a.C;",
          "class Test {",
          "  /** a",
          "   * {@link A} */",
          "  void f() {}",
          "}",
        },
        {
          "import a.A;", //
          "class Test {",
          "  /** a",
          "   * {@link A} */",
          "  void f() {}",
          "}",
        },
        {
          "class Test {", //
          "  /** a {@link a.A} */",
          "  void f() {}",
          "}",
        }
      },
      {
        {
          "import a.A;import a.B;", //
          "import a.C; // hello",
          "class Test {",
          "  B b;",
          "}",
        },
        {
          "import a.B;", //
          "// hello",
          "class Test {",
          "  B b;",
          "}",
        },
        {
          "import a.B;", //
          "// hello",
          "class Test {",
          "  B b;",
          "}",
        },
      },
      {
        {
          "import a.A;",
          "import b.B;",
          "import c.C;",
          "import d.D;",
          "import e.E;",
          "import f.F;",
          "import g.G;",
          "import h.H;",
          "/**",
          " * {@link A} {@linkplain B} {@value D#FOO}",
          " *",
          " * @exception E",
          " * @throws F",
          " * @see C",
          " * @see H#foo",
          " * @see <a href=\"whatever\">",
          " */",
          "class Test {",
          "}",
        },
        {
          "import a.A;",
          "import b.B;",
          "import c.C;",
          "import d.D;",
          "import e.E;",
          "import f.F;",
          "import h.H;",
          "/**",
          " * {@link A} {@linkplain B} {@value D#FOO}",
          " *",
          " * @exception E",
          " * @throws F",
          " * @see C",
          " * @see H#foo",
          " * @see <a href=\"whatever\">",
          " */",
          "class Test {",
          "}",
        },
        {
          "/**",
          " * {@link a.A} {@linkplain b.B} {@value d.D#FOO}",
          " *",
          " * @exception e.E",
          " * @throws f.F",
          " * @see c.C",
          " * @see h.H#foo",
          " * @see <a href=\"whatever\">",
          " */",
          "class Test {}",
        }
      },
      {
        {
          "import java.util.Map;",
          "/** {@link Map.Entry#containsKey(Object)} } */",
          "class Test {}",
        },
        {
          "import java.util.Map;",
          "/** {@link Map.Entry#containsKey(Object)} } */",
          "class Test {}",
        },
        {
          "/** {@link java.util.Map.Entry#containsKey(Object)} } */", //
          "class Test {}",
        }
      },
      {
        {
          "/** {@link #containsKey(Object)} } */", //
          "class Test {}",
        },
        {
          "/** {@link #containsKey(Object)} } */", //
          "class Test {}",
        },
        {
          "/** {@link #containsKey(Object)} } */", //
          "class Test {}",
        }
      },
      {
        {
          "import java.util.*;", //
          "class Test {",
          "  List<String> xs;",
          "}",
        },
        {
          "import java.util.*;", //
          "class Test {",
          "  List<String> xs;",
          "}",
        },
        {
          "import java.util.*;", //
          "class Test {",
          "  List<String> xs;",
          "}",
        }
      },
      {
        {
          "package com.foo;",
          "import static com.foo.Outer.A;",
          "import com.foo.*;",
          "import com.foo.B;",
          "import com.bar.C;",
          "class Test {",
          "  A a;",
          "  B b;",
          "  C c;",
          "}",
        },
        {
          "package com.foo;",
          "import static com.foo.Outer.A;",
          "import com.bar.C;",
          "class Test {",
          "  A a;",
          "  B b;",
          "  C c;",
          "}",
        },
        {
          "package com.foo;",
          "import static com.foo.Outer.A;",
          "import com.bar.C;",
          "class Test {",
          "  A a;",
          "  B b;",
          "  C c;",
          "}",
        }
      },
      {
        {
          "import java.util.Map;", //
          "import java.util.Map.Entry;",
          "/** {@link #foo(Map.Entry[])} */",
          "public class Test {}",
        },
        {
          "import java.util.Map;", //
          "/** {@link #foo(Map.Entry[])} */",
          "public class Test {}",
        },
        {
          "/** {@link #foo(Map.Entry[])} */", //
          // TODO(cushon): `Map` should be qualified, but javac's javadoc parser doesn't store
          // position information for parameters in references
          // "/** {@link #foo(java.util.Map.Entry[])} */",
          "public class Test {}",
        }
      },
      {
        {
          "import java.util.List;",
          "import java.util.Collection;",
          "/** {@link java.util.List#containsAll(Collection)} */",
          "public class Test {}",
        },
        {
          "import java.util.Collection;",
          "/** {@link java.util.List#containsAll(Collection)} */",
          "public class Test {}",
        },
        {
          "/** {@link java.util.List#containsAll(Collection)} */", //
          // TODO(cushon): `Collection` should be qualified, but javac's javadoc parser doesn't
          // store position information for parameters in references
          // "/** {@link java.util.List#containsAll(java.util.Collection)} */",
          "public class Test {}",
        },
      },
      {
        {
          "package p;",
          "import java.lang.Foo;",
          "import java.lang.Foo.Bar;",
          "import p.Baz;",
          "import p.Baz.Bork;",
          "public class Test implements Foo, Bar, Baz, Bork {}",
        },
        {
          "package p;",
          "import java.lang.Foo.Bar;",
          "import p.Baz.Bork;",
          "public class Test implements Foo, Bar, Baz, Bork {}",
        },
        {
          "package p;",
          "import java.lang.Foo.Bar;",
          "import p.Baz.Bork;",
          "public class Test implements Foo, Bar, Baz, Bork {}",
        },
      },
    };
    ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
    for (String[][] inputAndOutput : inputsOutputs) {
      assertThat(inputAndOutput.length).isEqualTo(3);
      String[] input = inputAndOutput[0];
      String[] output = inputAndOutput[1];
      String[] outputFixJavadoc = inputAndOutput[2];
      String[] parameters = {
        Joiner.on('\n').join(input) + '\n',
        Joiner.on('\n').join(output) + '\n',
        Joiner.on('\n').join(outputFixJavadoc) + '\n',
      };
      builder.add(parameters);
    }
    return builder.build();
  }

  private final String input;
  private final String expected;
  private final String expectedFixJavadoc;

  public RemoveUnusedImportsTest(String input, String expected, String expectedFixJavadoc) {
    this.input = input;
    this.expected = expected;
    this.expectedFixJavadoc = expectedFixJavadoc;
  }

  @Test
  public void removeUnused() throws FormatterException {
    assertThat(removeUnusedImports(input, KEEP)).isEqualTo(expected);
  }

  @Test
  public void fixJavadoc() throws FormatterException {
    assertThat(removeUnusedImports(input, REMOVE)).isEqualTo(expectedFixJavadoc);
  }
}
