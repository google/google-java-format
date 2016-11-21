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

package com.google.googlejavaformat.java.filer;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.base.Joiner;
import com.google.googlejavaformat.FormatterDiagnostic;
import com.google.googlejavaformat.FormattingError;
import com.google.googlejavaformat.java.FormatterException;
import com.google.testing.compile.CompilationRule;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link com.google.googlejavaformat.java.filer.FormattingFiler}. */
@RunWith(JUnit4.class)
public class FormattingFilerTest {

  @Rule public CompilationRule compilationRule = new CompilationRule();

  @Test
  public void invalidSyntaxThrowsError() throws IOException {
    String file = Joiner.on('\n').join("package foo;", "public class Bar {");
    FormattingFiler formattingFiler = new FormattingFiler(new FakeFiler());
    Writer writer = formattingFiler.createSourceFile("foo.Bar").openWriter();
    writer.write(file);
    try {
      writer.close();
      fail("An exception should be thrown if formatting fails");
    } catch (IOException expected) {
      assertThat(expected.getMessage()).contains("foo.Bar");
      assertThat(expected.getCause().getClass()).isAssignableTo(FormatterException.class);
    } catch (FormattingError error) {
      assertThat(error.diagnostics()).hasSize(1);
      FormatterDiagnostic diagnostic = error.diagnostics().get(0);
      assertThat(diagnostic.line()).isEqualTo(2);
      assertThat(diagnostic.column()).isEqualTo(19);
      assertThat(diagnostic.message()).contains("reached end of file while parsing");
    }
  }

  @Test
  public void formatsFile() throws IOException {
    FormattingFiler formattingFiler = new FormattingFiler(new FakeFiler());
    JavaFileObject sourceFile = formattingFiler.createSourceFile("foo.Bar");
    try (Writer writer = sourceFile.openWriter()) {
      writer.write("package foo;class Bar{private String      baz;\n\n\n\n}");
    }

    assertThat(sourceFile.getCharContent(false).toString())
        .isEqualTo(
            Joiner.on('\n')
                .join(
                    "package foo;",
                    "",
                    "class Bar {",
                    "  private String baz;",
                    "}",
                    "")); // trailing newline
  }

  private static class FakeFiler implements Filer {
    @Override
    public JavaFileObject createSourceFile(CharSequence name, Element... originatingElements)
        throws IOException {
      return new ObservingJavaFileObject(name.toString(), Kind.SOURCE);
    }

    @Override
    public JavaFileObject createClassFile(CharSequence name, Element... originatingElements)
        throws IOException {
      return new ObservingJavaFileObject(name.toString(), Kind.CLASS);
    }

    @Override
    public FileObject createResource(
        Location location,
        CharSequence pkg,
        CharSequence relativeName,
        Element... originatingElements)
        throws IOException {
      return new ObservingJavaFileObject(pkg.toString() + relativeName, Kind.OTHER);
    }

    @Override
    public FileObject getResource(Location location, CharSequence pkg, CharSequence relativeName)
        throws IOException {
      return new ObservingJavaFileObject(pkg.toString() + relativeName, Kind.OTHER);
    }
  }

  private static class ObservingJavaFileObject extends SimpleJavaFileObject {
    private final StringWriter output = new StringWriter();

    ObservingJavaFileObject(String name, Kind kind) {
      super(URI.create(name), kind);
    }

    @Override
    public Writer openWriter() throws IOException {
      return output;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
      return output.toString();
    }
  }
}
