/*
 * Copyright 2021 Google Inc.
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

package com.google.googlejavaformat.java.java15;

import com.google.googlejavaformat.Input.Token;
import com.google.googlejavaformat.java.ModifierOrderer;
import javax.lang.model.element.Modifier;

public class Java15ModifierOrderer extends ModifierOrderer {

  @Override
  protected Modifier asModifier(Token token) {
    Modifier m = super.asModifier(token);
    if (m != null) return m;
    else if (token.getTok().getText().equals("sealed")) return Modifier.valueOf("SEALED");
    else if (token.getTok().getText().equals("non-sealed")) return Modifier.valueOf("NON_SEALED");
    else return null;
  }
}
