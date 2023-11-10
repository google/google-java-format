package com.google.googlejavaformat.java.java21;

import static com.google.common.collect.Iterables.getOnlyElement;

import com.google.googlejavaformat.OpsBuilder;
import com.google.googlejavaformat.java.java17.Java17InputAstVisitor;
import com.sun.source.tree.*;
import java.util.List;

public class Java21InputAstVisitor extends Java17InputAstVisitor {

  public Java21InputAstVisitor(OpsBuilder builder, int indentMultiplier) {
    super(builder, indentMultiplier);
  }

  @Override
  public Void visitCase(CaseTree node, Void unused) {
    sync(node);
    markForPartialFormat();
    builder.forcedBreak();
    List<? extends CaseLabelTree> labels = node.getLabels();
    boolean isDefault =
        labels.size() == 1 && getOnlyElement(labels).getKind().name().equals("DEFAULT_CASE_LABEL");
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
    if (node.getGuard() != null) {
      builder.space();
      token("when");
      builder.space();
      scan(node.getGuard(), null);
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
}
