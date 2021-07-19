
Install for usage in your repo:

    http_archive(
        name = "com_google_google_java_format_source",
        urls = ["https://github.com/google/google-java-format/archive/google-java-format-1.6.zip"],
    )

    load("@com_google_google_java_format_source//tools/bazel:def.bzl", "java_format")

    java_format(
        name = "com_google_google_java_format",
        jar_url = "https://github.com/google/google-java-format/releases/download/google-java-format-1.5/google-java-format-1.5-all-deps.jar",
        jar_sha256 = "7b839bb7534a173f0ed0cd0e9a583181d20850fcec8cf6e3800e4420a1fad184",
        diff = "@com_google_google_java_format_source//scripts:google-java-format-diff.py",
    )
