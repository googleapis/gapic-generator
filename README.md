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
3. An active internet connection. The first time you build toolkit, it will download Java dependencies over
  the network.

### Process overview for generating a client library

1. Generate a descriptor file from the proto (once per API)
2. Run client config generation (once per API)
3. Manually tweak the generated client config
4. Run code generation (once per API x language combination of interest)

Each of these steps are described in more detail below. Note: the instructions assume you are running on a
Unix-y system; if you are using Windows, you will need to tweak the steps for yourself.

### Set up prerequisites for building from source and running directly

Firstly, make sure you have the
[Java 8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) installed.

Next, you need to install `protoc` if you don't have it yet: see
[protocol-compiler-installation](https://github.com/google/protobuf#protocol-compiler-installation).
The pre-built binaries are the easiest. There aren't instructions for installation;
you just have to download the correct binaries for your platform and put them on your path.
If you are using Linux x86_64:

1. Download the latest zip from https://github.com/google/protobuf/releases ending in "-linux-x86_64.zip"
2. Unzip the file, e.g. `unzip protoc-3.5.1-linux-x86_64.zip -d protoc-3.5.1`
3. Copy `protoc` to a location on your `$PATH`, e.g. `cp protoc-3.5.1/bin/protoc /usr/local/bin`

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

*(Skip this section for Node.js - it loads proto files into memory at runtime.)*

You need to locate/decide on the following before you can generate the proto message classes:

1. The output directory to contain the proto files. Hereafter, this will be referenced as `${GENERATED_PROTO_DIR}`.
2. The language you are generating for. The possible values for `protoc` are
`java`, `go`, `php`, `ruby`, `python`, and `csharp`. Note: Node.js is not present for the reason listed above.
Hereafter, this will be referenced as `${PROTO_OUTPUT_LANG}`.

Run the following command to generate the proto message classes:

```
rm -rf ${GENERATED_PROTO_DIR}
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
cd ${TOOLKIT_DIR}
java -cp build/libs/gapic-generator-*-fatjar.jar com.google.api.codegen.configgen.ConfigGeneratorTool \
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

1. The output file to contain the package metadata for your client generation. Hereafter, this will be
   referenced as `${PKG_META_CONFIG}`.
2. A short name, which typically does not include the major version. For example, Cloud Language v1's short name
   is `language`. Hereafter, this will be referenced as `${SHORT_NAME}`.
3. The major version. The first major version will typically be `v1`. Hereafter, this will be referenced as
   `${MAJOR_VERSION}`.
4. The package name, which typically includes the fully-qualified product name and major version.
   For example, Cloud Language v1's package name is `google-cloud-language-v1`. Hereafter, this will be
   referenced as `${PACKAGE_NAME}`.
5. The proto path, which is the proto's package with dots converted to slashes. For example, Cloud Language v1's
   proto path is `google/cloud/language/v1`. Hereafter, this will be referenced as `${PROTO_PATH}`.

First, copy the contents of api_defaults.yaml and dependencies.yaml into a single file, hereafter
referenced as `${PKG_META_CONFIG}`.

```
cp ${GOOGLEAPIS_DIR}/gapic/packaging/api_defaults.yaml ${PKG_META_CONFIG}
cat < ${GOOGLEAPIS_DIR}/gapic/packaging/dependencies.yaml >> ${PKG_META_CONFIG}
```

Add the following lines to the end:

```
artifact_type: GAPIC
proto_deps:
- google-common-protos
short_name: ${SHORT_NAME}
major_version: ${MAJOR_VERSION}
package_name:
  default: ${PACKAGE_NAME}
proto_path: ${PROTO_PATH}
```

### Run code generation

You need to locate/decide on the following before you call code generation:

1. The language-specific generator config for your desired output language. Given a language where
   `${language}` is one of `java`, `go`, `php`, `ruby`, `nodejs`, `py`, or `csharp`, the file is located at
   `src/main/resources/com/google/api/codegen/${language}/${language}_gapic.yaml` (exception: the Python file is named
   `python_gapic.yaml`). Hereafter, this will be referenced as `${LANGUAGE_CONFIG}`.
2. The output directory for your generated client classes. Hereafter, this will be
   referenced as `${GENERATED_CLIENT_DIR}`.

```
java -cp build/libs/gapic-generator-*-fatjar.jar com.google.api.codegen.CodeGeneratorTool \
  --descriptor_set=${YOUR_DESCRIPTOR_FILE} --service_yaml=${YOUR_SERVICE_YAML} \
  --gapic_yaml=${YOUR_CLIENT_CONFIG} --gapic_yaml=${LANGUAGE_CONFIG} \
  --package_yaml=${PKG_META_CONFIG} --o=${GENERATED_CLIENT_DIR}
```

The generated client library code will appear in `${GENERATED_CLIENT_DIR}`.

You can safely ignore the warning about control-presence.

Special note for java: several files will be dumped into the parent directory of `${GENERATED_CLIENT_DIR}`:

- gradle/
- gradlew
- gradlew.bat

(There is an open issue to fix this: https://github.com/googleapis/toolkit/issues/1918 )

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
https://github.com/googleapis/toolkit/issues/1917 ).

#### Node

Copy the proto files into the correct subdirectory of `${GENERATED_CLIENT_DIR}`.
(Exact instructions to be added later.)

#### Other languages

TODO
