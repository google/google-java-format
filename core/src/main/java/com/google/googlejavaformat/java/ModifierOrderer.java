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
import javax.lang.model.element.Modifier;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;

/** Fixes sequences of modifiers to be in JLS order. */
public class ModifierOrderer {

  /**
   * Returns the {@link javax.lang.model.element.Modifier} for the given token id, or {@code null}.
   */
  static Modifier getModifier(int tokenId) {
    switch (tokenId) {
      case ITerminalSymbols.TokenNamepublic:
        return Modifier.PUBLIC;
      case ITerminalSymbols.TokenNameprotected:
        return Modifier.PROTECTED;
      case ITerminalSymbols.TokenNameprivate:
        return Modifier.PRIVATE;
      case ITerminalSymbols.TokenNameabstract:
        return Modifier.ABSTRACT;
      case ITerminalSymbols.TokenNamestatic:
        return Modifier.STATIC;
      case ITerminalSymbols.TokenNamedefault:
        // TODO(cushon): handle default
        // return Modifier.DEFAULT;
        return null;
      case ITerminalSymbols.TokenNamefinal:
        return Modifier.FINAL;
      case ITerminalSymbols.TokenNametransient:
        return Modifier.TRANSIENT;
      case ITerminalSymbols.TokenNamevolatile:
        return Modifier.VOLATILE;
      case ITerminalSymbols.TokenNamesynchronized:
        return Modifier.SYNCHRONIZED;
      case ITerminalSymbols.TokenNamenative:
        return Modifier.NATIVE;
      case ITerminalSymbols.TokenNamestrictfp:
        return Modifier.STRICTFP;
      default:
        return null;
    }
  }

  /** Reorders all modifiers in the given text to be in JLS order. */
  public static String reorderModifiers(String text) throws FormatterException {
    return reorderModifiers(text, Collections.singleton(Range.closedOpen(0, text.length())));
  }

  /**
   * Reorders all modifiers in the given text and within the given character ranges to be in JLS
   * order.
   */
  public static String reorderModifiers(String text, Collection<Range<Integer>> characterRanges)
      throws FormatterException {
    JavaInput javaInput = new JavaInput(text);
    if (javaInput.getTokens().isEmpty()) {
      // There weren't any tokens, possible because of a lexing error.
      // Errors about invalid input will be reported later after parsing.
      return text;
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
    return applyReplacements(javaInput.getText(), replacements);
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
    return getModifier(((JavaInput.Tok) token.getTok()).id());
  }

  /** Applies replacements to the given string. */
  private static String applyReplacements(
      String text, TreeRangeMap<Integer, String> replacementMap) {
    // process in descending order so the replacement ranges aren't perturbed if any replacements
    // differ in size from the input
    Map<Range<Integer>, String> ranges = replacementMap.asDescendingMapOfRanges();
    if (ranges.isEmpty()) {
      return text;
    }
    StringBuilder sb = new StringBuilder(text);
    for (Entry<Range<Integer>, String> entry : ranges.entrySet()) {
      Range<Integer> range = entry.getKey();
      sb.replace(range.lowerEndpoint(), range.upperEndpoint(), entry.getValue());
    }
    return sb.toString();
  }
}
