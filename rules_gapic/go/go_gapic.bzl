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

load("//rules_gapic:gapic.bzl", "gapic_srcjar", "unzipped_srcjar")
load("@io_bazel_rules_go//go/private:rules/rule.bzl", "go_rule")
load("@io_bazel_rules_go//go:def.bzl", "GoSource", "go_context")
load("@io_bazel_rules_go//go:def.bzl", "go_library")


def _go_gapic_postprocessed_srcjar_impl(ctx):
    go_ctx = go_context(ctx)

    gapic_srcjar = ctx.file.gapic_srcjar
    output_main = ctx.outputs.main
    output_test = ctx.outputs.test
    output_smoke_test = ctx.outputs.smoke_test

    output_dir_name = ctx.label.name
    output_dir_path = "%s/%s" % (output_main.dirname, output_dir_name)

    formatter = get_gofmt(go_ctx)

    # Note the script is more complicated than it intuitively should be because of limitations
    # inherent to bazel execution environment: no absolute paths allowed, the generated artifacts
    # must ensure uniqueness within a build.
    script = """
    unzip -q {gapic_srcjar} -d {output_dir_path}
    {formatter} -w -l {output_dir_path}
    pushd .
    cd {output_dir_path}
    zip -q -r {output_dir_name}-smoke-test.srcjar . -i ./*_smoke_test.go
    find . -name "*_smoke_test.go" -delete
    zip -q -r {output_dir_name}-test.srcjar . -i ./*_test.go
    find . -name "*_test.go" -delete
    zip -q -r {output_dir_name}.srcjar . -i ./*.go
    popd
    mv {output_dir_path}/{output_dir_name}-smoke-test.srcjar {output_smoke_test}
    mv {output_dir_path}/{output_dir_name}-test.srcjar {output_test}
    mv {output_dir_path}/{output_dir_name}.srcjar {output_main}
    """.format(
        gapic_srcjar = gapic_srcjar.path,
        output_dir_name = output_dir_name,
        output_dir_path = output_dir_path,
        formatter = formatter.path,
        output_main = output_main.path,
        output_test = output_test.path,
        output_smoke_test = output_smoke_test.path,
    )

    ctx.actions.run_shell(
        inputs = [gapic_srcjar],
        tools = [formatter],
        command = script,
        outputs = [output_main, output_test, output_smoke_test],
    )

_go_gapic_postprocessed_srcjar = go_rule(
    _go_gapic_postprocessed_srcjar_impl,
    attrs = {
        "gapic_srcjar": attr.label(mandatory = True, allow_single_file = True),
    },
    outputs = {
        "main": "%{name}.srcjar",
        "test": "%{name}-test.srcjar",
        "smoke_test": "%{name}-smoke-test.srcjar",
    },
)

def go_gapic_srcjar(name, src, gapic_yaml, service_yaml, visibility = None):
    raw_srcjar_name = "%s_raw" % name

    gapic_srcjar(
        name = raw_srcjar_name,
        src = src,
        gapic_yaml = gapic_yaml,
        service_yaml = service_yaml,
        visibility = visibility,
        artifact_type = "GAPIC_CODE",
        language = "go",
    )

    _go_gapic_postprocessed_srcjar(
        name = name,
        gapic_srcjar = ":%s" % raw_srcjar_name,
        visibility = visibility,
    )

def go_gapic_library(
        name,
        src,
        gapic_yaml,
        service_yaml,
        importpath,
        deps = [],
        visibility = None):
    srcjar_name = "%s_srcjar" % name
    # TODO: make smoke_tests compilable (remove /internal/ dependency from generated code)
    # Smoke_test_library_name = "%s_smoke_test" % name

    go_gapic_srcjar(
        name = srcjar_name,
        src = src,
        gapic_yaml = gapic_yaml,
        service_yaml = service_yaml,
        visibility = visibility,
    )

    actual_deps = deps + [
        "@com_github_googleapis_gax_go//:go_default_library",
        "@org_golang_google_api//option:go_default_library",
        "@org_golang_google_api//iterator:go_default_library",
        "@org_golang_google_api//transport:go_default_library",
        "@org_golang_google_grpc//:go_default_library",
        "@org_golang_google_grpc//codes:go_default_library",
        "@org_golang_google_grpc//metadata:go_default_library",
        "@com_github_golang_protobuf//proto:go_default_library",
        "@com_github_golang_protobuf//ptypes:go_default_library",
        "@com_github_golang_protobuf//ptypes/empty:go_default_library",
        "@com_github_golang_protobuf//ptypes/timestamp:go_default_library",
        "@org_golang_google_genproto//protobuf/field_mask:go_default_library",
        "@com_google_googleapis//google/rpc:status_go_proto",
        "@org_golang_google_grpc//status:go_default_library",
    ]

    main_file = ":%s.srcjar" % srcjar_name
    main_dir = "%s_main" % srcjar_name

    unzipped_srcjar(
        name = main_dir,
        srcjar = main_file,
        extension = ".go",
    )

    go_library(
        name = name,
        srcs = [":%s" % main_dir],
        deps = actual_deps,
        importpath = importpath,
    )

    test_file = ":%s-test.srcjar" % srcjar_name
    test_dir = "%s_test" % srcjar_name

    unzipped_srcjar(
        name = test_dir,
        srcjar = test_file,
        extension = ".go",
    )

#    smoke_test_file = ":%s-smoke-test.srcjar" % srcjar_name
#    smoke_test_dir = "%s_smoke_test" % srcjar_name

#    unzipped_srcjar(
#        name = smoke_test_dir,
#        srcjar = smoke_test_file,
#        extension = ".go",
#    )

def get_gofmt(go_ctx):
    for tool in go_ctx.sdk.tools:
        if tool.basename == "gofmt":
            return tool
    return None