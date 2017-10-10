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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeMap;
import com.google.googlejavaformat.Input.Tok;
import com.google.googlejavaformat.Input.Token;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.openjdk.javax.lang.model.element.Modifier;
import org.openjdk.tools.javac.parser.Tokens.TokenKind;

/** Fixes sequences of modifiers to be in JLS order. */
final class ModifierOrderer {

  /**
   * Returns the {@link javax.lang.model.element.Modifier} for the given token kind, or {@code
   * null}.
   */
  private static Modifier getModifier(TokenKind kind) {
    if (kind == null) {
      return null;
    }
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
      default:
        return null;
    }
  }

  /** Reorders all modifiers in the given text to be in JLS order. */
  static JavaInput reorderModifiers(String text) throws FormatterException {
    return reorderModifiers(
        new JavaInput(text), ImmutableList.of(Range.closedOpen(0, text.length())));
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
      Token token = it.next();
      if (!tokenRanges.contains(token.getTok().getIndex())) {
        continue;
      }
      Modifier mod = asModifier(token);
      if (mod == null) {
        continue;
      }

      List<Token> modifierTokens = new ArrayList<>();
      List<Modifier> mods = new ArrayList<>();

      int begin = token.getTok().getPosition();
      mods.add(mod);
      modifierTokens.add(token);

      int end = -1;
      while (it.hasNext()) {
        token = it.next();
        mod = asModifier(token);
        if (mod == null) {
          break;
        }
        mods.add(mod);
        modifierTokens.add(token);
        end = token.getTok().getPosition() + token.getTok().length();
      }

      if (!Ordering.natural().isOrdered(mods)) {
        Collections.sort(mods);
        StringBuilder replacement = new StringBuilder();
        for (int i = 0; i < mods.size(); i++) {
          if (i > 0) {
            addTrivia(replacement, modifierTokens.get(i).getToksBefore());
          }
          replacement.append(mods.get(i).toString());
          if (i < (modifierTokens.size() - 1)) {
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

  /**
   * Returns the given token as a {@link javax.lang.model.element.Modifier}, or {@code null} if it
   * is not a modifier.
   */
  private static Modifier asModifier(Token token) {
    return getModifier(((JavaInput.Tok) token.getTok()).kind());
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
