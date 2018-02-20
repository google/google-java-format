/*
 * Copyright 2016 Google Inc.
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

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.googlejavaformat.Newlines;
import com.google.googlejavaformat.java.JavaInput.Tok;
import java.util.ArrayList;
import java.util.List;
import org.openjdk.tools.javac.parser.Tokens.TokenKind;

/** Orders imports in Java source code. */
public class ImportOrderer {
  /**
   * Reorder the inputs in {@code text}, a complete Java program. On success, another complete Java
   * program is returned, which is the same as the original except the imports are in order.
   *
   * @throws FormatterException if the input could not be parsed.
   */
  public static String reorderImports(String text) throws FormatterException {
    ImmutableList<Tok> toks = JavaInput.buildToks(text, CLASS_START);
    return new ImportOrderer(text, toks).reorderImports();
  }

  /**
   * {@link TokenKind}s that indicate the start of a type definition. We use this to avoid scanning
   * the whole file, since we know that imports must precede any type definition.
   */
  private static final ImmutableSet<TokenKind> CLASS_START =
      ImmutableSet.of(TokenKind.CLASS, TokenKind.INTERFACE, TokenKind.ENUM);

  /**
   * We use this set to find the first import, and again to check that there are no imports after
   * the place we stopped gathering them. An annotation definition ({@code @interface}) is two
   * tokens, the second which is {@code interface}, so we don't need a separate entry for that.
   */
  private static final ImmutableSet<String> IMPORT_OR_CLASS_START =
      ImmutableSet.of("import", "class", "interface", "enum");

  private final String text;
  private final ImmutableList<Tok> toks;
  private final String lineSeparator;

  private ImportOrderer(String text, ImmutableList<Tok> toks) throws FormatterException {
    this.text = text;
    this.toks = toks;
    this.lineSeparator = Newlines.guessLineSeparator(text);
  }

  /** An import statement. */
  private class Import implements Comparable<Import> {
    /** The name being imported, for example {@code java.util.List}. */
    final String imported;

    /** The characters after the final {@code ;}, up to and including the line terminator. */
    final String trailing;

    /** True if this is {@code import static}. */
    final boolean isStatic;

    Import(String imported, String trailing, boolean isStatic) {
      this.imported = imported;
      this.trailing = trailing.trim();
      this.isStatic = isStatic;
    }

    // This is how the sorting happens, including sorting static imports before non-static ones.
    @Override
    public int compareTo(Import that) {
      if (this.isStatic != that.isStatic) {
        return this.isStatic ? -1 : +1;
      }
      return this.imported.compareTo(that.imported);
    }

    // This is a complete line to be output for this import, including the line terminator.
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("import ");
      if (isStatic) {
        sb.append("static ");
      }
      sb.append(imported).append(';');
      if (!trailing.isEmpty()) {
        sb.append(' ').append(trailing);
      }
      sb.append(lineSeparator);
      return sb.toString();
    }
  }

  private String reorderImports() throws FormatterException {
    int firstImportStart;
    Optional<Integer> maybeFirstImport = findIdentifier(0, IMPORT_OR_CLASS_START);
    if (!maybeFirstImport.isPresent() || !tokenAt(maybeFirstImport.get()).equals("import")) {
      // No imports, so nothing to do.
      return text;
    }
    firstImportStart = maybeFirstImport.get();
    int unindentedFirstImportStart = unindent(firstImportStart);

    ImportsAndIndex imports = scanImports(firstImportStart);
    int afterLastImport = imports.index;

    // Make sure there are no more imports before the next class (etc) definition.
    Optional<Integer> maybeLaterImport = findIdentifier(afterLastImport, IMPORT_OR_CLASS_START);
    if (maybeLaterImport.isPresent() && tokenAt(maybeLaterImport.get()).equals("import")) {
      throw new FormatterException("Imports not contiguous (perhaps a comment separates them?)");
    }

    StringBuilder result = new StringBuilder();
    String prefix = tokString(0, unindentedFirstImportStart);
    result.append(prefix);
    if (!prefix.isEmpty() && Newlines.getLineEnding(prefix) == null) {
      result.append(lineSeparator).append(lineSeparator);
    }
    result.append(reorderedImportsString(imports.imports));

    List<String> tail = new ArrayList<>();
    tail.add(CharMatcher.whitespace().trimLeadingFrom(tokString(afterLastImport, toks.size())));
    if (!toks.isEmpty()) {
      Tok lastTok = getLast(toks);
      int tailStart = lastTok.getPosition() + lastTok.length();
      tail.add(text.substring(tailStart));
    }
    if (tail.stream().anyMatch(s -> !s.isEmpty())) {
      result.append(lineSeparator);
      tail.forEach(result::append);
    }

    return result.toString();
  }

  private String tokString(int start, int end) {
    StringBuilder sb = new StringBuilder();
    for (int i = start; i < end; i++) {
      sb.append(toks.get(i).getOriginalText());
    }
    return sb.toString();
  }

  private static class ImportsAndIndex {
    final ImmutableSortedSet<Import> imports;
    final int index;

    ImportsAndIndex(ImmutableSortedSet<Import> imports, int index) {
      this.imports = imports;
      this.index = index;
    }
  }

  /**
   * Scans a sequence of import lines. The parsing uses this approximate grammar:
   *
   * <pre>{@code
   * <imports> -> (<end-of-line> | <import>)*
   * <import> -> "import" <whitespace> ("static" <whitespace>)?
   *    <identifier> ("." <identifier>)* ("." "*")? <whitespace>? ";"
   *    <whitespace>? <line-comment>? <end-of-line>
   * }</pre>
   *
   * @param i the index to start parsing at.
   * @return the result of parsing the imports.
   * @throws FormatterException if imports could not parsed according to the grammar.
   */
  private ImportsAndIndex scanImports(int i) throws FormatterException {
    int afterLastImport = i;
    ImmutableSortedSet.Builder<Import> imports = ImmutableSortedSet.naturalOrder();
    // JavaInput.buildToks appends a zero-width EOF token after all tokens. It won't match any
    // of our tests here and protects us from running off the end of the toks list. Since it is
    // zero-width it doesn't matter if we include it in our string concatenation at the end.
    while (i < toks.size() && tokenAt(i).equals("import")) {
      i++;
      if (isSpaceToken(i)) {
        i++;
      }
      boolean isStatic = tokenAt(i).equals("static");
      if (isStatic) {
        i++;
        if (isSpaceToken(i)) {
          i++;
        }
      }
      if (!isIdentifierToken(i)) {
        throw new FormatterException("Unexpected token after import: " + tokenAt(i));
      }
      StringAndIndex imported = scanImported(i);
      String importedName = imported.string;
      i = imported.index;
      if (isSpaceToken(i)) {
        i++;
      }
      if (!tokenAt(i).equals(";")) {
        throw new FormatterException("Expected ; after import");
      }
      while (tokenAt(i).equals(";")) {
        // Extra semicolons are not allowed by the JLS but are accepted by javac.
        i++;
      }
      StringBuilder trailing = new StringBuilder();
      if (isSpaceToken(i)) {
        trailing.append(tokenAt(i));
        i++;
      }
      if (isSlashSlashCommentToken(i)) {
        trailing.append(tokenAt(i));
        i++;
      }
      if (isNewlineToken(i)) {
        trailing.append(tokenAt(i));
        i++;
      }
      imports.add(new Import(importedName, trailing.toString(), isStatic));
      // Remember the position just after the import we just saw, before skipping blank lines.
      // If the next thing after the blank lines is not another import then we don't want to
      // include those blank lines in the text to be replaced.
      afterLastImport = i;
      while (isNewlineToken(i) || isSpaceToken(i)) {
        i++;
      }
    }
    return new ImportsAndIndex(imports.build(), afterLastImport);
  }

  // Produces the sorted output based on the imports we have scanned.
  private String reorderedImportsString(ImmutableSortedSet<Import> imports) {
    Preconditions.checkArgument(!imports.isEmpty(), "imports");

    Import firstImport = imports.iterator().next();

    // Pretend that the first import was preceded by another import of the same kind
    // (static or non-static), so we don't insert a newline there.
    boolean lastWasStatic = firstImport.isStatic;

    StringBuilder sb = new StringBuilder();
    for (Import thisImport : imports) {
      if (lastWasStatic && !thisImport.isStatic) {
        // Blank line between static and non-static imports.
        sb.append(lineSeparator);
      }
      lastWasStatic = thisImport.isStatic;
      sb.append(thisImport);
    }
    return sb.toString();
  }

  private static class StringAndIndex {
    private final String string;
    private final int index;

    StringAndIndex(String string, int index) {
      this.string = string;
      this.index = index;
    }
  }

  /**
   * Scans the imported thing, the dot-separated name that comes after import [static] and before
   * the semicolon. We don't allow spaces inside the dot-separated name. Wildcard imports are
   * supported: if the input is {@code import java.util.*;} then the returned string will be {@code
   * java.util.*}.
   *
   * @param start the index of the start of the identifier. If the import is {@code import
   *     java.util.List;} then this index points to the token {@code java}.
   * @return the parsed import ({@code java.util.List} in the example) and the index of the first
   *     token after the imported thing ({@code ;} in the example).
   * @throws FormatterException if the imported name could not be parsed.
   */
  private StringAndIndex scanImported(int start) throws FormatterException {
    int i = start;
    StringBuilder imported = new StringBuilder();
    // At the start of each iteration of this loop, i points to an identifier.
    // On exit from the loop, i points to a token after an identifier or after *.
    while (true) {
      Preconditions.checkState(isIdentifierToken(i));
      imported.append(tokenAt(i));
      i++;
      if (!tokenAt(i).equals(".")) {
        return new StringAndIndex(imported.toString(), i);
      }
      imported.append('.');
      i++;
      if (tokenAt(i).equals("*")) {
        imported.append('*');
        return new StringAndIndex(imported.toString(), i + 1);
      } else if (!isIdentifierToken(i)) {
        throw new FormatterException("Could not parse imported name, at: " + tokenAt(i));
      }
    }
  }

  /**
   * Returns the index of the first place where one of the given identifiers occurs, or {@code
   * Optional.absent()} if there is none.
   *
   * @param start the index to start looking at
   * @param identifiers the identifiers to look for
   */
  private Optional<Integer> findIdentifier(int start, ImmutableSet<String> identifiers) {
    for (int i = start; i < toks.size(); i++) {
      if (isIdentifierToken(i)) {
        String id = tokenAt(i);
        if (identifiers.contains(id)) {
          return Optional.of(i);
        }
      }
    }
    return Optional.absent();
  }

  /** Returns the given token, or the preceding token if it is a whitespace token. */
  private int unindent(int i) {
    if (i > 0 && isSpaceToken(i - 1)) {
      return i - 1;
    } else {
      return i;
    }
  }

  private String tokenAt(int i) {
    return toks.get(i).getOriginalText();
  }

  private boolean isIdentifierToken(int i) {
    String s = tokenAt(i);
    return !s.isEmpty() && Character.isJavaIdentifierStart(s.codePointAt(0));
  }

  private boolean isSpaceToken(int i) {
    String s = tokenAt(i);
    if (s.isEmpty()) {
      return false;
    } else {
      return " \t\f".indexOf(s.codePointAt(0)) >= 0;
    }
  }

  private boolean isSlashSlashCommentToken(int i) {
    return toks.get(i).isSlashSlashComment();
  }

  private boolean isNewlineToken(int i) {
    return toks.get(i).isNewline();
  }
}
