# google-java-format Eclipse Plugin

## Enabling

See https://github.com/google/google-java-format#eclipse

## Development

1) Uncomment `<module>eclipse_plugin</module>` in the parent `pom.xml`

2) Run `mvn install`, which will copy the dependences of the plugin to
`eclipse_plugin/lib`.

2) If you are using Java 9, add

    ```
    -vm
    /Library/Java/JavaVirtualMachines/jdk1.8.0_91.jdk/Contents/Home/bin/java
    ```

    to `/Applications/Eclipse.app/Contents/Eclipse/eclipse.ini`.

3) Open the `eclipse_plugin` project in a recent Eclipse SDK build.

4) From `File > Export` select `Plugin-in Development > Deployable plugin-ins
and fragments` and follow the wizard to export a plugin jar.
