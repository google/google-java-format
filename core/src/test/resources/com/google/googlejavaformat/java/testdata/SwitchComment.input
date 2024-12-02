class T {
  void f(String v) {
    int x =
        switch (v) {
            // this is a line comment about "zero"
          case "zero" -> 0;
          case "one" ->
              // this is a line comment about "one"
              1;
          case "two" -> // this is a line comment about "two"
              2;
          default -> -1;
        };
  }

  void g(String v) {
    int x =
        switch (v) {
            // this is a line comment about "zero"
          case "zero":
              return 0;
          case "one":
            // this is a line comment about "one"
            return 1;
          case "two": // this is a line comment about "two"
            return 2;
          default:
            return -1;
        };
  }
}