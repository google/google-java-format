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

package com.google.googlejavaformat.java.java17;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getOnlyElement;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.googlejavaformat.OpsBuilder;
import com.google.googlejavaformat.OpsBuilder.BlankLineWanted;
import com.google.googlejavaformat.java.JavaInputAstVisitor;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BindingPatternTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseLabelTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.YieldTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeInfo;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Name;

/**
 * Extends {@link JavaInputAstVisitor} with support for AST nodes that were added or modified in
 * Java 17.
 */
public class Java17InputAstVisitor extends JavaInputAstVisitor {

  public Java17InputAstVisitor(OpsBuilder builder, int indentMultiplier) {
    super(builder, indentMultiplier);
  }

  @Override
  protected void handleModule(boolean first, CompilationUnitTree node) {
    ModuleTree module = node.getModule();
    if (module != null) {
      if (!first) {
        builder.blankLineWanted(BlankLineWanted.YES);
      }
      markForPartialFormat();
      visitModule(module, null);
      builder.forcedBreak();
    }
  }

  @Override
  protected List<? extends Tree> getPermitsClause(ClassTree node) {
    return node.getPermitsClause();
  }

  @Override
  public Void visitBindingPattern(BindingPatternTree node, Void unused) {
    sync(node);
    VariableTree variableTree = node.getVariable();
    visitBindingPattern(
        variableTree.getModifiers(), variableTree.getType(), variableTree.getName());
    return null;
  }

  private void visitBindingPattern(ModifiersTree modifiers, Tree type, Name name) {
    builder.open(plusFour);
    if (modifiers != null) {
      List<AnnotationTree> annotations =
          visitModifiers(modifiers, Direction.HORIZONTAL, Optional.empty());
      visitAnnotations(annotations, BreakOrNot.NO, BreakOrNot.YES);
    }
    scan(type, null);
    builder.breakOp(" ");
    if (name.isEmpty()) {
      token("_");
    } else {
      visit(name);
    }
    builder.close();
  }

  @Override
  public Void visitYield(YieldTree node, Void aVoid) {
    sync(node);
    token("yield");
    builder.space();
    scan(node.getValue(), null);
    token(";");
    return null;
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
    typeDeclarationModifiers(node.getModifiers());
    Verify.verify(node.getExtendsClause() == null);
    boolean hasSuperInterfaceTypes = !node.getImplementsClause().isEmpty();
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
      ImmutableList<JCVariableDecl> parameters = recordVariables(node);
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
    List<? extends CaseLabelTree> labels = node.getLabels();
    boolean isDefault =
        labels.size() == 1 && getOnlyElement(labels).getKind().name().equals("DEFAULT_CASE_LABEL");
    builder.open(
        node.getCaseKind().equals(CaseTree.CaseKind.RULE)
                && !node.getBody().getKind().equals(Tree.Kind.BLOCK)
            ? plusFour
            : ZERO);
    if (isDefault) {
      token("default", plusTwo);
    } else {
      token("case", plusTwo);
      builder.open(labels.size() > 1 ? plusFour : ZERO);
      builder.space();
      boolean first = true;
      for (Tree expression : labels) {
        if (!first) {
          token(",");
          builder.breakOp(" ");
        }
        scan(expression, null);
        first = false;
      }
      builder.close();
    }

    final ExpressionTree guard = getGuard(node);
    if (guard != null) {
      builder.space();
      token("when");
      builder.space();
      scan(guard, null);
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
        if (node.getBody().getKind() == Tree.Kind.BLOCK) {
          builder.space();
          // Explicit call with {@link CollapseEmptyOrNot.YES} to handle empty case blocks.
          visitBlock(
              (BlockTree) node.getBody(),
              CollapseEmptyOrNot.YES,
              AllowLeadingBlankLine.NO,
              AllowTrailingBlankLine.NO);
        } else {
          builder.breakOp(" ");
          scan(node.getBody(), null);
        }
        builder.guessToken(";");
        break;
      default:
        throw new AssertionError(node.getCaseKind());
    }
    builder.close();
    return null;
  }

  protected ExpressionTree getGuard(final CaseTree node) {
    return null;
  }
}
