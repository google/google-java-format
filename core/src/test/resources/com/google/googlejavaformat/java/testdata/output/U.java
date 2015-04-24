package com.google.googlejavaformat.java.test;

/** Tests for UnionTypes. */
class U {
  void f() {
    class Exception0 extends Exception {}
    class Exception1 extends Exception {}
    class Exception2 extends Exception {}
    class Exception3 extends Exception {}
    class Exception4 extends Exception {}
    class Exception5 extends Exception {}
    class Exception6 extends Exception {}
    class Exception7 extends Exception {}
    class Exception8 extends Exception {}
    class Exception9 extends Exception {}
    try {
      char c = '\123';
      switch (c) {
        case '0':
          throw new Exception0();
        case '1':
          throw new Exception1();
        case '2':
          throw new Exception2();
        case '3':
          throw new Exception3();
        case '4':
          throw new Exception4();
        case '5':
          throw new Exception5();
        case '6':
          throw new Exception6();
        case '7':
          throw new Exception7();
        case '8':
          throw new Exception8();
        case '9':
        default:
          throw new Exception9();
      }
    } catch (
        Exception0 | Exception1 | Exception2 | Exception3 | Exception4 | Exception5 | Exception6
                | Exception7 | Exception8 | Exception9
            e) {
    }
  }
}
