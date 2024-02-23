/*
 * Copyright 2021 Google Inc.
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

package com.google.googlejavaformat.java;

import static com.google.common.collect.Sets.toImmutableEnumSet;

import com.google.auto.service.AutoService;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.tools.Tool;

/** Provide a way to be invoked without necessarily starting a new VM. */
@AutoService(Tool.class)
public class GoogleJavaFormatTool implements Tool {
  @Override
  public String name() {
    return "google-java-format";
  }

  @Override
  public Set<SourceVersion> getSourceVersions() {
    return Arrays.stream(SourceVersion.values()).collect(toImmutableEnumSet());
  }

  @Override
  public int run(InputStream in, OutputStream out, OutputStream err, String... args) {
    PrintStream outStream = new PrintStream(out);
    PrintStream errStream = new PrintStream(err);
    try {
      return Main.main(in, outStream, errStream, args);
    } catch (RuntimeException e) {
      errStream.print(e.getMessage());
      errStream.flush();
      return 1; // pass non-zero value back indicating an error has happened
    }
  }
}
