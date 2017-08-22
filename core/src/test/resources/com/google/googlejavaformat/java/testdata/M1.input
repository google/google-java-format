import a.A;
import a.B;
import com.google.Foo;

@A @B
module com.google.m {
requires com.google.r1;
requires transitive com.google.r2;
requires static com.google.r3;
exports com.google.e1;
exports com.google.e1 to com.google.e2;
exports com.google.e1 to com.google.e2, com.google.e3;
exports com.google.e1 to com.google.e2, com.google.e3, com.google.e4;
opens com.google.o1;
opens com.google.o1 to com.google.o2;
opens com.google.o1 to com.google.o2, com.google.o3;
opens com.google.o1 to com.google.o2, com.google.o3, com.googleoe4;
uses Foo;
uses com.google.Bar;
provides com.google.Baz with Foo;
provides com.google.Baz with Foo, com.google.Bar;
}
