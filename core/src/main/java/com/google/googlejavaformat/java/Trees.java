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

import static com.google.googlejavaformat.java.Trees.getEndPosition;
import static com.google.googlejavaformat.java.Trees.getStartPosition;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import javax.lang.model.element.Name;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;

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
    return getEndPosition(expression, path.getCompilationUnit());
  }

  /** Returns the source end position of the node. */
  public static int getEndPosition(Tree tree, CompilationUnitTree unit) {
    return ((JCTree) tree).getEndPosition(((JCCompilationUnit) unit).endPositions);
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
        case CLASS, ENUM, INTERFACE, ANNOTATED_TYPE -> {
          return (ClassTree) path.getLeaf();
        }
        default -> {}
      }
    }
    throw new AssertionError();
  }

  /** Skips a single parenthesized tree. */
  static ExpressionTree skipParen(ExpressionTree node) {
    return ((ParenthesizedTree) node).getExpression();
  }

  static JCCompilationUnit parse(
      Context context,
      List<Diagnostic<? extends JavaFileObject>> errorDiagnostics,
      boolean allowStringFolding,
      String javaInput) {
    DiagnosticListener<JavaFileObject> diagnostics =
        diagnostic -> {
          if (errorDiagnostic(diagnostic)) {
            errorDiagnostics.add(diagnostic);
          }
        };
    context.put(DiagnosticListener.class, diagnostics);
    Options.instance(context).put("--enable-preview", "true");
    Options.instance(context).put("allowStringFolding", Boolean.toString(allowStringFolding));
    JavacFileManager fileManager = new JavacFileManager(context, /* register= */ true, UTF_8);
    try {
      fileManager.setLocation(StandardLocation.PLATFORM_CLASS_PATH, ImmutableList.of());
    } catch (IOException e) {
      // impossible
      throw new IOError(e);
    }
    SimpleJavaFileObject source =
        new SimpleJavaFileObject(URI.create("source"), JavaFileObject.Kind.SOURCE) {
          @Override
          public String getCharContent(boolean ignoreEncodingErrors) {
            return javaInput;
          }
        };
    Log.instance(context).useSource(source);
    ParserFactory parserFactory = ParserFactory.instance(context);
    JavacParser parser =
        parserFactory.newParser(
            javaInput,
            /* keepDocComments= */ true,
            /* keepEndPos= */ true,
            /* keepLineMap= */ true);
    JCCompilationUnit unit = parser.parseCompilationUnit();
    unit.sourcefile = source;
    return unit;
  }

  private static boolean errorDiagnostic(Diagnostic<?> input) {
    if (input.getKind() != Diagnostic.Kind.ERROR) {
      return false;
    }
    // accept constructor-like method declarations that don't match the name of their
    // enclosing class
    return !input.getCode().equals("compiler.err.invalid.meth.decl.ret.type.req");
  }
}
