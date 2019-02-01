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

def _gapic_srcjar_impl(ctx):
    output = ctx.outputs.output
    optional_arguments = []
    optional_action_inputs = []

    if ctx.file.service_yaml:
        optional_arguments.append("--service_yaml=%s" % ctx.file.service_yaml.path)
        optional_action_inputs.append(ctx.file.service_yaml)
    if ctx.attr.package_yaml2:
        optional_arguments.append("--package_yaml2=%s" % ctx.file.package_yaml2.path)
        optional_action_inputs.append(ctx.file.package_yaml2)

    if ctx.attr.artifact_type.find("DISCOGAPIC") >= 0:
        optional_arguments.append("--discovery_doc=%s" % ctx.file.src.path)
    else:
        if not ctx.attr.service_yaml:
            fail("Missing mandatory attribute `service_yaml`")
        optional_arguments.append("--descriptor_set=%s" % ctx.file.src.path)

    arguments = [
        ctx.attr.artifact_type,
        "--language=%s" % ctx.attr.language,
        "--gapic_yaml=%s" % ctx.file.gapic_yaml.path,
        "--output=%s" % output.path,
    ] + optional_arguments

    gapic_generator = ctx.executable.gapic_generator
    ctx.actions.run(
        inputs = [ctx.file.src, ctx.file.gapic_yaml] + optional_action_inputs,
        outputs = [output],
        arguments = arguments,
        progress_message = "%s: `%s %s`" % (ctx.label, gapic_generator.path, " ".join(arguments)),
        executable = gapic_generator,
    )

gapic_srcjar = rule(
    attrs = {
        # src is used instead of srcs, because of the limitation of gapic-generator
        # (more specifically the api-compiler, which is a dependency of gapic-generator), which
        # accepts only single descriptor (a fat one, with embedded imports)
        "src": attr.label(
            allow_single_file = True,
            mandatory = True,
        ),
        "gapic_yaml": attr.label(mandatory = True, allow_single_file = True),
        "artifact_type": attr.string(mandatory = False, default = "GAPIC_CODE"),
        "language": attr.string(mandatory = True),
        "service_yaml": attr.label(mandatory = False, allow_single_file = True),
        "package_yaml2": attr.label(mandatory = False),
        "gapic_generator": attr.label(
            default = Label("//:gapic_generator"),
            executable = True,
            cfg = "host",
        ),
    },
    outputs = {
        "output": "%{name}.srcjar",
    },
    implementation = _gapic_srcjar_impl,
)

def _proto_custom_library_impl(ctx):
    cur_package = ctx.label.package

    srcs_list = []
    imports_list = []
    check_dep_sources_list = []

    for dep in ctx.attr.deps:
        src = dep.proto.check_deps_sources
        srcs_list.append(src)
        # This is needed to properly support `go_proto_library`
        if cur_package == dep.label.package:
            check_dep_sources_list.append(src)
        imports_list.append(dep.proto.transitive_imports)

    srcs = depset(direct = [], transitive = srcs_list)
    imports = depset(direct = [], transitive = imports_list)
    check_dep_sources = depset(direct = [], transitive = check_dep_sources_list)

    protoc = ctx.executable._protoc
    output = ctx.outputs.output
    output_type = ctx.attr.output_type

    intermediate_output = output
    if output.extension == "srcjar":
        intermediate_output = ctx.actions.declare_file(
            "%s.jar" % output.basename,
            sibling = output,
        )

    output_type_name = "--%s_out" % output_type
    output_paths = [intermediate_output.path]
    extra_inputs = []
    calculated_args = []
    tools = []
    plugin = ctx.executable.plugin

    if plugin:
        extra_inputs.extend(ctx.files.plugin_args)
        tools.append(plugin)
        output_paths = [f.path for f in ctx.files.plugin_args] + output_paths
        calculated_args = [
            "--plugin=protoc-gen-%s=%s" % (output_type, plugin.path),
        ]
    calculated_args.append("%s=%s" % (output_type_name, ":".join(output_paths)))
    arguments = \
        ctx.attr.extra_args + \
        calculated_args + \
        ["-I{0}={1}".format(_path_ignoring_repository(imp), imp.path) for imp in imports.to_list()] + \
        [_path_ignoring_repository(src) for src in srcs.to_list()]

    # print("%s: `%s %s`" % (ctx.label, protoc.path, " ".join(arguments)))
    inputs = depset(transitive = [srcs, imports, depset(direct = extra_inputs)])
    ctx.actions.run(
        inputs = inputs,
        outputs = [intermediate_output],
        executable = protoc,
        tools = tools,
        arguments = arguments,
        progress_message = "%s: `%s %s`" % (ctx.label, protoc.path, " ".join(arguments)),
    )

    if intermediate_output != output:
        ctx.actions.run_shell(
            command = "cp $1 $2",
            inputs = [intermediate_output],
            outputs = [output],
            arguments = [intermediate_output.path, output.path],
        )

    # This makes `proto_custom_library` pretend that it returns same provider as the native
    # `proto_library rule` (ProtoInfo provider). This allows using proto_custom_library output as
    # its own input (deps). Copy other properties of ProtoSourcesProvider if ever needed
    # (currently only 'check_deps_sources' and 'transitive_imports' fields of ProtoSourcesProvider
    # are supported)
    return struct(
        proto = struct(
            direct_sources = check_dep_sources,
            check_deps_sources = check_dep_sources,
            transitive_imports = imports,
            transitive_descriptor_sets = depset(direct = [output]),
        ),
    )

proto_custom_library = rule(
    attrs = {
        "deps": attr.label_list(mandatory = True, allow_empty = False, providers = ["proto"]),
        "plugin": attr.label(mandatory = False, executable = True, cfg = "host"),
        "plugin_args": attr.label_list(
            mandatory = False,
            allow_empty = True,
            allow_files = True,
            default = [],
        ),
        "extra_args": attr.string_list(mandatory = False, default = []),
        "output_type": attr.string(mandatory = True),
        "output_suffix": attr.string(mandatory = True),
        "_protoc": attr.label(
            default = Label("@com_google_protobuf//:protoc"),
            executable = True,
            cfg = "host",
        ),
    },
    outputs = {
        "output": "%{name}%{output_suffix}",
    },
    implementation = _proto_custom_library_impl,
)

def proto_library_with_info(name, deps):
    proto_custom_library(
        name = name,
        deps = deps,
        extra_args = [
            "--include_imports",
            "--include_source_info",
        ],
        output_type = "descriptor_set",
        output_suffix = "-set.proto.bin",
    )

def _unzipped_srcjar_impl(ctx):
    srcjar = ctx.attr.srcjar.files.to_list()[0]
    output_dir = ctx.actions.declare_directory("%s%s" % (ctx.label.name, ctx.attr.extension))

    script = """
    unzip -q {srcjar} -d {output_dir}
    """.format(
        srcjar = srcjar.path,
        output_dir = output_dir.path,
    )

    ctx.actions.run_shell(
        inputs = [srcjar],
        command = script,
        outputs = [output_dir],
    )

    return [DefaultInfo(files = depset(direct = [output_dir]))]

unzipped_srcjar = rule(
    _unzipped_srcjar_impl,
    attrs = {
        "srcjar": attr.label(allow_files = True),
        "extension": attr.string(default = ""),
    },
)

#
# Private helper functions
#
def _path_ignoring_repository(f):
    if f.owner.workspace_root:
        return f.path[f.path.find(f.owner.workspace_root) + len(f.owner.workspace_root) + 1:]
    return f.short_path
