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
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.googlejavaformat.Doc;
import com.google.googlejavaformat.DocBuilder;
import com.google.googlejavaformat.InputOutput;
import com.google.googlejavaformat.Op;
import com.google.googlejavaformat.OpsBuilder;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

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
      JavaInput javaInput, JavaOutput javaOutput, int maxWidth, List<String> errors,
      int indentationMultiplier) {
    ASTParser parser = ASTParser.newParser(AST.JLS8);
    parser.setSource(javaInput.getText().toCharArray());
    @SuppressWarnings("unchecked") // safe by specification
    Map<String, String> options = JavaCore.getOptions();
    JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
    parser.setCompilerOptions(options);
    CompilationUnit unit = (CompilationUnit) parser.createAST(null);
    OpsBuilder builder = new OpsBuilder(javaInput, javaOutput, errors);
    // Output compilation unit.
    new JavaInputAstVisitor(builder, indentationMultiplier).visit(unit);
    builder.sync(javaInput.getText().length(), true);
    builder.sync(javaInput.getText().length() + 1, false);
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
    JavaInput javaInput = new JavaInput(input);
    JavaOutput javaOutput = new JavaOutput(javaInput, new JavaCommentsHelper(), false);
    List<String> errors = new ArrayList<>();
    format(javaInput, javaOutput, MAX_WIDTH, errors, 1);
    if (!errors.isEmpty()) {
      throw new FormatterException(errors.get(0));
    }
    StringBuilder result = new StringBuilder(input.length());
    RangeSet<Integer> lineRangeSet = TreeRangeSet.create();
    lineRangeSet.add(Range.<Integer>all());
    try {
      javaOutput.writeMerged(result, lineRangeSet, MAX_WIDTH, errors);
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
    JavaInput javaInput = new JavaInput(input);
    JavaOutput javaOutput = new JavaOutput(javaInput, new JavaCommentsHelper(), false);
    List<String> errors = new ArrayList<>();
    format(javaInput, javaOutput, MAX_WIDTH, errors, 1);
    if (!errors.isEmpty()) {
      throw new FormatterException(errors.get(0));
    }
    StringBuilder result = new StringBuilder(input.length());
    RangeSet<Integer> lineRangeSet = characterRangesToLineRangeSet(javaInput, characterRanges);
    try {
      javaOutput.writeMerged(result, lineRangeSet, MAX_WIDTH, errors);
    } catch (IOException ignored) {
      throw new AssertionError("IOException impossible for StringWriter");
    }
    return result.toString();
  }

  /**
   * Emit a list of {@link Replacement}s to convert from input to output.
   * @param input the input compilation unit
   * @param characterRanges the character ranges to reformat
   * @return a list of {@link Replacement}s, reverse-sorted from high index to low index, without
   *     overlaps
   * @throws FormatterException if the input string cannot be parsed
   */
  public ImmutableList<Replacement> getFormatReplacements(
      String input, List<Range<Integer>> characterRanges) throws FormatterException {
    List<Replacement> replacements = new ArrayList<>();
    JavaInput javaInput = new JavaInput(input);
    JavaOutput javaOutput = new JavaOutput(javaInput, new JavaCommentsHelper(), false);
    List<String> errors = new ArrayList<>();
    format(javaInput, javaOutput, MAX_WIDTH, errors, 1);
    if (!errors.isEmpty()) {
      throw new FormatterException(errors.get(0));
    }
    RangeSet<Integer> inputLineRangeSet = characterRangesToLineRangeSet(javaInput, characterRanges);
    List<JavaOutput.RunInfo> lineInfos = javaOutput.pickRuns(inputLineRangeSet);
    /*
     * There is one {@code lineInfo} for each line of the merged output: an unformatted
     * {@link From#INPUT} line or a formatted {@link From#OUTPUT} line. Each {@link Replacement}
     * represents a run of {@link From#OUTPUT}s; it specifies the character range of the replaced
     * input lines and the {@code String} value of the replacing output lines. The
     * {@link Replacement}s do not overlap, since the lines do not.
     */
    JavaOutput.From trackingFrom = JavaOutput.From.OUTPUT; // Which are we currently tracking?
    int inputLineI = 0; // Up to this line in the unformatted input, during From.INPUT.
    int inputCharacterI = 0; // Up to this character in the unformatted input, during From.INPUT.
    int outputLineJ = 0; // Up to this line in the formatted output, during From.OUTPUT.
    int outputLineJ0 = 0; // When did we switch to output?
    for (int ij = 0; ij < lineInfos.size(); ij++) {
      JavaOutput.RunInfo lineInfo = lineInfos.get(ij);
      if (trackingFrom == JavaOutput.From.INPUT) {
        if (lineInfo.from == JavaOutput.From.INPUT) {
          // Continue tracking input.
          while (inputLineI <= lineInfo.ij0) {
            inputCharacterI += javaInput.getLine(inputLineI++).length() + 1; // Include '\n'.
          }
        } else {
          // Done tracking input; switch to output.
          trackingFrom = JavaOutput.From.OUTPUT;
          outputLineJ0 = lineInfo.ij0;
          outputLineJ = lineInfo.ij0 + 1;
        }
      } else {
        if (lineInfo.from == JavaOutput.From.INPUT) {
          // Done tracking output; switch to input and emit a Replacement.
          int inputRange0 = inputCharacterI; // Where we switched to output.
          trackingFrom = JavaOutput.From.INPUT;
          while (inputLineI < lineInfo.ij0) {
            inputCharacterI += javaInput.getLine(inputLineI++).length() + 1; // Include '\n'.
          }
          int inputRange1 = inputCharacterI;
          inputCharacterI += javaInput.getLine(inputLineI++).length() + 1; // Include '\n'.
          Range<Integer> range = Range.closedOpen(inputRange0, inputRange1);
          if (!range.isEmpty() || outputLineJ0 < outputLineJ) {
            // Emit a Replacement.
            replacements.add(
                Replacement.create(range, getLines(javaOutput, outputLineJ0, outputLineJ)));
          }
        } else {
          // Continue tracking output.
          outputLineJ = lineInfo.ij0 + 1;
        }
      }
    }
    if (trackingFrom == JavaOutput.From.OUTPUT && outputLineJ0 < outputLineJ) {
      // Emit final Replacement.
      Range<Integer> range = Range.closedOpen(inputCharacterI, javaInput.getText().length());
      if (!range.isEmpty() || outputLineJ0 < outputLineJ) {
        // Emit a Replacement.
        replacements.add(
            Replacement.create(range, getLines(javaOutput, outputLineJ0, outputLineJ)));
      }
    }
    return ImmutableList.copyOf(Lists.reverse(replacements));
  }

  private static RangeSet<Integer> characterRangesToLineRangeSet(
      JavaInput javaInput, List<Range<Integer>> characterRanges) throws FormatterException {
    RangeSet<Integer> lineRangeSet = TreeRangeSet.create();
    for (Range<Integer> characterRange0 : characterRanges) {
      Range<Integer> characterRange = characterRange0.canonical(DiscreteDomain.integers());
      lineRangeSet.add(
          javaInput.characterRangeToLineRange(
              characterRange.lowerEndpoint(),
              characterRange.upperEndpoint() - characterRange.lowerEndpoint()));
    }
    return lineRangeSet.subRangeSet(Range.openClosed(0, javaInput.getLineCount()));
  }

  private static String getLines(InputOutput put, int ij0, int ij1) {
    StringBuilder builder = new StringBuilder();
    for (int ij = ij0; ij < ij1; ij++) {
      builder.append(put.getLine(ij)).append('\n');
    }
    return builder.toString();
  }
}
