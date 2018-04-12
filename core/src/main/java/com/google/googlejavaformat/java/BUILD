package(default_visibility = ["//:__subpackages__"])

java_library(
    name = "java",
    srcs = glob(["*.java"]),
    deps = [
        "//core/src/main/java/com/google/googlejavaformat",
        "//core/src/main/java/com/google/googlejavaformat/java/javadoc",
        "//thirdparty/jvm/com/google/code/findbugs:jsr305",
        "//thirdparty/jvm/com/google/errorprone:error_prone_annotations",
        "//thirdparty/jvm/com/google/errorprone:javac_shaded",
        "//thirdparty/jvm/com/google/guava",
    ],
)
