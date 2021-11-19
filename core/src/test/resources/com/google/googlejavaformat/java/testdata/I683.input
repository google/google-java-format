interface Test {

  static class Test1 implements Test{}
  static class Test2 implements Test{}
  
  public static void main(String[] args) {
    Test test = new Test1();
    switch (test) {
      case Test1 test1 -> {}
      case Test2 test2 -> {}
      default -> throw new IllegalStateException("Unexpected value: " + test);
    }
  }
}
