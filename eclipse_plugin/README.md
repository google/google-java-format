# Google Java Format Eclipse Plugin

## Enabling

See https://github.com/google/google-java-format#eclipse

## Development

### Prerequisites

Before building the plugin, make sure to run `mvn
tycho-versions:update-eclipse-metadata` to update the bundle version in
`META-INF/MANIFEST.MF`.

### Building the Plugin

1) Run `mvn clean package` in the `eclipse_plugin` directory. This will first copy the dependencies
of the plugin to `eclipse_plugin/lib/` and then triggers the tycho build that uses these
dependencies (as declared in `build.properties`) for the actual Eclipse plugin build.<br><br>
If you also want to add the build artifact to the local maven repository, you can use
`mvn clean install -Dtycho.localArtifacts=ignore` instead. Note, however, that you then must use
this build command for every build with that specific version number until you clear the build
artifact (or the
[p2-local-metadata.properties](https://wiki.eclipse.org/Tycho/Target_Platform#Locally_built_artifacts))
from your local repository. Otherwise, you might run into issues caused by the build using an
outdated build artifact created by a previous build instead of re-building the plugin. More
information on this issue is given
[in this thread](https://www.eclipse.org/lists/tycho-user/msg00952.html) and
[this bug tracker entry](https://bugs.eclipse.org/bugs/show_bug.cgi?id=355367).

2) You can find the built plugin in
`eclipse_plugin/target/google-java-format-eclipse-plugin-<version>.jar`

#### Building against a local (snapshot) release of the core

With the current build setup, the Eclipse plugin build pulls the needed build
artifacts of the google java format core from the maven repository and copies it
into the `eclipse_plugin/lib/` directory.

If you instead want to build against a local (snapshot) build of the core which
is not available in a maven repository (local or otherwise), you will have to
place the appropriate version into the `eclipse_plugin/lib/` directory yourself
before the build.
