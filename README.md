# GAPIC Generator

[![Build Status](https://travis-ci.org/googleapis/gapic-generator.svg?branch=master)](https://travis-ci.org/googleapis/gapic-generator)
[![CircleCI](https://circleci.com/gh/googleapis/gapic-generator.svg?style=svg)](https://circleci.com/gh/googleapis/gapic-generator)

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

The GAPIC Generator itself doesn't have a platform restriction (because it is purely Java). Since `protoc` needs to be used as
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

1. Build from source and run directly. This is detailed below.
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
3. An active internet connection. The first time you build gapic-generator, it will download Java dependencies over
  the network.

### Process overview for generating a client library

1. Generate a descriptor file from the proto (once per API)
2. Run client config generation (once per API)
3. Manually tweak the generated client config
4. Run code generation (once per API x language combination of interest)

Each of these steps are described in more detail below. Note: the instructions assume you are running on a
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
git clone https://github.com/googleapis/gapic-generator.git
cd gapic-generator
./gradlew fatJar
```

The `googleapis/gapic-generator` directory will hereafter be referenced as `${GAPIC_GENERATOR_DIR}`.

Clone the googleapis/googleapis repository (it has some config that is needed):

```
git clone https://github.com/googleapis/googleapis
```

The `googleapis/googleapis` directory will hereafter be referenced as `${GOOGLEAPIS_DIR}`.

### Generate a descriptor file from the proto

You need to locate/decide on the following before you can generate the descriptor file:

1. The include directory with the proto files bundled with `protoc` (from the `protoc` setup step).
   Hereafter, this will be referenced as `${PROTOC_INCLUDE_DIR}`.
2. Any directories containing protos that your API depends on. For Google APIs defined in
   [googleapis](https://github.com/googleapis/googleapis), this will be `${GOOGLEAPIS_DIR}`.
3. The directory containing your proto. Hereafter, this will be referenced as `${YOUR_PROTO_DIR}`.
4. Your proto file (or files). Hereafter, this will be referenced as `${YOUR_PROTO_FILE}`.
4. The output file name to contain the descriptor file. Hereafter, this will be referenced as
   `${YOUR_DESCRIPTOR_FILE}`.

Run the following command to generate the proto descriptor file:

```
protoc -I=${PROTOC_INCLUDE_DIR} -I=${GOOGLEAPIS_DIR} -I=${YOUR_PROTO_DIR} \
  --include_imports --include_source_info -o ${YOUR_DESCRIPTOR_FILE} ${YOUR_PROTO_FILE}
```

### Generate proto message classes

*(Skip this section for Node.js — it loads proto files into memory at runtime.)*

You need to locate/decide on the following before you can generate the proto message classes:

1. The output directory to contain the proto files. Hereafter, this will be referenced as `${GENERATED_PROTO_DIR}`.
2. The language you are generating for. The possible values for `protoc` are
`java`, `go`, `php`, `ruby`, `python`, and `csharp`. Note: Node.js is not present for the reason listed above.
Hereafter, this will be referenced as `${PROTO_OUTPUT_LANG}`.

Run the following command to generate the proto message classes:

```
mkdir -p ${GENERATED_PROTO_DIR}
protoc -I=${PROTOC_INCLUDE_DIR} -I=${GOOGLEAPIS_DIR} -I=${YOUR_PROTO_DIR} \
  --${PROTO_OUTPUT_LANG}_out=${GENERATED_PROTO_DIR} ${YOUR_PROTO_FILE}
```

### Generate grpc stubs

Generating grpc stubs is language-specific.

#### Java

Find the path to the grpc plugin (gradle will pull in the dependency if you don't have it already):

```
./gradlew showGrpcJavaPluginPath
```

The command will print out the path to the executable. Hereafter, this will be referenced as `${GRPC_JAVA_PLUGIN}`.

```
protoc -I=${PROTOC_INCLUDE_DIR} -I=${GOOGLEAPIS_DIR} -I=${YOUR_PROTO_DIR} \
  --plugin=protoc-gen-grpc=${GRPC_JAVA_PLUGIN} \
  --grpc_out=${GENERATED_PROTO_DIR} ${YOUR_PROTO_FILE}
```

#### Other languages

TODO

### Generate initial client config

You need to locate/decide on the following before you call config generation:

1. The service yaml file, hereafter referenced as `${YOUR_SERVICE_YAML}`.
2. The file name of your output client config file. Hereafter, this will be referenced as
   `${YOUR_CLIENT_CONFIG}`

Run the following command to generate the client config:

```
cd ${GAPIC_GENERATOR_DIR}
java -cp build/libs/gapic-generator-*-fatjar.jar \
  com.google.api.codegen.GeneratorMain GAPIC_CONFIG \
  --descriptor_set=${YOUR_DESCRIPTOR_FILE} --service_yaml=${YOUR_SERVICE_YAML} \
  -o=${YOUR_CLIENT_CONFIG}
```

You can safely ignore the warning about control-presence.

### Manually tweak the generated client config

The generated client config contains `FIXME` comments with instructions on how to choose values
in the client config. The client config should work as is, though; tweaks are only necessary to improve
the quality of the generated output.

### Create a package metadata config file

You need to locate/decide on the following before you create the package metadata config file:

1. The output file to contain the packaging config for your client generation. Hereafter, this will be
   referenced as `${PACKAGING_CONFIG}`.
2. The short name of your API, which typically does not include the major version. For example, Cloud Language v1's short name
   is `language`. Hereafter, this will be referenced as `${API_NAME}`.
3. The API major version. The first major version will typically be `v1`. Hereafter, this will be referenced as
   `${API_VERSION}`.
4. The organization name of the API. This would typically include the company name. For example,
   Cloud Language's organization name is `google-cloud`. This is used for the full artifact name.
   Hereafter, this will be referenced as `${ORGANIZATION_NAME}`.
5. The proto path, which is the proto's package with dots converted to slashes. For example, Cloud Language v1's
   proto path is `google/cloud/language/v1`. Hereafter, this will be referenced as `${PROTO_PATH}`.

Add the following lines to the ${PACKAGING_CONFIG} file:

```
artifact_type: GAPIC
proto_deps:
- google-common-protos
api_name: ${API_NAME}
api_version: ${API_VERSION}
organization_name: ${ORGANIZATION_NAME}
proto_path: ${PROTO_PATH}
```

### Run code generation

You need to locate/decide on the following before you call code generation:

1. Your target language, which can be one of `java`, `go`, `php`, `ruby`, `nodejs`, `python`, or `csharp`.
   Hereafter, this will be referenced as `${LANGUAGE}`.
2. The output directory for your generated client classes. Hereafter, this will be referenced
   as `${GENERATED_CLIENT_DIR}`.

```
java -cp build/libs/gapic-generator-*-fatjar.jar  \
  com.google.api.codegen.GeneratorMain LEGACY_GAPIC_AND_PACKAGE \
  --descriptor_set=${YOUR_DESCRIPTOR_FILE} --service_yaml=${YOUR_SERVICE_YAML} \
  --gapic_yaml=${YOUR_CLIENT_CONFIG} --language=${LANGUAGE} \
  --package_yaml2=${PACKAGING_CONFIG} --o=${GENERATED_CLIENT_DIR}
```

The generated client library code will appear in `${GENERATED_CLIENT_DIR}`.

You can safely ignore the warning about control-presence.

Special note for java: several files will be dumped into the parent directory of `${GENERATED_CLIENT_DIR}`:

- gradle/
- gradlew
- gradlew.bat

(There is an open issue to fix this: https://github.com/googleapis/gapic-generator/issues/1918 )

### Perform fixes to get a working library

The fixes here are language-specific.

#### Java

Copy the proto-generated classes into the client directory tree:

```
cp -r ${GENERATED_PROTO_DIR}/* ${GENERATED_CLIENT_DIR}/src/main/java
```

In `${GENERATED_CLIENT_DIR}/build.gradle`, there are a couple dependencies that need to be removed if you
are bundling your proto-generated classes and grpc stubs in the same package as the client. They have a
comment starting with "`// Remove this line if you are bundling`". Warning: If you regenerate again, you will
need to remove the lines from the build.gradle file again. (There is an open issue to fix this:
https://github.com/googleapis/gapic-generator/issues/1917 ).

#### Node

Copy the proto files into the correct subdirectory of `${GENERATED_CLIENT_DIR}`.
(Exact instructions to be added later.)

#### Other languages

TODO
