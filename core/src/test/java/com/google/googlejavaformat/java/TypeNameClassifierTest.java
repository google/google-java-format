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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Splitter;
import com.google.googlejavaformat.java.TypeNameClassifier.JavaCaseFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link TypeNameClassifier}Test */
@RunWith(JUnit4.class)
public final class TypeNameClassifierTest {
  @Test
  public void caseFormat() throws Exception {
    assertThat(JavaCaseFormat.from("CONST")).isEqualTo(JavaCaseFormat.UPPERCASE);
    assertThat(JavaCaseFormat.from("TypeName")).isEqualTo(JavaCaseFormat.UPPER_CAMEL);
    assertThat(JavaCaseFormat.from("fieldName")).isEqualTo(JavaCaseFormat.LOWER_CAMEL);
    assertThat(JavaCaseFormat.from("com")).isEqualTo(JavaCaseFormat.LOWERCASE);

    assertThat(JavaCaseFormat.from("CONST_$")).isEqualTo(JavaCaseFormat.UPPERCASE);
    assertThat(JavaCaseFormat.from("TypeName_$")).isEqualTo(JavaCaseFormat.UPPER_CAMEL);
    assertThat(JavaCaseFormat.from("fieldName_$")).isEqualTo(JavaCaseFormat.LOWER_CAMEL);
    assertThat(JavaCaseFormat.from("com_$")).isEqualTo(JavaCaseFormat.LOWERCASE);

    assertThat(JavaCaseFormat.from("A_$")).isEqualTo(JavaCaseFormat.UPPERCASE);
    assertThat(JavaCaseFormat.from("a_$")).isEqualTo(JavaCaseFormat.LOWERCASE);
    assertThat(JavaCaseFormat.from("_")).isEqualTo(JavaCaseFormat.LOWERCASE);
    assertThat(JavaCaseFormat.from("_A")).isEqualTo(JavaCaseFormat.UPPERCASE);
  }

  private static int getPrefix(String qualifiedName) {
    return TypeNameClassifier.typePrefixLength(Splitter.on('.').splitToList(qualifiedName));
  }

  @Test
  public void typePrefixLength() {
    assertThat(getPrefix("fieldName")).isEqualTo(-1);
    assertThat(getPrefix("CONST")).isEqualTo(-1);
    assertThat(getPrefix("ClassName")).isEqualTo(0);
    assertThat(getPrefix("com.ClassName")).isEqualTo(1);
    assertThat(getPrefix("ClassName.foo")).isEqualTo(1);
    assertThat(getPrefix("com.ClassName.foo")).isEqualTo(2);
    assertThat(getPrefix("ClassName.foo.bar")).isEqualTo(1);
    assertThat(getPrefix("com.ClassName.foo.bar")).isEqualTo(2);
    assertThat(getPrefix("ClassName.CONST")).isEqualTo(1);
    assertThat(getPrefix("ClassName.varName")).isEqualTo(1);
    assertThat(getPrefix("ClassName.Inner.varName")).isEqualTo(2);
  }

  @Test
  public void ambiguousClass() {
    assertThat(getPrefix("com.google.security.acl.proto2api.ACL.Entry.newBuilder")).isEqualTo(7);
    // A human would probably identify this as "class-shaped", but just looking
    // at the case we have to assume it could be something like `field1.field2.CONST`.
    assertThat(getPrefix("com.google.security.acl.proto2api.ACL.newBuilder")).isEqualTo(-1);
  }
}
