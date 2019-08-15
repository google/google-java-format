package com.google.googlejavaformat.java;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link StringWrapper}Test */
@RunWith(JUnit4.class)
public class StringWrapperTest {
  @Test
  public void testAwkwardLineEndWrapping() throws Exception {
    String input =
        lines(
            "class T {",
            // This is a wide line, but has to be split in code because of 100-char limit.
            "  String s = someMethodWithQuiteALongNameThatWillGetUsUpCloseToTheColumnLimit() "
                + "+ \"foo bar foo bar foo bar\";",
            "",
            "  String someMethodWithQuiteALongNameThatWillGetUsUpCloseToTheColumnLimit() {",
            "    return null;",
            "  }",
            "}");
    String output =
        lines(
            "class T {",
            "  String s =",
            "      someMethodWithQuiteALongNameThatWillGetUsUpCloseToTheColumnLimit()",
            "          + \"foo bar foo bar foo bar\";",
            "",
            "  String someMethodWithQuiteALongNameThatWillGetUsUpCloseToTheColumnLimit() {",
            "    return null;",
            "  }",
            "}");

    assertThat(StringWrapper.wrap(100, input, new Formatter())).isEqualTo(output);
  }

  private static String lines(String... line) {
    return Joiner.on('\n').join(line) + '\n';
  }
}
