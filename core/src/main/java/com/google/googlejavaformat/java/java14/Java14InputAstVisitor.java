/*
 * Copyright 2020 Google Inc.
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

package com.google.googlejavaformat.java.java14;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.MoreCollectors.toOptional;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.googlejavaformat.Op;
import com.google.googlejavaformat.OpsBuilder;
import com.google.googlejavaformat.java.JavaInputAstVisitor;
import com.sun.source.tree.BindingPatternTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.YieldTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeInfo;
import java.util.List;
import java.util.Optional;

/**
 * Extends {@link JavaInputAstVisitor} with support for AST nodes that were added or modified for
 * Java 14.
 */
public class Java14InputAstVisitor extends JavaInputAstVisitor {

  public Java14InputAstVisitor(OpsBuilder builder, int indentMultiplier) {
    super(builder, indentMultiplier);
  }

  @Override
  public Void visitBindingPattern(BindingPatternTree node, Void unused) {
    sync(node);
    scan(node.getType(), null);
    builder.breakOp(" ");
    visit(node.getBinding());
    return null;
  }

  @Override
  public Void visitYield(YieldTree node, Void aVoid) {
    sync(node);
    return super.visitYield(node, aVoid);
  }

  @Override
  public Void visitSwitchExpression(SwitchExpressionTree node, Void aVoid) {
    sync(node);
    visitSwitch(node.getExpression(), node.getCases());
    return null;
  }

  @Override
  public Void visitClass(ClassTree tree, Void unused) {
    switch (tree.getKind()) {
      case ANNOTATION_TYPE:
        visitAnnotationType(tree);
        break;
      case CLASS:
      case INTERFACE:
        visitClassDeclaration(tree);
        break;
      case ENUM:
        visitEnumDeclaration(tree);
        break;
      case RECORD:
        visitRecordDeclaration(tree);
        break;
      default:
        throw new AssertionError(tree.getKind());
    }
    return null;
  }

  public void visitRecordDeclaration(ClassTree node) {
    sync(node);
    List<Op> breaks =
        visitModifiers(
            node.getModifiers(),
            Direction.VERTICAL,
            /* declarationAnnotationBreak= */ Optional.empty());
    Verify.verify(node.getExtendsClause() == null);
    boolean hasSuperInterfaceTypes = !node.getImplementsClause().isEmpty();
    builder.addAll(breaks);
    token("record");
    builder.space();
    visit(node.getSimpleName());
    if (!node.getTypeParameters().isEmpty()) {
      token("<");
    }
    builder.open(plusFour);
    {
      if (!node.getTypeParameters().isEmpty()) {
        typeParametersRest(node.getTypeParameters(), hasSuperInterfaceTypes ? plusFour : ZERO);
      }
      ImmutableList<JCVariableDecl> parameters =
          compactRecordConstructor(node)
              .map(m -> ImmutableList.copyOf(m.getParameters()))
              .orElseGet(() -> recordVariables(node));
      token("(");
      if (!parameters.isEmpty()) {
        // Break before args.
        builder.breakToFill("");
      }
      // record headers can't declare receiver parameters
      visitFormals(/* receiver= */ Optional.empty(), parameters);
      token(")");
      if (hasSuperInterfaceTypes) {
        builder.breakToFill(" ");
        builder.open(node.getImplementsClause().size() > 1 ? plusFour : ZERO);
        token("implements");
        builder.space();
        boolean first = true;
        for (Tree superInterfaceType : node.getImplementsClause()) {
          if (!first) {
            token(",");
            builder.breakOp(" ");
          }
          scan(superInterfaceType, null);
          first = false;
        }
        builder.close();
      }
    }
    builder.close();
    if (node.getMembers() == null) {
      token(";");
    } else {
      List<Tree> members =
          node.getMembers().stream()
              .filter(t -> (TreeInfo.flags((JCTree) t) & Flags.GENERATED_MEMBER) == 0)
              .collect(toImmutableList());
      addBodyDeclarations(members, BracesOrNot.YES, FirstDeclarationsOrNot.YES);
    }
    dropEmptyDeclarations();
  }

  private static Optional<JCMethodDecl> compactRecordConstructor(ClassTree node) {
    return node.getMembers().stream()
        .filter(JCMethodDecl.class::isInstance)
        .map(JCMethodDecl.class::cast)
        .filter(m -> (m.mods.flags & COMPACT_RECORD_CONSTRUCTOR) == COMPACT_RECORD_CONSTRUCTOR)
        .collect(toOptional());
  }

  private static ImmutableList<JCVariableDecl> recordVariables(ClassTree node) {
    return node.getMembers().stream()
        .filter(JCVariableDecl.class::isInstance)
        .map(JCVariableDecl.class::cast)
        .filter(m -> (m.mods.flags & RECORD) == RECORD)
        .collect(toImmutableList());
  }

  @Override
  public Void visitInstanceOf(InstanceOfTree node, Void unused) {
    sync(node);
    builder.open(plusFour);
    scan(node.getExpression(), null);
    builder.breakOp(" ");
    builder.open(ZERO);
    token("instanceof");
    builder.breakOp(" ");
    if (node.getPattern() != null) {
      scan(node.getPattern(), null);
    } else {
      scan(node.getType(), null);
    }
    builder.close();
    builder.close();
    return null;
  }

  @Override
  public Void visitCase(CaseTree node, Void unused) {
    sync(node);
    markForPartialFormat();
    builder.forcedBreak();
    if (node.getExpressions().isEmpty()) {
      token("default", plusTwo);
    } else {
      token("case", plusTwo);
      builder.space();
      boolean first = true;
      for (ExpressionTree expression : node.getExpressions()) {
        if (!first) {
          token(",");
          builder.space();
        }
        scan(expression, null);
        first = false;
      }
    }
    switch (node.getCaseKind()) {
      case STATEMENT:
        token(":");
        builder.open(plusTwo);
        visitStatements(node.getStatements());
        builder.close();
        break;
      case RULE:
        builder.space();
        token("-");
        token(">");
        builder.space();
        scan(node.getBody(), null);
        token(";");
        break;
      default:
        throw new AssertionError(node.getCaseKind());
    }
    return null;
  }
}
