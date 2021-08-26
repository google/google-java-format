package com.google.googlejavaformat.java;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

/** Provide a way to be invoked without necessarily starting a new VM. */
public class GoogleJavaFormatToolProvider implements ToolProvider {
  public String name() {
    return "google-java-format";
  }

  public int run(PrintWriter out, PrintWriter err, String... args) {
    try {
      return Main.main(out, err, args);
    } catch (Exception e) {
      err.print(e.getMessage());
      return -1; // pass non-zero value back indicating an error has happened
    }
  }
}
