class Records {
  record R1() {}

  private record R2() {}

  @Deprecated
  private record R3() {}

  record R4<T>() {}

  record R5<T>(int x) {}

  record R6<T>(@Deprecated int x) {}

  record R7<T>(@Deprecated int x, int... y) {}

  record R8<T>() implements Comparable<R8<T>> {
    @Override
    public int compareTo(R8<T> other) {
      return 0;
    }
  }

  record R9(int x) {
    R9(int x) {
      this.x = x;
    }
  }
}
