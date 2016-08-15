class B23349153 {
  enum Foo {
    A,
    B,
    ;
  }

  {
    System.err.println("Hello");
    ;
  }
}

private enum UnsafeAtomicHelperFactory {
  REALLY_TRY_TO_CREATE {
    @Override
    AtomicHelper tryCreateUnsafeAtomicHelper() {
      return new UnsafeAtomicHelper();
    }
  },

  DONT_EVEN_TRY_TO_CREATE {
    @Override
    AtomicHelper tryCreateUnsafeAtomicHelper() {
      return null;
    }
  },
  ;

  abstract AtomicHelper tryCreateUnsafeAtomicHelper();
}
