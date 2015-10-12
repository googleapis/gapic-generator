# Development

[NOTE: these instructions are partly for development with git-on-borg and will need to be updated
for github.]

## Setup

Currently, several projects need to be cloned onto your local machine and build in a certain order.

Create a development root directory, e.g. `~/g` if you like it short.

Clone and install our version of grpc-java which includes the Callable abstraction:

    git clone https://github.com/wrwg/grpc-java.git
    cd grpc-java
    ./gradlew install
    cd ..

Clone and install our version of protobuf-gradle-plugin:

    git clone https://github.com/wrwg/protobuf-gradle-plugin.git
    cd protobuf-gradle-plugin
    ./gradlew install
    cd ..

Clone the gapi-tools (this). _Note_ the `--recursive` flag to the `git clone` call; it includes
the content of submodules in the clone. If you don't use
it, you need to init and update the submodules manually with `git submodule` (see e.g.
[here](https://git-scm.com/docs/git-submodule) for the submodule feature documentation):

    git clone --recursive sso://gapi/gapi-tools
    cd gapi-tools
    ./gradlew build install
    cd ..

Clone the GAX library:

    git clone --recursive sso://gapi/gapi-gax-java
    cd gapi-gax-java
    ./gradlew build install
    cd ..

Clone the library example, which currently also serves as a test that things are correctly setup:

    git clone --recursive sso://gapi/gapi-example-library-java
    cd gapi-example-library-java
    ./gradlew build

If all builds have succeeded, you should be set. To test your setup further, see the instructions
at [gapi-example-library-java](https://gapi.git.corp.google.com/gapi-example-library-java) for how
to play with the example.

## IDE Support

It is highly recommended to work with an IDE. Currently, only Eclipse has been tried, but IntelliJ
should work in theory as well, as it is supported by gradle.

### Eclipse

You may want to first install the [Eclipse Marketplace Client](https://marketplace.eclipse.org)
which provides a convenient UI in Eclipse to find and install plugins. Use the regular
`Install New Software` to locate Marketplace Client, for Eclipse 4.5 at this install site: [http://download.eclipse.org/releases/mars]()

1. (Required) Install Buildship gradle integration. There are other gradle plugins as well, but
   that one is confirmed to work. It is on marketplace.
2. (Required) Install Eclipse Groovy support. This is _not_ on marketplace, I installed it from
   here: [http://dist.springsource.org/snapshot/GRECLIPSE/e4.5/]()
3. (Optional) You find on marketplace editors for markdown and a 'github' style viewer, which are
   handy. Note that the viewer needs a `Show View` before it becomes visible.

Once you have setup everything, you can import gapi-tools project in Eclipse via File>Import>Gradle.
Before doing so, you should run tasks `build` and `eclipse` (in any order) . The `build` step is
needed to let protoc generate Java sources which Eclipse would not find otherwise. The `eclipse`
step generates Eclipse settings.

The importer can also generate settings by itself, but its unclear whether they are equivalent
with those generated from the command line. If the importer finds settings, it leaves them alone
and just uses them. That's why `./gradlew eclipse` plays well with the importer.

If the `build.gradle` has changed, you can regenerate Eclipse settings from the command line by
running `./gradlew eclipse`. The changes should be picked up automatically by Eclipse.

Note that `./gradlew eclipse` works incremental, it will not remove settings unless its sure
to have created them (and that seems to not work sometimes). To ensure your
settings are consistent, you can use `./gradlew cleanEclipse`.


### IntelliJ

TBD

## Note On Gradle Daemon

Gradle bugs you to configure it's daemon to speed up build time. If you follow this hint, be
careful that the daemon may not reload plugin and related code you may just have changed. Don't
use the daemon during plugin development, or restart it with `./gradlew --stop`
when needed.

## Comparing Delta with Google3

There is a utility script in `./gapi-tools/scripts/g3diff.sh` which allows to show the delta
with Google3 code. Use as in

    cd src/main/java
    g3diff.sh io/gapi/fx/model/Model.java # compare a single file
    g3diff.sh .                           # compare the entire tree
    g3diff.sh -n <arg>                    # don't call UI, just report which files differ

The script must always be called at the root of a source tree. It will ignore package names and
imports (it just deletes them before calling diff), and only actual code differences are shown.

