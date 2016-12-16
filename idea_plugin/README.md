# google-java-format IntelliJ IDEA Plugin

## Enabling

The plugin will not be enabled by default. To enable it in the current project,
go to "File→Settings...→google-java-format Settings" and check the "Enable"
checkbox.

To enable it by default in new projects, use "File→Other Settings→Default
Settings...".

## Set-up

1.  Build `google-java-format*all-deps.jar` by running `mvn install` in the
    `core` directory.
2.  Create a new "IntelliJ Platform Plugin" project.
3.  Add the `google-java-format.iml` module to this project by doing
    `File→New→Module from Existing Sources...` and selecting the `iml` file.
4.  Under "File→Project Structure→Libraries" add
    `core/target/google-java-format*all-deps.jar` file. IntelliJ will ask if you
    want to add it as a dependency to the google-java-format module. You do.
