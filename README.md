# google-java-format

`google-java-format` is a program that reformats Java source code to comply with
[Google Java Style][].

[Google Java Style]: http://google-styleguide.googlecode.com/svn/trunk/javaguide.html

Using the formatter from the command-line
----------------------------------------

[Download the formatter](https://github.com/google/google-java-format/releases)
and run it with:

```
java -jar /path/to/google-java-format-1.0-all-deps.jar <options> [files...]
```

The formatter can act on whole files, on limited lines (`--lines`), on specific
ofsets (`--offset`), passing through to standard-out (default) or altered
in-place (`--replace`).

To reformat changed lines in a specific patch, use
[`google-java-format-diff.py`](https://github.com/google/google-java-format/blob/master/scripts/google-java-format-diff.py)

***Note:*** *There is no configurability as to the formatter's algorithm for
formatting.  This is a deliberate design decision to unify our code formatting
on a single format.*

Using the formatter in code-generators
--------------------------------------

The formatter can be used in software which generates java to output more
legible java code. Just include the library in your maven/gradle/etc.
configuration.

#### Maven

```xml
<dependency>
  <groupId>com.google.googlejavaformat</groupId>
  <artifactId>google-java-format</artifactId>
  <version>1.0</version>
</dependency>
```

#### Gradle

```groovy
dependencies {
  compile 'com.google.googlejavaformat:google-java-format:1.0'
}
```

You can then use the formatter through the `formatSource` methods.  E.g.

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

Building from source
--------------------

    mvn install

Contributing
------------

Please see [the contributors guide](CONTRIBUTING.md) for details.

License
-------

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
