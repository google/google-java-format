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

import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.googlejavaformat.Doc;
import com.google.googlejavaformat.DocBuilder;
import com.google.googlejavaformat.FormatterDiagnostic;
import com.google.googlejavaformat.Op;
import com.google.googlejavaformat.OpsBuilder;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
 * <p>The formatter walks the AST to generate a Greg Nelson/Dereck Oppen-style list of formatting
 * {@link Op}s [1--2] that then generate a structured {@link Doc}. Each AST node type has a visitor
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
 * <p>[1] Nelson, Greg, and John DeTreville. Personal communication.
 * <p>[2] Oppen, Dereck C. "Prettyprinting". ACM Transactions on Programming Languages and Systems,
 *        Volume 2 Issue 4, Oct. 1980, pp. 465–483.
 */
public final class Formatter {
  static final int MAX_WIDTH = 100;
  static final Range<Integer> EMPTY_RANGE = Range.closedOpen(-1, -1);

  /**
   * Construct a {@code Formatter} given Java compilation unit. Parses the code; builds a
   * {@link JavaInput} and the corresponding {@link JavaOutput}.
   * @param javaInput the input, a Java compilation unit
   * @param javaOutput the {@link JavaOutput}
   * @param maxWidth the maximum formatted width
   * @param errors mutable list to receive errors
   * @param indentationMultiplier the multiplier for the unit of indent; the default is 1
   */
  public static void format(
      JavaInput javaInput,
      JavaOutput javaOutput,
      int maxWidth,
      List<FormatterDiagnostic> errors,
      int indentationMultiplier) {
    ASTParser parser = ASTParser.newParser(AST.JLS8);
    parser.setSource(javaInput.getText().toCharArray());
    @SuppressWarnings("unchecked") // safe by specification
    Map<String, String> options = JavaCore.getOptions();
    JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
    parser.setCompilerOptions(options);
    CompilationUnit unit = (CompilationUnit) parser.createAST(null);
    javaInput.setCompilationUnit(unit);
    if (unit.getMessages().length > 0) {
      for (Message message : unit.getMessages()) {
        errors.add(javaInput.createDiagnostic(message.getStartPosition(), message.getMessage()));
      }
      return;
    }
    OpsBuilder builder = new OpsBuilder(javaInput, javaOutput, errors);
    // Output compilation unit.
    new JavaInputAstVisitor(builder, indentationMultiplier).visit(unit);
    builder.sync(javaInput.getText().length());
    builder.drain();
    new DocBuilder().withOps(builder.build()).build().write(
        javaOutput, maxWidth, new Doc.State(+0, 0)); // Write the Doc to the Output.
    javaOutput.flush();
  }

  /**
   * Format an input string (a Java compilation unit) into an output string.
   * @param input the input string
   * @return the output string
   * @throws FormatterException if the input string cannot be parsed
   */
  public String formatSource(String input) throws FormatterException {
    JavaInput javaInput = new JavaInput(null, input);
    JavaOutput javaOutput = new JavaOutput(javaInput, new JavaCommentsHelper());
    List<FormatterDiagnostic> errors = new ArrayList<>();
    format(javaInput, javaOutput, MAX_WIDTH, errors, 1);
    if (!errors.isEmpty()) {
      throw new FormatterException(errors);
    }
    StringBuilder result = new StringBuilder(input.length());
    RangeSet<Integer> lineRangeSet = TreeRangeSet.create();
    lineRangeSet.add(Range.<Integer>all());
    try {
      javaOutput.writeMerged(result, lineRangeSet);
    } catch (IOException ignored) {
      throw new AssertionError("IOException impossible for StringWriter");
    }
    return result.toString();
  }

  /**
   * Format an input string (a compilation), for only the specified character ranges. These ranges
   * are extended as necessary (e.g., to encompass whole lines).
   * @param input the input string
   * @param characterRanges the character ranges to be reformatted
   * @return the output string
   * @throws FormatterException if the input string cannot be parsed
   */
  public String formatSource(String input, List<Range<Integer>> characterRanges)
      throws FormatterException {
    JavaInput javaInput = new JavaInput(null, input);
    JavaOutput javaOutput = new JavaOutput(javaInput, new JavaCommentsHelper());
    List<FormatterDiagnostic> errors = new ArrayList<>();
    format(javaInput, javaOutput, MAX_WIDTH, errors, 1);
    if (!errors.isEmpty()) {
      throw new FormatterException(errors);
    }
    StringBuilder result = new StringBuilder(input.length());
    RangeSet<Integer> tokenRangeSet = characterRangesToTokenRanges(javaInput, characterRanges);
    try {
      javaOutput.writeMerged(result, tokenRangeSet);
    } catch (IOException ignored) {
      throw new AssertionError("IOException impossible for StringWriter");
    }
    return result.toString();
  }

  /**
   * Emit a list of {@link Replacement}s to convert from input to output.
   * @param input the input compilation unit
   * @param characterRanges the character ranges to reformat
   * @return a list of {@link Replacement}s, sorted from low index to high index, without
   *     overlaps
   * @throws FormatterException if the input string cannot be parsed
   */
  public ImmutableList<Replacement> getFormatReplacements(
      String input, List<Range<Integer>> characterRanges) throws FormatterException {
    JavaInput javaInput = new JavaInput(null, input);
    JavaOutput javaOutput = new JavaOutput(javaInput, new JavaCommentsHelper());
    List<FormatterDiagnostic> errors = new ArrayList<>();
    format(javaInput, javaOutput, MAX_WIDTH, errors, 1);
    if (!errors.isEmpty()) {
      throw new FormatterException(errors);
    }
    RangeSet<Integer> tokenRangeSet = characterRangesToTokenRanges(javaInput, characterRanges);
    return javaOutput.getFormatReplacements(tokenRangeSet);
  }

  private static RangeSet<Integer> characterRangesToTokenRanges(
      JavaInput javaInput, List<Range<Integer>> characterRanges) throws FormatterException {
    RangeSet<Integer> tokenRangeSet = TreeRangeSet.create();
    for (Range<Integer> characterRange0 : characterRanges) {
      Range<Integer> characterRange = characterRange0.canonical(DiscreteDomain.integers());
      tokenRangeSet.add(
          javaInput.characterRangeToTokenRange(
              characterRange.lowerEndpoint(),
              characterRange.upperEndpoint() - characterRange.lowerEndpoint()));
    }
    return tokenRangeSet;
  }
}
