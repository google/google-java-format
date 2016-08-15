class TryWtihResources {
  {
    try (@A C c = c(); ) {}
    try (final @A C c = c(); ) {}
    try (@A final C c = c(); ) {}
    try (@A final @B C c = c(); ) {}

    try (final BufferedWriter writer =
        new BufferedWriter(new OutputStreamWriter(testFile, Charset.defaultCharset()))) {
      writer.append("tom cruise\n").append("avatar\n");
      writer.flush();
    }

    try (@SuppressWarnings("resource")
        Scanner inputScanner = new Scanner(inputStream).useDelimiter("\\s+|,")) {
      while (inputScanner.hasNextLong()) {
        placementIds.add(inputScanner.nextLong());
      }
    }
  }
}
