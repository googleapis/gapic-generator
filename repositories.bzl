# Copyright 2019 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_jar")

def com_google_api_codegen_repositories(
        omit_com_google_protobuf = False,
        omit_com_atlassian_commonmark_commonmark = False,
        omit_com_fasterxml_jackson_core_jackson_annotations = False,
        omit_com_fasterxml_jackson_core_jackson_core = False,
        omit_com_fasterxml_jackson_core_jackson_databind = False,
        omit_com_google_api_api_common = False,
        omit_com_google_api_api_compiler = False,
        omit_com_google_api_grpc_proto_google_common_protos = False,
        omit_com_google_auto_value_auto_value = False,
        omit_com_google_auto_value_auto_value_annotations = False,
        omit_com_google_code_findbugs_jsr305 = False,
        omit_com_google_code_gson_gson = False,
        omit_com_google_guava_guava = False,
        omit_com_google_inject_guice = False,
        omit_commons_cli_commons_cli = False,
        omit_io_grpc_grpc_core = False,
        omit_org_apache_commons_commons_lang3 = False,
        omit_org_threeten_threetenbp = False,
        omit_org_yaml_snakeyaml = False,
        omit_cglib_cglib = False,
        omit_javax_inject_javax_inject = False,
        omit_org_ow2_asm_asm = False):
    if not omit_com_google_protobuf:
        com_google_protobuf()
    if not omit_com_atlassian_commonmark_commonmark:
        com_atlassian_commonmark_commonmark()
    if not omit_com_fasterxml_jackson_core_jackson_annotations:
        com_fasterxml_jackson_core_jackson_annotations()
    if not omit_com_fasterxml_jackson_core_jackson_core:
        com_fasterxml_jackson_core_jackson_core()
    if not omit_com_fasterxml_jackson_core_jackson_databind:
        com_fasterxml_jackson_core_jackson_databind()
    if not omit_com_google_api_api_common:
        com_google_api_api_common()
    if not omit_com_google_api_api_compiler:
        com_google_api_api_compiler()
    if not omit_com_google_api_grpc_proto_google_common_protos:
        com_google_api_grpc_proto_google_common_protos()
    if not omit_com_google_auto_value_auto_value:
        com_google_auto_value_auto_value()
    if not omit_com_google_auto_value_auto_value_annotations:
        com_google_auto_value_auto_value_annotations()
    if not omit_com_google_code_findbugs_jsr305:
        com_google_code_findbugs_jsr305()
    if not omit_com_google_code_gson_gson:
        com_google_code_gson_gson()
    if not omit_com_google_guava_guava:
        com_google_guava_guava()
    if not omit_com_google_inject_guice:
        com_google_inject_guice()
    if not omit_commons_cli_commons_cli:
        commons_cli_commons_cli()
    if not omit_io_grpc_grpc_core:
        io_grpc_grpc_core()
    if not omit_org_apache_commons_commons_lang3:
        org_apache_commons_commons_lang3()
    if not omit_org_threeten_threetenbp:
        org_threeten_threetenbp()
    if not omit_org_yaml_snakeyaml:
        org_yaml_snakeyaml()
    if not omit_cglib_cglib:
        cglib_cglib()
    if not omit_javax_inject_javax_inject:
        javax_inject_javax_inject()
    if not omit_org_ow2_asm_asm:
        org_ow2_asm_asm()

def com_google_api_codegen_test_repositories(
        omit_junit_junit = False,
        omit_com_google_truth_truth = False,
        omit_com_google_truth_extensions_truth_java8_extension = False,
        omit_com_google_api_api_compiler_testing = False,
        omit_pl_pragmatists_JUnitParams = False,
        omit_org_mockito_mockito_core = False,
        omit_org_hamcrest_hamcrest_core = False,
        omit_net_bytebuddy_byte_buddy = False,
        omit_org_objenesis_objenesis = False,
        omit_google_java_format_all_deps = False):
    if not omit_junit_junit:
        junit_junit()
    if not omit_com_google_truth_truth:
        com_google_truth_truth()
    if not omit_com_google_truth_extensions_truth_java8_extension:
        com_google_truth_extensions_truth_java8_extension()
    if not omit_com_google_api_api_compiler_testing:
        com_google_api_api_compiler_testing()
    if not omit_pl_pragmatists_JUnitParams:
        pl_pragmatists_JUnitParams()
    if not omit_org_mockito_mockito_core:
        org_mockito_mockito_core()
    if not omit_org_hamcrest_hamcrest_core:
        org_hamcrest_hamcrest_core()
    if not omit_net_bytebuddy_byte_buddy:
        net_bytebuddy_byte_buddy()
    if not omit_org_objenesis_objenesis:
        org_objenesis_objenesis()

def com_google_api_codegen_tools_repositories(omit_google_java_format_all_deps = False):
    if not omit_google_java_format_all_deps:
        google_java_format_all_deps()

def com_google_protobuf():
    # TODO: try loading precompiled protoc (it comes with common protos) and use it for everything.
    # Using protobuf requires building it before usage, which takes time.
    git_repository(
        name = "com_google_protobuf",
        remote = "https://github.com/google/protobuf.git",
        tag = "v3.6.1",
    )

def com_atlassian_commonmark_commonmark():
    native.maven_jar(
        name = "com_atlassian_commonmark_commonmark",
        artifact = "com.atlassian.commonmark:commonmark:0.9.0",
    )

def com_fasterxml_jackson_core_jackson_annotations():
    native.maven_jar(
        name = "com_fasterxml_jackson_core_jackson_annotations",
        artifact = "com.fasterxml.jackson.core:jackson-annotations:2.9.0",
    )

def com_fasterxml_jackson_core_jackson_core():
    native.maven_jar(
        name = "com_fasterxml_jackson_core_jackson_core",
        artifact = "com.fasterxml.jackson.core:jackson-core:2.9.0",
    )

def com_fasterxml_jackson_core_jackson_databind():
    native.maven_jar(
        name = "com_fasterxml_jackson_core_jackson_databind",
        artifact = "com.fasterxml.jackson.core:jackson-databind:2.9.0",
    )

def com_google_api_api_common():
    native.maven_jar(
        name = "com_google_api_api_common",
        artifact = "com.google.api:api-common:1.1.0",
    )

def com_google_api_api_compiler():
    native.maven_jar(
        name = "com_google_api_api_compiler",
        artifact = "com.google.api:api-compiler:0.0.8",
    )

def com_google_api_grpc_proto_google_common_protos():
    native.maven_jar(
        name = "com_google_api_grpc_proto_google_common_protos",
        artifact = "com.google.api.grpc:proto-google-common-protos:1.13.0-pre3",
    )

def com_google_auto_value_auto_value():
    native.maven_jar(
        name = "com_google_auto_value_auto_value",
        artifact = "com.google.auto.value:auto-value:1.6",
    )

def com_google_auto_value_auto_value_annotations():
    native.maven_jar(
        name = "com_google_auto_value_auto_value_annotations",
        artifact = "com.google.auto.value:auto-value-annotations:1.6",
    )

def com_google_code_findbugs_jsr305():
    native.maven_jar(
        name = "com_google_code_findbugs_jsr305",
        artifact = "com.google.code.findbugs:jsr305:3.0.0",
    )

def com_google_code_gson_gson():
    native.maven_jar(
        name = "com_google_code_gson_gson",
        artifact = "com.google.code.gson:gson:2.7",
    )

def com_google_guava_guava():
    native.maven_jar(
        name = "com_google_guava_guava",
        artifact = "com.google.guava:guava:26.0-jre",
    )

def com_google_inject_guice():
    native.maven_jar(
        name = "com_google_inject_guice",
        artifact = "com.google.inject:guice:4.0",
    )

def commons_cli_commons_cli():
    native.maven_jar(
        name = "commons_cli_commons_cli",
        artifact = "commons-cli:commons-cli:1.4",
    )

def io_grpc_grpc_core():
    native.maven_jar(
        name = "io_grpc_grpc_core",
        artifact = "io.grpc:grpc-core:1.0.1",
    )

def org_apache_commons_commons_lang3():
    native.maven_jar(
        name = "org_apache_commons_commons_lang3",
        artifact = "org.apache.commons:commons-lang3:3.6",
    )

def org_threeten_threetenbp():
    native.maven_jar(
        name = "org_threeten_threetenbp",
        artifact = "org.threeten:threetenbp:1.3.3",
    )

def org_yaml_snakeyaml():
    native.maven_jar(
        name = "org_yaml_snakeyaml",
        artifact = "org.yaml:snakeyaml:1.18",
    )

def cglib_cglib():
    native.maven_jar(
        name = "cglib_cglib",
        artifact = "cglib:cglib:3.1",
    )

def javax_inject_javax_inject():
    native.maven_jar(
        name = "javax_inject_javax_inject",
        artifact = "javax.inject:javax.inject:1",
    )

def org_ow2_asm_asm():
    native.maven_jar(
        name = "org_ow2_asm_asm",
        artifact = "org.ow2.asm:asm:4.2",
    )

def junit_junit():
    native.maven_jar(
        name = "junit_junit",
        artifact = "junit:junit:4.12",
    )

def com_google_truth_truth():
    native.maven_jar(
        name = "com_google_truth_truth",
        artifact = "com.google.truth:truth:0.42",
    )

def com_google_truth_extensions_truth_java8_extension():
    native.maven_jar(
        name = "com_google_truth_extensions_truth_java8_extension",
        artifact = "com.google.truth.extensions:truth-java8-extension:0.42",
    )

def com_google_api_api_compiler_testing():
    native.maven_jar(
        name = "com_google_api_api_compiler_testing",
        artifact = "com.google.api:api-compiler:jar:testing:0.0.8",
    )

def pl_pragmatists_JUnitParams():
    native.maven_jar(
        name = "pl_pragmatists_JUnitParams",
        artifact = "pl.pragmatists:JUnitParams:1.1.1",
    )

def org_mockito_mockito_core():
    native.maven_jar(
        name = "org_mockito_mockito_core",
        artifact = "org.mockito:mockito-core:2.21.0",
    )

def org_hamcrest_hamcrest_core():
    native.maven_jar(
        name = "org_hamcrest_hamcrest_core",
        artifact = "org.hamcrest:hamcrest-core:1.3",
    )

def net_bytebuddy_byte_buddy():
    native.maven_jar(
        name = "net_bytebuddy_byte_buddy",
        artifact = "net.bytebuddy:byte-buddy:1.8.15",
    )

def org_objenesis_objenesis():
    native.maven_jar(
        name = "org_objenesis_objenesis",
        artifact = "org.objenesis:objenesis:2.6",
    )

def google_java_format_all_deps():
    http_jar(
        name = "google_java_format_all_deps",
        url = "https://github.com/google/google-java-format/releases/download/google-java-format-1.6/google-java-format-1.6-all-deps.jar",
    )
