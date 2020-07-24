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

load("//rules_gapic:gapic.bzl", "gapic_srcjar", "proto_custom_library")
load("@com_google_api_gax_java_properties//:dependencies.properties.bzl", "PROPERTIES")

def _java_gapic_postprocessed_srcjar_impl(ctx):
    gapic_srcjar = ctx.file.gapic_srcjar
    output_main = ctx.outputs.main
    output_test = ctx.outputs.test
    formatter = ctx.executable.formatter

    output_dir_name = ctx.label.name
    output_dir_path = "%s/%s" % (output_main.dirname, output_dir_name)

    # Note the script is more complicated than it intuitively should be because of limitations
    # inherent to bazel execution environment: no absolute paths allowed, the generated artifacts
    # must ensure uniqueness within a build.
    script = """
    unzip -q {gapic_srcjar} -d {output_dir_path}
    # This may fail if there are spaces and/or too many files (exceed max length of command length).
    {formatter} --replace $(find {output_dir_path} -type f -printf "%p ")
    pushd .
    cd {output_dir_path}
    cd src/main/java && zip -q -r ../../../{output_dir_name}.srcjar ./* && cd -
    cd src/test/java && zip -q -r ../../../{output_dir_name}-tests.srcjar ./* && cd -
    popd
    mv {output_dir_path}/{output_dir_name}.srcjar {output_main}
    mv {output_dir_path}/{output_dir_name}-tests.srcjar {output_test}
    """.format(
        gapic_srcjar = gapic_srcjar.path,
        output_dir_name = output_dir_name,
        output_dir_path = output_dir_path,
        formatter = formatter.path,
        output_main = output_main.path,
        output_test = output_test.path,
    )

    ctx.actions.run_shell(
        inputs = [gapic_srcjar],
        tools = [formatter],
        command = script,
        outputs = [output_main, output_test],
    )

_java_gapic_postprocessed_srcjar = rule(
    attrs = {
        "gapic_srcjar": attr.label(mandatory = True, allow_single_file = True),
        "formatter": attr.label(
            default = Label("//:google_java_format_binary"),
            executable = True,
            cfg = "host",
        ),
    },
    outputs = {
        "main": "%{name}.srcjar",
        "test": "%{name}-test.srcjar",
    },
    implementation = _java_gapic_postprocessed_srcjar_impl,
)

def java_gapic_srcjar(
        name,
        src,
        gapic_yaml,
        artifact_type,
        package = None,
        service_yaml = None,
        grpc_service_config = None,
        transport_protocol = None,
        **kwargs):
    raw_srcjar_name = "%s_raw" % name

    gapic_srcjar(
        name = raw_srcjar_name,
        src = src,
        gapic_yaml = gapic_yaml,
        service_yaml = service_yaml,
        artifact_type = artifact_type,
        language = "java",
        package = package,
        grpc_service_config = grpc_service_config,
        transport_protocol = transport_protocol,
        **kwargs
    )

    _java_gapic_postprocessed_srcjar(
        name = name,
        gapic_srcjar = ":%s" % raw_srcjar_name,
        **kwargs
    )

def java_resource_name_proto_library(name, deps, gapic_yaml, **kwargs):
    srcjar_target_name = "%s_srcjar" % name
    srcjar_output_suffix = ".srcjar"

    proto_custom_library(
        name = srcjar_target_name,
        deps = deps,
        plugin = Label("@com_google_protoc_java_resource_names_plugin//:gapic_plugin"),
        plugin_file_args = {gapic_yaml: ""},
        output_type = "resourcename",
        output_suffix = srcjar_output_suffix,
        **kwargs
    )

    native.java_library(
        name = name,
        srcs = [":%s%s" % (srcjar_target_name, srcjar_output_suffix)],
        deps = [
            "@com_google_api_api_common//jar",
            "@com_google_guava_guava//jar",
            "@javax_annotation_javax_annotation_api//jar",
        ],
        **kwargs
    )

def java_gapic_library(
        name,
        src,
        gapic_yaml,
        service_yaml,
        package = None,
        gen_resource_name = True,
        grpc_service_config = None,
        transport_protocol = None,
        deps = [],
        test_deps = [],
        **kwargs):
    srcjar_name = "%s_srcjar" % name
    java_gapic_srcjar(
        name = srcjar_name,
        src = src,
        gapic_yaml = gapic_yaml,
        service_yaml = service_yaml,
        artifact_type = "GAPIC_CODE",
        package = package,
        grpc_service_config = grpc_service_config,
        transport_protocol = transport_protocol,
        **kwargs
    )

    resource_name_dep = []
    if gen_resource_name:
        resource_name_name = "%s_resource_name" % name
        resource_name_dep = ["%s" % resource_name_name]
        java_resource_name_proto_library(
            name = resource_name_name,
            gapic_yaml = gapic_yaml,
            deps = [src],
        )

    actual_deps = deps + resource_name_dep + [
        "@com_google_googleapis//google/longrunning:longrunning_java_proto",
        "@com_google_protobuf//:protobuf_java",
        "@com_google_api_api_common//jar",
        "@com_google_api_gax_java//gax:gax",
        "@com_google_api_gax_java//gax-grpc:gax_grpc",
        "@com_google_guava_guava//jar",
        "@io_grpc_grpc_java//core:core",
        "@io_grpc_grpc_java//protobuf:protobuf",
        "@com_google_code_findbugs_jsr305//jar",
        "@org_threeten_threetenbp//jar",
        "@io_opencensus_opencensus_api//jar",
        "@com_google_auth_google_auth_library_credentials//jar",
        "@com_google_auth_google_auth_library_oauth2_http//jar",
        "@com_google_http_client_google_http_client//jar",
        "@javax_annotation_javax_annotation_api//jar",
    ]

    native.java_library(
        name = name,
        srcs = [":%s.srcjar" % srcjar_name],
        deps = actual_deps,
        **kwargs
    )

    actual_test_deps = test_deps + [
        "@com_google_api_gax_java//gax-grpc:gax_grpc_testlib",
        "@com_google_api_gax_java//gax:gax_testlib",
        "@com_google_code_gson_gson//jar",
        "@io_grpc_grpc_java//auth:auth",
        "@io_grpc_grpc_netty_shaded//jar",
        "@io_grpc_grpc_java//stub:stub",
        "@io_opencensus_opencensus_contrib_grpc_metrics//jar",
        "@junit_junit//jar",
    ]

    native.java_library(
        name = "%s_test" % name,
        srcs = [":%s-test.srcjar" % srcjar_name],
        deps = [":%s" % name] + actual_deps + actual_test_deps,
        **kwargs
    )

def java_discogapic_library(
        name,
        src,
        gapic_yaml,
        deps = [],
        test_deps = [],
        **kwargs):
    srcjar_name = "%s_srcjar" % name

    java_gapic_srcjar(
        name = srcjar_name,
        src = src,
        gapic_yaml = gapic_yaml,
        artifact_type = "DISCOGAPIC_CODE",
        **kwargs
    )

    actual_deps = deps + [
        "@com_google_protobuf//:protobuf_java",
        "@com_google_api_api_common//jar",
        "@com_google_api_gax_java//gax:gax",
        "@com_google_api_gax_java//gax-httpjson:gax_httpjson",
        "@com_google_guava_guava//jar",
        "@com_google_code_findbugs_jsr305//jar",
        "@org_threeten_threetenbp//jar",
        "@io_opencensus_opencensus_api//jar",
        "@com_google_auth_google_auth_library_credentials//jar",
        "@com_google_auth_google_auth_library_oauth2_http//jar",
        "@com_google_http_client_google_http_client//jar",
        "@com_google_code_gson_gson//jar",
        "@javax_annotation_javax_annotation_api//jar",
    ]

    native.java_library(
        name = name,
        srcs = [":%s.srcjar" % srcjar_name],
        deps = actual_deps,
        **kwargs
    )

    actual_test_deps = test_deps + [
        "@com_google_api_gax_java//gax-httpjson:gax_httpjson_testlib",
        "@com_google_http_client_google_http_client_jackson2//jar",
        "@com_fasterxml_jackson_core_jackson_core//jar",
        "@com_google_api_gax_java//gax:gax_testlib",
        "@junit_junit//jar",
    ]

    native.java_library(
        name = "%s_test" % name,
        srcs = [":%s-test.srcjar" % srcjar_name],
        deps = [":%s" % name] + actual_deps + actual_test_deps,
        **kwargs
    )

def java_gapic_test(name, runtime_deps, test_classes, **kwargs):
    for test_class in test_classes:
        native.java_test(
            name = test_class,
            test_class = test_class,
            runtime_deps = runtime_deps,
            **kwargs
        )
    native.test_suite(
        name = name,
        tests = test_classes,
        **kwargs
    )
