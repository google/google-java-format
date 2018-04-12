java_test(
    name = "FormatterTest",
    srcs = ["FormatterTest.java"],
    resources = [
        "//core/src/test/resources/com/google/googlejavaformat/java/testimports:A.formatting-and-import-sorting",
        "//core/src/test/resources/com/google/googlejavaformat/java/testimports:A.formatting-and-unused-import-removal",
        "//core/src/test/resources/com/google/googlejavaformat/java/testimports:A.imports-and-formatting",
        "//core/src/test/resources/com/google/googlejavaformat/java/testimports:A.imports-only",
        "//core/src/test/resources/com/google/googlejavaformat/java/testimports:A.input",
    ],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/guava",
        "//thirdparty/jvm/com/google/truth",
        "//thirdparty/jvm/junit",
    ],
)

java_test(
    name = "SnippetFormatterTest",
    srcs = ["SnippetFormatterTest.java"],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/guava",
        "//thirdparty/jvm/com/google/truth",
        "//thirdparty/jvm/junit",
    ],
)

java_test(
    name = "RemoveUnusedImportsTest",
    srcs = ["RemoveUnusedImportsTest.java"],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/guava",
        "//thirdparty/jvm/com/google/truth",
        "//thirdparty/jvm/junit",
    ],
)

java_test(
    name = "FormatterIntegrationTest",
    srcs = ["FormatterIntegrationTest.java"],
    resources = ["//core/src/test/resources/com/google/googlejavaformat/java/testdata"],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat",
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/guava",
        "//thirdparty/jvm/junit",
    ],
)

java_test(
    name = "ImportOrdererTest",
    srcs = ["ImportOrdererTest.java"],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/guava",
        "//thirdparty/jvm/com/google/truth",
        "//thirdparty/jvm/junit",
    ],
)

java_test(
    name = "CommandLineFlagsTest",
    srcs = ["CommandLineFlagsTest.java"],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/truth",
        "//thirdparty/jvm/junit",
    ],
)

java_test(
    name = "TypeNameClassifierTest",
    srcs = ["TypeNameClassifierTest.java"],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/guava",
        "//thirdparty/jvm/com/google/truth",
        "//thirdparty/jvm/junit",
    ],
)

java_test(
    name = "JavadocFormattingTest",
    srcs = ["JavadocFormattingTest.java"],
    resources = [
        "//core/src/test/resources/com/google/googlejavaformat/java/testjavadoc:B28750242.input",
        "//core/src/test/resources/com/google/googlejavaformat/java/testjavadoc:B28750242.output",
        "//core/src/test/resources/com/google/googlejavaformat/java/testjavadoc:B31404367.input",
        "//core/src/test/resources/com/google/googlejavaformat/java/testjavadoc:B31404367.output",
    ],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/guava",
        "//thirdparty/jvm/com/google/truth",
        "//thirdparty/jvm/junit",
    ],
)

java_test(
    name = "LineRangesToCharRangesTest",
    srcs = ["LineRangesToCharRangesTest.java"],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/guava",
        "//thirdparty/jvm/com/google/truth",
        "//thirdparty/jvm/junit",
    ],
)

java_test(
    name = "DiagnosticTest",
    srcs = ["DiagnosticTest.java"],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/guava",
        "//thirdparty/jvm/com/google/truth",
        "//thirdparty/jvm/junit",
    ],
)

java_test(
    name = "CommandLineOptionsParserTest",
    srcs = ["CommandLineOptionsParserTest.java"],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/guava",
        "//thirdparty/jvm/com/google/truth",
        "//thirdparty/jvm/com/google/truth/extensions:truth_java8_extension",
        "//thirdparty/jvm/junit",
    ],
)

java_test(
    name = "ArrayDimensionTest",
    srcs = ["ArrayDimensionTest.java"],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/guava",
        "//thirdparty/jvm/com/google/truth",
        "//thirdparty/jvm/junit",
    ],
)

java_test(
    name = "MainTest",
    srcs = ["MainTest.java"],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/guava",
        "//thirdparty/jvm/com/google/truth",
        "//thirdparty/jvm/junit",
    ],
)

java_test(
    name = "PartialFormattingTest",
    srcs = ["PartialFormattingTest.java"],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/guava",
        "//thirdparty/jvm/com/google/truth",
        "//thirdparty/jvm/junit",
    ],
)

java_test(
    name = "ModifierOrdererTest",
    srcs = ["ModifierOrdererTest.java"],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat/java",
        "//thirdparty/jvm/com/google/guava",
        "//thirdparty/jvm/com/google/truth",
        "//thirdparty/jvm/junit",
    ],
)
