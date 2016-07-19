package com.google.googlejavaformat.java.maven;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Helps collecting Java source files during the mojo execution.
 *
 * <p>This class is essentially a function of {@link Path} to {@link List} of {@link Path}. The
 * implementation does not reflect that just in case for backward compatibility with old Java
 * versions.
 *
 * <p>TODO: Suggestion: Move to API? It can be used later in the core collect all the Java source
 * files if the input parameter to CLI is a directory.
 *
 * @see #collect(Path)
 */
class FileCollector {

  /**
   * Concurrently collects all the files with provided {@code extension} from the list of paths.
   *
   * @param paths the {@link List} of {@link Path} to collect files from
   * @param extension the extension to match with when collecting files
   * @return a {@link List} of collected files
   * @throws InterruptedException thrown if the concurrent execution is interrupted
   * @throws ExecutionException thrown if execution fails processing a directory or a file
   */
  static List<Path> collectAll(List<Path> paths, String extension)
      throws InterruptedException, ExecutionException {
    List<FileCollectorTask> fileCollectorTasks = createFileCollectorTasks(paths, extension);
    List<List<Path>> collectedPaths = Executor.execute(fileCollectorTasks);
    List<Path> allPaths = Lists.newLinkedList();
    for (List<Path> pathList : collectedPaths) {
      allPaths.addAll(pathList);
    }
    return Collections.unmodifiableList(allPaths);
  }

  /**
   * Creates tasks for collecting files.
   *
   * @param paths the {@link List} of paths to collect files from.
   * @param extension the extension of expected files
   * @return a {@link List} of {@link FileCollectorTask}
   */
  static List<FileCollectorTask> createFileCollectorTasks(List<Path> paths, String extension) {
    List<FileCollectorTask> tasks = Lists.newLinkedList();
    for (Path path : paths) {
      tasks.add(new FileCollectorTask(path, extension));
    }
    return tasks;
  }

  private final String extension;

  /**
   * C'tor.
   *
   * @param extension the expected extension for this file collector
   */
  public FileCollector(String extension) {
    Objects.requireNonNull(extension);
    this.extension = extension;
  }

  /**
   * Collects files that have the expected extension from a specific directory path. This method
   * uses {@link Files#walk(Path, java.nio.file.FileVisitOption...)} with an instance of {@link
   * CollectingFileVisitor} to collect the files.
   *
   * @param path the directory path to collect files from
   * @return the {@link List} of {@link Path} of collected files
   * @throws IOException thrown if either the starting path is not directory, or there's a
   *     processing error for a directory or path
   */
  public List<Path> collect(final Path path) throws IOException {
    if (!Files.isDirectory(path)) {
      throw new IOException("Path must be a directory: " + path);
    }
    final Set<Path> collectedFiles = Sets.newLinkedHashSet();
    FileVisitor<Path> visitor = new CollectingFileVisitor(collectedFiles, extension);
    Files.walkFileTree(path, visitor);
    return Lists.newLinkedList(collectedFiles);
  }
}
