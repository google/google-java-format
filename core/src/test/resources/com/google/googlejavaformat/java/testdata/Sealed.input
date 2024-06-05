class T {
  sealed interface I extends A permits C, B {}
  final class C implements I {}
  sealed private interface A permits I {}
  non-sealed private interface B extends I {}
}
