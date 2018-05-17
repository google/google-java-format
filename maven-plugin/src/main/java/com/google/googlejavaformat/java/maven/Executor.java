package com.google.googlejavaformat.java.maven;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.collect.Lists;

/** A utility class around {@link ExecutorService} to execute and collect results. */
class Executor {

  /**
   * Execute a {@link List} of {@link Callable} tasks and wait for the results to collect them or
   * fail as soon as one fails. An {@link ExecutorService} is created and shut down; when shutting
   * down the executor service, the unfinished tasks by {@link ExecutorService#shutdownNow()} is
   * ignored.
   *
   * @param <T> the type of expected results
   * @param <C> the bound type for task implementing {@link Callable} of {@code T}
   * @param tasks the {@link List} of tasks as instances of {@link Callable}
   * @return the {@link List} of result values collected from {@link Future}'s
   * @throws InterruptedException thrown if the execution is interrupted
   * @throws ExecutionException thrown if a task fails during processing
   */
  static <T, C extends Callable<T>> List<T> execute(List<C> tasks)
      throws InterruptedException, ExecutionException {
    // Use at most half of the available processors from runtime not to consume too many resources
    final ExecutorService executor =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2 + 1);
    try {
      // #invokeAll preserves the order of submitted tasks in the returned futures
      List<Future<T>> taskResults = executor.invokeAll(tasks);
      List<T> results = Lists.newLinkedList();
      for (Future<T> f : taskResults) {
        results.add(f.get());
      }
      return Collections.unmodifiableList(results);
    } finally {
      executor.shutdownNow();
    }
  }
}
