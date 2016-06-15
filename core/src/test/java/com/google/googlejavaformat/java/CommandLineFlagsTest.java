/*
 * Copyright 2015 Google Inc.
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

import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for command-line flags.
 */
@RunWith(JUnit4.class)
public class CommandLineFlagsTest {

  // TODO(eaftan): Disallow passing both -lines and -offset/-length, like clang-format.

  @Test
  public void formatInPlaceRequiresAtLeastOneFile() {
    try {
      Main.processArgs("-i");
      fail();
    } catch (UsageException e) {
      // expected
    }

    try {
      Main.processArgs("-i", "-");
      fail();
    } catch (UsageException e) {
      // expected
    }

    try {
      Main.processArgs("-i", "Foo.java");
      Main.processArgs("-i", "Foo.java", "Bar.java");
    } catch (UsageException e) {
      fail();
    }
  }

  @Test
  public void formatASubsetRequiresExactlyOneFile() {
    try {
      Main.processArgs("-lines", "10", "Foo.java");
    } catch (UsageException e) {
      fail();
    }

    try {
      Main.processArgs("-lines", "10");
      fail();
    } catch (UsageException e) {
      // expected
    }

    try {
      Main.processArgs("-lines", "10", "Foo.java", "Bar.java");
      fail();
    } catch (UsageException e) {
      // expected
    }

    try {
      Main.processArgs("-offset", "10", "-length", "10", "Foo.java");
    } catch (UsageException e) {
      fail();
    }

    try {
      Main.processArgs("-offset", "10", "-length", "10");
      fail();
    } catch (UsageException e) {
      // expected
    }

    try {
      Main.processArgs("-offset", "10", "-length", "10", "Foo.java", "Bar.java");
      fail();
    } catch (UsageException e) {
      // expected
    }
  }

  // TODO(eaftan): clang-format allows a single offset with no length, which means to format
  // up to the end of the file.  We should match that behavior.
  @Test
  public void numberOfOffsetsMustMatchNumberOfLengths() {
    try {
      Main.processArgs("-offset", "10", "-length", "20", "Foo.java");
    } catch (UsageException e) {
      fail();
    }

    try {
      Main.processArgs("-offset", "10", "-length", "20", "-offset", "50", "Foo.java");
      fail();
    } catch (UsageException e) {
      // expected
    }

    try {
      Main.processArgs("-offset", "10", "-length", "20", "-length", "50", "Foo.java");
      fail();
    } catch (UsageException e) {
      // expected
    }
  }

  @Test
  public void noFilesToFormatRequiresEitherHelpOrVersion() {
    try {
      Main.processArgs("-version");
    } catch (UsageException e) {
      fail();
    }

    try {
      Main.processArgs("-help");
    } catch (UsageException e) {
      fail();
    }

    try {
      Main.processArgs();
      fail();
    } catch (UsageException e) {
      // expected
    }

    try {
      Main.processArgs("-aosp");
      fail();
    } catch (UsageException e) {
      // expected
    }
  }
}
