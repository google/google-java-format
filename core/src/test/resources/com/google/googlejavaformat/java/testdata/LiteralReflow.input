class LiteralReflow {
  static class TestLineBreak {
    String doesNotBreakAt100 = "A very long long long long long long long long long loong sentence";
    String breaksAt101 = "A very long long long long long long long long long long loooong sentence";
  }

  static class TestReflowLimit {
    String doesNotReflowAt100 =
        "A very long long long long long long long long long long long long long looooong sentence";
    String reflowsWhenLongerThan100 =
        "A very long long long long long long long long long long long long long long long sentence";
  }

  static class TestReflowLocation {
    String accommodatesWordsUpTo100 =
        "A very long long long long long long long long long long long long long long long looooong sentence";
    String breaksBeforeWordsReach101 =
        "A very long long long long long long long long long long long long long long long loooooong sentence";
  }

  static class Test2LineReflowLimit {
    String doesNotReflowEitherLinesAt100 =
        "A very long long long long long long long long long long long long long looooong sentence. And a second very long long long long long long long long long long loong sentence";
    String reflowsLastLineAt101 =
        "A very long long long long long long long long long long long long long looooong sentence. And a second very long long long long long long long long long long looong sentence";
  }

  static class TestWithTrailingCharacters {
    String fitsLastLineUpTo100WithTrailingCharacters =
        f(
            f(
                "A very long long long long long long long long long long long long loong sentence. And a second very long long long long long long long long loong sentence"));
    String reflowsLastLineToAccommodateTrailingCharacters =
        f(
            f(
                "A very long long long long long long long long long long long long loong sentence. And a second very long long long long long long long long looong sentence"));
    // Tests an off-by-one issue, but see b/179561701 for a similar issue that is not yet fixed
    String doesNotOverTriggerLastLineReflow =
        f(
            f(
                "A very long long long long long long long long long long long long loong sentence."
                    + " And a second very loong sentence with trailing a a a a a a a a a a a a a a a"));
  }
}
