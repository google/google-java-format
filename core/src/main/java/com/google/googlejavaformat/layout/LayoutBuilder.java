/*
 * Copyright 2026 Google Inc.
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

package com.google.googlejavaformat.layout;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.Math.max;
import static java.lang.Math.min;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Range;
import com.google.googlejavaformat.CloseOp;
import com.google.googlejavaformat.Doc.Break;
import com.google.googlejavaformat.Doc.FillMode;
import com.google.googlejavaformat.Doc.Space;
import com.google.googlejavaformat.Doc.Token;
import com.google.googlejavaformat.Doc.Token.RealOrImaginary;
import com.google.googlejavaformat.FormatterDiagnostic;
import com.google.googlejavaformat.FormattingError;
import com.google.googlejavaformat.Indent;
import com.google.googlejavaformat.Input;
import com.google.googlejavaformat.Op;
import com.google.googlejavaformat.OpenOp;
import com.google.googlejavaformat.OpsBuilder.BlankLineWanted;
import com.google.googlejavaformat.Output;
import com.google.googlejavaformat.Output.BreakTag;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// A builder that interfaces with AST visitors and directly produces a declarative [Doc] layout
/// tree.
///
/// The token handling and comment splicing are adapted from
/// [com.google.googlejavaformat.OpsBuilder], which builds the input to the Oppen-style
/// [com.google.googlejavaformat.Doc] engine.
public final class LayoutBuilder {

  private static final DiscreteDomain<Integer> INTEGERS = DiscreteDomain.integers();
  private static final Indent.Const ZERO = Indent.Const.ZERO;

  private final Input input;
  private final Output output;
  private final List<Event> events = new ArrayList<>();

  private int tokenI = 0;
  private int inputPosition = Integer.MIN_VALUE;
  private int depth = 0;
  private int lastPartialFormatBoundary = -1;

  public LayoutBuilder(Input input, Output output) {
    this.input = input;
    this.output = output;
  }

  public Input getInput() {
    return input;
  }

  public int depth() {
    return depth;
  }

  public void checkClosed(int previous) {
    if (depth != previous) {
      throw new FormattingError(diagnostic(String.format("saw %d unclosed ops", depth)));
    }
  }

  public FormatterDiagnostic diagnostic(String message) {
    return input.createDiagnostic(inputPosition, message);
  }

  // Adapted from OpsBuilder#sync.
  public void sync(int inputPosition) {
    if (inputPosition > this.inputPosition) {
      ImmutableList<? extends Input.Token> tokens = input.getTokens();
      int tokensN = tokens.size();
      this.inputPosition = inputPosition;
      if (tokenI < tokensN && inputPosition > tokens.get(tokenI).getTok().getPosition()) {
        Input.Token token = tokens.get(tokenI++);
        throw new FormattingError(
            diagnostic(String.format("did not generate token \"%s\"", token.getTok().getText())));
      }
    }
  }

  // Adapted from OpsBuilder#drain.
  /// Emits any input tokens the AST visitor didn't, as imaginary tokens
  /// ([RealOrImaginary#IMAGINARY]) that are reproduced verbatim along with the whitespace and
  /// comments around them. In practice this is the end-of-input token and any trailing whitespace.
  public void drain() {
    int endPosition = input.getText().length() + 1;
    if (this.inputPosition < endPosition) {
      ImmutableList<? extends Input.Token> tokens = input.getTokens();
      int tokensN = tokens.size();
      while (tokenI < tokensN && tokens.get(tokenI).getTok().getPosition() < endPosition) {
        Input.Token token = tokens.get(tokenI++);
        events.add(
            new TokenOp(
                token,
                RealOrImaginary.IMAGINARY,
                ZERO,
                /* breakAndIndentTrailingComment= */ Optional.empty(),
                /* linesIndentedToTokenStart= */ Optional.empty()));
      }
    }
    this.inputPosition = endPosition;
    checkClosed(0);
  }

  public void open(Indent plusIndent) {
    open(plusIndent, ZERO, HugRole.NONE, null, /* fallbackIfFlatOnNextLine= */ true);
  }

  private void open(
      Indent plusIndent,
      Indent huggedPlusIndent,
      HugRole hugRole,
      HugPoint hugPoint,
      boolean fallbackIfFlatOnNextLine) {
    depth++;
    events.add(new Open(plusIndent, huggedPlusIndent, hugRole, hugPoint, fallbackIfFlatOnNextLine));
  }

  /// The start of the body of a construct that can be hugged, i.e. laid out starting on the same
  /// line as preceding content, e.g. the body of a lambda. See [#openHuggable].
  public static final class HugPoint {
    /// A zero-width marker for this point in the [Doc].
    private final Doc marker = new Doc.Text("");

    private HugPoint() {}
  }

  /// Returns a new [HugPoint], to be passed to [#hugPoint] and an enclosing level.
  public static HugPoint newHugPoint() {
    return new HugPoint();
  }

  /// Marks the position of `hugPoint`.
  public void hugPoint(HugPoint hugPoint) {
    events.add(new HugPointOp(hugPoint));
  }

  /// Opens a level for an argument list whose argument may be hugged, e.g. `f(x -> { ... })`. The
  /// level's direct breaks, and the direct breaks of a nested level opened with
  /// [#openHuggedArguments], are the ones that are not taken when hugging, and everything up to
  /// `hugPoint` must then fit on the current line.
  ///
  /// When hugged, the hugged content is indented relative to the line containing the call plus
  /// `huggedPlusIndent`, instead of by the enclosing levels' extra indents plus `plusIndent`. If
  /// `fallbackIfFlatOnNextLine` is true, the fallback layout is preferred when a single direct
  /// break lets its suffix fit flat on the next line.
  public void openHuggable(
      Indent plusIndent,
      Indent huggedPlusIndent,
      HugPoint hugPoint,
      boolean fallbackIfFlatOnNextLine) {
    open(plusIndent, huggedPlusIndent, HugRole.HUGGABLE, hugPoint, fallbackIfFlatOnNextLine);
  }

  /// Opens the level holding the arguments of an enclosing [#openHuggable] level.
  public void openHuggedArguments() {
    open(ZERO, ZERO, HugRole.ARGUMENTS, null, /* fallbackIfFlatOnNextLine= */ true);
  }

  public void close() {
    depth--;
    if (depth < 0) {
      throw new AssertionError();
    }
    events.add(new Close());
  }

  // Adapted from OpsBuilder#peekToken.
  public Optional<String> peekToken() {
    return peekToken(0);
  }

  public Optional<String> peekToken(int skip) {
    ImmutableList<? extends Input.Token> tokens = input.getTokens();
    int idx = tokenI + skip;
    return idx < tokens.size()
        ? Optional.of(tokens.get(idx).getTok().getOriginalText())
        : Optional.empty();
  }

  // Adapted from OpsBuilder#peekTokens.
  /// Returns the toks of the upcoming tokens, starting with the current one, for as long as they
  /// satisfy `predicate`. The tokens aren't consumed.
  ///
  /// @param expectedStartPosition the position of the current token, which is checked
  public ImmutableList<Input.Tok> peekTokens(
      int expectedStartPosition, Predicate<Input.Tok> predicate) {
    ImmutableList<? extends Input.Token> tokens = input.getTokens();
    Preconditions.checkState(
        tokens.get(tokenI).getTok().getPosition() == expectedStartPosition,
        "Expected the current token to be at position %s, found: %s",
        expectedStartPosition,
        tokens.get(tokenI));
    return tokens.subList(tokenI, tokens.size()).stream()
        .map(Input.Token::getTok)
        .takeWhile(predicate)
        .collect(toImmutableList());
  }

  // Adapted from OpsBuilder#actualSize.
  /// Returns the size of the source text spanned by the tokens starting at `position`, including
  /// leading and trailing comments.
  public int actualSize(int position, int length) {
    Input.Token startToken = input.getPositionTokenMap().get(position);
    int start = startToken.getTok().getPosition();
    for (Input.Tok tok : startToken.getToksBefore()) {
      if (tok.isComment()) {
        start = min(start, tok.getPosition());
      }
    }
    Input.Token endToken = input.getPositionTokenMap().get(position + length - 1);
    int end = endToken.getTok().getPosition() + endToken.getTok().length();
    for (Input.Tok tok : endToken.getToksAfter()) {
      if (tok.isComment()) {
        end = max(end, tok.getPosition() + tok.length());
      }
    }
    return end - start;
  }

  // Adapted from OpsBuilder#actualStartColumn.
  public Integer actualStartColumn(int position) {
    Input.Token startToken = input.getPositionTokenMap().get(position);
    int start = startToken.getTok().getPosition();
    int line0 = input.getLineNumber(start);
    for (Input.Tok tok : startToken.getToksBefore()) {
      if (line0 != input.getLineNumber(tok.getPosition())) {
        return start;
      }
      if (tok.isComment()) {
        start = min(start, tok.getPosition());
      }
    }
    return start;
  }

  public void guessToken(String token) {
    token(token, RealOrImaginary.IMAGINARY, ZERO, Optional.empty());
  }

  // Adapted from OpsBuilder#token.
  public void token(
      String token,
      RealOrImaginary realOrImaginary,
      Indent plusIndentCommentsBefore,
      Optional<Indent> breakAndIndentTrailingComment) {
    token(
        token,
        realOrImaginary,
        plusIndentCommentsBefore,
        breakAndIndentTrailingComment,
        Optional.empty());
  }

  private void token(
      String token,
      RealOrImaginary realOrImaginary,
      Indent plusIndentCommentsBefore,
      Optional<Indent> breakAndIndentTrailingComment,
      Optional<ImmutableList<String>> linesIndentedToTokenStart) {
    ImmutableList<? extends Input.Token> tokens = input.getTokens();
    if (peekToken().filter(token::equals).isPresent()) {
      events.add(
          new TokenOp(
              tokens.get(tokenI++),
              RealOrImaginary.REAL,
              plusIndentCommentsBefore,
              breakAndIndentTrailingComment,
              linesIndentedToTokenStart));
    } else if (realOrImaginary.isReal()) {
      // Generated a "bad" token, which doesn't exist on the input. Complain, unless (for example)
      // we're guessing at an optional token, in which case it's dropped.
      throw new FormattingError(
          diagnostic(
              String.format(
                  "expected token: '%s'; generated %s instead", peekToken().orElse(null), token)));
    }
  }

  /// Emits a real multi-line token whose non-empty lines after the first are indented to the column
  /// where the token starts, for example a text block.
  public void multilineToken(String token, ImmutableList<String> lines) {
    token(token, RealOrImaginary.REAL, ZERO, Optional.empty(), Optional.of(lines));
  }

  public void op(String op) {
    int opN = op.length();
    for (int i = 0; i < opN; i++) {
      token(op.substring(i, i + 1), RealOrImaginary.REAL, ZERO, Optional.empty());
    }
  }

  public void space() {
    events.add(new SpaceOp());
  }

  public void breakOp() {
    breakOp(FillMode.UNIFIED, "", ZERO);
  }

  public void breakOp(Indent plusIndent) {
    breakOp(FillMode.UNIFIED, "", plusIndent);
  }

  public void breakOp(String flat) {
    breakOp(FillMode.UNIFIED, flat, ZERO);
  }

  public void breakOp(FillMode fillMode, String flat, Indent plusIndent) {
    breakOp(fillMode, flat, plusIndent, Optional.empty());
  }

  public void breakOp(
      FillMode fillMode, String flat, Indent plusIndent, Optional<BreakTag> optionalTag) {
    Doc.BreakMode mode =
        switch (fillMode) {
          case UNIFIED -> Doc.BreakMode.UNIFIED;
          case INDEPENDENT -> Doc.BreakMode.FILL;
          case FORCED -> Doc.BreakMode.FORCED;
        };
    events.add(new LineBreak(mode, flat, plusIndent, optionalTag));
  }

  public void breakToFill() {
    breakOp(FillMode.INDEPENDENT, "", ZERO);
  }

  public void breakToFill(String flat) {
    breakOp(FillMode.INDEPENDENT, flat, ZERO);
  }

  public void forcedBreak() {
    breakOp(FillMode.FORCED, "", ZERO);
  }

  public void forcedBreak(Indent plusIndent) {
    breakOp(FillMode.FORCED, "", plusIndent);
  }

  // Adapted from OpsBuilder#markForPartialFormat.
  public void markForPartialFormat() {
    if (lastPartialFormatBoundary == -1) {
      lastPartialFormatBoundary = tokenI;
      return;
    }
    if (tokenI == lastPartialFormatBoundary) {
      return;
    }
    Input.Token start = input.getTokens().get(lastPartialFormatBoundary);
    Input.Token end = input.getTokens().get(tokenI - 1);
    output.markForPartialFormat(start, end);
    lastPartialFormatBoundary = tokenI;
  }

  public void blankLineWanted(BlankLineWanted wanted) {
    output.blankLine(getI(input.getTokens().get(tokenI)), wanted);
  }

  /// Prevents comments before upcoming tokens from being hoisted into enclosing groups.
  public void fenceComments() {
    events.add(new FenceComments());
  }

  public void add(Op op) {
    switch (op) {
      case OpenOp openOp -> open(openOp.plusIndent());
      case CloseOp unused -> close();
      case Break b -> breakOp(b.fillMode(), b.flat(), b.plusIndent(), b.optTag());
      case Space unused -> space();
      case Token t ->
          token(
              t.getToken().getTok().getOriginalText(),
              t.realOrImaginary(),
              t.getPlusIndentCommentsBefore(),
              t.breakAndIndentTrailingComment());
      default -> throw new IllegalArgumentException("unsupported op: " + op);
    }
  }

  public void addAll(List<Op> ops) {
    for (Op op : ops) {
      add(op);
    }
  }

  // Adapted from OpsBuilder#getI.
  private static int getI(Input.Token token) {
    for (Input.Tok tok : token.getToksBefore()) {
      if (tok.getIndex() >= 0) {
        return tok.getIndex();
      }
    }
    return token.getTok().getIndex();
  }

  /// Converts the recorded events and comments into a declarative [Doc] tree.
  public Doc build() {
    markForPartialFormat();
    return buildTree(spliceEvents(commentEvents()));
  }

  // Adapted from OpsBuilder#build. Token events can have associated non-tokens, including
  // comments, which we need to insert. They can also cause line breaks, so we insert them before
  // or after the current level, when possible.
  /// Returns the events for the non-tokens (comments and whitespace) around each token, keyed by
  /// the index in `events` that they should be inserted before.
  private ListMultimap<Integer, Event> commentEvents() {
    ListMultimap<Integer, Event> tokEvents = ArrayListMultimap.create();
    for (int i = 0; i < events.size(); i++) {
      if (events.get(i) instanceof TokenOp tokenOp) {
        // Insert toksBefore before any groups the token opens, and toksAfter after any groups it
        // closes.
        List<Event> before = tokEvents.get(firstOpenBefore(i));
        List<Event> after = tokEvents.get(lastCloseAfter(i) + 1);
        if (tokenOp.realOrImaginary().isReal()) {
          addToksBefore(tokenOp, before);
          addToksAfter(tokenOp, after);
        } else {
          addImaginaryToks(tokenOp.token(), before, after);
        }
      }
    }
    return tokEvents;
  }

  /// Returns the index of the first of the `Open` events immediately preceding `events[i]`, or `i`
  /// if there are none.
  private int firstOpenBefore(int i) {
    int j = i;
    while (j > 0 && events.get(j - 1) instanceof Open) {
      j--;
    }
    return j;
  }

  /// Returns the index of the last of the `Close` events immediately following `events[i]`, or `i`
  /// if there are none.
  private int lastCloseAfter(int i) {
    int k = i;
    while (k + 1 < events.size() && events.get(k + 1) instanceof Close) {
      k++;
    }
    return k;
  }

  /// Adds the comments before a real token to `out`, with the breaks and spaces around them.
  private void addToksBefore(TokenOp tokenOp, List<Event> out) {
    Input.Token token = tokenOp.token();
    int newlines = 0; // Count of newlines in a row.
    boolean space = false; // Do we need an extra space after a previous "/*" comment?
    boolean lastWasComment = false; // Was the last thing we output a comment?
    boolean allowBlankAfterLastComment = false;
    for (Input.Tok tokBefore : token.getToksBefore()) {
      if (tokBefore.isNewline()) {
        newlines++;
      } else if (tokBefore.isComment()) {
        out.add(
            new LineBreak(
                tokBefore.isSlashSlashComment() ? Doc.BreakMode.FORCED : Doc.BreakMode.UNIFIED,
                "",
                tokenOp.plusIndentCommentsBefore(),
                Optional.empty()));
        out.add(new TokOp(tokBefore));
        if (tokBefore.isSlashSlashComment()) {
          out.add(forcedLineBreak());
        }
        space = tokBefore.isSlashStarComment();
        newlines = 0;
        lastWasComment = true;
        if (tokBefore.isJavadocComment()) {
          out.add(forcedLineBreak());
        }
        allowBlankAfterLastComment =
            tokBefore.isSlashSlashComment()
                || (tokBefore.isSlashStarComment() && !tokBefore.isJavadocComment());
      }
    }
    if (allowBlankAfterLastComment && newlines > 1) {
      // Force a blank line after two newlines in a row following a line or block comment.
      output.blankLine(token.getTok().getIndex(), BlankLineWanted.YES);
    }
    if (lastWasComment && newlines > 0) {
      out.add(forcedLineBreak());
    } else if (space) {
      out.add(new SpaceOp());
    }
  }

  /// Adds the comments after a real token to `out`, with the breaks and spaces around them.
  private static void addToksAfter(TokenOp tokenOp, List<Event> out) {
    for (Input.Tok tokAfter : tokenOp.token().getToksAfter()) {
      if (!tokAfter.isComment()) {
        continue;
      }
      boolean breakAfter =
          tokAfter.isJavadocComment()
              || (tokAfter.isSlashStarComment()
                  && tokenOp.breakAndIndentTrailingComment().isPresent());
      out.add(
          breakAfter
              ? forcedLineBreak(tokenOp.breakAndIndentTrailingComment().orElse(ZERO))
              : new SpaceOp());
      out.add(new TokOp(tokAfter));
      // OpsBuilder adds two forced breaks after a trailing `///` comment (which is both a line
      // comment and a doc comment); one is enough.
      if (breakAfter || tokAfter.isSlashSlashComment()) {
        out.add(forcedLineBreak());
      }
    }
  }

  // Adapted from OpsBuilder#build. This input token was not generated for output (in practice,
  // it's the end-of-input token from drain). As no whitespace or comments were generated for it,
  // copy all of its non-tokens literally, even spaces and newlines.
  private static void addImaginaryToks(Input.Token token, List<Event> before, List<Event> after) {
    int newlines = 0;
    boolean lastWasComment = false;
    for (Input.Tok tokBefore : token.getToksBefore()) {
      if (tokBefore.isNewline()) {
        newlines++;
      } else if (tokBefore.isComment()) {
        newlines = 0;
        lastWasComment = true;
      }
      if (lastWasComment && newlines > 0) {
        before.add(forcedLineBreak());
      }
      before.add(new TokOp(tokBefore));
    }
    for (Input.Tok tokAfter : token.getToksAfter()) {
      after.add(new TokOp(tokAfter));
    }
  }

  // Adapted from OpsBuilder#build.
  /// Returns `events` with the events from `tokEvents` spliced in, suppressing spaces after forced
  /// breaks.
  private List<Event> spliceEvents(ListMultimap<Integer, Event> tokEvents) {
    List<Event> result = new ArrayList<>();
    boolean afterForcedBreak = false;
    for (int i = 0; i < events.size(); i++) {
      afterForcedBreak = addSuppressingSpaces(tokEvents.get(i), afterForcedBreak, result);
      Event ev = events.get(i);
      if (afterForcedBreak && isSpaceLike(ev)) {
        continue;
      }
      result.add(ev);
      // An `Open`, hug point, or comment fence doesn't output anything, so it doesn't end the run
      // that follows a forced break.
      if (!(ev instanceof Open || ev instanceof HugPointOp || ev instanceof FenceComments)) {
        afterForcedBreak = isForcedBreak(ev);
      }
    }
    var unused = addSuppressingSpaces(tokEvents.get(events.size()), afterForcedBreak, result);
    return result;
  }

  /// Appends `toAdd` to `out`, dropping any space that directly follows a forced break, and returns
  /// whether the last event appended was a forced break.
  ///
  /// @param afterForcedBreak whether the last event already in `out` is a forced break
  private static boolean addSuppressingSpaces(
      List<Event> toAdd, boolean afterForcedBreak, List<Event> out) {
    for (Event ev : toAdd) {
      if (!(afterForcedBreak && ev instanceof SpaceOp)) {
        out.add(ev);
        afterForcedBreak = isForcedBreak(ev);
      }
    }
    return afterForcedBreak;
  }

  /// Returns true for an event that's redundant directly after a forced break, which has already
  /// ended the line: a space, or a break with no extra indent that's a space when not taken.
  private static boolean isSpaceLike(Event ev) {
    return switch (ev) {
      case SpaceOp unused -> true;
      case LineBreak lb -> lb.plusIndent().eval() == 0 && lb.flat().equals(" ");
      default -> false;
    };
  }

  /// Builds the tree of groups from `events`.
  ///
  /// Following `DocBuilder`, non-break docs are appended to the group that contains the most recent
  /// break, rather than to the innermost open group. This moves trailing text (e.g. the `;` after a
  /// closed group) into the preceding group, so it's accounted for when deciding whether that group
  /// fits on the line.
  private static Doc buildTree(List<Event> events) {
    LevelBuilder base = new LevelBuilder(Open.BASE);
    ArrayDeque<LevelBuilder> stack = new ArrayDeque<>();
    stack.addLast(base);
    LevelBuilder appendLevel = base;
    for (Event ev : events) {
      switch (ev) {
        case Open open -> stack.addLast(new LevelBuilder(open));
        case Close close -> {
          LevelBuilder top = stack.removeLast();
          stack.getLast().add(top);
        }
        case LineBreak lb -> {
          appendLevel = stack.getLast();
          appendLevel.add(new Doc.Break(lb.mode(), lb.flat(), lb.plusIndent(), lb.tag()));
        }
        case TokenOp tokenOp -> {
          Input.Tok tok = tokenOp.token().getTok();
          Range<Integer> range = Range.singleton(tok.getIndex()).canonical(INTEGERS);
          appendLevel.add(
              tokenOp.linesIndentedToTokenStart().isPresent()
                  ? new Doc.Lines(tokenOp.linesIndentedToTokenStart().get(), range)
                  : new Doc.Text(tok.getOriginalText(), range));
        }
        case TokOp tokOp -> appendLevel.add(new Doc.Tok(tokOp.tok()));
        case HugPointOp hugPointOp -> appendLevel.add(hugPointOp.hugPoint().marker);
        case FenceComments unused -> {}
        case SpaceOp spaceOp -> appendLevel.add(Doc.Space.INSTANCE);
      }
    }
    return base.build();
  }

  private static boolean isForcedBreak(Event ev) {
    return ev instanceof LineBreak lb && lb.mode() == Doc.BreakMode.FORCED;
  }

  private static LineBreak forcedLineBreak() {
    return forcedLineBreak(ZERO);
  }

  private static LineBreak forcedLineBreak(Indent plusIndent) {
    return new LineBreak(Doc.BreakMode.FORCED, "", plusIndent, Optional.empty());
  }

  /// A mutable group under construction; see [#buildTree].
  private static final class LevelBuilder {
    final Indent plusIndent;
    final Indent huggedPlusIndent;
    final HugRole hugRole;
    final HugPoint hugPoint;
    final boolean fallbackIfFlatOnNextLine;
    final List<Object> items = new ArrayList<>();

    LevelBuilder(Open open) {
      this.plusIndent = open.plusIndent();
      this.huggedPlusIndent = open.huggedPlusIndent();
      this.hugRole = open.hugRole();
      this.hugPoint = open.hugPoint();
      this.fallbackIfFlatOnNextLine = open.fallbackIfFlatOnNextLine();
    }

    void add(Object item) {
      items.add(item);
    }

    Doc build() {
      return hugRole == HugRole.HUGGABLE
          ? buildHuggable()
          : new Doc.Group(plusIndent, buildItems());
    }

    ImmutableList<Doc> buildItems() {
      ImmutableList.Builder<Doc> docs = ImmutableList.builder();
      for (Object item : items) {
        docs.add(item instanceof LevelBuilder lb ? lb.build() : (Doc) item);
      }
      return docs.build();
    }

    /// Builds a choice between hugging and the regular layout.
    ///
    /// The hugged layout has the same content as the regular one, but the breaks of this level and
    /// of its [HugRole#ARGUMENTS] level (if any) are replaced by their flat text, and this level's
    /// extra indent is replaced by [#huggedPlusIndent]. Everything up to the hug point must fit on
    /// the current line.
    private Doc buildHuggable() {
      ImmutableList.Builder<Doc> fallback = ImmutableList.builder();
      ImmutableList.Builder<Doc> preferred = ImmutableList.builder();
      boolean canHug = true;
      for (Object item : items) {
        ImmutableList<Doc> flattened;
        Doc fallbackDoc;
        switch (item) {
          case LevelBuilder lb when lb.hugRole == HugRole.ARGUMENTS -> {
            flattened = lb.buildItems();
            fallbackDoc = new Doc.Group(lb.plusIndent, flattened);
          }
          case LevelBuilder lb -> {
            fallbackDoc = lb.build();
            flattened = ImmutableList.of(fallbackDoc);
          }
          case Doc doc -> {
            fallbackDoc = doc;
            flattened = ImmutableList.of(doc);
          }
          default -> throw new AssertionError(item);
        }
        fallback.add(fallbackDoc);
        for (Doc doc : flattened) {
          if (doc instanceof Doc.Break b) {
            canHug &= canFlatten(b);
            preferred.add(new Doc.Text(b.flatText()));
          } else {
            preferred.add(doc);
          }
        }
      }
      Doc.Group fallbackGroup = new Doc.Group(plusIndent, fallback.build());
      Doc.Group preferredGroup = new Doc.Group(huggedPlusIndent, preferred.build());
      int prefixWidth = canHug ? widthBefore(preferredGroup, hugPoint.marker) : -1;
      if (prefixWidth < 0) {
        return fallbackGroup;
      }
      return new Doc.Choice(preferredGroup, prefixWidth, fallbackGroup, fallbackIfFlatOnNextLine);
    }

    /// Forced breaks (e.g. after line comments) must not be flattened.
    private static boolean canFlatten(Doc.Break b) {
      return b.mode() != Doc.BreakMode.FORCED;
    }

    /// Returns the flat width of the content of `doc` before `marker`, or -1 if `doc` doesn't
    /// contain `marker` within fewer than [Doc#MAX_LINE_WIDTH] columns.
    // Hug point markers are equal to any other empty Text, so they must be found by identity.
    @SuppressWarnings("ReferenceEquality")
    private static int widthBefore(Doc doc, Doc marker) {
      if (doc == marker) {
        return 0;
      }
      return switch (doc) {
        case Doc.Group g -> {
          int width = 0;
          for (Doc child : g.docs()) {
            int before = widthBefore(child, marker);
            if (before >= 0) {
              yield min(width + before, Doc.MAX_LINE_WIDTH);
            }
            width += child.width();
            if (width >= Doc.MAX_LINE_WIDTH) {
              yield -1;
            }
          }
          yield -1;
        }
        case Doc.Choice c -> widthBefore(c.fallback(), marker);
        default -> -1;
      };
    }
  }

  private sealed interface Event
      permits Open, Close, LineBreak, TokenOp, TokOp, SpaceOp, HugPointOp, FenceComments {}

  private record Open(
      Indent plusIndent,
      Indent huggedPlusIndent,
      HugRole hugRole,
      HugPoint hugPoint,
      boolean fallbackIfFlatOnNextLine)
      implements Event {
    /// The implicit outermost level.
    static final Open BASE = new Open(ZERO, ZERO, HugRole.NONE, null, true);
  }

  private record HugPointOp(HugPoint hugPoint) implements Event {}

  private record FenceComments() implements Event {}

  /// The role of a level in a huggable argument list; see [#openHuggable].
  private enum HugRole {
    NONE,
    HUGGABLE,
    ARGUMENTS
  }

  private record Close() implements Event {}

  private record LineBreak(
      Doc.BreakMode mode, String flat, Indent plusIndent, Optional<BreakTag> tag)
      implements Event {}

  private record TokenOp(
      Input.Token token,
      RealOrImaginary realOrImaginary,
      Indent plusIndentCommentsBefore,
      Optional<Indent> breakAndIndentTrailingComment,
      Optional<ImmutableList<String>> linesIndentedToTokenStart)
      implements Event {}

  private record TokOp(Input.Tok tok) implements Event {}

  private record SpaceOp() implements Event {}

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("input", input)
        .add("output", output)
        .add("tokenI", tokenI)
        .add("depth", depth)
        .toString();
  }
}
