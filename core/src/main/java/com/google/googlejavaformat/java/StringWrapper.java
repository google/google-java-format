/*
 * Copyright 2019 Google Inc.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.googlejavaformat.java.Trees.getEndPosition;
import static com.google.googlejavaformat.java.Trees.getStartPosition;
import static java.util.stream.Collectors.joining;

import com.google.common.base.CharMatcher;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeMap;
import com.google.googlejavaformat.Newlines;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.YieldTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Position;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.jspecify.annotations.Nullable;

/// Wraps string literals that exceed the column limit.
public final class StringWrapper {

  private static final String TEXT_BLOCK_DELIMITER = "\"\"\"";

  /// Reflows long string literals in the given Java source code.
  public static String wrap(String input, Formatter formatter) throws FormatterException {
    return StringWrapper.wrap(formatter.options().maxLineLength(), input, formatter);
  }

  /// Reflows string literals in the given Java source code that extend past the given column limit.
  static String wrap(final int columnLimit, String input, Formatter formatter)
      throws FormatterException {
    return wrap(
        columnLimit, input, formatter, ImmutableRangeSet.of(Range.closedOpen(0, input.length())));
  }

  /// Reflows string literals in the given character ranges of the given Java source code that
  /// extend past the given column limit.
  static String wrap(
      final int columnLimit, String input, Formatter formatter, RangeSet<Integer> characterRanges)
      throws FormatterException {
    return wrap(
        columnLimit,
        input,
        formatter,
        characterRanges,
        /* parsedUnit= */ null,
        /* alreadyFormattedWholeFile= */ false);
  }

  static String wrap(
      final int columnLimit,
      String input,
      Formatter formatter,
      RangeSet<Integer> characterRanges,
      JCTree.@Nullable JCCompilationUnit parsedUnit,
      boolean alreadyFormattedWholeFile)
      throws FormatterException {
    JavaFormatterOptions options = formatter.options();
    if (!needWrapping(columnLimit, input, options, characterRanges)) {
      // fast path
      return input;
    }

    ReflowResult reflow =
        getReflowReplacements(columnLimit, input, options, characterRanges, parsedUnit);
    if (reflow.replacements().asMapOfRanges().isEmpty()) {
      return input;
    }
    if (!alreadyFormattedWholeFile) {
      Edit firstPass =
          Edit.apply(
              input,
              formatter.getFormatReplacements(
                  input,
                  reflow.replacements().asMapOfRanges().keySet(),
                  /* reindent= */ false,
                  reflow.unit()));

      if (!firstPass.contents().equals(input)) {
        // If formatting the replacement ranges resulted in a change, recalculate the replacements
        // on the updated input.
        input = firstPass.contents();
        characterRanges = firstPass.mapRanges(characterRanges.asRanges());
        reflow =
            getReflowReplacements(
                columnLimit, input, options, characterRanges, /* parsedUnit= */ null);
        if (reflow.replacements().asMapOfRanges().isEmpty()) {
          return input;
        }
      }
    }

    String result = applyReplacements(input, reflow.replacements());

    verifyReplacements(input, result, reflow);

    return result;
  }

  /// Verifies that reflowing string literals did not produce unparseable code or change the AST.
  ///
  /// When all replacements occur in syntactic contexts where replacing a string literal with a
  /// string `+` concatenation cannot change operator precedence, we parse a tiny synthetic class
  /// containing only the replacement expressions (with `allowStringFolding=true`) and verify each
  /// folds to the exact original string literal value. Otherwise, we fall back to full-file AST
  /// comparison.
  private static void verifyReplacements(String input, String result, ReflowResult reflow)
      throws FormatterException {
    if (reflow.needsFullAstVerification()) {
      verifyFullAst(input, result);
      return;
    }
    Map<Range<Integer>, String> replacementMap = reflow.replacements().asMapOfRanges();
    StringBuilder synthetic = new StringBuilder();
    synthetic.append("class C {\n");
    List<String> expectedValues = new ArrayList<>(replacementMap.size());
    int idx = 0;
    for (Map.Entry<Range<Integer>, String> entry : replacementMap.entrySet()) {
      synthetic
          .append("  String s")
          .append(idx++)
          .append(" = ")
          .append(entry.getValue())
          .append(";\n");
      expectedValues.add(reflow.expectedValues().get(entry.getKey()));
    }
    synthetic.append("}\n");

    JCTree.JCCompilationUnit syntheticUnit;
    try {
      syntheticUnit = parse(synthetic.toString(), /* allowStringFolding= */ true);
    } catch (FormatterException e) {
      throw somethingHasGoneTerriblyWrong(
          "it would produce code that does not parse (" + e.getMessage() + ")", result, input);
    }

    if (syntheticUnit.getTypeDecls().size() == 1
        && syntheticUnit.getTypeDecls().get(0) instanceof ClassTree classTree
        && classTree.getMembers().size() == expectedValues.size()) {
      boolean allMatch = true;
      for (int i = 0; i < expectedValues.size(); i++) {
        Tree member = classTree.getMembers().get(i);
        if (member instanceof VariableTree varTree
            && varTree.getInitializer() instanceof LiteralTree literalTree
            && literalTree.getKind() == Kind.STRING_LITERAL
            && expectedValues.get(i).equals(literalTree.getValue())) {
          continue;
        }
        allMatch = false;
        break;
      }
      if (allMatch) {
        return;
      }
    }
    // Unexpected mismatch in synthetic verification; run full AST comparison to produce the full
    // diagnostic diff.
    verifyFullAst(input, result);
  }

  private static void verifyFullAst(String input, String result) throws FormatterException {
    // We really don't want bugs in this pass to change the behaviour of programs we're
    // formatting, so check that the pretty-printed AST is the same before and after reformatting.
    String expected = parse(input, /* allowStringFolding= */ true).toString();
    String actual;
    try {
      actual = parse(result, /* allowStringFolding= */ true).toString();
    } catch (FormatterException e) {
      // Diagnostics for the reflowed output don't correspond to positions in the original input,
      // and the error is a bug in this pass rather than in the input, so report it as such.
      throw somethingHasGoneTerriblyWrong(
          "it would produce code that does not parse (" + e.getMessage() + ")", result, input);
    }
    if (!expected.equals(actual)) {
      throw somethingHasGoneTerriblyWrong("it would unexpectedly change the AST", actual, expected);
    }
  }

  private static FormatterException somethingHasGoneTerriblyWrong(
      String reason, String actual, String expected) {
    return new FormatterException(
        String.format(
            "Something has gone terribly wrong. We planned to make the below formatting change,"
                + " but have aborted because %s.\n"
                + "Please file a bug: "
                + "https://github.com/google/google-java-format/issues/new"
                + "\n\n=== Actual: ===\n%s\n=== Expected: ===\n%s\n",
            reason, actual, expected));
  }

  private record ReflowResult(
      TreeRangeMap<Integer, String> replacements,
      JCTree.JCCompilationUnit unit,
      Map<Range<Integer>, String> expectedValues,
      boolean needsFullAstVerification) {}

  private static ReflowResult getReflowReplacements(
      int columnLimit,
      final String input,
      JavaFormatterOptions options,
      RangeSet<Integer> characterRanges,
      JCTree.@Nullable JCCompilationUnit parsedUnit)
      throws FormatterException {
    return new Reflower(columnLimit, input, options, characterRanges, parsedUnit)
        .getReflowReplacements();
  }

  private static class Reflower {

    private final String input;
    private final int columnLimit;
    private final JavaFormatterOptions options;
    private final RangeSet<Integer> characterRanges;
    private final String separator;
    private final JCTree.JCCompilationUnit unit;
    private final Position.LineMap lineMap;

    Reflower(
        int columnLimit,
        String input,
        JavaFormatterOptions options,
        RangeSet<Integer> characterRanges,
        JCTree.@Nullable JCCompilationUnit parsedUnit)
        throws FormatterException {
      this.columnLimit = columnLimit;
      this.input = input;
      this.options = options;
      this.characterRanges = characterRanges;
      this.separator = Newlines.guessLineSeparator(input);
      this.unit = parsedUnit != null ? parsedUnit : parse(input, /* allowStringFolding= */ false);
      this.lineMap = unit.getLineMap();
    }

    private int visualColumn(int position) {
      int lineStart = lineMap.getStartPosition(lineMap.getLineNumber(position));
      return options.visualLength(input, lineStart, position);
    }

    ReflowResult getReflowReplacements() {
      // Paths to string literals that extend past the column limit.
      List<TreePath> longStringLiterals = new ArrayList<>();
      new LongStringsScanner(longStringLiterals).scan(new TreePath(unit), null);
      TreeRangeMap<Integer, String> replacements = TreeRangeMap.create();
      Map<Range<Integer>, String> expectedValues = new LinkedHashMap<>();
      boolean needsFullAstVerification =
          wrapLongStrings(replacements, expectedValues, longStringLiterals);
      return new ReflowResult(replacements, unit, expectedValues, needsFullAstVerification);
    }

    private class LongStringsScanner extends TreePathScanner<Void, Void> {

      private final List<TreePath> longStringLiterals;

      LongStringsScanner(List<TreePath> longStringLiterals) {
        this.longStringLiterals = longStringLiterals;
      }

      @Override
      public Void visitLiteral(LiteralTree literalTree, Void aVoid) {
        if (literalTree.getKind() != Kind.STRING_LITERAL) {
          return null;
        }
        if (input.startsWith(TEXT_BLOCK_DELIMITER, getStartPosition(literalTree))) {
          return null;
        }
        Tree parent = getCurrentPath().getParentPath().getLeaf();
        if (parent instanceof MemberSelectTree memberSelectTree
            && memberSelectTree.getExpression().equals(literalTree)) {
          return null;
        }
        int endPosition = getEndPosition(literalTree, unit);
        if (!characterRanges.intersects(
            Range.closedOpen(getStartPosition(literalTree), endPosition))) {
          return null;
        }
        int lineEnd = endPosition;
        while (Newlines.hasNewlineAt(input, lineEnd) == -1) {
          lineEnd++;
        }
        if (visualColumn(lineEnd) <= columnLimit) {
          return null;
        }
        longStringLiterals.add(getCurrentPath());
        return null;
      }
    }

    private boolean wrapLongStrings(
        TreeRangeMap<Integer, String> replacements,
        Map<Range<Integer>, String> expectedValues,
        List<TreePath> longStringLiterals) {
      boolean needsFullAstVerification = false;
      for (TreePath path : longStringLiterals) {
        // Find the outermost contiguous enclosing concatenation expression
        TreePath enclosing = path;
        while (enclosing.getParentPath().getLeaf().getKind() == Kind.PLUS) {
          enclosing = enclosing.getParentPath();
        }
        // Is the literal being wrapped the first in a chain of concatenation expressions?
        // i.e. `ONE + TWO + THREE`
        // We need this information to handle continuation indents.
        AtomicBoolean first = new AtomicBoolean(false);
        // Finds the set of string literals in the concat expression that includes the one that
        // needs to be wrapped.
        List<Tree> flat = flatten(input, unit, path, enclosing, first);
        Range<Integer> replaceRange =
            Range.closedOpen(getStartPosition(flat.get(0)), getEndPosition(getLast(flat), unit));
        if (replacements.get(replaceRange.lowerEndpoint()) != null) {
          continue;
        }
        if (!isSafeParentForConcat(enclosing)) {
          needsFullAstVerification = true;
        }
        // Zero-indexed start column
        int startColumn = visualColumn(getStartPosition(flat.get(0)));

        // Handling leaving trailing non-string tokens at the end of the literal,
        // e.g. the trailing `);` in `foo("...");`.
        int end = getEndPosition(getLast(flat), unit);
        int lineEnd = end;
        while (Newlines.hasNewlineAt(input, lineEnd) == -1) {
          lineEnd++;
        }
        int trailing = lineEnd - end;

        // Get the original source text of the string literals, excluding `"` and `+`.
        ImmutableList<String> components = stringComponents(input, unit, flat);
        replacements.put(
            replaceRange,
            reflow(
                separator, columnLimit, startColumn, trailing, components, first.get(), options));
        StringBuilder expected = new StringBuilder();
        for (Tree literal : flat) {
          expected.append(((LiteralTree) literal).getValue());
        }
        expectedValues.put(replaceRange, expected.toString());
      }
      return needsFullAstVerification;
    }
  }

  /// Returns true if replacing a string literal (or `+` chain of string literals) with a `+`
  /// concatenation under `enclosing`'s parent cannot alter surrounding operator precedence.
  private static boolean isSafeParentForConcat(TreePath enclosing) {
    TreePath parentPath = enclosing.getParentPath();
    if (parentPath == null) {
      return false;
    }
    Tree parent = parentPath.getLeaf();
    if (parent instanceof BinaryTree binaryTree) {
      return switch (binaryTree.getKind()) {
        case MULTIPLY, DIVIDE, REMAINDER, MINUS, PLUS -> false;
        default -> true;
      };
    }
    return parent instanceof VariableTree
        || parent instanceof AssignmentTree
        || parent instanceof CompoundAssignmentTree
        || parent instanceof MethodInvocationTree
        || (parent instanceof NewClassTree newClassTree
            && newClassTree.getArguments().contains(enclosing.getLeaf()))
        || parent instanceof ReturnTree
        || parent instanceof ExpressionStatementTree
        || parent instanceof ConditionalExpressionTree
        || parent instanceof ParenthesizedTree
        || parent instanceof NewArrayTree
        || parent instanceof AnnotationTree
        || parent instanceof CaseTree
        || parent instanceof YieldTree
        || parent instanceof ThrowTree
        || parent instanceof AssertTree
        || parent instanceof LambdaExpressionTree
        || parent instanceof SwitchExpressionTree;
  }

  /// Returns the source text of the given string literal trees, excluding the leading and trailing
  /// double-quotes and the `+` operator.
  private static ImmutableList<String> stringComponents(
      String input, JCTree.JCCompilationUnit unit, List<Tree> flat) {
    ImmutableList.Builder<String> result = ImmutableList.builder();
    StringBuilder piece = new StringBuilder();
    for (Tree tree : flat) {
      // adjust for leading and trailing double quotes
      String text = input.substring(getStartPosition(tree) + 1, getEndPosition(tree, unit) - 1);
      int start = 0;
      int next;
      for (int idx = 0; idx < text.length(); idx = next) {
        int splitAt;
        int length;
        if (CharMatcher.whitespace().matches(text.charAt(idx))) {
          // split before whitespace
          splitAt = idx;
          next = idx + 1;
        } else if ((length = hasEscapedWhitespaceAt(text, idx)) != -1) {
          // split before escaped whitespace
          splitAt = idx;
          next = idx + length;
        } else if (hasEscapedNewlineAt(text, idx) != -1) {
          // Split after a run of escaped newlines, rather than before it, so the newlines end the
          // preceding piece. `reflow` ends a line after a piece ending in an escaped newline, so
          // where possible each line of the wrapped literal corresponds to a line of its value.
          next = idx;
          while ((length = hasEscapedNewlineAt(text, next)) != -1) {
            next += length;
          }
          splitAt = next;
        } else {
          // Skip over the entire escape sequence, so the second character of an escape like `\\` is
          // not mistaken for the start of another escape. Octal and Unicode escapes don't need
          // special handling: after the backslash they only contain letters and digits, which are
          // never split points.
          next = idx + (text.charAt(idx) == '\\' ? 2 : 1);
          continue;
        }
        if (splitAt > start || piece.length() > 0) {
          piece.append(text, start, splitAt);
          result.add(piece.toString());
          piece = new StringBuilder();
          start = splitAt;
        }
      }
      if (piece.length() > 0) {
        result.add(piece.toString());
        piece = new StringBuilder();
      }
      if (start < text.length()) {
        piece.append(text, start, text.length());
      }
    }
    if (piece.length() > 0) {
      result.add(piece.toString());
    }
    return result.build();
  }

  private static int hasEscapedWhitespaceAt(String input, int idx) {
    if (input.startsWith("\\t", idx)) {
      return 2;
    }
    return -1;
  }

  private static int hasEscapedNewlineAt(String input, int idx) {
    int offset = 0;
    if (input.startsWith("\\r", idx)) {
      offset += 2;
    }
    if (input.startsWith("\\n", idx)) {
      offset += 2;
    }
    return offset > 0 ? offset : -1;
  }

  /// Reflows the given source text, trying to split on word boundaries.
  ///
  /// @param separator the line separator
  /// @param columnLimit the number of columns to wrap at
  /// @param startColumn the column position of the beginning of the original text
  /// @param trailing extra space to leave after the last line, to accommodate a ; or )
  /// @param components the text to reflow. This is a list of “words” of a single literal. Its first
  ///   and last quotes have been stripped
  /// @param first0 true if the text includes the beginning of its enclosing concat chain
  private static String reflow(
      String separator,
      int columnLimit,
      int startColumn,
      int trailing,
      ImmutableList<String> components,
      boolean first0,
      JavaFormatterOptions options) {
    // We have space between the start column and the limit to output the first line.
    // Reserve two spaces for the start and end quotes.
    int width = columnLimit - startColumn - 2;
    Deque<String> input = new ArrayDeque<>(components);
    List<String> lines = new ArrayList<>();
    boolean first = first0;
    while (!input.isEmpty()) {
      int length = 0;
      List<String> line = new ArrayList<>();
      // If we know this is going to be the last line, then remove a bit of width to account for the
      // trailing characters.
      if (totalLengthLessThanOrEqual(input, width)) {
        // This isn’t quite optimal, but arguably good enough. See b/179561701
        width -= trailing;
      }
      while (!input.isEmpty() && (length <= 4 || (length + input.peekFirst().length()) <= width)) {
        String text = input.removeFirst();
        line.add(text);
        length += text.length();
        if (text.endsWith("\\n") || text.endsWith("\\r")) {
          break;
        }
      }
      if (line.isEmpty()) {
        line.add(input.removeFirst());
      }
      // add the split line to the output, and process whatever's left
      lines.add(String.join("", line));
      if (first) {
        width -= 6; // subsequent lines have a four-space continuation indent and a `+ `
        first = false;
      }
    }

    return lines.stream()
        .collect(
            joining(
                "\"" + separator + options.indentString(startColumn + (first0 ? 4 : -2)) + "+ \"",
                "\"",
                "\""));
  }

  private static boolean totalLengthLessThanOrEqual(Iterable<String> input, int length) {
    int total = 0;
    for (String s : input) {
      total += s.length();
      if (total > length) {
        return false;
      }
    }
    return true;
  }

  /// Flattens the given binary expression tree, and extracts the subset that contains the given
  /// path and any adjacent nodes that are also string literals.
  private static List<Tree> flatten(
      String input,
      JCTree.JCCompilationUnit unit,
      TreePath path,
      TreePath parent,
      AtomicBoolean firstInChain) {
    List<Tree> flat = new ArrayList<>();

    // flatten the expression tree with a pre-order traversal
    ArrayDeque<Tree> todo = new ArrayDeque<>();
    todo.add(parent.getLeaf());
    while (!todo.isEmpty()) {
      Tree first = todo.removeFirst();
      if (first.getKind() == Tree.Kind.PLUS) {
        BinaryTree bt = (BinaryTree) first;
        todo.addFirst(bt.getRightOperand());
        todo.addFirst(bt.getLeftOperand());
      } else {
        flat.add(first);
      }
    }

    int idx = flat.indexOf(path.getLeaf());
    Verify.verify(idx != -1);

    // walk outwards from the leaf for adjacent string literals to also reflow
    int startIdx = idx;
    int endIdx = idx + 1;
    while (startIdx > 0
        && flat.get(startIdx - 1).getKind() == Tree.Kind.STRING_LITERAL
        && noComments(input, unit, flat.get(startIdx - 1), flat.get(startIdx))) {
      startIdx--;
    }
    while (endIdx < flat.size()
        && flat.get(endIdx).getKind() == Tree.Kind.STRING_LITERAL
        && noComments(input, unit, flat.get(endIdx - 1), flat.get(endIdx))) {
      endIdx++;
    }

    firstInChain.set(startIdx == 0);
    return ImmutableList.copyOf(flat.subList(startIdx, endIdx));
  }

  private static boolean noComments(
      String input, JCTree.JCCompilationUnit unit, Tree one, Tree two) {
    return STRING_CONCAT_DELIMITER.matchesAllOf(
        input.subSequence(getEndPosition(one, unit), getStartPosition(two)));
  }

  private static final CharMatcher STRING_CONCAT_DELIMITER =
      CharMatcher.whitespace().or(CharMatcher.anyOf("\"+"));

  /// Returns true if any line in the given Java source exceeds the column limit, contains a double
  /// quote (so it might contain a string literal), and intersects `characterRanges`.
  private static boolean needWrapping(
      int columnLimit,
      String input,
      JavaFormatterOptions options,
      RangeSet<Integer> characterRanges) {
    int len = input.length();
    int lineStart = 0;
    while (lineStart < len) {
      int lineEnd = lineStart;
      while (lineEnd < len) {
        char c = input.charAt(lineEnd);
        if (c == '\n' || c == '\r') {
          break;
        }
        lineEnd++;
      }
      if (options.visualLength(input, lineStart, lineEnd) > columnLimit) {
        int quoteIdx = input.indexOf('"', lineStart);
        if (quoteIdx != -1
            && quoteIdx < lineEnd
            && characterRanges.intersects(Range.closedOpen(lineStart, lineEnd))) {
          return true;
        }
      }
      if (lineEnd < len) {
        if (input.charAt(lineEnd) == '\r'
            && lineEnd + 1 < len
            && input.charAt(lineEnd + 1) == '\n') {
          lineStart = lineEnd + 2;
        } else {
          lineStart = lineEnd + 1;
        }
      } else {
        break;
      }
    }
    return false;
  }

  /// Parses the given Java source.
  private static JCTree.JCCompilationUnit parse(String source, boolean allowStringFolding)
      throws FormatterException {
    List<Diagnostic<? extends JavaFileObject>> errorDiagnostics = new ArrayList<>();
    Context context = new Context();
    JCTree.JCCompilationUnit unit =
        Trees.parse(context, errorDiagnostics, allowStringFolding, source);
    if (!errorDiagnostics.isEmpty()) {
      // error handling is done during formatting
      throw FormatterException.fromJavacDiagnostics(errorDiagnostics);
    }
    return unit;
  }

  /// Applies replacements to the given string.
  private static String applyReplacements(
      String javaInput, TreeRangeMap<Integer, String> replacementMap) throws FormatterException {
    // process in descending order so the replacement ranges aren't perturbed if any replacements
    // differ in size from the input
    Map<Range<Integer>, String> ranges = replacementMap.asDescendingMapOfRanges();
    if (ranges.isEmpty()) {
      return javaInput;
    }
    StringBuilder sb = new StringBuilder(javaInput);
    for (Map.Entry<Range<Integer>, String> entry : ranges.entrySet()) {
      Range<Integer> range = entry.getKey();
      sb.replace(range.lowerEndpoint(), range.upperEndpoint(), entry.getValue());
    }
    return sb.toString();
  }

  private StringWrapper() {}
}
