package com.google.googlejavaformat.java.java21;

import com.google.googlejavaformat.OpsBuilder;
import com.google.googlejavaformat.java.java17.Java17InputAstVisitor;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;

public class Java21InputAstVisitor extends Java17InputAstVisitor {

  public Java21InputAstVisitor(OpsBuilder builder, int indentMultiplier) {
    super(builder, indentMultiplier);
  }

  @Override
  protected ExpressionTree getGuard(final CaseTree node) {
    return node.getGuard();
  }
}
