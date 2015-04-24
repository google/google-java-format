class B19950815 {
  void m() {
    checkArgument(
        truncationLength >= 0, "maxLength (%s) must be >= length of the truncation indicator (%s)",
        maxLength, truncationIndicator.length());
  }

  private String finishCollapseFrom(
      CharSequence sequence, int start, int end, char replacement, StringBuilder builder,
      boolean inMatchingGroup) {
  }
}
