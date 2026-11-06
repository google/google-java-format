/*
 * Copyright 2026 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.googlejavaformat.intellij;

import static com.google.common.truth.Truth.assertThat;

import com.google.googlejavaformat.intellij.GoogleJavaFormatSettings.State;
import com.google.googlejavaformat.java.JavaFormatterOptions.Style;
import com.intellij.configurationStore.XmlSerializer;
import org.jdom.Element;
import org.junit.Test;

public class GoogleJavaFormatSettingsTest {
  @Test
  public void defaultMaxLineLengthMatchesGoogleStyle() {
    State state = new State();
    assertThat(state.maxLineLength).isEqualTo(Style.GOOGLE.maxLineLength());
  }

  @Test
  public void serializeAndDeserialize() {
    State state = new State();
    state.setEnabled("true");
    state.style = UiFormatterStyle.AOSP;
    state.maxLineLength = 120;

    Element element = XmlSerializer.serialize(state);
    State loaded = XmlSerializer.deserialize(element, State.class);

    assertThat(loaded.getEnabled()).isEqualTo("true");
    assertThat(loaded.style).isEqualTo(UiFormatterStyle.AOSP);
    assertThat(loaded.maxLineLength).isEqualTo(120);
  }

  @Test
  public void deserializeExistingProjectSettings() {
    Element element = new Element("state");
    Element optAosp = new Element("option");
    optAosp.setAttribute("name", "style");
    optAosp.setAttribute("value", "AOSP");
    element.addContent(optAosp);

    State loaded = XmlSerializer.deserialize(element, State.class);
    assertThat(loaded.style).isEqualTo(UiFormatterStyle.AOSP);
  }

  @Test
  public void nullStyleFallsBackToGoogle() {
    State state = new State();
    state.style = null;
    GoogleJavaFormatSettings settings = new GoogleJavaFormatSettings(null);
    settings.loadState(state);

    assertThat(settings.getStyle()).isEqualTo(Style.GOOGLE);
  }

  @Test
  public void getAndSetStylePreservesMaxLineLength() {
    GoogleJavaFormatSettings.State state = new GoogleJavaFormatSettings.State();
    state.maxLineLength = 140;
    state.style = UiFormatterStyle.GOOGLE;

    assertThat(
            state.style.convert().toBuilder()
                .maxLineLength(state.maxLineLength)
                .build()
                .maxLineLength())
        .isEqualTo(140);
  }
}
