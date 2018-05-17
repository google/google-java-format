package com.google.googlejavaformat.java.maven;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Iterators;

/**
 * An implementation of {@link FileVisitor} that collects all the visited files that have a specific
 * extension.
 *
 * <p>TODO: Suggestion: Move to API?
 */
class CollectingFileVisitor extends SimpleFileVisitor<Path> {
  private final Set<Path> collectedFiles;
  private final String extension;

  /**
   * Creates an instance.
   *
   * @param collectedFiles the {@link Set} to hold the {@link Path} to files being collected
   * @param extension the expected extension
   */
  public CollectingFileVisitor(final Set<Path> collectedFiles, final String extension) {
    Objects.requireNonNull(collectedFiles);
    this.collectedFiles = collectedFiles;
    this.extension = extension;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
    if (Iterators.size(Files.newDirectoryStream(dir).iterator()) == 0) {
      return FileVisitResult.SKIP_SUBTREE;
    }
    return super.preVisitDirectory(dir, attrs);
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
    if (matchesExtension(file)) {
      addPath(file);
    }
    return super.visitFile(file, attrs);
  }

  protected void addPath(Path file) {
    this.collectedFiles.add(file);
  }

  protected boolean matchesExtension(Path file) {
    return file.getFileName().toString().endsWith(extension);
  }
}
