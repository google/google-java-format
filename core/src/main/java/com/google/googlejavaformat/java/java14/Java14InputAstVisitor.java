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
import static com.google.common.collect.Iterables.getOnlyElement;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.googlejavaformat.OpsBuilder;
import com.google.googlejavaformat.OpsBuilder.BlankLineWanted;
import com.google.googlejavaformat.java.JavaInputAstVisitor;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BindingPatternTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
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
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Name;

/**
 * Extends {@link JavaInputAstVisitor} with support for AST nodes that were added or modified for
 * Java 14.
 */
public class Java14InputAstVisitor extends JavaInputAstVisitor {
  private static final Method COMPILATION_UNIT_TREE_GET_MODULE =
      maybeGetMethod(CompilationUnitTree.class, "getModule");
  private static final Method CLASS_TREE_GET_PERMITS_CLAUSE =
      maybeGetMethod(ClassTree.class, "getPermitsClause");
  private static final Method BINDING_PATTERN_TREE_GET_VARIABLE =
      maybeGetMethod(BindingPatternTree.class, "getVariable");
  private static final Method BINDING_PATTERN_TREE_GET_TYPE =
      maybeGetMethod(BindingPatternTree.class, "getType");
  private static final Method BINDING_PATTERN_TREE_GET_BINDING =
      maybeGetMethod(BindingPatternTree.class, "getBinding");
  private static final Method CASE_TREE_GET_LABELS = maybeGetMethod(CaseTree.class, "getLabels");

  public Java14InputAstVisitor(OpsBuilder builder, int indentMultiplier) {
    super(builder, indentMultiplier);
  }

  @Override
  protected void handleModule(boolean first, CompilationUnitTree node) {
    if (COMPILATION_UNIT_TREE_GET_MODULE == null) {
      // Java < 17, see https://bugs.openjdk.java.net/browse/JDK-8255464
      return;
    }
    ModuleTree module = (ModuleTree) invoke(COMPILATION_UNIT_TREE_GET_MODULE, node);
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
    if (CLASS_TREE_GET_PERMITS_CLAUSE != null) {
      return (List<? extends Tree>) invoke(CLASS_TREE_GET_PERMITS_CLAUSE, node);
    } else {
      // Java < 15
      return super.getPermitsClause(node);
    }
  }

  @Override
  public Void visitBindingPattern(BindingPatternTree node, Void unused) {
    sync(node);
    if (BINDING_PATTERN_TREE_GET_VARIABLE != null) {
      VariableTree variableTree = (VariableTree) invoke(BINDING_PATTERN_TREE_GET_VARIABLE, node);
      visitBindingPattern(
          variableTree.getModifiers(), variableTree.getType(), variableTree.getName());
    } else if (BINDING_PATTERN_TREE_GET_TYPE != null && BINDING_PATTERN_TREE_GET_BINDING != null) {
      Tree type = (Tree) invoke(BINDING_PATTERN_TREE_GET_TYPE, node);
      Name name = (Name) invoke(BINDING_PATTERN_TREE_GET_BINDING, node);
      visitBindingPattern(/* modifiers= */ null, type, name);
    } else {
      throw new LinkageError(
          "BindingPatternTree must have either getVariable() or both getType() and getBinding(),"
              + " but does not");
    }
    return null;
  }

  private void visitBindingPattern(ModifiersTree modifiers, Tree type, Name name) {
    if (modifiers != null) {
      List<AnnotationTree> annotations =
          visitModifiers(modifiers, Direction.HORIZONTAL, Optional.empty());
      visitAnnotations(annotations, BreakOrNot.NO, BreakOrNot.YES);
    }
    scan(type, null);
    builder.breakOp(" ");
    visit(name);
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
    List<? extends Tree> labels;
    boolean isDefault;
    if (CASE_TREE_GET_LABELS != null) {
      labels = (List<? extends Tree>) invoke(CASE_TREE_GET_LABELS, node);
      isDefault =
          labels.size() == 1
              && getOnlyElement(labels).getKind().name().equals("DEFAULT_CASE_LABEL");
    } else {
      labels = node.getExpressions();
      isDefault = labels.isEmpty();
    }
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
        if (node.getBody().getKind() == Tree.Kind.BLOCK) {
          // Explicit call with {@link CollapseEmptyOrNot.YES} to handle empty case blocks.
          visitBlock(
              (BlockTree) node.getBody(),
              CollapseEmptyOrNot.YES,
              AllowLeadingBlankLine.NO,
              AllowTrailingBlankLine.NO);
        } else {
          scan(node.getBody(), null);
        }
        builder.guessToken(";");
        break;
      default:
        throw new AssertionError(node.getCaseKind());
    }
    return null;
  }

  private static Method maybeGetMethod(Class<?> c, String name) {
    try {
      return c.getMethod(name);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  private static Object invoke(Method m, Object target) {
    try {
      return m.invoke(target);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }
}
