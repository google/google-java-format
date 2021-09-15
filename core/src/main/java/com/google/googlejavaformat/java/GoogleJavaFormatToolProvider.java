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

import com.google.auto.service.AutoService;
import java.io.PrintWriter;
import java.util.spi.ToolProvider;

/** Provide a way to be invoked without necessarily starting a new VM. */
@AutoService(ToolProvider.class)
public class GoogleJavaFormatToolProvider implements ToolProvider {
  @Override
  public String name() {
    return "google-java-format";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      return Main.main(out, err, args);
    } catch (RuntimeException e) {
      err.print(e.getMessage());
      return -1; // pass non-zero value back indicating an error has happened
    }
  }
}
