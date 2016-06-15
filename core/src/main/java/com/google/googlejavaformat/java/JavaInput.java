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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getLast;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.Verify;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.googlejavaformat.Input;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

/**
 * {@code JavaInput} extends {@link Input} to represent a Java input document.
 */
public final class JavaInput extends Input {
  /**
   * A {@code JavaInput} is a sequence of {@link Tok}s that cover the Java input. A {@link Tok} is
   * either a token (if {@code isToken()}), or a non-token, which is a comment (if
   * {@code isComment()}) or a newline (if {@code isNewline()}) or a maximal sequence of other
   * whitespace characters (if {@code isSpaces()}). Each {@link Tok} contains a sequence of
   * characters, an index (sequential starting at {@code 0} for tokens and comments, else
   * {@code -1}), and an Eclipse-compatible ({@code 0}-origin) position in the input. The
   * concatenation of the texts of all the {@link Tok}s equals the input. Each Input ends with a
   * token EOF {@link Tok}, with empty text.
   *
   * <p>A {@code /*} comment possibly contains newlines; a {@code //} comment does not contain the
   * terminating newline character, but is followed by a newline {@link Tok}.
   */
  static final class Tok implements Input.Tok {
    private final int index;
    private final String originalText;
    private final String text;
    private final int position;
    private final int columnI;
    private final boolean isToken;
    private final int id;

    /**
     * The {@code Tok} constructor.
     * @param index its index
     * @param originalText its original text, before removing Unicode escapes
     * @param text its text after removing Unicode escapes
     * @param position its {@code 0}-origin position in the input
     * @param columnI its {@code 0}-origin column number in the input
     * @param isToken whether the {@code Tok} is a token
     * @param id the token id as defined by {@link org.eclipse.jdt.core.compiler.ITerminalSymbols}
     */
    Tok(
        int index,
        String originalText,
        String text,
        int position,
        int columnI,
        boolean isToken,
        int id) {
      this.index = index;
      this.originalText = originalText;
      this.text = text;
      this.position = position;
      this.columnI = columnI;
      this.isToken = isToken;
      this.id = id;
    }

    @Override
    public int getIndex() {
      return index;
    }

    @Override
    public String getText() {
      return text;
    }

    @Override
    public String getOriginalText() {
      return originalText;
    }

    @Override
    public int getPosition() {
      return position;
    }

    @Override
    public int getColumn() {
      return columnI;
    }

    boolean isToken() {
      return isToken;
    }

    @Override
    public boolean isNewline() {
      return "\n".equals(text);
    }

    @Override
    public boolean isSlashSlashComment() {
      return text.startsWith("//");
    }

    @Override
    public boolean isSlashStarComment() {
      return text.startsWith("/*");
    }

    @Override
    public boolean isJavadocComment() {
      return text.startsWith("/**") && text.length() > 4;
    }

    @Override
    public boolean isComment() {
      return isSlashSlashComment() || isSlashStarComment();
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("index", index)
          .add("text", text)
          .add("position", position)
          .add("columnI", columnI)
          .add("isToken", isToken)
          .toString();
    }

    /**
     * The token id used by the eclipse scanner. See
     * {@link org.eclipse.jdt.core.compiler.ITerminalSymbols} for possible values.
     */
    public int id() {
      return id;
    }
  }

  /**
   * A {@link Token} contains a token {@link Tok} and its associated non-tokens; each non-token
   * {@link Tok} belongs to one {@link Token}. Each {@link Token} has an immutable list of its
   * non-tokens that appear before it, and another list of its non-tokens that appear after it. The
   * concatenation of the texts of all the {@link Token}s' {@link Tok}s, each preceded by the texts
   * of its {@code toksBefore} and followed by the texts of its {@code toksAfter}, equals the input.
   */
  static final class Token implements Input.Token {
    private final Tok tok;
    private final ImmutableList<Tok> toksBefore;
    private final ImmutableList<Tok> toksAfter;

    /**
     * Token constructor.
     * @param toksBefore the earlier non-token {link Tok}s assigned to this {@code Token}
     * @param tok this token {@link Tok}
     * @param toksAfter the later non-token {link Tok}s assigned to this {@code Token}
     */
    Token(List<Tok> toksBefore, Tok tok, List<Tok> toksAfter) {
      this.toksBefore = ImmutableList.copyOf(toksBefore);
      this.tok = tok;
      this.toksAfter = ImmutableList.copyOf(toksAfter);
    }

    /**
     * Get the token's {@link Tok}.
     * @return the token's {@link Tok}
     */
    @Override
    public Tok getTok() {
      return tok;
    }

    /**
     * Get the earlier {@link Tok}s assigned to this {@code Token}.
     * @return the earlier {@link Tok}s assigned to this {@code Token}
     */
    @Override
    public ImmutableList<? extends Input.Tok> getToksBefore() {
      return toksBefore;
    }

    /**
     * Get the later {@link Tok}s assigned to this {@code Token}.
     * @return the later {@link Tok}s assigned to this {@code Token}
     */
    @Override
    public ImmutableList<? extends Input.Tok> getToksAfter() {
      return toksAfter;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("tok", tok)
          .add("toksBefore", toksBefore)
          .add("toksAfter", toksAfter)
          .toString();
    }
  }

  private static final Splitter NEWLINE_SPLITTER = Splitter.on('\n');

  private final String text; // The input.
  private int kN; // The number of numbered toks (tokens or comments), excluding the EOF.
  private Map<Integer, Range<Integer>> kToI = null; // Map from token indices to line numbers.

  /*
   * The following lists record the sequential indices of the {@code Tok}s on each input line. (Only
   * tokens and comments have sequential indices.) Tokens and {@code //} comments lie on just one
   * line; {@code /*} comments can lie on multiple lines. These data structures (along with
   * equivalent ones for the formatted output) let us compute correspondences between the input and
   * output.
   */

  private final ImmutableMap<Integer, Integer> positionToColumnMap; // Map Tok position to column.
  private final ImmutableList<Token> tokens; // The Tokens for this input.
  private final ImmutableSortedMap<Integer, Token> positionTokenMap; // Map position to Token.

  /** Map from Tok index to the associated Token. */
  private final Token[] kToToken;

  /**
   * Input constructor.
   * @param text the input text
   * @throws FormatterException if the input cannot be parsed
   */
  public JavaInput(String text) throws FormatterException {
    this.text = checkNotNull(text);
    List<String> lines = NEWLINE_SPLITTER.splitToList(text);
    setLines(ImmutableList.copyOf(lines));
    ImmutableList<Tok> toks = buildToks(text);
    positionToColumnMap = makePositionToColumnMap(toks);
    tokens = buildTokens(toks);
    ImmutableSortedMap.Builder<Integer, Token> locationTokenMap = ImmutableSortedMap.naturalOrder();
    for (Token token : tokens) {
      locationTokenMap.put(JavaOutput.startPosition(token), token);
    }
    positionTokenMap = locationTokenMap.build();

    // adjust kN for EOF
    kToToken = new Token[kN + 1];
    for (Token token : tokens) {
      for (Input.Tok tok : token.getToksBefore()) {
        if (tok.getIndex() < 0) {
          continue;
        }
        kToToken[tok.getIndex()] = token;
      }
      kToToken[token.getTok().getIndex()] = token;
      for (Input.Tok tok : token.getToksAfter()) {
        if (tok.getIndex() < 0) {
          continue;
        }
        kToToken[tok.getIndex()] = token;
      }
    }
  }

  private static ImmutableMap<Integer, Integer> makePositionToColumnMap(List<Tok> toks) {
    ImmutableMap.Builder<Integer, Integer> builder = ImmutableMap.builder();
    for (Tok tok : toks) {
      builder.put(tok.getPosition(), tok.getColumn());
    }
    return builder.build();
  }

  /**
   * Get the input text.
   * @return the input text
   */
  @Override
  public String getText() {
    return text;
  }

  @Override
  public ImmutableMap<Integer, Integer> getPositionToColumnMap() {
    return positionToColumnMap;
  }

  /** Lex the input and build the list of toks. */
  private ImmutableList<Tok> buildToks(String text) throws FormatterException {
    try {
      ImmutableList<Tok> toks = buildToks(text, ImmutableSet.<Integer>of());
      kN = getLast(toks).getIndex();
      computeRanges(toks);
      return toks;
    } catch (InvalidInputException e) {
      // jdt's scanner elects not to produce error messages, so we don't either
      //
      // problems will get caught (again!) and reported (with error messages!)
      // during parsing
      return ImmutableList.of();
    }
  }

  /**
   * Lex the input and build the list of toks.
   *
   * @param text the text to be lexed.
   * @param stopIds a set of Eclipse token names which should cause lexing to stop. If one of these
   *     is found, the returned list will include tokens up to but not including that token.
   */
  static ImmutableList<Tok> buildToks(String text, ImmutableSet<Integer> stopIds)
      throws InvalidInputException, FormatterException {
    stopIds =
        ImmutableSet.<Integer>builder().addAll(stopIds).add(ITerminalSymbols.TokenNameEOF).build();
    int kN = 0;
    IScanner scanner = ToolFactory.createScanner(true, true, true, "1.8");
    scanner.setSource(text.toCharArray());
    int textLength = text.length();
    List<Tok> toks = new ArrayList<>();
    int charI = 0;
    int columnI = 0;
    while (scanner.getCurrentTokenEndPosition() < textLength - 1) {
      int tokenId = scanner.getNextToken();
      if (stopIds.contains(tokenId)) {
        break;
      }
      int charI0 = scanner.getCurrentTokenStartPosition();
      // Get string, possibly with Unicode escapes.
      String originalTokText = text.substring(charI0, scanner.getCurrentTokenEndPosition() + 1);
      String tokText = new String(scanner.getCurrentTokenSource()); // Unicode escapes removed.
      char tokText0 = tokText.charAt(0); // The token's first character.
      final boolean isToken; // Is this tok a token?
      final boolean isNumbered; // Is this tok numbered? (tokens and comments)
      boolean extraNewline = false; // Extra newline at end?
      List<String> strings = new ArrayList<>();
      if (Character.isWhitespace(tokText0)) {
        isToken = false;
        isNumbered = false;
        boolean first = true;
        for (String spaces : NEWLINE_SPLITTER.split(originalTokText)) {
          if (!first) {
            strings.add("\n");
          }
          if (!spaces.isEmpty()) {
            strings.add(spaces);
          }
          first = false;
        }
      } else if (tokText.startsWith("'") || tokText.startsWith("\"")) {
        isToken = true;
        isNumbered = true;
        strings.add(originalTokText);
      } else if (tokText.startsWith("//") || tokText.startsWith("/*")) {
        // For compatibility with an earlier lexer, the newline after a // comment is its own tok.
        if (tokText.startsWith("//") && originalTokText.endsWith("\n")) {
          originalTokText = originalTokText.substring(0, originalTokText.length() - 1);
          tokText = tokText.substring(0, tokText.length() - 1);
          extraNewline = true;
        }
        isToken = false;
        isNumbered = true;
        strings.add(originalTokText);
      } else if (Character.isJavaIdentifierStart(tokText0)
          || Character.isDigit(tokText0)
          || tokText0 == '.' && tokText.length() > 1 && Character.isDigit(tokText.charAt(1))) {
        // Identifier, keyword, or numeric literal (a dot may begin a number, as in .2D).
        isToken = true;
        isNumbered = true;
        strings.add(tokText);
      } else {
        // Other tokens ("+" or "++" or ">>" are broken into one-character toks, because ">>"
        // cannot be lexed without syntactic knowledge. This implementation fails if the token
        // contains Unicode escapes.
        isToken = true;
        isNumbered = true;
        for (char c : tokText.toCharArray()) {
          strings.add(String.valueOf(c));
        }
      }
      if (strings.size() == 1) {
        toks.add(
            new Tok(
                isNumbered ? kN++ : -1,
                originalTokText,
                tokText,
                charI,
                columnI,
                isToken,
                tokenId));
        for (char c : originalTokText.toCharArray()) {
          if (c == '\n') {
            columnI = 0;
          } else {
            ++columnI;
          }
          ++charI;
        }
      } else {
        if (strings.size() != 1 && !tokText.equals(originalTokText)) {
          throw new FormatterException(
              "Unicode escapes not allowed in whitespace or multi-character operators");
        }
        for (String str : strings) {
          toks.add(new Tok(isNumbered ? kN++ : -1, str, str, charI, columnI, isToken, tokenId));
          for (char c : str.toCharArray()) {
            if (c == '\n') {
              columnI = 0;
            } else {
              ++columnI;
            }
            ++charI;
          }
        }
      }
      if (extraNewline) {
        toks.add(new Tok(-1, "\n", "\n", charI, columnI, false, tokenId));
        columnI = 0;
        ++charI;
      }
    }
    toks.add(new Tok(kN, "", "", charI, columnI, true, ITerminalSymbols.TokenNameEOF)); // EOF tok.
    return ImmutableList.copyOf(toks);
  }

  private static ImmutableList<Token> buildTokens(List<Tok> toks) {
    ImmutableList.Builder<Token> tokens = ImmutableList.builder();
    int k = 0;
    int kN = toks.size();

    while (k < kN) {
      // Remaining non-tokens before the token go here.
      ImmutableList.Builder<Tok> toksBefore = ImmutableList.builder();

      while (!toks.get(k).isToken()) {
        toksBefore.add(toks.get(k++));
      }
      Tok tok = toks.get(k++);

      // Non-tokens starting on the same line go here too.
      ImmutableList.Builder<Tok> toksAfter = ImmutableList.builder();
      OUTER:
      while (k < kN && !toks.get(k).isToken()) {
        // Don't attach inline comments to leading '('s, e.g. for `f(/*flag1=*/true).
        //
        // Attaching inline comments to the right token is hard, and this barely
        // scratches the surface. But it's enough to do a better job with parameter
        // name comments.
        //
        // TODO(cushon): find a better strategy.
        if (toks.get(k).isSlashStarComment()) {
          switch (tok.getText()) {
            case "(":
            case "<":
            case ".":
              break OUTER;
            default:
              break;
          }
        }
        Tok nonTokenAfter = toks.get(k++);
        toksAfter.add(nonTokenAfter);
        if (nonTokenAfter.getText().contains("\n")) {
          break;
        }
      }
      tokens.add(new Token(toksBefore.build(), tok, toksAfter.build()));
    }
    return tokens.build();
  }

  /**
   * Returns the lowest line number the {@link Token} or one of its {@code tokBefore}s lies on in
   * the {@code JavaInput}.
   * @param token the {@link Token}
   * @return the {@code 0}-based line number
   */
  int getLineNumberLo(Token token) {
    int k = -1;
    for (Tok tok : token.toksBefore) {
      k = tok.getIndex();
      if (k >= 0) {
        break;
      }
    }
    if (k < 0) {
      k = token.tok.getIndex();
    }
    if (kToI == null) {
      kToI = makeKToIJ(this, kN);
    }
    return kToI.get(k).lowerEndpoint();
  }

  /**
   * Returns the highest line number the {@link Token} or one of its {@code tokAfter}s lies on in
   * the {@code JavaInput}.
   * @param token the {@link Token}
   * @return the {@code 0}-based line number
   */
  int getLineNumberHi(Token token) {
    int k = -1;
    for (Tok tok : token.toksAfter.reverse()) {
      k = tok.getIndex();
      if (k >= 0) {
        break;
      }
    }
    if (k < 0) {
      k = token.tok.getIndex();
    }
    if (kToI == null) {
      kToI = makeKToIJ(this, kN);
    }
    return kToI.get(k).upperEndpoint() - 1;
  }

  /**
   * Convert from an offset and length flag pair to a token range.
   * @param offset the {@code 0}-based offset in characters
   * @param length the length in characters
   * @return the {@code 0}-based {@link Range} of tokens
   * @throws FormatterException
   */
  Range<Integer> characterRangeToTokenRange(int offset, int length) throws FormatterException {
    int requiredLength = offset + length;
    if (requiredLength > text.length()) {
      throw new FormatterException(
          String.format(
              "error: invalid length %d, offset + length (%d) is outside the file",
              length, requiredLength));
    }
    if (length <= 0) {
      return Formatter.EMPTY_RANGE;
    }
    NavigableMap<Integer, JavaInput.Token> map = getPositionTokenMap();
    Map.Entry<Integer, JavaInput.Token> tokenEntryLo =
        firstNonNull(map.floorEntry(offset), map.firstEntry());
    Map.Entry<Integer, JavaInput.Token> tokenEntryHi =
        firstNonNull(map.floorEntry(offset + length - 1), map.lastEntry());
    return Range.closedOpen(
        tokenEntryLo.getValue().getTok().getIndex(),
        tokenEntryHi.getValue().getTok().getIndex() + 1);
  }

  /**
   * Get the number of toks.
   * @return the number of toks, including the EOF tok
   */
  int getkN() {
    return kN;
  }

  /**
   * Get the Token by index.
   * @param k the token index
   */
  Token getToken(int k) {
    return kToToken[k];
  }

  /**
   * Get the input tokens.
   * @return the input tokens
   */
  @Override
  public ImmutableList<? extends Input.Token> getTokens() {
    return tokens;
  }

  /**
   * Get the navigable map from position to {@link Token}. Used to look for tokens following a given
   * one, and to implement the --offset and --length flags to reformat a character range in the
   * input file.
   * @return the navigable map from position to {@link Token}
   */
  @Override
  public NavigableMap<Integer, Token> getPositionTokenMap() {
    return positionTokenMap;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("tokens", tokens)
        .add("super", super.toString())
        .toString();
  }

  private CompilationUnit unit;

  @Override
  public int getLineNumber(int inputPosition) {
    Verify.verifyNotNull(unit, "Expected compilation unit to be set.");
    return unit.getLineNumber(inputPosition);
  }

  @Override
  public int getColumnNumber(int inputPosition) {
    Verify.verifyNotNull(unit, "Expected compilation unit to be set.");
    return unit.getColumnNumber(inputPosition);
  }

  // TODO(cushon): refactor JavaInput so the CompilationUnit can be passed into
  // the constructor.
  public void setCompilationUnit(CompilationUnit unit) {
    this.unit = unit;
  }

  public RangeSet<Integer> characterRangesToTokenRanges(Collection<Range<Integer>> characterRanges)
      throws FormatterException {
    RangeSet<Integer> tokenRangeSet = TreeRangeSet.create();
    for (Range<Integer> characterRange0 : characterRanges) {
      Range<Integer> characterRange = characterRange0.canonical(DiscreteDomain.integers());
      tokenRangeSet.add(
          characterRangeToTokenRange(
              characterRange.lowerEndpoint(),
              characterRange.upperEndpoint() - characterRange.lowerEndpoint()));
    }
    return tokenRangeSet;
  }
}
