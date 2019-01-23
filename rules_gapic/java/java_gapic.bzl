load("//rules_gapic:gapic.bzl", "gapic_srcjar", "proto_custom_library")

def _java_gapic_srcjar_impl(ctx):
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
    echo '--replace' > {output_dir_path}/{output_dir_name}.txt
    find {output_dir_path} -type f >> {output_dir_path}/{output_dir_name}.txt
    {formatter} @{output_dir_path}/{output_dir_name}.txt
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

java_gapic_srcjar = rule(
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
    implementation = _java_gapic_srcjar_impl,
)

def java_resource_name_proto_library(name, deps, gapic_yaml, visibility = None):
    srcjar_target_name = "%s_srcjar" % name
    srcjar_output_suffix = ".srcjar"

    proto_custom_library(
        name = srcjar_target_name,
        deps = deps,
        plugin = Label("@com_google_protoc_java_resource_names_plugin//:gapic_plugin"),
        plugin_args = [gapic_yaml],
        output_type = "resourcename",
        output_suffix = srcjar_output_suffix,
    )

    native.java_library(
        name = name,
        srcs = [":%s%s" % (srcjar_target_name, srcjar_output_suffix)],
        deps = [
            "@com_google_api_api_common//jar",
            "@com_google_guava_guava//jar",
        ],
        visibility = visibility,
    )

def java_gapic_library_raw(
        name,
        src,
        gapic_yaml,
        service_yaml,
        deps,
        test_deps,
        artifact_type,
        visibility):
    srcjar_target_name = "%s_srcjar" % name
    srcjar_raw_target_name = "%s_srcjar_raw" % srcjar_target_name
    test_library_target_name = "%s_test" % name

    gapic_srcjar(
        name = srcjar_raw_target_name,
        src = src,
        gapic_yaml = gapic_yaml,
        service_yaml = service_yaml,
        visibility = visibility,
        artifact_type = artifact_type,
        language = "java",
    )

    java_gapic_srcjar(
        name = srcjar_target_name,
        gapic_srcjar = srcjar_raw_target_name,
        visibility = visibility,
    )

    native.java_library(
        name = name,
        srcs = [":%s.srcjar" % srcjar_target_name],
        deps = deps,
        visibility = visibility,
    )

    native.java_library(
        name = test_library_target_name,
        srcs = [":%s-test.srcjar" % srcjar_target_name],
        deps = [":%s" % name] + deps + test_deps,
        visibility = visibility,
    )

def java_gapic_library(
        name,
        src,
        gapic_yaml,
        service_yaml,
        deps = [],
        test_deps = [],
        visibility = None):
    java_gapic_library_raw(
        name = name,
        src = src,
        deps = deps + [
            "@com_google_protobuf_protobuf_java//jar",
            "@com_google_api_api_common//jar",
            "@com_google_api_gax//jar",
            "@com_google_api_gax_grpc//jar",
            "@com_google_guava_guava//jar",
            "@io_grpc_grpc_core//jar",
            "@io_grpc_grpc_protobuf//jar",
            "@com_google_code_findbugs_jsr305//jar",
            "@org_threeten_threetenbp//jar",
            "@io_opencensus_opencensus_api//jar",
            "@com_google_auth_google_auth_library_credentials//jar",
            "@com_google_auth_google_auth_library_oauth2_http//jar",
            "@com_google_http_client_google_http_client//jar",
        ],
        test_deps = test_deps + [
            "@com_google_api_gax_grpc_testlib//jar",
            "@com_google_api_gax_testlib//jar",
            "@com_google_code_gson_gson//jar",
            "@io_grpc_grpc_auth//jar",
            "@io_grpc_grpc_netty_shaded//jar",
            "@io_grpc_grpc_stub//jar",
            "@io_opencensus_opencensus_contrib_grpc_metrics//jar",
            "@junit_junit//jar",
        ],
        gapic_yaml = gapic_yaml,
        service_yaml = service_yaml,
        artifact_type = "GAPIC_CODE",
        visibility = visibility,
    )

def java_discogapic_library(
        name,
        src,
        gapic_yaml,
        deps = [],
        test_deps = [],
        visibility = None):
    java_gapic_library_raw(
        name = name,
        src = src,
        deps = deps + [
            "@com_google_protobuf_protobuf_java//jar",
            "@com_google_api_api_common//jar",
            "@com_google_api_gax//jar",
            "@com_google_api_gax_httpjson//jar",
            "@com_google_guava_guava//jar",
            "@com_google_code_findbugs_jsr305//jar",
            "@org_threeten_threetenbp//jar",
            "@io_opencensus_opencensus_api//jar",
            "@com_google_auth_google_auth_library_credentials//jar",
            "@com_google_auth_google_auth_library_oauth2_http//jar",
            "@com_google_http_client_google_http_client//jar",
        ],
        test_deps = test_deps + [
            "@com_google_api_gax_httpjson_testlib//jar",
            "@com_google_http_client_google_http_client_jackson2//jar",
            "@com_fasterxml_jackson_core_jackson_core//jar",
            "@com_google_api_gax_testlib//jar",
            "@com_google_code_gson_gson//jar",
            "@junit_junit//jar",
        ],
        gapic_yaml = gapic_yaml,
        service_yaml = None,
        artifact_type = "DISCOGAPIC_CODE",
        visibility = visibility,
    )
