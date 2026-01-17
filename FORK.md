# Configurable Java Format

[![Coverity Scan Build Status](https://scan.coverity.com/projects/31383/badge.svg)](https://scan.coverity.com/projects/configurable-java-format)

This is a fork of `google-java-format` with extended configurability. This project is not affiliated with Google.

## Changes from the Original

- Removed all plugins – this fork contains only the core formatter.
- Added the `--width` option to specify a custom page width.
- Supports setting options via environment variables.

## Usage

```sh
# Help
java -jar configurable-java-format.jar --help

# Format
java -jar configurable-java-format.jar --width=120 File.java

# Format in place
java -jar configurable-java-format.jar --width=120 -i File.java
```

Alternatively, options can be passed as environment variables:

```sh
export JAVA_FORMAT_WIDTH=120
java -jar configurable-java-format.jar File.java
```

## IntelliJ Plugin

[![Version](https://img.shields.io/jetbrains/plugin/v/23827.svg)](https://plugins.jetbrains.com/plugin/23827)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/23827.svg)](https://plugins.jetbrains.com/plugin/23827)

The IntelliJ plugin is separate from the formatter and can be found here:

Plugin page: https://plugins.jetbrains.com/plugin/23827-external-java-formatter

Source code: https://github.com/MrDolch/external-java-formatter-intellij-plugin

## License

This project is based on `google-java-format` and follows the same licensing terms.