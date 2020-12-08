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

def php_proto_library(name, deps, plugin_args = [], **kwargs):
    srcjar_target_name = name
    proto_custom_library(
        name = srcjar_target_name,
        deps = deps,
        output_type = "php",
        output_suffix = ".srcjar",
        plugin_args = plugin_args,
        extra_args = [
            "--include_source_info",
        ],
        **kwargs
    )

def php_grpc_library(name, srcs, deps, plugin_args = [], **kwargs):
    srcjar_target_name = name

    # `deps` is not used now but may be used if php_grpc_library ever tries to "compile" its output
    proto_custom_library(
        name = srcjar_target_name,
        deps = srcs,
        plugin = Label("@com_github_grpc_grpc//src/compiler:grpc_php_plugin"),
        plugin_args = ["class_suffix=GrpcClient"] + plugin_args,
        output_type = "grpc",
        output_suffix = ".srcjar",
        extra_args = [
            "--include_source_info",
        ],
        **kwargs
    )

def _php_gapic_postprocessed_srcjar_impl(ctx):
    gapic_srcjar = ctx.file.gapic_srcjar
    code_fixer = ctx.file.code_fixer
    code_sniffer = ctx.file.code_sniffer
    php = ctx.executable.php

    output_main = ctx.outputs.main
    output_test = ctx.outputs.test

    output_dir_name = ctx.label.name
    output_dir_path = "%s/%s" % (output_main.dirname, output_dir_name)

    script = """
    unzip -q {gapic_srcjar} -d {output_dir_path}
    {php} {code_fixer} fix --using-cache=false \
        --rules=@Symfony,-phpdoc_annotation_without_dot {output_dir_path} || exit 1
    {php} {code_fixer} fix --using-cache=false \
        --rules='{{"phpdoc_no_alias_tag":{{"replacements":{{"var":"type"}}}}}}' {output_dir_path}  || exit 1
    {php} {code_sniffer} --standard=PSR2 {output_dir_path}  || exit 1
    pushd {output_dir_path}
    zip -q -r {output_dir_name}.srcjar src
    zip -q -r {output_dir_name}-test.srcjar tests
    popd
    mv {output_dir_path}/{output_dir_name}-test.srcjar {output_test}
    mv {output_dir_path}/{output_dir_name}.srcjar {output_main}
    rm -rf {output_dir_path}
    """.format(
        php = php.path,
        gapic_srcjar = gapic_srcjar.path,
        output_dir_path = output_dir_path,
        output_dir_name = output_dir_name,
        code_fixer = code_fixer.path,
        code_sniffer = code_sniffer.path,
        output_main = output_main.path,
        output_test = output_test.path,
    )

    ctx.actions.run_shell(
        inputs = [gapic_srcjar, code_fixer, code_sniffer],
        tools = [php],
        command = script,
        outputs = [output_main, output_test],
    )

_php_gapic_postprocessed_srcjar = rule(
    _php_gapic_postprocessed_srcjar_impl,
    attrs = {
        "gapic_srcjar": attr.label(mandatory = True, allow_single_file = True),
        "code_fixer": attr.label(
            default = Label("@php_cs_fixer//file"),
            allow_single_file = True,
            cfg = "host",
        ),
        "code_sniffer": attr.label(
            default = Label("@php_code_sniffer//file"),
            allow_single_file = True,
            cfg = "host",
        ),
        "php": attr.label(
            default = Label("@php//:bin/php"),
            allow_single_file = True,
            executable = True,
            cfg = "host",
        ),
    },
    outputs = {
        "main": "%{name}.srcjar",
        "test": "%{name}-test.srcjar",
    },
)

def php_gapic_srcjar(name, src, gapic_yaml, service_yaml, package, grpc_service_config = None, transport = None, **kwargs):
    raw_srcjar_name = "%s_raw" % name

    gapic_srcjar(
        name = raw_srcjar_name,
        src = src,
        gapic_yaml = gapic_yaml,
        service_yaml = service_yaml,
        artifact_type = "GAPIC_CODE",
        language = "php",
        package = package,
        grpc_service_config = grpc_service_config,
        transport = transport,
        **kwargs
    )

    _php_gapic_postprocessed_srcjar(
        name = name,
        gapic_srcjar = ":%s" % raw_srcjar_name,
        **kwargs
    )

def php_gapic_library(name, src, gapic_yaml, service_yaml, package = None, deps = [], grpc_service_config = None, transport = None, **kwargs):
    srcjar_name = "%s_srcjar" % name

    php_gapic_srcjar(
        name = srcjar_name,
        src = src,
        gapic_yaml = gapic_yaml,
        service_yaml = service_yaml,
        package = package,
        grpc_service_config = grpc_service_config,
        transport = transport,
        **kwargs
    )

    # Change with php_library if PHP gets full support in Bazel
    native.alias(
        name = name,
        actual = ":%s" % srcjar_name,
    )
