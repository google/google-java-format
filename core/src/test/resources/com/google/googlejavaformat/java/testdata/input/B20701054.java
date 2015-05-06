class B20701054 {
  void m() {
    ImmutableList<String> x = ImmutableList.builder().add(1).build();
    OptionalBinder.<ASD>newOptionalBinder(binder(), InputWhitelist.class).setBinding().to(
        AllInputWhitelist.class);

    Foo z = Foo.INSTANCE.field;
    Foo z = Foo.INSTANCE.field.field;
    Foo z = Foo.INSTANCE.field.field.field;
    Foo z = Foo.INSTANCE.field.field.field.field;
    Foo z = Foo.INSTANCE.field.field.field.field.field;

    ImmutableList<String> x = ImmutableList.BUILDER.add(1).build();
    ImmutableList<String> x = ImmutableList.BUILDER.add(1).add(2).build();
    ImmutableList<String> x = ImmutableList.BUILDER.add(1).add(2).add(3).build();
    ImmutableList<String> x = ImmutableList.BUILDER.add(1).add(2).add(3).add(4).build();

    ImmutableList<String> x = ImmutableList.builder().add(1).build();
    ImmutableList<String> x = ImmutableList.builder().add(1).add(2).build();
    ImmutableList<String> x = ImmutableList.builder().add(1).add(2).add(3).build();
    ImmutableList<String> x = ImmutableList.builder().add(1).add(2).add(3).add(4).build();
    ImmutableList<String> x = ImmutableList.builder().add(1).add(2).add(3).add(4).add(5).build();

    ImmutableList<String> x =
        new ImmutableList.Builder<>()
            .add(xxxxx)
            .add(xxxxx)
            .add(xxxxx)
            .add(xxxxx)
            .add(xxxxx)
            .add(xxxxx)
            .add(xxxxx)
            .add(xxxxx)
            .build();

    ImmutableList<String> x =
        ImmutableList.new Builder<>()
            .add(xxxxx)
            .add(xxxxx)
            .add(xxxxx)
            .add(xxxxx)
            .add(xxxxx)
            .add(xxxxx)
            .add(xxxxx)
            .add(xxxxx)
            .build();

    System.err.println(
        xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx);

    Class.my.contrived.example.function(
        veryLongArgumentxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx);

    PTable<Long, List<PageSpeedUrlResult>> latestResults =
        FJ.joinOneShot2(accountSummaryProvider.get(), results).parallelDo(
            "extractPageSpeedUrls", new ExtractPageSpeedUrlsFn(false));

    PTable<Long, Long> adImpressionsByAccount =
        impressionExtractor.getImpressionBreakdownByAccountId().parallelDo(
            "rekeyAdImpressionsByAccountId", new ExtractTotalImpressionsFn());

    PTable<String, CrawlError> crawlerErrorsByCode =
        crawlReportSource.read("readCrawlReportTable").parallelDo(
            "reKeyErrorsByPropertyCode", new RekeyErrorsByPropertyCodeFn());

    if (ImmutableList.builder().add(1).add(2).add(3)) {
    }

    if (value.fst.name.toString().equals("value")) {
    }

    analysis().analyze(compilationUnit, context, configuration, new DescriptionListener() {
      @Override
      public void onDescribed(Description description) {
        listener.onDescribed(description.filterFixes(new Predicate<Fix>() {
          @Override
          public boolean apply(Fix fix) {
            return compiles(fix, (JCCompilationUnit) compilationUnit, context);
          }
        }));
      }
    });
  }
}
