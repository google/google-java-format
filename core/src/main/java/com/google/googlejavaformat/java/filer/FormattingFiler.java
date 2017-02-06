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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.googlejavaformat.java.Formatter;
import java.io.IOException;
import javax.annotation.Nullable;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

/**
 * A decorating {@link Filer} implementation which formats Java source files with a {@link
 * Formatter}.
 */
public final class FormattingFiler implements Filer {

  private final Filer delegate;
  // TODO(ronshapiro): consider allowing users to create their own Formatter instance
  private final Formatter formatter = new Formatter();
  private final Messager messager;

  /** @param delegate filer to decorate */
  public FormattingFiler(Filer delegate) {
    this(delegate, null);
  }

  /**
   * Create a new {@link FormattingFiler}. An optional {@link Messager} may be specified to make
   * logs more visible.
   *
   * @param delegate filer to decorate
   * @param messager to log warnings to
   */
  public FormattingFiler(Filer delegate, @Nullable Messager messager) {
    this.delegate = checkNotNull(delegate);
    this.messager = messager;
  }

  @Override
  public JavaFileObject createSourceFile(CharSequence name, Element... originatingElements)
      throws IOException {
    return new FormattingJavaFileObject(
        delegate.createSourceFile(name, originatingElements), formatter, messager);
  }

  @Override
  public JavaFileObject createClassFile(CharSequence name, Element... originatingElements)
      throws IOException {
    return delegate.createClassFile(name, originatingElements);
  }

  @Override
  public FileObject createResource(
      JavaFileManager.Location location,
      CharSequence pkg,
      CharSequence relativeName,
      Element... originatingElements)
      throws IOException {
    return delegate.createResource(location, pkg, relativeName, originatingElements);
  }

  @Override
  public FileObject getResource(
      JavaFileManager.Location location, CharSequence pkg, CharSequence relativeName)
      throws IOException {
    return delegate.getResource(location, pkg, relativeName);
  }
}
