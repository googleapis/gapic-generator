# GAPIC Generator

The GAPIC (Generated API Client) Generator auto-generates client libraries in various programming languages for both
grpc-based APIs and Discovery-document-based APIs. Currently, the following
programming languages are supported for grpc-based APIs:

- Java
- Python
- Go
- C#
- Ruby
- Node.js
- PHP

For discovery-based APIs, only Java is supported and its output is not stable yet.

The GAPIC Generator also generates packaging files for proto-generated classes and grpc-generated stubs for Java
and Python. This feature is not yet documented here in detail - the instructions below will work without it.

## Supported platforms

Toolkit itself doesn't have a platform restriction (because it is purely Java). Since `protoc` needs to be used as
part of the code generation process, the platforms are naturally restricted to what protoc supports.

As of release 3.5.1 of `protoc`, below are the known platforms that `protoc` has pre-built binaries for:

- linux-aarch_64
- linux-x86_32
- linux-x86_64
- osx-x86_32
- osx-x86_64
- win32

More platforms are likely supported when building protoc from C++ source.

## Usage

There are two main ways to generate clients:

1. Build from source and run directly. See below.
2. Invoke a code generation pipeline through
   [artman]( https://github.com/googleapis/artman/#usage). This takes care of several of the steps
   below and is not documented here.

### Prerequisites for code generation

1. You need a proto file describing your API. Proto files of existing Google APIs are available at
[googleapis](https://github.com/googleapis/googleapis).
  - Example: https://github.com/googleapis/googleapis/blob/master/google/cloud/language/v1/language_service.proto
2. You need a yaml file describing certain service values. This is not yet well-documented, but you can
  figure out some of the settings by looking at existing files.
  - Example: https://github.com/googleapis/googleapis/blob/master/google/cloud/language/language_v1.yaml
3. An active internet connection. The first time you build toolkit, it will download Java dependencies over
  the network.

### Process overview for generating a client library

0. Set up prerequisites for building from source and running directly.
1. Generate a descriptor file from the proto (once per API)
2. Run client config generation (once per API)
3. Manually tweak the generated client config
4. Run code generation (once per API x language combination of interest)

Step 0 is described below. Subsequent steps are described in the `./generate` script. Run `./generate` to see a description of the commands and manual steps in sequence. Run `./generate usage` to see how to use the same script, after setting the appropriate environment variables, to actually execute the steps.

Note: the instructions assume you are running on a
Unix-y system; if you are using Windows, you will need to tweak the steps for yourself.

### Set up prerequisites for building from source and running directly

First, make sure you have the
[Java 8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) installed.

Next, you need to install `protoc` if you don't have it yet: see
[protocol-compiler-installation](https://github.com/google/protobuf#protocol-compiler-installation).
The pre-built binaries are the easiest. There aren't instructions for installation;
you just have to download the correct binaries for your platform and put them on your path.
Given a platform ${PROTOC_PLATFORM} and a protoc version ${PROTOC_VERSION}:

1. Download the latest zip file ending in "-${PROTOC_PLATFORM}.zip" from https://github.com/google/protobuf/releases
2. Unzip the file: `unzip protoc-${PROTOC_VERSION}-${PROTOC_PLATFORM}.zip -d protoc-${PROTOC_VERSION}`
3. Copy `protoc` to a location on your `$PATH`: `cp protoc-${PROTOC_VERSION}/bin/protoc /usr/local/bin`

Clone the present repository and build API Client Generator:

```
git clone https://github.com/googleapis/toolkit.git
cd toolkit
./gradlew fatJar
```

The `googleapis/toolkit` directory will hereafter be referenced as `${TOOLKIT_DIR}`.

Clone the googleapis/googleapis repository (it has some config that is needed):

```
git clone https://github.com/googleapis/googleapis
```

The `googleapis/googleapis` directory will hereafter be referenced as `${GOOGLEAPIS_DIR}` in the `generate` script that documents subsequent steps.
