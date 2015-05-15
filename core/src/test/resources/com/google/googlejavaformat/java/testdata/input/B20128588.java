@Foo @Bar @Baz package edu.oswego.cs.dl.util.concurrent;

@Foo @Bar @Baz class Test {

  @Foo @Bar @Baz Object f() {
  }

  @Foo @Bar @Baz public Object f() {
  }

  @Foo @Bar @Baz void f() {
  }

  @Foo @Bar @Baz static Object field;

  static @Foo @Bar @Baz Object field;

  @Foo @Bar @Baz Object field;

  @Foo(xs=42) @Bar @Baz Object field;

  {
    @Foo @Bar @Baz final Object var;

    final @Foo @Bar @Baz Object var;

    @Foo @Bar @Baz Object var;

    @Foo(xs=42) @Bar @Baz Object var;
  }

  void f(
      @Foo @Bar @Baz final Object var,
      final @Foo @Bar @Baz Object var,
      @Foo @Bar @Baz Object var,
      @Foo(xs=42) @Bar @Baz Object var) {}

  <@TA T extends @TA Object> @TA T f(List<? extends @TA T> a, List<? super @TA T> b) throws @TA Exception {}

  @FooXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX @BarXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX @BazXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX int x; 

@FooXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX @BarXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX @BazXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX int x; 

  @Deprecated @Deprecated @Deprecated @Deprecated @Deprecated @Deprecated @Deprecated @Deprecated @Deprecated @Deprecated @Deprecated @Deprecated @Deprecated @Deprecated Object var; 

  @Deprecated @Deprecated @Deprecated @Deprecated @Deprecated Object var; 

  @Deprecated(x=42) @Deprecated @Deprecated @Deprecated @Deprecated Object var;

  @Deprecated(
      x = 42,
      y = {
        XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,
        XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,
        XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,
        XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
      })
  Object var;

  @Deprecated(
      y = {
        XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,
        XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,
        XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX,
        XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
      })
  Object var;
}

@Frozzle({
  @Mirror(in = edu.oswego.cs.dl.util.concurrent.F.class, methods = "foo"),
  @Mirror(in = edu.oswego.cs.dl.util.concurrent.F.class, methods = "foo"),
  @Mirror(in = edu.oswego.cs.dl.util.concurrent.F.class, enable = false, methods = {
    "foo", "foo", "foo", "foo", "foo",
    "foo", "foo", "foo", "foo", "foo",
    "foo", "foo", "foo", "foo", "foo",
  }),
  @Mirror(
    in = edu.oswego.cs.dl.util.concurrent.F.class,
    enable = false,
    methods = {"foo", "foo", "foo", "foo", "foo", "foo", "foo", "foo", "foo", "foo", "foo"}),
  @Mirror(in = edu.oswego.cs.dl.util.concurrent.F.class, methods = "foo"),
})
class C {}
