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

package com.google.googlejavaformat;

import com.google.googlejavaformat.Input.Tok;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Rewrite comments. This interface is implemented by {@link
 * com.google.googlejavaformat.java.JavaCommentsHelper JavaCommentsHelper}.
 */
public interface CommentsHelper {
  /**
   * Try to rewrite comments, returning rewritten text.
   *
   * @param tok the comment's tok
   * @param maxWidth the line length for the output
   * @param column0 the current column
   * @return the rewritten comment
   */
  String rewrite(Input.Tok tok, int maxWidth, int column0);

  static Optional<String> reformatParameterComment(Tok tok) {
    if (!tok.isSlashStarComment()) {
      return Optional.empty();
    }
    var match = PARAMETER_COMMENT.matcher(tok.getOriginalText());
    if (!match.matches()) {
      return Optional.empty();
    }
    return Optional.of(String.format("/* %s= */", match.group(1)));
  }

  Pattern PARAMETER_COMMENT =
      Pattern.compile(
          "/\\*\\s*(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\Q...\\E)?)\\s*=\\s*\\*/");
}
