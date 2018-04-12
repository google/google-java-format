package(default_visibility = ["//:__subpackages__"])

VERSION = "1.5"

java_binary(
    name = "Main",
    runtime_deps = [
        ":java",
    ],
)

java_library(
    name = "java",
    srcs = glob(["*.java"]) + [":GoogleJavaFormatVersion"],
    deps = [
        "//core/src/main/java/com/google/googlejavaformat",
        "//core/src/main/java/com/google/googlejavaformat/java/javadoc",
        "//thirdparty/jvm/com/google/code/findbugs:jsr305",
        "//thirdparty/jvm/com/google/errorprone:error_prone_annotations",
        "//thirdparty/jvm/com/google/errorprone:javac_shaded",
        "//thirdparty/jvm/com/google/guava",
    ],
)

genrule(
    name = "GoogleJavaFormatVersion",
    srcs = ["GoogleJavaFormatVersion.java.template"],
    outs = ["GoogleJavaFormatVersion.java"],
    cmd = "sed 's/%VERSION%/" + VERSION + "/' $< > $@ ",
)
