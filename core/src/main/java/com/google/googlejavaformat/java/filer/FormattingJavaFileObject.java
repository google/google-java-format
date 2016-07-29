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

import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import java.io.IOException;
import java.io.Writer;
import javax.tools.ForwardingJavaFileObject;
import javax.tools.JavaFileObject;

/** A {@link JavaFileObject} decorator which {@linkplain Formatter formats} source code. */
final class FormattingJavaFileObject extends ForwardingJavaFileObject<JavaFileObject> {
  /** A rough estimate of the average file size: 80 chars per line, 500 lines. */
  private static final int DEFAULT_FILE_SIZE = 80 * 500;

  private final Formatter formatter;

  /** @param delegate {@link JavaFileObject} to decorate */
  FormattingJavaFileObject(JavaFileObject delegate, Formatter formatter) {
    super(checkNotNull(delegate));
    this.formatter = checkNotNull(formatter);
  }

  @Override
  public Writer openWriter() throws IOException {
    final StringBuilder stringBuilder = new StringBuilder(DEFAULT_FILE_SIZE);
    return new Writer() {
      @Override
      public void write(char[] chars, int start, int end) throws IOException {
        stringBuilder.append(chars, start, end - start);
      }

      @Override
      public void flush() throws IOException {}

      @Override
      public void close() throws IOException {
        try {
          formatter.formatSource(
              CharSource.wrap(stringBuilder),
              new CharSink() {
                @Override
                public Writer openStream() throws IOException {
                  return fileObject.openWriter();
                }
              });
        } catch (FormatterException e) {
          throw new IOException("Error formatting " + getName(), e);
        }
      }
    };
  }
}
