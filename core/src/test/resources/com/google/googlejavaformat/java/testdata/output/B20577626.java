class B20577626 {
  private @Mock GsaConfigFlags mGsaConfig;

  @Foo @Bar private @Mock GsaConfigFlags mGsaConfig;

  @Foo
  abstract @Bar void m() {}

  @Foo
  @Baz
  abstract @Bar void m() {}
}
