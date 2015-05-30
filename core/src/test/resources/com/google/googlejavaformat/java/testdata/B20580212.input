class B20580212 {
  void m() {
    GroupExpansionReply mockIsgReply =
        buildRecipientListSubGroupReply(
            RECIPIENT1,
            alternatives /* isgExpansions */,
            alternativesDeltas /* isgExpansionsScoreDeltas */,
            false /* withRecipient */,
            0 /* recipientScoreDelta */);

    try {
    } catch (IllegalStateException e) { /* expected */ }
  }

  static class ThrowsAtEndException extends RuntimeException { /* nothing */ }
}
