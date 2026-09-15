
def _java_format_impl(repository_ctx):
    repository_ctx.download(
        url = repository_ctx.attr.jar_url,
        output = "jar/google-java-format.jar",
        sha256 = repository_ctx.attr.jar_sha256,
    )
    
    repository_ctx.file(
        "jar/BUILD.bazel",
        content = """
java_import(
    name = "jar",
    jars = [
        "google-java-format.jar",
    ],
    visibility = ["//visibility:public"],
)
""",
    )

    repository_ctx.file(
        "BUILD.bazel",
        content = """
java_binary(
    name = "google-java-format",
    main_class = "com.google.googlejavaformat.java.Main",
    runtime_deps = [
        "//jar",
    ],
)

py_binary(
    name = "google-java-format-diff",
    srcs = [
        "{}",
    ],
)
""".format(repository_ctx.attr.diff),
    )

java_format = repository_rule(
    implementation = _java_format_impl,
    attrs = {
        "jar_url": attr.string(mandatory = True),
        "jar_sha256": attr.string(),
        "diff": attr.label(allow_single_file = [".py"], mandatory = True),
    },
)

