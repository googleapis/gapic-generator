# GAPIC Generator

GAPIC (Generated API Client) Generator is a client library code generator which auto-generates
clients for both grpc-based APIs and Discovery-document-based APIs. Currently, the following
programming languages are supported:

- Java
- Python
- Go
- C#
- Ruby
- Node.js
- PHP

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
2. You need a yaml file describing certain service values. This is unfortunately not
well-documented.
  - Example: https://github.com/googleapis/googleapis/blob/master/google/cloud/language/language_v1.yaml
3. Java 8 JDK

### Process overview for generating a client library

1. Generate a descriptor file from the proto (once per API)
2. Run client config generation (once per API)
3. Manually tweak the generated client config
4. Run code generation (once per API + language)

Each of these steps are described in more detail below.

### Setup for building from source and running directly

First, you need to install protoc if you don't have it yet: see
[protocol-compiler-installation](https://github.com/google/protobuf#protocol-compiler-installation).
The pre-built binaries are the easiest, but there aren't really instructions for installation -
you have to just download the correct binaries for your platform at put them on your path.
If you are using Linux x86_64:

1. Download the latest zip from https://github.com/google/protobuf/releases ending in "-linux-x86_64.zip"
2. Unzip the file, e.g. `unzip protoc-3.5.1-linux-x86_64.zip -d protoc-3.5.1`
3. Copy protoc to a location on $PATH, e.g. `cp protoc-3.5.1/bin/protoc /usr/local/bin`

Clone the present repository and build API Client Generator:

```
git clone https://github.com/googleapis/toolkit.git
cd toolkit
./gradlew fatJar
```

The googleapis/toolkit directory will hereafter be referenced as `${TOOLKIT_DIR}`.

Clone the googleapis/googleapis repository (it has some config that is needed):

```
git clone https://github.com/googleapis/googleapis
```

The googleapis/googleapis directory will hereafter be referenced as `${GOOGLEAPIS_DIR}`.

### Generate a descriptor file from the proto

You need to locate/decide on a few directories and filenames before you can call protoc.

1. The include directory with the proto files bundled with protoc (from the protoc setup step).
   Hereafter, this will be referenced as `${PROTOC_INCLUDE_DIR}`.
2. Any directories containing protos that your API depends on. For Google APIs defined in
   [googleapis](https://github.com/googleapis/googleapis), this will be `${GOOGLEAPIS_DIR}`.
3. The directory containing your proto. Hereafter, this will be referenced as `${YOUR_PROTO_DIR}`.
4. Your proto file. Hereafter, this will be referenced as `${YOUR_PROTO_FILE}`.
4. The output file name to contain the descriptor file. Hereafter, this will be referenced as
   `${YOUR_DESCRIPTOR_FILE}`.

Run the following command to generate the proto descriptor file:

```
protoc -I=${PROTOC_INCLUDE_DIR} -I=${GOOGLEAPIS_DIR} -I=${YOUR_PROTO_DIR} \
  --include_imports --include_source_info -o ${YOUR_DESCRIPTOR_FILE} ${YOUR_PROTO_FILE}
```

### Run client config generation

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

The generated client config contains FIXME comments with instructions on how to choose values
in the client config. The client config should work as-is though; tweaks are only necessary to improve
the quality of the generated output.

### Create a package metadata config file

You need to decide on the following before you perform the work in this section:

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
   `${language}` is in `["csharp", "go", "java", "nodejs", "php", "py", "ruby"]`, the file is located at
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
