class B20578077 {
  {
    new IteratorTester<Integer>(
        4, MODIFIABLE, newArrayList(1, 2), IteratorTester.KnownOrder.KNOWN_ORDER) {
      @Override
      protected Iterator<Integer> newTargetIterator() {
        Iterator<Integer> iterator = Lists.newArrayList(1, 2).iterator();
        return new IteratorWithSunJavaBug6529795<Integer>(iterator);
      }
    }.test();
  }
}
