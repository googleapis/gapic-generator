# Introduction

GAPI tools consist of the following sub projects:

- The [GAPI tools framework](https://gapi.git.corp.google.com/gapi-tools-framework), a set of
  libraries to process _API services_, that is a collection of protocol buffer definitions and
  configuration files.
- The [GAPI generators and synchronizers](./vgen), which produce wrappers around GRPC apis
  which can be enriched by manually edited code.

For a usage example, see the
[gapi-example-library-java](https://gapi.git.corp.google.com/gapi-example-library-java) repository.

For instructions how to setup a development environment, see [gapi-dev/README.md](https://gapi.git.corp.google.com/gapi-dev/+/master/README.md).


# Design Notes

## The GAPI Org

Google APIs and tools are expected to live in a dedicated organization on github, with tentative
name "gapi". (We currently model gapi on [git-on-borg](https://go/git-on-borg) at URL
[https://gapi.git.corp.google.com]()).

The GAPI org contains projects for protos and projects for compiled and enhanced ('veneered')
GRPC clients for the according protos. The later ones are language specific. It also contains
the tools to work with those projects.

Language specific client projects aggregate the according proto projects as git
[submodules](https://git-scm.com/docs/git-submodule), so their source can be
included in the compilation (note that we may later change this by introducing a proto packaging
mechanism instead). For example:

- [gapi-example-library-proto](https://gapi.git.corp.google.com/gapi-example-library-proto)
  contains the protocol buffers and service configuration for the book library example.
- [gapi-example-library-java](https://gapi.git.corp.google.com/gapi-example-library-java) contains
  the generated Java GRPC client, as well as veneers for it. The above proto repository is
  aggregated as a submodule at `${projectDir}/modules/gapi-example-library-proto`.

## The Veneer Synchronizer

Veneer wrappers are stored at the regular source location, i.e. `./src/${sourceSetName}`. The
veneer synchronizer creates or updates wrappers at this location. The synchronizer is not
called implicitly as part of a regular build step but explicitly when the protos or configurations
have changed, or for initial veneer creation.

The synchronizer can be invoked directly through a gradle task(see build.gradle:runSynchronizer
task) or [the code generation pipeline](https://gapi.git.corp.google.com/pipeline).

Synchronization works as follows:

1. The veneer generator is called, and the output stored in a temporary location.
2. An automatic 3-way merge is attempted using
   - the persisted baseline file for the old generated output found in
     `./baseline/${sourceSet}`;
   - the newly generated output;
   - and the edited source of the wrapper, found in `./src/${sourceSet}`.
3. If merge succeeds, the baseline and the edited source are updated.

In case no source for the veneer exists yet, the generated one is simply copied into the source
as well as the baseline folder.

The process currently hard-wires `kdiff3` for merging and conflict resolution. `kdiff3` runs in auto
mode and will only pop up a UI if conflict resolution is not possible (note: X display needed in
this case).

# Deployment

Use the following command to deploy Gapi tools framework to a private maven
repository:

    ./gradlew uploadArchives

Note: you will need to set privateOssrhUsername and privateOssrhPassword in
{USER_HOME}/.gradle/gradle.properties file (create one if it doesn't exist).

    privateOssrhUsername=deployment
    privateOssrhPassword=<Check your inbox>

