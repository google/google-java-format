public sealed interface A {
  record AA(Long a) implements A {}

  static Long mySwitch(A a) {
    switch (a) {
      case AA(_) -> {
        return 1L;
      }
    }
  }
}
