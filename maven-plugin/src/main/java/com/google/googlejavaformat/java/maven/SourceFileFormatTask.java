package com.google.googlejavaformat.java.maven;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.google.googlejavaformat.java.Formatter;

/**
 * An implementation of {@link Callable} that enables executing {@link JavaSourceFormatter} as a
 * task in a concurrent way.
 */
class SourceFileFormatTask implements Callable<String> {
  private final Path source;
  private final Formatter formatter;

  /**
   * Creates an instance.
   *
   * @param source the {@link Path} to Java source file
   * @param formatter the {@link Formatter} instance
   */
  public SourceFileFormatTask(Path source, Formatter formatter) {
    this.source = source;
    this.formatter = formatter;
  }

  @Override
  public String call() throws Exception {
    return new JavaSourceFormatter(source, formatter).format();
  }
}
