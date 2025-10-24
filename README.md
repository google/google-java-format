# google-java-format

`google-java-format` is a program that reformats Java source code to comply with
[Google Java Style][].

[Google Java Style]: https://google.github.io/styleguide/javaguide.html

## Using the formatter

### From the command-line

[Download the formatter](https://github.com/google/google-java-format/releases)
and run it with:

```
java -jar /path/to/google-java-format-${GJF_VERSION?}-all-deps.jar <options> [files...]
```

Note that it uses the `jdk.compiler` module to parse the Java source code. The
`java` binary version used must therefore be from a JDK (not JRE) with a version
equal to or newer than the Java language version of the files being formatted.
The minimum Java version can be found in `core/pom.xml` (currently Java 17). An
alternative is to use the available GraalVM based native binaries instead.

The formatter can act on whole files, on limited lines (`--lines`), on specific
offsets (`--offset`), passing through to standard-out (default) or altered
in-place (`--replace`).

Option `--help` will print full usage details; including built-in documentation
about other flags, such as `--aosp`, `--fix-imports-only`,
`--skip-sorting-imports`, `--skip-removing-unused-import`,
`--skip-reflowing-long-strings`, `--skip-javadoc-formatting`, or the `--dry-run`
and `--set-exit-if-changed`.

Using `@<filename>` reads options and filenames from a file, instead of
arguments.

To reformat changed lines in a specific patch, use
[`google-java-format-diff.py`](https://github.com/google/google-java-format/blob/master/scripts/google-java-format-diff.py).

***Note:*** *There is no configurability as to the formatter's algorithm for
formatting. This is a deliberate design decision to unify our code formatting on
a single format.*

### IntelliJ, Android Studio, and other JetBrains IDEs

A
[google-java-format IntelliJ plugin](https://plugins.jetbrains.com/plugin/8527)
is available from the plugin repository. To install it, go to your IDE's
settings and select the `Plugins` category. Click the `Marketplace` tab, search
for the `google-java-format` plugin, and click the `Install` button.

The plugin will be disabled by default. To enable,
[open the Project settings](https://www.jetbrains.com/help/idea/configure-project-settings.html),
then click "google-java-format Settings" and check the "Enable
google-java-format" checkbox.

To enable it by default in new projects,
[open the default settings for new projects](https://www.jetbrains.com/help/idea/configure-project-settings.html#new-default-settings)
and configure it under "Other Settings/google-java-format Settings".

When enabled, it will replace the normal `Reformat Code` and `Optimize Imports`
actions.

#### IntelliJ JRE Config

The google-java-format plugin uses some internal classes that aren't available
without extra configuration. To use the plugin, you need to
[add some options to your IDE's Java runtime](https://www.jetbrains.com/help/idea/tuning-the-ide.html#procedure-jvm-options).
To do that, go to `Help→Edit Custom VM Options...` and paste in these lines:

```
--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```

Once you've done that, restart the IDE.

### Eclipse

The latest version of the `google-java-format` Eclipse plugin can be downloaded
from the [releases page](https://github.com/google/google-java-format/releases).
Drop it into the Eclipse
[drop-ins folder](http://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Freference%2Fmisc%2Fp2_dropins_format.html)
to activate the plugin.

The plugin adds two formatter implementations:

*   `google-java-format`: using 2 spaces indent
*   `aosp-java-format`: using 4 spaces indent

These that can be selected in "Window" > "Preferences" > "Java" > "Code Style" >
"Formatter" > "Formatter Implementation".

#### Eclipse JRE Config

The plugin uses some internal classes that aren't available without extra
configuration. To use the plugin, you will need to edit the
[`eclipse.ini`](https://wiki.eclipse.org/Eclipse.ini) file.

Open the `eclipse.ini` file in any editor and paste in these lines towards the
end (but anywhere after `-vmargs` will do):

```
--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```

Once you've done that, restart the IDE.

### Third-party integrations

*   Visual Studio Code
    *   [google-java-format-for-vs-code](https://marketplace.visualstudio.com/items?itemName=JoseVSeb.google-java-format-for-vs-code)
*   Gradle plugins
    *   [spotless](https://github.com/diffplug/spotless/tree/main/plugin-gradle#google-java-format)
    *   [sherter/google-java-format-gradle-plugin](https://github.com/sherter/google-java-format-gradle-plugin)
*   Apache Maven plugins
    *   [spotless](https://github.com/diffplug/spotless/tree/main/plugin-maven#google-java-format)
    *   [spotify/fmt-maven-plugin](https://github.com/spotify/fmt-maven-plugin)
    *   [talios/googleformatter-maven-plugin](https://github.com/talios/googleformatter-maven-plugin)
    *   [Cosium/maven-git-code-format](https://github.com/Cosium/maven-git-code-format):
        A maven plugin that automatically deploys google-java-format as a
        pre-commit git hook.
*   SBT plugins
    *   [sbt/sbt-java-formatter](https://github.com/sbt/sbt-java-formatter)
*   [Github Actions](https://github.com/features/actions)
    *   [googlejavaformat-action](https://github.com/axel-op/googlejavaformat-action):
        Automatically format your Java files when you push on github

### as a library

The formatter can be used in software which generates java to output more
legible java code. Just include the library in your maven/gradle/etc.
configuration.

`google-java-format` uses internal javac APIs for parsing Java source. The
following JVM flags are required when running on JDK 16 and newer, due to
[JEP 396: Strongly Encapsulate JDK Internals by Default](https://openjdk.java.net/jeps/396):

```
--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```

#### Maven

```xml
<dependency>
  <groupId>com.google.googlejavaformat</groupId>
  <artifactId>google-java-format</artifactId>
  <version>${google-java-format.version}</version>
</dependency>
```

#### Gradle

```groovy
dependencies {
  implementation 'com.google.googlejavaformat:google-java-format:$googleJavaFormatVersion'
}
```

You can then use the formatter through the `formatSource` methods. E.g.

```java
String formattedSource = new Formatter().formatSource(sourceString);
```

or

```java
CharSource source = ...
CharSink output = ...
new Formatter().formatSource(source, output);
```

Your starting point should be the instance methods of
`com.google.googlejavaformat.java.Formatter`.

## Building from source

```
mvn install
```

## Contributing

Please see [the contributors guide](CONTRIBUTING.md) for details.

## License

```text
Copyright 2015 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
