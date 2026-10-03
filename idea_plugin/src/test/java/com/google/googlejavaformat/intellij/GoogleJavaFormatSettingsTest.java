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
