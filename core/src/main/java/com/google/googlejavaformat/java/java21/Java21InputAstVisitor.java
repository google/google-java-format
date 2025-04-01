/*
 * Copyright 2023 The google-java-format Authors.
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

package com.google.googlejavaformat.java.java21;

import com.google.googlejavaformat.OpsBuilder;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import com.google.googlejavaformat.java.JavaInputAstVisitor;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ConstantCaseLabelTree;
import com.sun.source.tree.DeconstructionPatternTree;
import com.sun.source.tree.DefaultCaseLabelTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.PatternCaseLabelTree;
import com.sun.source.tree.PatternTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import javax.lang.model.element.Name;

/**
 * Extends {@link JavaInputAstVisitor} with support for AST nodes that were added or modified in
 * Java 21.
 */
public class Java21InputAstVisitor extends JavaInputAstVisitor {

  public Java21InputAstVisitor(OpsBuilder builder, int indentMultiplier, Style style) {
    super(builder, indentMultiplier, style);
  }

  @Override
  protected ExpressionTree getGuard(final CaseTree node) {
    return node.getGuard();
  }

  @Override
  public Void visitDefaultCaseLabel(DefaultCaseLabelTree node, Void unused) {
    token("default");
    return null;
  }

  @Override
  public Void visitPatternCaseLabel(PatternCaseLabelTree node, Void unused) {
    scan(node.getPattern(), null);
    return null;
  }

  @Override
  public Void visitConstantCaseLabel(ConstantCaseLabelTree node, Void aVoid) {
    scan(node.getConstantExpression(), null);
    return null;
  }

  @Override
  public Void visitDeconstructionPattern(DeconstructionPatternTree node, Void unused) {
    scan(node.getDeconstructor(), null);
    builder.open(plusFour);
    token("(");
    builder.breakOp();
    boolean afterFirstToken = false;
    for (PatternTree pattern : node.getNestedPatterns()) {
      if (afterFirstToken) {
        token(",");
        builder.breakOp(" ");
      }
      afterFirstToken = true;
      scan(pattern, null);
    }
    builder.close();
    token(")");
    return null;
  }

  @Override
  protected void variableName(Name name) {
    if (name.isEmpty()) {
      token("_");
    } else {
      visit(name);
    }
  }

  @Override
  public Void scan(Tree tree, Void unused) {
    // Pre-visit AST for preview features, since com.sun.source.tree.AnyPattern can't be
    // accessed directly without --enable-preview.
    if (tree instanceof JCTree.JCAnyPattern) {
      visitJcAnyPattern((JCTree.JCAnyPattern) tree);
      return null;
    } else {
      return super.scan(tree, null);
    }
  }

  private void visitJcAnyPattern(JCTree.JCAnyPattern unused) {
    token("_");
  }
}
