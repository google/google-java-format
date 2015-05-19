package com.google.googlejavaformat.java.test;

class Test {
  void m() {
    if (!metadata.ignoreOutputTransformations()
        && Producers.isListenableFutureMapKey(outputKey)) {
      ImmutableList<ProducerNode<?>> nodes = createMapNodes((ProducerNode) node);
      checkCollectionNodesAgainstWhitelist(nodes, whitelist);
      return nodes;

    } else if (!metadata.ignoreOutputTransformations()
        && Producers.isListenableFutureListKey(outputKey)) {
      ImmutableList<ProducerNode<?>> nodes = createListNodes((ProducerNode) node);
      return nodes;
    }

    Set<Short> shorts = new HashSet<>();
    for (short i = 0; i < 99; ++i) {

      shorts.add(i);
      shorts.remove(i - 1);

    }

    short i = 0;
    do {

      shorts.add(i);
      shorts.remove(i - 1);
      i++;

    } while (i < 99);

    if (true) {

      System.err.println("Hi");

    }

    if (true) {

      System.err.println("Hi");

    } else {

      System.err.println("Hi");

    }

    if (true) {

      System.err.println("Hi");

    } else if (true) {

      System.err.println("Hi");

    } else {

      System.err.println("Hi");

    }

    try {

      throw new Exception();

    } catch (Exception e) {

      System.err.println("Hi");

    }

    try {

      throw new Exception();

    } finally {

      System.err.println("Hi");

    }

    try {

      throw new Exception();

    } catch (Exception e) {

      System.err.println("Hi");

    } finally {

      System.err.println("Hi");

    }

    try {

      throw new Exception();

    } catch (Exception e) {

      System.err.println("Hi");

    } catch (Exception e) {

      System.err.println("Hi");

    } finally {

      System.err.println("Hi");

    }

    try (Lock l = lock.lock()) {}

    try (Lock l = lock.lock()) {} finally {}

    for (;;) {}

    while (true) {}

    do {} while (true);

    if (true) {}

    if (true) {} else if (true) {}

    if (true) {} else if (true) {} else if (true) {}

    if (true) {} else if (true) {} else {}

    if (true) {} else if (true) {} else if (true) {} else {}

    try {} catch (Exception e) {} catch (Exception e) {} finally {}

    try {} catch (Exception e) {} finally {}

    try {} catch (Exception e) {} catch (Exception e) {}

    try {} catch (Exception e) {}

  }

}
