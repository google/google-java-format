public class B20844369 {
  private static final String ID_PATTERN =
  // TODO(daw): add min/max lengths for the numbers here, e.g. android ID
  "(?:(?<androidId>\\d+)\\+)?" // optional Android ID
      + "(?<type>\\d+)" // type
      + ":"
      + "(?<timestamp>\\d+)" // timestamp
      + "(?<subtype>%" // begin optional subtype
      + "(?:(?<userId>\\d+)#)?" // subtype's optional user ID, followed by a hash
      + "(?<categoryHash>[0-9a-fA-F]{8})" // subtype's category hash
      + "(?<tokenHash>[0-9a-fA-F]{8})" // subtype's token hash
      + ")?"; // end optional subtype

  int x = //foo
      42 + //bar
          1;

  int x =
      //foo
      42 + //bar
          1;

  int x = /*foo*/
      42 + //bar
          1;

  int x =
      /*foo*/
      42 + //bar
          1;
}
