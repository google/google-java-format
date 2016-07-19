package com.google.googlejavaformat.java.maven;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * A {@link Callable} implementation to execute {@link FileCollector#collect(Path)} as a task for
 * concurrent execution.
 */
class FileCollectorTask implements Callable<List<Path>> {
  private final Path path;
  private final String extension;

  public FileCollectorTask(Path path, String extension) {
    this.path = path;
    this.extension = extension;
  }

  @Override
  public List<Path> call() throws Exception {
    return new FileCollector(extension).collect(path);
  }
}
