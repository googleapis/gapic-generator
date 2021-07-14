# Gapic Generator

## Overview

Warning: The monolith generator has been deprecated. Please use the
microgenerators instead.

The [Gapic generator](https://github.com/googleapis/gapic-generator-java) allows
you create highly idiomatic and usable Java client libraries for One Platform
APIs.

## Development

### Set Up

1.  Clone the repo.

    ```
    REPO_ROOT=~/gapic-generator
    git clone git@github.com:googleapis/gapic-generator.git $REPO_ROOT
    ```

2.  Install Gradle. Please refer to the steps
    [here](https://gradle.org/install/)

### Common Commands.

1.  Run google-java-format.

    ```
    cd $REPO_ROOT
    ./gradlew googleJavaFormat
    ```

2.  Build the code.

    ```
    ./gradlew fatJar
    ```

3.  Run tests.

    ```
    ./gradlew test
    ```

## Generating GAPIC code from protobufs.

For our example, we'll use
[gapic-showcase](https://github.com/googleapis/gapic-showcase/).

1.  Clone [googleapis](https://github.com/googleapis/googleapis) and
    [gapic-showcase](https://github.com/googleapis/gapic-showcase/) and install
    protoc.

    ```
    GOOGLEAPIS=~/googleapis
    GAPIC_SHOWCASE=~/gapic-showcase
    git clone git@github.com:googleapis/googleapis.git $GOOGLEAPIS
    git clone git@github.com:googleapis/gapic-showcase $GAPIC_SHOWCASE
    ```

2.  Copy the protos from Showcase into googleapis/google/showcase.

    ```sh
    cp $GAPIC_SHOWCASE/schema/google/showcase/v1beta1 $GOOGLEAPIS/google/showcase/v1beta
    ```

3.  Generate the Bazel build rules.

    ```
    cd $GOOGLEAPIS
    bazel run //:build_gen -- --src=google/showcase/v1beta1
    ```

4.  Call `bazel build` on the appropriate rule.

    ```
    bazel build google/showcase/v1beta1:showcase_java_gapic
    ```

5.  To find the generated sources, build the following rule. (Replace "java"
    with the approrpriate language to see other languagues' generated source.)

    ```
    bazel build google/showcase/v1beta1:google-cloud-showcase-v1beta1
    ```

6.  To use a local copy of the GAPIC generator with changes, point the WORKSPACE
    directory to your local one instead. Replace the `http_archive` rule with
    the `local_repository` rule below.

    ```py
    local_repository(
      name = "com_google_api_codegen",
      path = /usr/local/google/home/{{USERNAME}}/gapic-generator
    )
    ```
