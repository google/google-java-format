java_library(
    name = "error_prone_annotation",
    exports = [
        "//external:jar/com/google/errorprone/error_prone_annotation"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "error_prone_annotations",
    exports = [
        "//external:jar/com/google/errorprone/error_prone_annotations"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "error_prone_check_api",
    exports = [
        "//external:jar/com/google/errorprone/error_prone_check_api"
    ],
    runtime_deps = [
        "//thirdparty/jvm/com/github/kevinstern:software_and_algorithms",
        "//thirdparty/jvm/com/googlecode/java_diff_utils:diffutils"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "error_prone_core",
    exports = [
        "//external:jar/com/google/errorprone/error_prone_core"
    ],
    runtime_deps = [
        "//thirdparty/jvm/com/github/stephenc/jcip:jcip_annotations",
        "//thirdparty/jvm/com/google/auto:auto_common",
        "//thirdparty/jvm/com/google/code/findbugs:jFormatString",
        "//thirdparty/jvm/com/google/code/findbugs:jsr305",
        "//thirdparty/jvm/com/google/guava:guava",
        "//thirdparty/jvm/org/checkerframework:dataflow",
        "//thirdparty/jvm/org/pcollections:pcollections",
        ":error_prone_annotation",
        ":error_prone_annotations",
        ":error_prone_check_api",
        ":javac"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "javac",
    exports = [
        "//external:jar/com/google/errorprone/javac"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "javac_shaded",
    exports = [
        "//external:jar/com/google/errorprone/javac_shaded"
    ],
    visibility = [
        "//visibility:public"
    ]
)


