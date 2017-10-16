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

package com.google.googlejavaformat.java;

import java.io.IOError;
import java.io.IOException;
import org.openjdk.javax.lang.model.element.Name;
import org.openjdk.source.tree.ClassTree;
import org.openjdk.source.tree.CompoundAssignmentTree;
import org.openjdk.source.tree.ExpressionTree;
import org.openjdk.source.tree.IdentifierTree;
import org.openjdk.source.tree.MemberSelectTree;
import org.openjdk.source.tree.MethodInvocationTree;
import org.openjdk.source.tree.ParenthesizedTree;
import org.openjdk.source.tree.Tree;
import org.openjdk.source.util.TreePath;
import org.openjdk.tools.javac.tree.JCTree;
import org.openjdk.tools.javac.tree.Pretty;
import org.openjdk.tools.javac.tree.TreeInfo;

/** Utilities for working with {@link Tree}s. */
class Trees {
  /** Returns the length of the source for the node. */
  static int getLength(Tree tree, TreePath path) {
    return getEndPosition(tree, path) - getStartPosition(tree);
  }

  /** Returns the source start position of the node. */
  static int getStartPosition(Tree expression) {
    return ((JCTree) expression).getStartPosition();
  }

  /** Returns the source end position of the node. */
  static int getEndPosition(Tree expression, TreePath path) {
    return ((JCTree) expression)
        .getEndPosition(((JCTree.JCCompilationUnit) path.getCompilationUnit()).endPositions);
  }

  /** Returns the source text for the node. */
  static String getSourceForNode(Tree node, TreePath path) {
    CharSequence source;
    try {
      source = path.getCompilationUnit().getSourceFile().getCharContent(false);
    } catch (IOException e) {
      throw new IOError(e);
    }
    return source.subSequence(getStartPosition(node), getEndPosition(node, path)).toString();
  }

  /** Returns the simple name of a (possibly qualified) method invocation expression. */
  static Name getMethodName(MethodInvocationTree methodInvocation) {
    ExpressionTree select = methodInvocation.getMethodSelect();
    return select instanceof MemberSelectTree
        ? ((MemberSelectTree) select).getIdentifier()
        : ((IdentifierTree) select).getName();
  }

  /** Returns the receiver of a qualified method invocation expression, or {@code null}. */
  static ExpressionTree getMethodReceiver(MethodInvocationTree methodInvocation) {
    ExpressionTree select = methodInvocation.getMethodSelect();
    return select instanceof MemberSelectTree ? ((MemberSelectTree) select).getExpression() : null;
  }

  /** Returns the string name of an operator, including assignment and compound assignment. */
  static String operatorName(ExpressionTree expression) {
    JCTree.Tag tag = ((JCTree) expression).getTag();
    if (tag == JCTree.Tag.ASSIGN) {
      return "=";
    }
    boolean assignOp = expression instanceof CompoundAssignmentTree;
    if (assignOp) {
      tag = tag.noAssignOp();
    }
    String name = new Pretty(/*writer*/ null, /*sourceOutput*/ true).operatorName(tag);
    return assignOp ? name + "=" : name;
  }

  /** Returns the precedence of an expression's operator. */
  static int precedence(ExpressionTree expression) {
    return TreeInfo.opPrec(((JCTree) expression).getTag());
  }

  /**
   * Returns the enclosing type declaration (class, enum, interface, or annotation) for the given
   * path.
   */
  static ClassTree getEnclosingTypeDeclaration(TreePath path) {
    for (; path != null; path = path.getParentPath()) {
      switch (path.getLeaf().getKind()) {
        case CLASS:
        case ENUM:
        case INTERFACE:
        case ANNOTATED_TYPE:
          return (ClassTree) path.getLeaf();
        default:
          break;
      }
    }
    throw new AssertionError();
  }

  /** Skips a single parenthesized tree. */
  static ExpressionTree skipParen(ExpressionTree node) {
    return ((ParenthesizedTree) node).getExpression();
  }
}
