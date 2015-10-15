# Development

[NOTE: these instructions are partly for development with git-on-borg and will need to be updated
for github.]

## Setup

You need to run `prodaccess` to access git-on-borg.

If you haven't done so earlier, install Google's git protocol extensions:

    sudo apt-get install git-remote-google

The client library projects (e.g. gapi-example-library-java) require kdiff3; make sure you have
this installed:

    sudo apt-get install kdiff3

Currently, several projects need to be cloned onto your local machine and be built in a certain order.

The short version to install those is to execute the `enroll.sh` script.

Create a development root directory, e.g. `~/g` if you like it short. Download
the file
[enroll.sh](https://gapi.git.corp.google.com/gapi-tools/+/master/scripts/enroll.sh)
to this directory (note: git-on-borg does not seem to support the `raw` protocol, so
we can't do this download from a bash command; once we are on github, we can).

Now execute in `~/g`:

    bash enroll.sh

If all builds have succeeded, you should be set. To test your setup further, see the examples
at [gapi-example-library-java](https://gapi.git.corp.google.com/gapi-example-library-java) for how
to play with the book library example, which was cloned by `enroll.sh` in the last step.

If you are interested in what is happening under the hood, here is the content of `enroll.sh`
script (at some point in time):

    git clone https://github.com/wrwg/protobuf-gradle-plugin.git || exit 1
    (cd protobuf-gradle-plugin && ./gradlew install) || exit 1

    git clone --recursive sso://gapi/gapi-tools || exit 1
    (cd gapi-tools && ./gradlew build install) || exit 1

    git clone --recursive sso://gapi/gapi-gax-java || exit 1
    (cd gapi-gax-java && ./gradlew build install) || exit 1

    git clone --recursive sso://gapi/gapi-core-java || exit 1
    (cd gapi-core-java && ./gradlew build install) || exit 1

    git clone --recursive sso://gapi/gapi-example-library-java || exit 1
    (cd gapi-example-library-java && ./gradlew build)

Note: cloning a larger number of repositories for setting up a development
environment is also what Android needs. They created the
[repo](https://source.android.com/source/using-repo.html) tool which we may
reuse here.


## IDE Support

It is highly recommended to work with an IDE. Currently, only Eclipse has been tried, but IntelliJ
should work in theory as well, as it is supported by gradle.

### Eclipse

You may want to first install the
[Eclipse Marketplace Client](https://marketplace.eclipse.org) which provides a
convenient UI in Eclipse to find and install plugins. Use the regular `Help > Install
New Software` to locate Marketplace Client, for Eclipse 4.5 at this install
site: [http://download.eclipse.org/releases/mars]()

1. (Required) Install Buildship gradle integration. There are other gradle plugins as well, but
   that one is confirmed to work. It is on marketplace.
2. (Required) Install Eclipse Groovy support. This is _not_ on marketplace; you can install it
   using this site: [http://dist.springsource.org/snapshot/GRECLIPSE/e4.5/]() - put it in the
   sites field in the `Install New Software` dialog. 
3. (Optional) On marketplace, you can find editors for markdown and a 'github' style viewer, which are
   handy. Note that the viewer needs a `Show View` before it becomes visible.

Once you have set up everything, you can import the gapi-tools project in Eclipse via File>Import>Gradle.
Before doing so, you should run the tasks `build` and `eclipse` (in any order) . The `build` step is
needed to let protoc generate Java sources which Eclipse would not find otherwise. The `eclipse`
step generates Eclipse settings.

The importer can also generate settings by itself, but it's unclear whether they are equivalent
with those generated from the command line. If the importer finds settings, it leaves them alone
and just uses them. That's why `./gradlew eclipse` plays well with the importer.

If the `build.gradle` has changed, you can regenerate Eclipse settings from the command line by
running `./gradlew eclipse`. The changes should be picked up automatically by Eclipse.

Note that `./gradlew eclipse` works incrementally; it will not remove settings unless it's sure
to have created them (and that seems to not work sometimes). To ensure your
settings are consistent, you can use `./gradlew cleanEclipse`.


### IntelliJ

TBD

## Note On Gradle Daemon

Gradle bugs you to configure its daemon to speed up build time. If you follow this hint, be
careful that the daemon may not reload the plugin and related code you may just have changed. Don't
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

## Note on Submodules

If you clone a repository with submodules using the `--recursive` flag, or other
means, the submodule ends at a state which is specified in the parent repository
via a commit id (such as `87fa7afc7d`). That state is not necessarily the same as the
current 'master' in the origin repository.

That is because 'master` is just a symbol for a given commit id:

    master -> 87fa7afc7

Git does not store the symbol but the commit id in the parent repository, so that a
submodule does not pull in arbitrary changes from its master.

This behavior has been subject of lots of criticism about sub-modules, but it reflects
the inherent complexity of the problem domain.

TBD: recipes on how to work with submodules.
