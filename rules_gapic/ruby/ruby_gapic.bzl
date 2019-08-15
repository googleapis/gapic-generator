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

load("//rules_gapic:gapic.bzl", "gapic_srcjar", "proto_custom_library")

def ruby_proto_library(name, deps, **kwargs):
    srcjar_target_name = name
    proto_custom_library(
        name = srcjar_target_name,
        deps = deps,
        output_type = "ruby",
        output_suffix = ".srcjar",
        extra_args = [
            "--include_source_info",
        ],
        **kwargs
    )

def ruby_grpc_library(name, srcs, deps, **kwargs):
    srcjar_target_name = name
    # `deps` is not used now but may be used if ruby_grpc_library ever tries to "compile" its output
    proto_custom_library(
        name = srcjar_target_name,
        deps = srcs,
        plugin = Label("@com_github_grpc_grpc//:grpc_ruby_plugin"),
        output_type = "grpc",
        output_suffix = ".srcjar",
        extra_args = [
            "--include_source_info",
        ],
        **kwargs
    )

def _ruby_gapic_postprocessed_srcjar_impl(ctx):
    gapic_srcjar = ctx.file.gapic_srcjar

    output_main = ctx.outputs.main
    output_test = ctx.outputs.test
    output_smoke_test = ctx.outputs.smoke_test
    output_pkg = ctx.outputs.pkg

    output_dir_name = ctx.label.name
    output_dir_path = "%s/%s" % (output_main.dirname, output_dir_name)

    script = """
    unzip -q {gapic_srcjar} -d {output_dir_path}
    pushd {output_dir_path}
    zip -q -r {output_dir_name}.srcjar lib
    zip -q -r {output_dir_name}-test.srcjar test
    if [ -d "acceptance" ]; then
        zip -q -r {output_dir_name}-smoke-test.srcjar acceptance
    else
        touch empty_file
        zip -q -r {output_dir_name}-smoke-test.srcjar empty_file
        zip -d {output_dir_name}-smoke-test.srcjar empty_file
    fi
    zip -q -r {output_dir_name}-pkg.srcjar . -i \
        './*.yaml' \
        './*.yml' \
        './*.md' \
        './*file' \
        './*.gemspec' \
        './LICENSE' \
        './.gitignore' \
        './.yardopts'
    popd
    mv {output_dir_path}/{output_dir_name}.srcjar {output_main}
    mv {output_dir_path}/{output_dir_name}-test.srcjar {output_test}
    mv {output_dir_path}/{output_dir_name}-smoke-test.srcjar {output_smoke_test}
    mv {output_dir_path}/{output_dir_name}-pkg.srcjar {output_pkg}
    rm -rf {output_dir_path}
    """.format(
        gapic_srcjar = gapic_srcjar.path,
        output_dir_path = output_dir_path,
        output_dir_name = output_dir_name,
        output_main = output_main.path,
        output_test = output_test.path,
        output_smoke_test = output_smoke_test.path,
        output_pkg = output_pkg.path,
    )

    ctx.actions.run_shell(
        inputs = [gapic_srcjar],
        tools = [],
        command = script,
        outputs = [output_main, output_test, output_pkg, output_smoke_test],
    )

_ruby_gapic_postprocessed_srcjar = rule(
    _ruby_gapic_postprocessed_srcjar_impl,
    attrs = {
        "gapic_srcjar": attr.label(mandatory = True, allow_single_file = True),
    },
    outputs = {
        "main": "%{name}.srcjar",
        "test": "%{name}-test.srcjar",
        "smoke_test": "%{name}-smoke-test.srcjar",
        "pkg": "%{name}-pkg.srcjar"
    },
)

def ruby_gapic_srcjar(name, src, gapic_yaml, service_yaml, **kwargs):
    raw_srcjar_name = "%s_raw" % name

    gapic_srcjar(
        name = raw_srcjar_name,
        src = src,
        gapic_yaml = gapic_yaml,
        service_yaml = service_yaml,
        artifact_type = "GAPIC_CODE",
        language = "ruby",
        **kwargs
    )

    _ruby_gapic_postprocessed_srcjar(
        name = name,
        gapic_srcjar = ":%s" % raw_srcjar_name,
        **kwargs
    )

def ruby_gapic_library(name, src, gapic_yaml, service_yaml, deps = [], **kwargs):
    srcjar_name = "%s_srcjar" % name

    ruby_gapic_srcjar(
        name = srcjar_name,
        src = src,
        gapic_yaml = gapic_yaml,
        service_yaml = service_yaml,
        **kwargs
    )

    # Change with ruby_library if Ruby gets full support in Bazel
    native.alias(
        name = name,
        actual = ":%s" % srcjar_name,
    )
