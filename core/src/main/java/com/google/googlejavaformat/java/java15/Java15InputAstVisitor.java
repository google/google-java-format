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

package com.google.googlejavaformat.java.java15;

import com.google.googlejavaformat.OpsBuilder;
import com.google.googlejavaformat.java.java14.Java14InputAstVisitor;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import java.util.List;

/** Extends {@link Java14InputAstVisitor} with support for AST nodes that were added for Java 15. */
public class Java15InputAstVisitor extends Java14InputAstVisitor {

  public Java15InputAstVisitor(OpsBuilder builder, int indentMultiplier) {
    super(builder, indentMultiplier);
  }

  @Override
  protected List<? extends Tree> getPermitsClause(ClassTree node) {
    return node.getPermitsClause();
  }
}
