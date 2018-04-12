# Do not edit. bazel-deps autogenerates this file from maven_deps.yaml.

def declare_maven(hash):
    native.maven_jar(
        name = hash["name"],
        artifact = hash["artifact"],
        sha1 = hash["sha1"],
        repository = hash["repository"]
    )
    native.bind(
        name = hash["bind"],
        actual = hash["actual"]
    )

def list_dependencies():
    return [
    {"artifact": "com.github.kevinstern:software-and-algorithms:1.0", "lang": "java", "sha1": "5e77666b72c6c5dd583c36148d17fc47f944dfb5", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_github_kevinstern_software_and_algorithms", "actual": "@com_github_kevinstern_software_and_algorithms//jar", "bind": "jar/com/github/kevinstern/software_and_algorithms"},
    {"artifact": "com.github.stephenc.jcip:jcip-annotations:1.0-1", "lang": "java", "sha1": "ef31541dd28ae2cefdd17c7ebf352d93e9058c63", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_github_stephenc_jcip_jcip_annotations", "actual": "@com_github_stephenc_jcip_jcip_annotations//jar", "bind": "jar/com/github/stephenc/jcip/jcip_annotations"},
    {"artifact": "com.google.auto.value:auto-value:1.5.3", "lang": "java", "sha1": "514df6a7c7938de35c7f68dc8b8f22df86037f38", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_auto_value_auto_value", "actual": "@com_google_auto_value_auto_value//jar", "bind": "jar/com/google/auto/value/auto_value"},
# duplicates in com.google.auto:auto-common promoted to 0.9
# - com.google.errorprone:error_prone_core:2.1.3 wanted version 0.7
# - com.google.testing.compile:compile-testing:0.15 wanted version 0.9
    {"artifact": "com.google.auto:auto-common:0.9", "lang": "java", "sha1": "766dd79e7e81cfefec890ffd6d63aa2807538def", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_auto_auto_common", "actual": "@com_google_auto_auto_common//jar", "bind": "jar/com/google/auto/auto_common"},
    {"artifact": "com.google.code.findbugs:jFormatString:3.0.0", "lang": "java", "sha1": "d3995f9be450813bc2ccee8f0774c1a3033a0f30", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_code_findbugs_jFormatString", "actual": "@com_google_code_findbugs_jFormatString//jar", "bind": "jar/com/google/code/findbugs/jFormatString"},
# duplicates in com.google.code.findbugs:jsr305 fixed to 2.0.1
# - com.google.errorprone:error_prone_core:2.1.3 wanted version 3.0.0
# - com.google.guava:guava-testlib:22.0 wanted version 1.3.9
# - com.google.guava:guava:22.0 wanted version 1.3.9
    {"artifact": "com.google.code.findbugs:jsr305:2.0.1", "lang": "java", "sha1": "516c03b21d50a644d538de0f0369c620989cd8f0", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_code_findbugs_jsr305", "actual": "@com_google_code_findbugs_jsr305//jar", "bind": "jar/com/google/code/findbugs/jsr305"},
    {"artifact": "com.google.errorprone:error_prone_annotation:2.1.3", "lang": "java", "sha1": "64eea069f9cbae65cb1c8b272a5adcb0452e9797", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_errorprone_error_prone_annotation", "actual": "@com_google_errorprone_error_prone_annotation//jar", "bind": "jar/com/google/errorprone/error_prone_annotation"},
# duplicates in com.google.errorprone:error_prone_annotations fixed to 2.0.8
# - com.google.errorprone:error_prone_core:2.1.3 wanted version 2.1.3
# - com.google.guava:guava-testlib:22.0 wanted version 2.0.18
# - com.google.guava:guava:22.0 wanted version 2.0.18
# - com.google.truth:truth:0.36 wanted version 2.0.19
    {"artifact": "com.google.errorprone:error_prone_annotations:2.0.8", "lang": "java", "sha1": "54e2d56cb157df08cbf183149bcf50c9f5151ed4", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_errorprone_error_prone_annotations", "actual": "@com_google_errorprone_error_prone_annotations//jar", "bind": "jar/com/google/errorprone/error_prone_annotations"},
    {"artifact": "com.google.errorprone:error_prone_check_api:2.1.3", "lang": "java", "sha1": "44b5615f42c0d607722d06ac59107834ace86ad7", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_errorprone_error_prone_check_api", "actual": "@com_google_errorprone_error_prone_check_api//jar", "bind": "jar/com/google/errorprone/error_prone_check_api"},
    {"artifact": "com.google.errorprone:error_prone_core:2.1.3", "lang": "java", "sha1": "9637cc24413c781f68396cd7c9144e1445e4c9b4", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_errorprone_error_prone_core", "actual": "@com_google_errorprone_error_prone_core//jar", "bind": "jar/com/google/errorprone/error_prone_core"},
    {"artifact": "com.google.errorprone:javac-shaded:9+181-r4173-1", "lang": "java", "sha1": "a399ee380b6d6b6ea53af1cfbcb086b108d1efb7", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_errorprone_javac_shaded", "actual": "@com_google_errorprone_javac_shaded//jar", "bind": "jar/com/google/errorprone/javac_shaded"},
    {"artifact": "com.google.errorprone:javac:9-dev-r4023-3", "lang": "java", "sha1": "408c45e1a677c9c8dc4fa6470aefbd960462803d", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_errorprone_javac", "actual": "@com_google_errorprone_javac//jar", "bind": "jar/com/google/errorprone/javac"},
    {"artifact": "com.google.guava:guava-testlib:22.0", "lang": "java", "sha1": "3be1b88f1cfc6592acbcbfe1f3a420f79eb2b146", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_guava_guava_testlib", "actual": "@com_google_guava_guava_testlib//jar", "bind": "jar/com/google/guava/guava_testlib"},
# duplicates in com.google.guava:guava fixed to 22.0
# - com.google.errorprone:error_prone_core:2.1.3 wanted version 22.0
# - com.google.guava:guava-testlib:22.0 wanted version 22.0
# - com.google.testing.compile:compile-testing:0.15 wanted version 23.5-jre
# - com.google.truth:truth:0.36 wanted version 22.0-android
    {"artifact": "com.google.guava:guava:22.0", "lang": "java", "sha1": "3564ef3803de51fb0530a8377ec6100b33b0d073", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_guava_guava", "actual": "@com_google_guava_guava//jar", "bind": "jar/com/google/guava/guava"},
    {"artifact": "com.google.j2objc:j2objc-annotations:1.1", "lang": "java", "sha1": "ed28ded51a8b1c6b112568def5f4b455e6809019", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_j2objc_j2objc_annotations", "actual": "@com_google_j2objc_j2objc_annotations//jar", "bind": "jar/com/google/j2objc/j2objc_annotations"},
    {"artifact": "com.google.testing.compile:compile-testing:0.15", "lang": "java", "sha1": "d6619b8484ee928fdd7520c0aa6d1c1ffb1d781b", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_testing_compile_compile_testing", "actual": "@com_google_testing_compile_compile_testing//jar", "bind": "jar/com/google/testing/compile/compile_testing"},
    {"artifact": "com.google.truth.extensions:truth-java8-extension:0.37", "lang": "java", "sha1": "9d4bbea5ff8da23ed9c7004e5d1f31a2b3e32429", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_truth_extensions_truth_java8_extension", "actual": "@com_google_truth_extensions_truth_java8_extension//jar", "bind": "jar/com/google/truth/extensions/truth_java8_extension"},
    {"artifact": "com.google.truth:truth:0.36", "lang": "java", "sha1": "7485219d2c1d341097a19382c02bde07e69ff5d2", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_google_truth_truth", "actual": "@com_google_truth_truth//jar", "bind": "jar/com/google/truth/truth"},
    {"artifact": "com.googlecode.java-diff-utils:diffutils:1.3.0", "lang": "java", "sha1": "7e060dd5b19431e6d198e91ff670644372f60fbd", "repository": "https://repo.maven.apache.org/maven2/", "name": "com_googlecode_java_diff_utils_diffutils", "actual": "@com_googlecode_java_diff_utils_diffutils//jar", "bind": "jar/com/googlecode/java_diff_utils/diffutils"},
# duplicates in junit:junit fixed to 4.12
# - com.google.guava:guava-testlib:22.0 wanted version 4.8.2
# - com.google.testing.compile:compile-testing:0.15 wanted version 4.12
# - com.google.truth:truth:0.36 wanted version 4.12
    {"artifact": "junit:junit:4.12", "lang": "java", "sha1": "2973d150c0dc1fefe998f834810d68f278ea58ec", "repository": "https://repo.maven.apache.org/maven2/", "name": "junit_junit", "actual": "@junit_junit//jar", "bind": "jar/junit/junit"},
    {"artifact": "org.checkerframework:dataflow:2.1.14", "lang": "java", "sha1": "082f915f29b30290435ef54f533a68975ecd5bdb", "repository": "https://repo.maven.apache.org/maven2/", "name": "org_checkerframework_dataflow", "actual": "@org_checkerframework_dataflow//jar", "bind": "jar/org/checkerframework/dataflow"},
    {"artifact": "org.checkerframework:javacutil:2.1.14", "lang": "java", "sha1": "fdbbdf36db6c8ea1586adaa882e1b32d167b9dee", "repository": "https://repo.maven.apache.org/maven2/", "name": "org_checkerframework_javacutil", "actual": "@org_checkerframework_javacutil//jar", "bind": "jar/org/checkerframework/javacutil"},
    {"artifact": "org.codehaus.mojo:animal-sniffer-annotations:1.14", "lang": "java", "sha1": "775b7e22fb10026eed3f86e8dc556dfafe35f2d5", "repository": "https://repo.maven.apache.org/maven2/", "name": "org_codehaus_mojo_animal_sniffer_annotations", "actual": "@org_codehaus_mojo_animal_sniffer_annotations//jar", "bind": "jar/org/codehaus/mojo/animal_sniffer_annotations"},
    {"artifact": "org.hamcrest:hamcrest-core:1.3", "lang": "java", "sha1": "42a25dc3219429f0e5d060061f71acb49bf010a0", "repository": "https://repo.maven.apache.org/maven2/", "name": "org_hamcrest_hamcrest_core", "actual": "@org_hamcrest_hamcrest_core//jar", "bind": "jar/org/hamcrest/hamcrest_core"},
    {"artifact": "org.pcollections:pcollections:2.1.2", "lang": "java", "sha1": "15925fd6c32a29fe3f40a048d238c5ca58cb8362", "repository": "https://repo.maven.apache.org/maven2/", "name": "org_pcollections_pcollections", "actual": "@org_pcollections_pcollections//jar", "bind": "jar/org/pcollections/pcollections"},
    ]

def maven_dependencies(callback = declare_maven):
    for hash in list_dependencies():
        callback(hash)
