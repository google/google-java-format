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
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.base.Splitter;
import com.google.googlejavaformat.java.TypeNameClassifier.JavaCaseFormat;
import java.util.Optional;
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

  private static Optional<Integer> getPrefix(String qualifiedName) {
    return TypeNameClassifier.typePrefixLength(Splitter.on('.').splitToList(qualifiedName));
  }

  @Test
  public void typePrefixLength() {
    assertThat(getPrefix("fieldName")).isEmpty();
    assertThat(getPrefix("CONST")).isEmpty();
    assertThat(getPrefix("ClassName")).hasValue(0);
    assertThat(getPrefix("com.ClassName")).hasValue(1);
    assertThat(getPrefix("ClassName.foo")).hasValue(1);
    assertThat(getPrefix("com.ClassName.foo")).hasValue(2);
    assertThat(getPrefix("ClassName.foo.bar")).hasValue(1);
    assertThat(getPrefix("com.ClassName.foo.bar")).hasValue(2);
    assertThat(getPrefix("ClassName.CONST")).hasValue(1);
    assertThat(getPrefix("ClassName.varName")).hasValue(1);
    assertThat(getPrefix("ClassName.Inner.varName")).hasValue(2);
  }

  @Test
  public void ambiguousClass() {
    assertThat(getPrefix("com.google.security.acl.proto2api.ACL.Entry.newBuilder")).hasValue(7);
    // A human would probably identify this as "class-shaped", but just looking
    // at the case we have to assume it could be something like `field1.field2.CONST`.
    assertThat(getPrefix("com.google.security.acl.proto2api.ACL.newBuilder")).isEmpty();
  }
}
