/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins { id("org.jetbrains.intellij") version "1.15.0" }

apply(plugin = "org.jetbrains.intellij")

apply(plugin = "java")

repositories { mavenCentral() }

val googleJavaFormatVersion = "1.17.0"

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

intellij {
  pluginName.set("google-java-format")
  plugins.set(listOf("java"))
  version.set("2021.3")
}

tasks {
  patchPluginXml {
    version.set("${googleJavaFormatVersion}.0")
    sinceBuild.set("213")
    untilBuild.set("")
  }

  publishPlugin {
    val jetbrainsPluginRepoToken: String by project
    token.set(jetbrainsPluginRepoToken)
  }

  withType<Test>().configureEach {
    jvmArgs(
      "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
      "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
      "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
      "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
      "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
      "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
    )
  }
}

dependencies {
  implementation("com.google.googlejavaformat:google-java-format:${googleJavaFormatVersion}")
  testImplementation("junit:junit:4.13.2")
  testImplementation("com.google.truth:truth:1.1.5")
}
