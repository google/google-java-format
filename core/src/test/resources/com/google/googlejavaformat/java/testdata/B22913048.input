private class Yellow<B> extends Red<B>.Orange {
  Yellow(Red<B> red) {
    red.super();
  }

  Class<?> getClassB() {
    return new TypeToken<B>(getClass()) {}.getRawType();
  }
}
