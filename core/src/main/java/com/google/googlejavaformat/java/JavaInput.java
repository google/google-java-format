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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getLast;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.MoreObjects;
import com.google.common.base.Verify;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.googlejavaformat.Input;
import com.google.googlejavaformat.Newlines;
import com.google.googlejavaformat.java.JavacTokens.RawTok;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.openjdk.javax.tools.Diagnostic;
import org.openjdk.javax.tools.DiagnosticCollector;
import org.openjdk.javax.tools.DiagnosticListener;
import org.openjdk.javax.tools.JavaFileObject;
import org.openjdk.javax.tools.JavaFileObject.Kind;
import org.openjdk.javax.tools.SimpleJavaFileObject;
import org.openjdk.tools.javac.file.JavacFileManager;
import org.openjdk.tools.javac.parser.Tokens.TokenKind;
import org.openjdk.tools.javac.tree.JCTree.JCCompilationUnit;
import org.openjdk.tools.javac.util.Context;
import org.openjdk.tools.javac.util.Log;
import org.openjdk.tools.javac.util.Log.DeferredDiagnosticHandler;

/** {@code JavaInput} extends {@link Input} to represent a Java input document. */
public final class JavaInput extends Input {
  /**
   * A {@code JavaInput} is a sequence of {@link Tok}s that cover the Java input. A {@link Tok} is
   * either a token (if {@code isToken()}), or a non-token, which is a comment (if {@code
   * isComment()}) or a newline (if {@code isNewline()}) or a maximal sequence of other whitespace
   * characters (if {@code isSpaces()}). Each {@link Tok} contains a sequence of characters, an
   * index (sequential starting at {@code 0} for tokens and comments, else {@code -1}), and a
   * ({@code 0}-origin) position in the input. The concatenation of the texts of all the {@link
   * Tok}s equals the input. Each Input ends with a token EOF {@link Tok}, with empty text.
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
    private final TokenKind kind;

    /**
     * The {@code Tok} constructor.
     *
     * @param index its index
     * @param originalText its original text, before removing Unicode escapes
     * @param text its text after removing Unicode escapes
     * @param position its {@code 0}-origin position in the input
     * @param columnI its {@code 0}-origin column number in the input
     * @param isToken whether the {@code Tok} is a token
     * @param kind the token kind
     */
    Tok(
        int index,
        String originalText,
        String text,
        int position,
        int columnI,
        boolean isToken,
        TokenKind kind) {
      this.index = index;
      this.originalText = originalText;
      this.text = text;
      this.position = position;
      this.columnI = columnI;
      this.isToken = isToken;
      this.kind = kind;
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
    public int length() {
      return originalText.length();
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
      return Newlines.isNewline(text);
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

    public TokenKind kind() {
      return kind;
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
     *
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
     *
     * @return the token's {@link Tok}
     */
    @Override
    public Tok getTok() {
      return tok;
    }

    /**
     * Get the earlier {@link Tok}s assigned to this {@code Token}.
     *
     * @return the earlier {@link Tok}s assigned to this {@code Token}
     */
    @Override
    public ImmutableList<? extends Input.Tok> getToksBefore() {
      return toksBefore;
    }

    /**
     * Get the later {@link Tok}s assigned to this {@code Token}.
     *
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

  private final String text; // The input.
  private int kN; // The number of numbered toks (tokens or comments), excluding the EOF.

  /*
   * The following lists record the sequential indices of the {@code Tok}s on each input line. (Only
   * tokens and comments have sequential indices.) Tokens and {@code //} comments lie on just one
   * line; {@code /*} comments can lie on multiple lines. These data structures (along with
   * equivalent ones for the formatted output) let us compute correspondences between the input and
   * output.
   */

  private final ImmutableMap<Integer, Integer> positionToColumnMap; // Map Tok position to column.
  private final ImmutableList<Token> tokens; // The Tokens for this input.
  private final ImmutableRangeMap<Integer, Token> positionTokenMap; // Map position to Token.

  /** Map from Tok index to the associated Token. */
  private final Token[] kToToken;

  /**
   * Input constructor.
   *
   * @param text the input text
   * @throws FormatterException if the input cannot be parsed
   */
  public JavaInput(String text) throws FormatterException {
    this.text = checkNotNull(text);
    setLines(ImmutableList.copyOf(Newlines.lineIterator(text)));
    ImmutableList<Tok> toks = buildToks(text);
    positionToColumnMap = makePositionToColumnMap(toks);
    tokens = buildTokens(toks);
    ImmutableRangeMap.Builder<Integer, Token> tokenLocations = ImmutableRangeMap.builder();
    for (Token token : tokens) {
      Input.Tok end = JavaOutput.endTok(token);
      int upper = end.getPosition();
      if (!end.getText().isEmpty()) {
        upper += end.length() - 1;
      }
      tokenLocations.put(Range.closed(JavaOutput.startTok(token).getPosition(), upper), token);
    }
    positionTokenMap = tokenLocations.build();

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
   *
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
    ImmutableList<Tok> toks = buildToks(text, ImmutableSet.of());
    kN = getLast(toks).getIndex();
    computeRanges(toks);
    return toks;
  }

  /**
   * Lex the input and build the list of toks.
   *
   * @param text the text to be lexed.
   * @param stopTokens a set of tokens which should cause lexing to stop. If one of these is found,
   *     the returned list will include tokens up to but not including that token.
   */
  static ImmutableList<Tok> buildToks(String text, ImmutableSet<TokenKind> stopTokens)
      throws FormatterException {
    stopTokens = ImmutableSet.<TokenKind>builder().addAll(stopTokens).add(TokenKind.EOF).build();
    Context context = new Context();
    new JavacFileManager(context, true, UTF_8);
    DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    context.put(DiagnosticListener.class, diagnosticCollector);
    Log log = Log.instance(context);
    log.useSource(
        new SimpleJavaFileObject(URI.create("Source.java"), Kind.SOURCE) {
          @Override
          public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return text;
          }
        });
    DeferredDiagnosticHandler diagnostics = new DeferredDiagnosticHandler(log);
    ImmutableList<RawTok> rawToks = JavacTokens.getTokens(text, context, stopTokens);
    if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)) {
      return ImmutableList.of(new Tok(0, "", "", 0, 0, true, null)); // EOF
    }
    int kN = 0;
    List<Tok> toks = new ArrayList<>();
    int charI = 0;
    int columnI = 0;
    for (RawTok t : rawToks) {
      if (stopTokens.contains(t.kind())) {
        break;
      }
      int charI0 = t.pos();
      // Get string, possibly with Unicode escapes.
      String originalTokText = text.substring(charI0, t.endPos());
      String tokText =
          t.kind() == TokenKind.STRINGLITERAL
              ? t.stringVal() // Unicode escapes removed.
              : originalTokText;
      char tokText0 = tokText.charAt(0); // The token's first character.
      final boolean isToken; // Is this tok a token?
      final boolean isNumbered; // Is this tok numbered? (tokens and comments)
      String extraNewline = null; // Extra newline at end?
      List<String> strings = new ArrayList<>();
      if (Character.isWhitespace(tokText0)) {
        isToken = false;
        isNumbered = false;
        Iterator<String> it = Newlines.lineIterator(originalTokText);
        while (it.hasNext()) {
          String line = it.next();
          String newline = Newlines.getLineEnding(line);
          if (newline != null) {
            String spaces = line.substring(0, line.length() - newline.length());
            if (!spaces.isEmpty()) {
              strings.add(spaces);
            }
            strings.add(newline);
          } else if (!line.isEmpty()) {
            strings.add(line);
          }
        }
      } else if (tokText.startsWith("'") || tokText.startsWith("\"")) {
        isToken = true;
        isNumbered = true;
        strings.add(originalTokText);
      } else if (tokText.startsWith("//") || tokText.startsWith("/*")) {
        // For compatibility with an earlier lexer, the newline after a // comment is its own tok.
        if (tokText.startsWith("//")
            && (originalTokText.endsWith("\n") || originalTokText.endsWith("\r"))) {
          extraNewline = Newlines.getLineEnding(originalTokText);
          tokText = tokText.substring(0, tokText.length() - extraNewline.length());
          originalTokText =
              originalTokText.substring(0, originalTokText.length() - extraNewline.length());
        }
        isToken = false;
        isNumbered = true;
        strings.add(originalTokText);
      } else if (Character.isJavaIdentifierStart(tokText0)
          || Character.isDigit(tokText0)
          || (tokText0 == '.' && tokText.length() > 1 && Character.isDigit(tokText.charAt(1)))) {
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
                t.kind()));
        charI += originalTokText.length();
        columnI = updateColumn(columnI, originalTokText);

      } else {
        if (strings.size() != 1 && !tokText.equals(originalTokText)) {
          throw new FormatterException(
              "Unicode escapes not allowed in whitespace or multi-character operators");
        }
        for (String str : strings) {
          toks.add(new Tok(isNumbered ? kN++ : -1, str, str, charI, columnI, isToken, null));
          charI += str.length();
          columnI = updateColumn(columnI, originalTokText);
        }
      }
      if (extraNewline != null) {
        toks.add(new Tok(-1, extraNewline, extraNewline, charI, columnI, false, null));
        columnI = 0;
        charI += extraNewline.length();
      }
    }
    toks.add(new Tok(kN, "", "", charI, columnI, true, null)); // EOF tok.
    return ImmutableList.copyOf(toks);
  }

  private static int updateColumn(int columnI, String originalTokText) {
    Integer last = Iterators.getLast(Newlines.lineOffsetIterator(originalTokText));
    if (last > 0) {
      columnI = originalTokText.length() - last;
    } else {
      columnI += originalTokText.length();
    }
    return columnI;
  }

  private static ImmutableList<Token> buildTokens(List<Tok> toks) {
    ImmutableList.Builder<Token> tokens = ImmutableList.builder();
    int k = 0;
    int kN = toks.size();

    // Remaining non-tokens before the token go here.
    ImmutableList.Builder<Tok> toksBefore = ImmutableList.builder();

    OUTERMOST:
    while (k < kN) {
      while (!toks.get(k).isToken()) {
        Tok tok = toks.get(k++);
        toksBefore.add(tok);
        if (isParamComment(tok)) {
          while (toks.get(k).isNewline()) {
            // drop newlines after parameter comments
            k++;
          }
        }
      }
      Tok tok = toks.get(k++);

      // Non-tokens starting on the same line go here too.
      ImmutableList.Builder<Tok> toksAfter = ImmutableList.builder();
      OUTER:
      while (k < kN && !toks.get(k).isToken()) {
        // Don't attach inline comments to certain leading tokens, e.g. for `f(/*flag1=*/true).
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
        if (toks.get(k).isJavadocComment()) {
          switch (tok.getText()) {
            case ";":
              break OUTER;
            default:
              break;
          }
        }
        if (isParamComment(toks.get(k))) {
          tokens.add(new Token(toksBefore.build(), tok, toksAfter.build()));
          toksBefore = ImmutableList.<Tok>builder().add(toks.get(k++));
          // drop newlines after parameter comments
          while (toks.get(k).isNewline()) {
            k++;
          }
          continue OUTERMOST;
        }
        Tok nonTokenAfter = toks.get(k++);
        toksAfter.add(nonTokenAfter);
        if (Newlines.containsBreaks(nonTokenAfter.getText())) {
          break;
        }
      }
      tokens.add(new Token(toksBefore.build(), tok, toksAfter.build()));
      toksBefore = ImmutableList.builder();
    }
    return tokens.build();
  }

  private static boolean isParamComment(Tok tok) {
    return tok.isSlashStarComment()
        && tok.getText().matches("\\/\\*[A-Za-z0-9\\s_\\-]+=\\s*\\*\\/");
  }

  /**
   * Convert from an offset and length flag pair to a token range.
   *
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
    if (length < 0) {
      return EMPTY_RANGE;
    }
    if (length == 0) {
      // 0 stands for "format the line under the cursor"
      length = 1;
    }
    ImmutableCollection<Token> enclosed =
        getPositionTokenMap()
            .subRangeMap(Range.closedOpen(offset, offset + length))
            .asMapOfRanges()
            .values();
    if (enclosed.isEmpty()) {
      return EMPTY_RANGE;
    }
    return Range.closedOpen(
        enclosed.iterator().next().getTok().getIndex(), getLast(enclosed).getTok().getIndex() + 1);
  }

  /**
   * Get the number of toks.
   *
   * @return the number of toks, including the EOF tok
   */
  int getkN() {
    return kN;
  }

  /**
   * Get the Token by index.
   *
   * @param k the token index
   */
  Token getToken(int k) {
    return kToToken[k];
  }

  /**
   * Get the input tokens.
   *
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
   *
   * @return the navigable map from position to {@link Token}
   */
  @Override
  public ImmutableRangeMap<Integer, Token> getPositionTokenMap() {
    return positionTokenMap;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("tokens", tokens)
        .add("super", super.toString())
        .toString();
  }

  private JCCompilationUnit unit;

  @Override
  public int getLineNumber(int inputPosition) {
    Verify.verifyNotNull(unit, "Expected compilation unit to be set.");
    return unit.getLineMap().getLineNumber(inputPosition);
  }

  @Override
  public int getColumnNumber(int inputPosition) {
    Verify.verifyNotNull(unit, "Expected compilation unit to be set.");
    return unit.getLineMap().getColumnNumber(inputPosition);
  }

  // TODO(cushon): refactor JavaInput so the CompilationUnit can be passed into
  // the constructor.
  public void setCompilationUnit(JCCompilationUnit unit) {
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
