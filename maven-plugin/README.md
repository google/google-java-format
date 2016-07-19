# Google Java Format Maven Plugin

## Usage

The plugin can be configured in the `build` section:

```xml
<plugin>
	<groupId>com.google.goolejavaformat</groupId>
	<artifactId>google-java-format-maven-plugin</artifactId>
	<version>RELEASE</version>
	<configuration>
		<failOnError>false</failOnError>
		<style>google</style>
	</configuration>
	<executions>
		<execution>
			<id>default</id>
			<phase>process-sources</phase>
			<goals>
				<goal>format</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

## Build

To build the plugin

```bash
$ mvn clean install
```

and to apply it against itself

```bash
$ mvn verify -Prun-self
```
