java_library(
    name = "guava",
    exports = [
        "//external:jar/com/google/guava/guava"
    ],
    runtime_deps = [
        "//thirdparty/jvm/com/google/code/findbugs:jsr305",
        "//thirdparty/jvm/com/google/errorprone:error_prone_annotations",
        "//thirdparty/jvm/com/google/j2objc:j2objc_annotations",
        "//thirdparty/jvm/org/codehaus/mojo:animal_sniffer_annotations"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "guava_testlib",
    exports = [
        "//external:jar/com/google/guava/guava_testlib"
    ],
    runtime_deps = [
        "//thirdparty/jvm/com/google/code/findbugs:jsr305",
        "//thirdparty/jvm/com/google/errorprone:error_prone_annotations",
        "//thirdparty/jvm/com/google/j2objc:j2objc_annotations",
        "//thirdparty/jvm/junit:junit",
        ":guava"
    ],
    visibility = [
        "//visibility:public"
    ]
)


