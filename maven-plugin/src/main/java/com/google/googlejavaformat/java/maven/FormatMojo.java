package com.google.googlejavaformat.java.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.JavaFormatterOptions;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;

/**
 * Applies coding style formatting on the Java sources in the configured Maven project using Google
 * Java Format. Note that not all the command line options are available through this Maven plugin.
 */
@Mojo(
  name = "format",
  defaultPhase = LifecyclePhase.PROCESS_SOURCES,
  threadSafe = false,
  aggregator = true,
  requiresReports = false
)
public class FormatMojo extends AbstractMojo {

  private static final String JAVA_SOURCE_EXTENSION = ".java";

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  /**
   * The coding style to be applied to the sources of the project Java sources. The default style is
   * {@value Style#GOOGLE}. The value is not case-sensitive.
   */
  @Parameter(defaultValue = "google", property = "gjf.style", required = false)
  private String style;

  /**
   * Whether to fail the build or not when formatting the sources fail. The default is {@code
   * false}. If the configuration is invalid, it always breaks the build.
   */
  @Parameter(defaultValue = "false", property = "gjf.failOnError", required = false)
  private boolean failOnError = false;

  /* (non-Javadoc)
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  public void execute() throws MojoExecutionException, MojoFailureException {
    final long start = System.currentTimeMillis();
    List<String> sourceRoots = Lists.newArrayList();
    sourceRoots.addAll(project.getCompileSourceRoots());
    sourceRoots.addAll(project.getTestCompileSourceRoots());
    format(sourceRoots, buildJavaFormatterOptions());
    getLog().info("Formatting completed in " + (System.currentTimeMillis() - start) + "ms");
  }

  /**
   * Runs {@link FormatterException} on the provided source roots. Java sources in the Maven project
   * are <i>concurrently</i> collected. Formatting is also <i>concurrently</i> executed.
   *
   * @param sourceRoots the {@link List} of root of Java source directories
   * @param javaFormatterOptions the options configured for the Maven plugin for {@link Formatter}
   * @throws MojoExecutionException thrown if concurrent execution of collecting Java sources or
   *     formatting fails, or an {@link IOException} when find Java sources or reading Java sources.
   */
  protected void format(List<String> sourceRoots, JavaFormatterOptions javaFormatterOptions)
      throws MojoExecutionException {
    try {
      List<Path> sources = findJavaSources(sourceRoots);
      getLog().info("Formatting " + sources.size() + " Java sources ...");
      format(new Formatter(javaFormatterOptions), sources);
    } catch (ExecutionException e) {
      onException("Failed formatting sources: " + e.getCause().getMessage(), e.getCause());
    } catch (InterruptedException e) {
      onException("Formatting interrupted.", e);
    } catch (IOException e) {
      onException("I/O error during formatting: " + e.getMessage(), e);
    }
  }

  /**
   * Find all Java source files from a list of directories. A Java source file is presumed to have
   * the extension {@code ".java"}.
   *
   * @param sourceRoots the {@link List} of directory paths to collect files from
   * @return the {@link List} of collected Java source files
   * @throws InterruptedException thrown if concurrent execution is interrupted
   * @throws ExecutionException thrown as soon as an exception occurs in collecting Java sources
   * @throws IOException thrown if a directory or file cannot be read
   */
  protected List<Path> findJavaSources(List<String> sourceRoots)
      throws InterruptedException, ExecutionException, IOException {
    List<Path> sources = Lists.newLinkedList();
    for (String sourceRoot : sourceRoots) {
      Path pathRoot = Paths.get(sourceRoot);
      if (!Files.isDirectory(pathRoot)
          || Iterators.size(Files.newDirectoryStream(pathRoot).iterator()) == 0) {
        getLog().info("Skipping non-existing or empty directory: " + pathRoot);
        continue;
      }
      sources.add(pathRoot);
    }
    return FileCollector.collectAll(sources, JAVA_SOURCE_EXTENSION);
  }

  /**
   * Build {@link JavaFormatterOptions} based on configuration. The build always fails if the
   * configuration is not valid regardless if {@code failOnError} is {@code false}.
   *
   * @return an instance of {@link JavaFormatterOptions}
   * @throws MojoExecutionException thrown if the configuration is invalid
   */
  protected JavaFormatterOptions buildJavaFormatterOptions() throws MojoExecutionException {
    try {
      return JavaFormatterOptions.builder().style(Style.valueOf(this.style.toUpperCase())).build();
    } catch (Exception e) {
      throw new MojoExecutionException("Invalid configuration: ", e);
    }
  }

  /**
   * Runs the formatter against all the given source files.
   *
   * @param formatter the {@link Formatter} instance
   * @param sources the {@link List} of Java source {@link Path}s
   * @throws InterruptedException thrown if the concurrent execution of formatting is interrupted
   * @throws ExecutionException thrown as soon as formatting fails for one of the source files
   */
  protected void format(Formatter formatter, List<Path> sources)
      throws InterruptedException, ExecutionException {
    List<SourceFileFormatTask> sourceFileFormatTasks =
        createSourceFileFormatTasks(sources, formatter);
    Executor.execute(sourceFileFormatTasks);
  }

  /**
   * Create a list of formatting tasks for concurrent execution of {@link Formatter}.
   *
   * @param sources the {@link List} of Java source {@link Path}s
   * @param formatter the {@link Formatter} instance
   * @return a {@link List} of {@link SourceFileFormatTask}
   */
  protected List<SourceFileFormatTask> createSourceFileFormatTasks(
      List<Path> sources, Formatter formatter) {
    List<SourceFileFormatTask> tasks = Lists.newLinkedList();
    for (Path source : sources) {
      tasks.add(new SourceFileFormatTask(source, formatter));
    }
    return tasks;
  }

  /**
   * Checks if {@code failOnError} and either throws an instance of {@link MojoExecutionException}
   * or just logs a message without breaking the build.
   *
   * @param logMessage the message to log
   * @param e the exception to be thrown as the cause
   * @throws MojoExecutionException thrown to break the build and contains the cause of failure
   */
  protected void onException(String logMessage, Throwable e) throws MojoExecutionException {
    if (failOnError) {
      throw new MojoExecutionException(logMessage, e);
    } else {
      getLog().error(logMessage);
    }
  }
}
