/*
 * Copyright 2015 Google Inc.
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

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.errorprone.annotations.Immutable;
import com.google.googlejavaformat.Doc;
import com.google.googlejavaformat.DocBuilder;
import com.google.googlejavaformat.FormatterDiagnostic;
import com.google.googlejavaformat.Op;
import com.google.googlejavaformat.OpsBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Message;

/**
 * This is google-java-format, a new Java formatter that follows the Google Java Style Guide quite
 * precisely---to the letter and to the spirit.
 *
 * <p>This formatter uses the Eclipse parser to generate an AST. Because the Eclipse AST loses
 * information about the non-tokens in the input (including newlines, comments, etc.), and even some
 * tokens (e.g., optional commas or semicolons), this formatter lexes the input again and follows
 * along in the resulting list of tokens. Its lexer splits all multi-character operators (like ">>")
 * into multiple single-character operators. Each non-token is assigned to a token---non-tokens
 * following a token on the same line go with that token; those following go with the next token---
 * and there is a final EOF token to hold final comments.
 *
 * <p>The formatter walks the AST to generate a Greg Nelson/Derek Oppen-style list of formatting
 * {@link Op}s [1--2] that then generates a structured {@link Doc}. Each AST node type has a visitor
 * to emit a sequence of {@link Op}s for the node.
 *
 * <p>Some data-structure operations are easier in the list of {@link Op}s, while others become
 * easier in the {@link Doc}. The {@link Op}s are walked to attach the comments. As the {@link Op}s
 * are generated, missing input tokens are inserted and incorrect output tokens are dropped,
 * ensuring that the output matches the input even in the face of formatter errors. Finally, the
 * formatter walks the {@link Doc} to format it in the given width.
 *
 * <p>This formatter also produces data structures of which tokens and comments appear where on the
 * input, and on the output, to help output a partial reformatting of a slightly edited input.
 *
 * <p>Instances of the formatter are immutable and thread-safe.
 *
 * <p>[1] Nelson, Greg, and John DeTreville. Personal communication.
 * <p>[2] Oppen, Derek C. "Prettyprinting". ACM Transactions on Programming Languages and Systems,
 *        Volume 2 Issue 4, Oct. 1980, pp. 465–483.
 */
@Immutable
public final class Formatter {

  static final Range<Integer> EMPTY_RANGE = Range.closedOpen(-1, -1);

  private final JavaFormatterOptions options;

  /**
   * A new Formatter instance with default options.
   */
  public Formatter() {
    this(JavaFormatterOptions.defaultOptions());
  }

  public Formatter(JavaFormatterOptions options) {
    this.options = options;
  }

  /**
   * Construct a {@code Formatter} given a Java compilation unit. Parses the code; builds a
   * {@link JavaInput} and the corresponding {@link JavaOutput}.
   * @param javaInput the input, a Java compilation unit
   * @param javaOutput the {@link JavaOutput}
   * @param options the {@link JavaFormatterOptions}
   * @param errors mutable list to receive errors
   */
  static void format(
      JavaInput javaInput,
      JavaOutput javaOutput,
      JavaFormatterOptions options,
      List<FormatterDiagnostic> errors) {
    ASTParser parser = ASTParser.newParser(AST.JLS8);
    parser.setSource(javaInput.getText().toCharArray());
    @SuppressWarnings("unchecked") // safe by specification
    Map<String, String> parserOptions = JavaCore.getOptions();
    JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, parserOptions);
    parser.setCompilerOptions(parserOptions);
    CompilationUnit unit = (CompilationUnit) parser.createAST(null);
    javaInput.setCompilationUnit(unit);
    if (unit.getMessages().length > 0) {
      for (Message message : unit.getMessages()) {
        errors.add(javaInput.createDiagnostic(message.getStartPosition(), message.getMessage()));
      }
      return;
    }
    OpsBuilder builder = new OpsBuilder(javaInput, javaOutput, errors);
    // Output the compilation unit.
    new JavaInputAstVisitor(builder, options.indentationMultiplier()).visit(unit);
    builder.sync(javaInput.getText().length());
    builder.drain();
    Doc doc = new DocBuilder().withOps(builder.build()).build();
    doc.computeBreaks(
        javaOutput.getCommentsHelper(), options.maxLineLength(), new Doc.State(+0, 0));
    doc.write(javaOutput);
    javaOutput.flush();
  }

  /**
   * Format the given input (a Java compilation unit) into the output stream.
   *
   * @throws FormatterException if the input cannot be parsed
   */
  public void formatSource(CharSource input, CharSink output)
      throws FormatterException, IOException {
    // TODO(cushon): proper support for streaming input/output. Input may
    // not be feasible (parsing) but output should be easier.
    output.write(formatSource(input.read()));
  }

  /**
   * Format an input string (a Java compilation unit) into an output string.
   *
   * @param input the input string
   * @return the output string
   * @throws FormatterException if the input string cannot be parsed
   */
  public String formatSource(String input) throws FormatterException {
    return formatSource(input, Collections.singleton(Range.closedOpen(0, input.length())));
  }

  /**
   * Format an input string (a Java compilation unit), for only the specified character ranges.
   * These ranges are extended as necessary (e.g., to encompass whole lines).
   *
   * @param input the input string
   * @param characterRanges the character ranges to be reformatted
   * @return the output string
   * @throws FormatterException if the input string cannot be parsed
   */
  public String formatSource(String input, Collection<Range<Integer>> characterRanges)
      throws FormatterException {
    return JavaOutput.applyReplacements(input, getFormatReplacements(input, characterRanges));
  }

  /**
   * Emit a list of {@link Replacement}s to convert from input to output.
   *
   * @param input the input compilation unit
   * @param characterRanges the character ranges to reformat
   * @return a list of {@link Replacement}s, sorted from low index to high index, without overlaps
   * @throws FormatterException if the input string cannot be parsed
   */
  public ImmutableList<Replacement> getFormatReplacements(
      String input, Collection<Range<Integer>> characterRanges) throws FormatterException {

    // TODO(cushon): this is only safe because the modifier ordering doesn't affect whitespace,
    // and doesn't change the replacements that are output. This is not true in general for
    // 'de-linting' changes (e.g. import ordering).
    input = ModifierOrderer.reorderModifiers(input, characterRanges);

    JavaInput javaInput = new JavaInput(input);
    JavaOutput javaOutput = new JavaOutput(javaInput, new JavaCommentsHelper(options));
    List<FormatterDiagnostic> errors = new ArrayList<>();
    format(javaInput, javaOutput, options, errors);
    if (!errors.isEmpty()) {
      throw new FormatterException(errors);
    }
    RangeSet<Integer> tokenRangeSet = javaInput.characterRangesToTokenRanges(characterRanges);
    return javaOutput.getFormatReplacements(tokenRangeSet);
  }

  static final CharMatcher NEWLINE = CharMatcher.is('\n');

  /**
   * Converts zero-indexed, [closed, open) line ranges in the given source file to character
   * ranges.
   */
  public static RangeSet<Integer> lineRangesToCharRanges(
      String input, RangeSet<Integer> lineRanges) {
    List<Integer> lines = new ArrayList<>();
    lines.add(0);
    int idx = NEWLINE.indexIn(input);
    while (idx >= 0) {
      lines.add(idx + 1);
      idx = NEWLINE.indexIn(input, idx + 1);
    }
    lines.add(input.length() + 1);

    final RangeSet<Integer> characterRanges = TreeRangeSet.create();
    for (Range<Integer> lineRange :
        lineRanges.subRangeSet(Range.closedOpen(0, lines.size() - 1)).asRanges()) {
      int lineStart = lines.get(lineRange.lowerEndpoint());
      // Exclude the trailing newline. This isn't strictly necessary, but handling blank lines
      // as empty ranges is convenient.
      int lineEnd = lines.get(lineRange.upperEndpoint()) - 1;
      Range<Integer> range = Range.closedOpen(lineStart, lineEnd);
      characterRanges.add(range);
    }
    return characterRanges;
  }
}
