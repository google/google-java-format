package com.google.googlejavaformat.java.maven;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

/**
 * Simplifies formatting Java source files. See {@link #format()}.
 *
 * <p>TODO: Suggestion: Candidate to move to API?
 */
class JavaSourceFormatter {
  private final Path source;
  private final boolean replace;
  private final Formatter formatter;

  /**
   * Creates an instance that replaces the original source by the formatted source.
   *
   * @param source the {@link Path} to the Java source file
   * @param formatter the {@link Formatter} instance to use
   */
  JavaSourceFormatter(Path source, Formatter formatter) {
    this(source, formatter, true);
  }

  /**
   * Creates an instance.
   *
   * @param source the {@link Path} to the Java source file
   * @param formatter the {@link Formatter} instance to use
   * @param replace if the original source file should be replaced with the formatted source
   */
  JavaSourceFormatter(Path source, Formatter formatter, final boolean replace) {
    this.source = source;
    this.formatter = formatter;
    this.replace = replace;
  }

  /**
   * Reads the Java source file using {@link Files#readAllBytes(Path)}, uses {@link
   * Formatter#formatSource(String)}, and finally if configured to replace, then writes the
   * formatted source to the source file using {@link Files#write(Path, byte[],
   * java.nio.file.OpenOption...)}.
   *
   * @return the formatted source
   * @throws IOException thrown if reading/writing from/to the source file fails
   * @throws FormatterException See {@link Formatter#formatSource(String)}
   */
  public String format() throws IOException, FormatterException {
    String sourceChars = read();
    String formattedSource = formatter.formatSource(sourceChars);
    if (replace) {
      write(formattedSource);
    }
    return formattedSource;
  }

  protected void write(String formattedSource) throws IOException {
    Files.write(source, formattedSource.getBytes(StandardCharsets.UTF_8));
  }

  protected String read() throws IOException {
    return new String(Files.readAllBytes(this.source), StandardCharsets.UTF_8);
  }
}
