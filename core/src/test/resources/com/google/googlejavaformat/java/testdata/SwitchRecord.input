record SwitchRecord(int i) {
  int x(Object o) {
    return switch (o) {
      case SwitchRecord(int i) -> i;
      default -> 0;
    };
  }
  int f(Object o) {
    return switch (o) {
      case SwitchRecord(int one, int two, int three, int four, int five, int six, int seven, int eight, int nine) -> nine;
      default -> 0;
    };
  }
  int g(Object o) {
    return switch (o) {
      case SwitchRecord(int one, int two, int three, int four, int five, int six, int seven, int eight, int nine) -> 
      System.err.println("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.");
      default -> 0;
    };
  }
}
