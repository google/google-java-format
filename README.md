# google-java-format

[![Build Status](https://travis-ci.org/google/google-java-format.svg?branch=master)](https://travis-ci.org/google/google-java-format)

`google-java-format` is a program that reformats Java source code to comply with
[Google Java Style][].

[Google Java Style]: http://google-styleguide.googlecode.com/svn/trunk/javaguide.html

Building
--------

To build google-java-format from source, you will need copy IntelliJ's
platform JARs from a local IntelliJ install; see
[`install-idea-jars.sh`](idea_plugin/src/main/scripts/install-idea-jars.sh).
Alternatively, you can skip building the IntelliJ plugin:

    mvn -pl '!idea_plugin' install
