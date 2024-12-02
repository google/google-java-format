/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.google.googlejavaformat.java;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getLast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeMap;
import com.google.googlejavaformat.Input.Tok;
import com.google.googlejavaformat.Input.Token;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.lang.model.element.Modifier;
import org.jspecify.annotations.Nullable;

/** Fixes sequences of modifiers to be in JLS order. */
final class ModifierOrderer {

  /** Reorders all modifiers in the given text to be in JLS order. */
  static JavaInput reorderModifiers(String text) throws FormatterException {
    return reorderModifiers(
        new JavaInput(text), ImmutableList.of(Range.closedOpen(0, text.length())));
  }

  /**
   * A class that contains the tokens corresponding to a modifier. This is usually a single token
   * (e.g. for {@code public}), but may be multiple tokens for modifiers containing {@code -} (e.g.
   * {@code non-sealed}).
   */
  static class ModifierTokens implements Comparable<ModifierTokens> {
    private final ImmutableList<Token> tokens;
    private final Modifier modifier;

    static ModifierTokens create(ImmutableList<Token> tokens) {
      return new ModifierTokens(tokens, asModifier(tokens));
    }

    static ModifierTokens empty() {
      return new ModifierTokens(ImmutableList.of(), null);
    }

    ModifierTokens(ImmutableList<Token> tokens, Modifier modifier) {
      this.tokens = tokens;
      this.modifier = modifier;
    }

    boolean isEmpty() {
      return tokens.isEmpty() || modifier == null;
    }

    Modifier modifier() {
      return modifier;
    }

    ImmutableList<Token> tokens() {
      return tokens;
    }

    private Token first() {
      return tokens.get(0);
    }

    private Token last() {
      return getLast(tokens);
    }

    int startPosition() {
      return first().getTok().getPosition();
    }

    int endPosition() {
      return last().getTok().getPosition() + last().getTok().getText().length();
    }

    ImmutableList<? extends Tok> getToksBefore() {
      return first().getToksBefore();
    }

    ImmutableList<? extends Tok> getToksAfter() {
      return last().getToksAfter();
    }

    @Override
    public int compareTo(ModifierTokens o) {
      checkState(!isEmpty()); // empty ModifierTokens are filtered out prior to sorting
      return modifier.compareTo(o.modifier);
    }
  }

  /**
   * Reorders all modifiers in the given text and within the given character ranges to be in JLS
   * order.
   */
  static JavaInput reorderModifiers(JavaInput javaInput, Collection<Range<Integer>> characterRanges)
      throws FormatterException {
    if (javaInput.getTokens().isEmpty()) {
      // There weren't any tokens, possible because of a lexing error.
      // Errors about invalid input will be reported later after parsing.
      return javaInput;
    }
    RangeSet<Integer> tokenRanges = javaInput.characterRangesToTokenRanges(characterRanges);
    Iterator<? extends Token> it = javaInput.getTokens().iterator();
    TreeRangeMap<Integer, String> replacements = TreeRangeMap.create();
    while (it.hasNext()) {
      ModifierTokens tokens = getModifierTokens(it);
      if (tokens.isEmpty()
          || !tokens.tokens().stream()
              .allMatch(token -> tokenRanges.contains(token.getTok().getIndex()))) {
        continue;
      }

      List<ModifierTokens> modifierTokens = new ArrayList<>();

      int begin = tokens.startPosition();
      modifierTokens.add(tokens);

      int end = -1;
      while (it.hasNext()) {
        tokens = getModifierTokens(it);
        if (tokens.isEmpty()) {
          break;
        }
        modifierTokens.add(tokens);
        end = tokens.endPosition();
      }

      if (!Ordering.natural().isOrdered(modifierTokens)) {
        List<ModifierTokens> sorted = Ordering.natural().sortedCopy(modifierTokens);
        StringBuilder replacement = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
          if (i > 0) {
            addTrivia(replacement, modifierTokens.get(i).getToksBefore());
          }
          replacement.append(sorted.get(i).modifier());
          if (i < (sorted.size() - 1)) {
            addTrivia(replacement, modifierTokens.get(i).getToksAfter());
          }
        }
        replacements.put(Range.closedOpen(begin, end), replacement.toString());
      }
    }
    return applyReplacements(javaInput, replacements);
  }

  private static void addTrivia(StringBuilder replacement, ImmutableList<? extends Tok> toks) {
    for (Tok tok : toks) {
      replacement.append(tok.getText());
    }
  }

  private static @Nullable ModifierTokens getModifierTokens(Iterator<? extends Token> it) {
    Token token = it.next();
    ImmutableList.Builder<Token> result = ImmutableList.builder();
    result.add(token);
    if (!token.getTok().getText().equals("non")) {
      return ModifierTokens.create(result.build());
    }
    if (!it.hasNext()) {
      return ModifierTokens.empty();
    }
    Token dash = it.next();
    result.add(dash);
    if (!dash.getTok().getText().equals("-") || !it.hasNext()) {
      return ModifierTokens.empty();
    }
    result.add(it.next());
    return ModifierTokens.create(result.build());
  }

  private static @Nullable Modifier asModifier(ImmutableList<Token> tokens) {
    if (tokens.size() == 1) {
      return asModifier(tokens.get(0));
    }
    Modifier modifier = asModifier(getLast(tokens));
    if (modifier == null) {
      return null;
    }
    return Modifier.valueOf("NON_" + modifier.name());
  }

  /**
   * Returns the given token as a {@link javax.lang.model.element.Modifier}, or {@code null} if it
   * is not a modifier.
   */
  private static @Nullable Modifier asModifier(Token token) {
    TokenKind kind = ((JavaInput.Tok) token.getTok()).kind();
    if (kind != null) {
      switch (kind) {
        case PUBLIC:
          return Modifier.PUBLIC;
        case PROTECTED:
          return Modifier.PROTECTED;
        case PRIVATE:
          return Modifier.PRIVATE;
        case ABSTRACT:
          return Modifier.ABSTRACT;
        case STATIC:
          return Modifier.STATIC;
        case DEFAULT:
          return Modifier.DEFAULT;
        case FINAL:
          return Modifier.FINAL;
        case TRANSIENT:
          return Modifier.TRANSIENT;
        case VOLATILE:
          return Modifier.VOLATILE;
        case SYNCHRONIZED:
          return Modifier.SYNCHRONIZED;
        case NATIVE:
          return Modifier.NATIVE;
        case STRICTFP:
          return Modifier.STRICTFP;
        default: // fall out
      }
    }
    switch (token.getTok().getText()) {
      case "sealed":
        return Modifier.valueOf("SEALED");
      default:
        return null;
    }
  }

  /** Applies replacements to the given string. */
  private static JavaInput applyReplacements(
      JavaInput javaInput, TreeRangeMap<Integer, String> replacementMap) throws FormatterException {
    // process in descending order so the replacement ranges aren't perturbed if any replacements
    // differ in size from the input
    Map<Range<Integer>, String> ranges = replacementMap.asDescendingMapOfRanges();
    if (ranges.isEmpty()) {
      return javaInput;
    }
    StringBuilder sb = new StringBuilder(javaInput.getText());
    for (Entry<Range<Integer>, String> entry : ranges.entrySet()) {
      Range<Integer> range = entry.getKey();
      sb.replace(range.lowerEndpoint(), range.upperEndpoint(), entry.getValue());
    }
    return new JavaInput(sb.toString());
  }
}
