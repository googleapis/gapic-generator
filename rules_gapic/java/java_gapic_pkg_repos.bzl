# Copyright 2018 Google LLC
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

# The versions which are most likely to be updated frequently
_PROTOBUF_VERSION = "3.6.1"
_GAX_VERSION = "1.32.0"
_GAX_BETA_VERSION = "0.49.0"
_GRPC_VERSION = "1.13.1"
_COMMON_PROTOS_VERSION = "1.12.0"
_AUTH_VERSION = "0.11.0"
_HTTP_CLIENT_VERSION = "1.24.1"

# Generated libraries direct dependencies
def java_gapic_direct_repositories(
        omit_com_google_api_gax = False,
        omit_com_google_api_gax_grpc = False,
        omit_com_google_api_gax_httpjson = False,
        omit_com_google_api_gax_testlib = False,
        omit_com_google_api_gax_grpc_testlib = False,
        omit_com_google_api_gax_httpjson_testlib = False,
        omit_junit_junit = False):
    if not omit_com_google_api_gax:
        com_google_api_gax()
    if not omit_com_google_api_gax_grpc:
        com_google_api_gax_grpc()
    if not omit_com_google_api_gax_httpjson:
        com_google_api_gax_httpjson()
    if not omit_com_google_api_gax_testlib:
        com_google_api_gax_testlib()
    if not omit_com_google_api_gax_grpc_testlib:
        com_google_api_gax_grpc_testlib()
    if not omit_com_google_api_gax_httpjson_testlib:
        com_google_api_gax_httpjson_testlib()
    if not omit_junit_junit:
        junit_junit()

# Java Gax Dependencies
# TODO: "bazelify" gax-java instead, so these are inherited automatically
def java_gapic_gax_repositories(
        omit_com_google_protobuf_protobuf_java = False,
        omit_io_grpc_grpc_core = False,
        omit_io_grpc_grpc_stub = False,
        omit_io_grpc_grpc_auth = False,
        omit_io_grpc_grpc_protobuf = False,
        omit_io_grpc_grpc_netty_shaded = False,
        omit_com_google_api_grpc_proto_google_common_protos = False,
        omit_com_google_api_grpc_grpc_google_common_protos = False,
        omit_com_google_auth_google_auth_library_oauth2_http = False,
        omit_com_google_auth_google_auth_library_credentials = False,
        omit_io_opencensus_opencensus_api = False,
        omit_io_opencensus_opencensus_contrib_grpc_metrics = False,
        omit_com_google_code_gson_gson = False,
        omit_com_google_guava_guava = False,
        omit_com_google_code_findbugs_jsr305 = False,
        omit_com_google_api_api_common = False,
        omit_org_threeten_threetenbp = False,
        omit_com_google_api_grpc_grpc_google_iam_v1 = False,
        omit_com_google_api_grpc_proto_google_iam_v1 = False,
        omit_com_google_http_client_google_http_client = False,
        omit_com_google_http_client_google_http_client_jackson2 = False,
        omit_com_fasterxml_jackson_core_jackson_core = False):
    if not omit_com_google_protobuf_protobuf_java:
        com_google_protobuf_protobuf_java()
    if not omit_io_grpc_grpc_core:
        io_grpc_grpc_core()
    if not omit_io_grpc_grpc_stub:
        io_grpc_grpc_stub()
    if not omit_io_grpc_grpc_auth:
        io_grpc_grpc_auth()
    if not omit_io_grpc_grpc_protobuf:
        io_grpc_grpc_protobuf()
    if not omit_io_grpc_grpc_netty_shaded:
        io_grpc_grpc_netty_shaded()
    if not omit_com_google_api_grpc_proto_google_common_protos:
        com_google_api_grpc_proto_google_common_protos()
    if not omit_com_google_api_grpc_grpc_google_common_protos:
        com_google_api_grpc_grpc_google_common_protos()
    if not omit_com_google_auth_google_auth_library_oauth2_http:
        com_google_auth_google_auth_library_oauth2_http()
    if not omit_com_google_auth_google_auth_library_credentials:
        com_google_auth_google_auth_library_credentials()
    if not omit_io_opencensus_opencensus_api:
        io_opencensus_opencensus_api()
    if not omit_io_opencensus_opencensus_contrib_grpc_metrics:
        io_opencensus_opencensus_contrib_grpc_metrics()
    if not omit_com_google_code_gson_gson:
        com_google_code_gson_gson()
    if not omit_com_google_guava_guava:
        com_google_guava_guava()
    if not omit_com_google_code_findbugs_jsr305:
        com_google_code_findbugs_jsr305()
    if not omit_com_google_api_api_common:
        com_google_api_api_common()
    if not omit_org_threeten_threetenbp:
        org_threeten_threetenbp()
    if not omit_com_google_api_grpc_grpc_google_iam_v1:
        com_google_api_grpc_grpc_google_iam_v1()
    if not omit_com_google_api_grpc_proto_google_iam_v1:
        com_google_api_grpc_proto_google_iam_v1()
    if not omit_com_google_http_client_google_http_client:
        com_google_http_client_google_http_client()
    if not omit_com_google_http_client_google_http_client_jackson2:
        com_google_http_client_google_http_client_jackson2()
    if not omit_com_fasterxml_jackson_core_jackson_core:
        com_fasterxml_jackson_core_jackson_core()

def com_google_api_gax():
    native.maven_jar(
        name = "com_google_api_gax",
        artifact = "com.google.api:gax:%s" % _GAX_VERSION,
    )

def com_google_api_gax_grpc():
    native.maven_jar(
        name = "com_google_api_gax_grpc",
        artifact = "com.google.api:gax-grpc:%s" % _GAX_VERSION,
    )

def com_google_api_gax_httpjson():
    native.maven_jar(
        name = "com_google_api_gax_httpjson",
        artifact = "com.google.api:gax-httpjson:%s" % _GAX_BETA_VERSION,
    )

def com_google_api_gax_testlib():
    native.maven_jar(
        name = "com_google_api_gax_testlib",
        artifact = "com.google.api:gax:jar:testlib:%s" % _GAX_VERSION,
    )

def com_google_api_gax_grpc_testlib():
    native.maven_jar(
        name = "com_google_api_gax_grpc_testlib",
        artifact = "com.google.api:gax-grpc:jar:testlib:%s" % _GAX_VERSION,
    )

def com_google_api_gax_httpjson_testlib():
    native.maven_jar(
        name = "com_google_api_gax_httpjson_testlib",
        artifact = "com.google.api:gax-httpjson:jar:testlib:%s" % _GAX_BETA_VERSION,
    )

def junit_junit():
    native.maven_jar(
        name = "junit_junit",
        artifact = "junit:junit:4.12",
    )

def com_google_protobuf_protobuf_java():
    native.maven_jar(
        name = "com_google_protobuf_protobuf_java",
        artifact = "com.google.protobuf:protobuf-java:" + _PROTOBUF_VERSION,
    )

def io_grpc_grpc_core():
    native.maven_jar(
        name = "io_grpc_grpc_core",
        artifact = "io.grpc:grpc-core:%s" % _GRPC_VERSION,
    )

def io_grpc_grpc_stub():
    native.maven_jar(
        name = "io_grpc_grpc_stub",
        artifact = "io.grpc:grpc-stub:%s" % _GRPC_VERSION,
    )

def io_grpc_grpc_auth():
    native.maven_jar(
        name = "io_grpc_grpc_auth",
        artifact = "io.grpc:grpc-auth:%s" % _GRPC_VERSION,
    )

def io_grpc_grpc_protobuf():
    native.maven_jar(
        name = "io_grpc_grpc_protobuf",
        artifact = "io.grpc:grpc-protobuf:%s" % _GRPC_VERSION,
    )

def io_grpc_grpc_netty_shaded():
    native.maven_jar(
        name = "io_grpc_grpc_netty_shaded",
        artifact = "io.grpc:grpc-netty-shaded:%s" % _GRPC_VERSION,
    )

def com_google_api_grpc_proto_google_common_protos():
    native.maven_jar(
        name = "com_google_api_grpc_proto_google_common_protos",
        artifact = "com.google.api.grpc:proto-google-common-protos:%s" % _COMMON_PROTOS_VERSION,
    )

def com_google_api_grpc_grpc_google_common_protos():
    native.maven_jar(
        name = "com_google_api_grpc_grpc_google_common_protos",
        artifact = "com.google.api.grpc:grpc-google-common-protos:%s" % _COMMON_PROTOS_VERSION,
    )

def com_google_auth_google_auth_library_oauth2_http():
    native.maven_jar(
        name = "com_google_auth_google_auth_library_oauth2_http",
        artifact = "com.google.auth:google-auth-library-oauth2-http:%s" % _AUTH_VERSION,
    )

def com_google_auth_google_auth_library_credentials():
    native.maven_jar(
        name = "com_google_auth_google_auth_library_credentials",
        artifact = "com.google.auth:google-auth-library-credentials:%s" % _AUTH_VERSION,
    )

def io_opencensus_opencensus_api():
    native.maven_jar(
        name = "io_opencensus_opencensus_api",
        artifact = "io.opencensus:opencensus-api:0.15.0",
    )

def io_opencensus_opencensus_contrib_grpc_metrics():
    native.maven_jar(
        name = "io_opencensus_opencensus_contrib_grpc_metrics",
        artifact = "io.opencensus:opencensus-contrib-grpc-metrics:0.12.3",
    )

def com_google_code_gson_gson():
    native.maven_jar(
        name = "com_google_code_gson_gson",
        artifact = "com.google.code.gson:gson:2.7",
    )

def com_google_guava_guava():
    native.maven_jar(
        name = "com_google_guava_guava",
        artifact = "com.google.guava:guava:20.0",
    )

def com_google_code_findbugs_jsr305():
    native.maven_jar(
        name = "com_google_code_findbugs_jsr305",
        artifact = "com.google.code.findbugs:jsr305:3.0.2",
    )

def com_google_api_api_common():
    native.maven_jar(
        name = "com_google_api_api_common",
        artifact = "com.google.api:api-common:1.7.0",
    )

def org_threeten_threetenbp():
    native.maven_jar(
        name = "org_threeten_threetenbp",
        artifact = "org.threeten:threetenbp:1.3.3",
    )

def com_google_api_grpc_grpc_google_iam_v1():
    native.maven_jar(
        name = "com_google_api_grpc_grpc_google_iam_v1",
        artifact = "com.google.api.grpc:grpc-google-iam-v1:0.12.0"
    )

def com_google_api_grpc_proto_google_iam_v1():
    native.maven_jar(
        name = "com_google_api_grpc_proto_google_iam_v1",
        artifact = "com.google.api.grpc:proto-google-iam-v1:0.12.0"
    )

def com_google_http_client_google_http_client():
    native.maven_jar(
        name = "com_google_http_client_google_http_client",
        artifact = "com.google.http-client:google-http-client:%s" % _HTTP_CLIENT_VERSION,
    )

def com_google_http_client_google_http_client_jackson2():
    native.maven_jar(
        name = "com_google_http_client_google_http_client_jackson2",
        artifact = "com.google.http-client:google-http-client-jackson2:%s" % _HTTP_CLIENT_VERSION,
    )

def com_fasterxml_jackson_core_jackson_core():
    native.maven_jar(
        name = "com_fasterxml_jackson_core_jackson_core",
        artifact = "com.fasterxml.jackson.core:jackson-core:2.9.2",
    )
