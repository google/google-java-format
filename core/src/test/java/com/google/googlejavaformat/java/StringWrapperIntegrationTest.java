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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** {@link StringWrapper}IntegrationTest */
@RunWith(Parameterized.class)
public class StringWrapperIntegrationTest {

  @Parameters
  public static Collection<Object[]> parameters() {
    String[][][] inputsAndOutputs = {
      {
        {
          "class T {", //
          "  String s =",
          "      \"one long incredibly unbroken sentence\"",
          "          + \" moving from topic to topic\"",
          "          + \" so that no-one had a chance to\"",
          "          + \" interrupt\";",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "      \"one long incredibly unbroken\"",
          "          + \" sentence moving from\"",
          "          + \" topic to topic so that\"",
          "          + \" no-one had a chance to\"",
          "          + \" interrupt\";",
          "}",
        }
      },
      {
        {
          "class T {", //
          "  String s =",
          "      \"one long incredibly unbroken\"",
          "          + \" sentence moving from topic to topic so that\"",
          "          + \" no-one had a chance to\"",
          "          + \" interrupt\";",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "      \"one long incredibly unbroken\"",
          "          + \" sentence moving from\"",
          "          + \" topic to topic so that\"",
          "          + \" no-one had a chance to\"",
          "          + \" interrupt\";",
          "}",
        }
      },
      {
        {
          "class T {", //
          "  String s =",
          "      \"one long incredibly unbroken\"",
          "          + \" sentence moving from topic to topic\"",
          "          + \" so that no-one had a chance to interr\"",
          "          + \"upt\";",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "      \"one long incredibly unbroken\"",
          "          + \" sentence moving from\"",
          "          + \" topic to topic so that\"",
          "          + \" no-one had a chance to\"",
          "          + \" interrupt\";",
          "}",
        }
      },
      {
        {
          "class T {", //
          "  String s = \"one long incredibly unbroken sentence moving from topic to topic so that"
              + " no-one had a chance to interrupt\";",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "      \"one long incredibly unbroken\"",
          "          + \" sentence moving from\"",
          "          + \" topic to topic so that\"",
          "          + \" no-one had a chance to\"",
          "          + \" interrupt\";",
          "}",
        },
      },
      {
        {
          "class T {", //
          "  String s =",
          "      \"one long incredibly unbroken sentence\"",
          "          + \" moving from topic to topic\"",
          "          + 42",
          "          + \" so that no-one had a chance to interr\"",
          "          + \"upt\";",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "      \"one long incredibly unbroken\"",
          "          + \" sentence moving from\"",
          "          + \" topic to topic\"",
          "          + 42",
          "          + \" so that no-one had a\"",
          "          + \" chance to interrupt\";",
          "}",
        }
      },
      {
        {
          "class T {", //
          "  String s ="
              + " \"onelongincrediblyunbrokensentencemovingfromtopictotopicsothatnoonehadachanceto"
              + " interrupt\";",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "     "
              + " \"onelongincrediblyunbrokensentencemovingfromtopictotopicsothatnoonehadachanceto\"",
          "          + \" interrupt\";",
          "}",
        }
      },
      {
        {
          "class T {", //
          "  String s = \"\\n\\none\\nlong\\nincredibly\\nunbroken\\nsentence\\nmoving\\nfrom\\n"
              + " topic\\nto\\n topic\\nso\\nthat\\nno-one\\nhad\\na\\nchance\\nto\\ninterrupt\";",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "      \"\\n\\n\"",
          "          + \"one\\n\"",
          "          + \"long\\n\"",
          "          + \"incredibly\\n\"",
          "          + \"unbroken\\n\"",
          "          + \"sentence\\n\"",
          "          + \"moving\\n\"",
          "          + \"from\\n\"",
          "          + \" topic\\n\"",
          "          + \"to\\n\"",
          "          + \" topic\\n\"",
          "          + \"so\\n\"",
          "          + \"that\\n\"",
          "          + \"no-one\\n\"",
          "          + \"had\\n\"",
          "          + \"a\\n\"",
          "          + \"chance\\n\"",
          "          + \"to\\n\"",
          "          + \"interrupt\";",
          "}",
        },
      },
      {
        {
          "class T {", //
          "  String s = \"\\n\\n\\none\\n\\nlong\\n\\nincredibly\\n\\nunbroken\\n\\nsentence\\n\\n"
              + "moving\\n\\nfrom\\n\\n topic\\n\\nto\\n\\n topic\\n\\nso\\n\\nthat\\n\\nno-one"
              + "\\n\\nhad\\n\\na\\n\\nchance\\n\\nto\\n\\ninterrupt\\n\\n\";",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "      \"\\n\\n\\n\"",
          "          + \"one\\n\\n\"",
          "          + \"long\\n\\n\"",
          "          + \"incredibly\\n\\n\"",
          "          + \"unbroken\\n\\n\"",
          "          + \"sentence\\n\\n\"",
          "          + \"moving\\n\\n\"",
          "          + \"from\\n\\n\"",
          "          + \" topic\\n\\n\"",
          "          + \"to\\n\\n\"",
          "          + \" topic\\n\\n\"",
          "          + \"so\\n\\n\"",
          "          + \"that\\n\\n\"",
          "          + \"no-one\\n\\n\"",
          "          + \"had\\n\\n\"",
          "          + \"a\\n\\n\"",
          "          + \"chance\\n\\n\"",
          "          + \"to\\n\\n\"",
          "          + \"interrupt\\n\\n\";",
          "}",
        },
      },
      {
        {
          "class T {", //
          "  String s = \"onelongincrediblyunbrokensenten\\tcemovingfromtopictotopicsothatnoonehada"
              + "chance tointerrupt\";",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "      \"onelongincrediblyunbrokensenten\"",
          "          + \"\\tcemovingfromtopictotopicsothatnoonehadachance\"",
          "          + \" tointerrupt\";",
          "}",
        }
      },
      {
        {
          "class T {", //
          "  String s = \"onelongincrediblyunbrokensentencemovingfromtopictotopicsothatnoonehada"
              + "chancetointerrupt_____________________)_\";",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "      \"onelongincrediblyunbrokensentencemovingfromtopictotopicsothatnoonehada"
              + "chancetointerrupt_____________________)_\";",
          "}",
        }
      },
      {
        {
          "class T {", //
          "  String s = \"onelongincrediblyunbrokensentencemovingfromtopictotopicsot atnoonehada"
              + "chancetointerrupt______________________\";;",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "      \"onelongincrediblyunbrokensentencemovingfromtopictotopicsot\"",
          "          + \" atnoonehadachancetointerrupt______________________\";",
          "  ;",
          "}",
        }
      },
      {
        {
          "class T {", //
          "  String s = \"__ onelongincrediblyunbrokensentencemovingfromtopictotopicsothatnoonehada"
              + "chanceto interrupt\";",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "      \"__"
              + " onelongincrediblyunbrokensentencemovingfromtopictotopicsothatnoonehadachanceto\"",
          "          + \" interrupt\";",
          "}",
        }
      },
      {
        {
          "class T {", //
          "  String s =",
          "      \"one long incredibly unbroken sentence\"",
          "          // comment",
          "          + \" moving from topic to topic\"",
          "          // comment",
          "          + \" so that no-one had a chance to\"",
          "          // comment",
          "          + \" interrupt\";",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "      \"one long incredibly unbroken\"",
          "          + \" sentence\"",
          "          // comment",
          "          + \" moving from topic to\"",
          "          + \" topic\"",
          "          // comment",
          "          + \" so that no-one had a\"",
          "          + \" chance to\"",
          "          // comment",
          "          + \" interrupt\";",
          "}",
        }
      },
      {
        {
          "class T {", //
          "  String s =",
          "      \"aaaaaaaaaaaaaaaaaaaaaaaa bbbbbbbbbbbbbbbbbb ccccccccccccccccccccccccccc"
              + " dddddddddddddddddd\";",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "      \"aaaaaaaaaaaaaaaaaaaaaaaa\"",
          "          + \" bbbbbbbbbbbbbbbbbb\"",
          "          + \" ccccccccccccccccccccccccccc\"",
          "          + \" dddddddddddddddddd\";",
          "}",
        }
      },
      {
        {
          "class T {", //
          "  String s =",
          "      \"aaaaaaaaaaaaaaaaaaaaaaaa \"",
          "          + \"bbbbbbbbbbbbbbbbbb \"",
          "          + \"ccccccccccccccccccccccccccc \"",
          "          + \"dddddddddddddddddd\";",
          "}"
        },
        {
          "class T {",
          "  String s =",
          "      \"aaaaaaaaaaaaaaaaaaaaaaaa\"",
          "          + \" bbbbbbbbbbbbbbbbbb\"",
          "          + \" ccccccccccccccccccccccccccc\"",
          "          + \" dddddddddddddddddd\";",
          "}",
        }
      },
      {
        {
          "class T {", //
          "  byte[] bytes =",
          "      \"one long incredibly unbroken sentence moving from topic to topic so that no-one"
              + " had a chance to interrupt\".getBytes();",
          "}"
        },
        {
          "class T {", //
          "  byte[] bytes =",
          "      \"one long incredibly unbroken sentence moving from topic to topic so that no-one"
              + " had a chance to interrupt\"",
          "          .getBytes();",
          "}"
        },
      },
    };
    return Arrays.stream(inputsAndOutputs)
        .map(
            inputAndOutput -> {
              assertThat(inputAndOutput).hasLength(2);
              return new String[] {
                Joiner.on('\n').join(inputAndOutput[0]) + '\n', //
                Joiner.on('\n').join(inputAndOutput[1]) + '\n',
              };
            })
        .collect(toImmutableList());
  }

  private final String input;
  private final String output;

  public StringWrapperIntegrationTest(String input, String output) {
    this.input = input;
    this.output = output;
  }

  @Test
  public void test() throws Exception {
    assertThat(StringWrapper.wrap(40, new Formatter().formatSource(input))).isEqualTo(output);
  }

  @Test
  public void testCR() throws Exception {
    assertThat(StringWrapper.wrap(40, new Formatter().formatSource(input.replace("\n", "\r"))))
        .isEqualTo(output.replace("\n", "\r"));
  }

  @Test
  public void testCRLF() throws Exception {
    assertThat(StringWrapper.wrap(40, new Formatter().formatSource(input.replace("\n", "\r\n"))))
        .isEqualTo(output.replace("\n", "\r\n"));
  }

  @Test
  public void idempotent() throws Exception {
    String wrap = StringWrapper.wrap(40, new Formatter().formatSource(input));
    assertThat(wrap).isEqualTo(new Formatter().formatSource(wrap));
  }
}
