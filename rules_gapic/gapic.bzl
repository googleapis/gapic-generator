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

GapicInfo = provider(fields = ["main", "test", "smoke_test", "pkg"])

def _set_args(arg, arg_name, args, inputs = None, required = False):
    if not arg:
        if required:
            fail("Missing required argument", arg_name)
        return
    args.append("%s%s" % (arg_name, arg.files.to_list()[0].path if hasattr(arg, "files") else arg))
    if inputs != None:
        inputs.append(arg.files.to_list()[0])

def _gapic_srcjar_impl(ctx):
    arguments = []
    inputs = []

    attr = ctx.attr
    if attr.artifact_type:
        _set_args(attr.artifact_type, "", arguments)
        if ctx.attr.artifact_type.find("DISCOGAPIC") >= 0:
            _set_args(attr.src, "--discovery_doc=", arguments, inputs)
        else:
            _set_args(attr.src, "--descriptor_set=", arguments, inputs)
        _set_args(attr.gapic_yaml, "--gapic_yaml=", arguments, inputs)
        _set_args(attr.package, "--package=", arguments)
        _set_args(attr.language, "--language=", arguments)
        _set_args(attr.service_yaml, "--service_yaml=", arguments, inputs)
        _set_args(attr.package_yaml2, "--package_yaml2=", arguments, inputs)
        _set_args(attr.grpc_service_config, "--grpc_service_config=", arguments, inputs)
        _set_args(attr.transport_protocol, "--transport_protocol=", arguments)
    else:
        _set_args(attr.language, "--language=", arguments)
        _set_args(attr.src, "--descriptor=", arguments, inputs)
        _set_args(attr.package, "--package=", arguments)
        _set_args(attr.grpc_service_config, "--grpc_service_config=", arguments, inputs)
        _set_args(attr.transport_protocol, "--transport_protocol=", arguments)

    gapic_generator = ctx.executable.gapic_generator
    ctx.actions.run(
        inputs = inputs,
        outputs = [ctx.outputs.output],
        arguments = arguments + ["--output=%s" % ctx.outputs.output.path],
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
        "artifact_type": attr.string(mandatory = False),  #default = "GAPIC_CODE"
        "gapic_yaml": attr.label(mandatory = False, allow_single_file = True),
        "language": attr.string(mandatory = False),
        "service_yaml": attr.label(mandatory = False, allow_single_file = True),
        "package_yaml2": attr.label(mandatory = False),
        "package": attr.string(mandatory = False),
        "output_suffix": attr.string(mandatory = False, default = ".srcjar"),
        "grpc_service_config": attr.label(mandatory = False, allow_single_file = True),
        "transport_protocol": attr.string(mandatory = False),
        "gapic_generator": attr.label(
            default = Label("//:gapic_generator"),
            executable = True,
            cfg = "host",
        ),
    },
    outputs = {
        "output": "%{name}%{output_suffix}",
    },
    implementation = _gapic_srcjar_impl,
)

CustomProtoInfo = provider(
    fields = [
        "direct_sources",
        "check_deps_sources",
        "transitive_imports",
        "transitive_descriptor_sets",
    ],
)

def _proto_custom_library_impl(ctx):
    cur_package = ctx.label.package

    srcs_list = []
    imports_list = []
    check_dep_sources_list = []

    for dep in ctx.attr.deps:
        prov = ProtoInfo if ProtoInfo in dep else CustomProtoInfo
        src = dep[prov].check_deps_sources
        srcs_list.append(src)
        check_dep_sources_list.append(src)
        imports_list.append(dep[prov].transitive_imports)

    srcs = depset(direct = [], transitive = srcs_list)
    imports = depset(direct = [], transitive = imports_list)
    check_dep_sources = depset(direct = [], transitive = check_dep_sources_list)

    protoc = ctx.executable._protoc
    output = ctx.outputs.output
    output_type = ctx.attr.output_type

    intermediate_output = output
    if output.extension == "srcjar":
        intermediate_output = ctx.actions.declare_file(
            "%s.zip" % output.basename,
            sibling = output,
        )

    output_type_name = "--%s_out" % output_type
    opt_type_name = "--%s_opt" % output_type
    output_paths = [intermediate_output.path]
    extra_inputs = []
    calculated_args = []
    tools = []
    plugin = ctx.executable.plugin
    plugin_file_args = []

    if plugin:
        tools.append(plugin)
        for t, k in ctx.attr.plugin_file_args.items():
            for f in t.files.to_list():
                extra_inputs.append(f)
                plugin_file_args.append("%s=%s" % (k, f.path) if k else f.path)
        calculated_args = [
            "--plugin=protoc-gen-%s=%s" % (output_type, plugin.path),
        ]

    if ctx.attr.plugin_args or plugin_file_args:
        output_paths.insert(0, ",".join(ctx.attr.plugin_args + plugin_file_args))
    calculated_args.append("%s=%s" % (output_type_name, ":".join(output_paths)))

    # taking the file opt args, that come in a form of 
    # label_list -> arg_name dictionary
    # and tranforming them to the list of form "arg_name=file"
    # realistically most of the time there should only be one file per arg name
    # so the label_list will be of one label only
    opt_args_strings = []
    if ctx.attr.opt_file_args:
        for labels, opt_arg_name in ctx.attr.opt_file_args.items():
            for file in labels.files.to_list():
                extra_inputs.append(file)
                opt_args_strings.append("%s=%s" % (opt_arg_name, file.path))

    # now if there are opt_args given we need to merge the opt_file args
    # to the opt_args. opt_args are already list of strings so no preprocessing required
    if ctx.attr.opt_args:
        opt_args_strings = ctx.attr.opt_args + opt_args_strings

    # now if there are any opt args we put them into the plugin_opt= protoc param
    if ctx.attr.opt_args or ctx.attr.opt_file_args:
        calculated_args.append("%s=%s" % (opt_type_name, ",".join(opt_args_strings)))

    arguments = \
        ctx.attr.extra_args + \
        ["--experimental_allow_proto3_optional"] + \
        calculated_args + \
        ["-I{0}={1}".format(path_ignoring_repository(imp), imp.path) for imp in imports.to_list()] + \
        [path_ignoring_repository(src) for src in srcs.to_list()]

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
    # its own input (deps). Copy other properties of ProtoSourcesProvider if ever needed.
    # Currently only the following fields are provided:
    #   - direct_sources
    #   - check_deps_sources
    #   - transitive_imports
    #   - transitive_descriptor_sets
    return [
        CustomProtoInfo(
            direct_sources = check_dep_sources,
            check_deps_sources = check_dep_sources,
            transitive_imports = imports,
            transitive_descriptor_sets = depset(direct = [output]),
        ),
    ]

proto_custom_library = rule(
    attrs = {
        "deps": attr.label_list(
            mandatory = True,
            allow_empty = False,
            providers = [[ProtoInfo], [CustomProtoInfo]],
        ),
        "plugin": attr.label(mandatory = False, executable = True, cfg = "host"),
        "plugin_file_args": attr.label_keyed_string_dict(
            mandatory = False,
            allow_empty = True,
            allow_files = True,
            default = {},
        ),
        "plugin_args": attr.string_list(mandatory = False, allow_empty = True, default = []),
        "opt_args": attr.string_list(
            mandatory = False,
            allow_empty = True,
            default = [],
            doc = """list of opt arguments for the plugin. 
            Will be rendered in protoc command line in the form of "<plugin>_opt=arg1,..."
            """,
        ),
        "opt_file_args": attr.label_keyed_string_dict(
            mandatory = False,
            allow_empty = True,
            allow_files = True,
            default = {},
            doc = """dictionary for the keyed opt arguments for the plugin that pass a file.
            File in form of a label goes as the key, the argument name as a value.
            The result will be rendered in protoc command line in the form of "<plugin>_opt=key1=path/to/file1,..."
            """,
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

def moved_proto_library(
        name,
        srcs,
        import_prefix = None,
        strip_import_prefix = None,
        deps = []):
    srcs_name = "%s_srcs" % name

    actual_strip_import_prefix = strip_import_prefix
    if not actual_strip_import_prefix:
        actual_strip_import_prefix = native.package_name()

    actual_import_prefix = import_prefix
    if not actual_import_prefix:
        actual_import_prefix = _calculate_import_prefix(actual_strip_import_prefix)

    replaced_import_proto_srcs(
        name = srcs_name,
        import_prefix = actual_import_prefix,
        strip_import_prefix = actual_strip_import_prefix,
        deps = srcs,
    )

    native.proto_library(
        name = name,
        srcs = [":%s" % srcs_name],
        deps = deps,
        import_prefix = actual_import_prefix,
        strip_import_prefix = srcs_name,
    )

def _replaced_import_proto_srcs_impl(ctx):
    for dep in ctx.attr.deps:
        dep_srcs = dep[ProtoInfo].check_deps_sources.to_list()

    strip_import_prefixes = {}
    outs = []

    for dep in ctx.attr.deps:
        srcs = dep[ProtoInfo].check_deps_sources.to_list()
        for src in srcs:
            out = ctx.actions.declare_file(
                "%s/%s" % (ctx.label.name, src.basename),
                sibling = src,
            )
            outs.append(out)
            ctx.actions.expand_template(
                template = src,
                output = out,
                substitutions = {ctx.attr.strip_import_prefix: ctx.attr.import_prefix},
            )

    return [DefaultInfo(files = depset(direct = outs))]

replaced_import_proto_srcs = rule(
    attrs = {
        "deps": attr.label_list(mandatory = True, allow_empty = False, providers = [ProtoInfo]),
        "import_prefix": attr.string(mandatory = True),
        "strip_import_prefix": attr.string(mandatory = True),
    },
    implementation = _replaced_import_proto_srcs_impl,
)

def path_ignoring_repository(f):
    virtual_imports = "/_virtual_imports/"
    if virtual_imports in f.path:
        return f.path.split(virtual_imports)[1].split("/", 1)[1]
    if f.owner.workspace_root:
        return f.path[f.path.find(f.owner.workspace_root) + len(f.owner.workspace_root) + 1:]
    return f.short_path

def discogapic_config(name, src, **kwargs):
    gapic_srcjar(
        name = name,
        src = src,
        artifact_type = "DISCOGAPIC_CONFIG",
        output_suffix = ".yaml",
        **kwargs
    )

#
# Private helper functions
#
def _calculate_import_prefix(strip_import_prefix):
    tokens = strip_import_prefix.split("/")
    new_tokens = [tokens[0]]
    api_tokens = tokens[1:]
    # This logic is executed only if import_prefix is not specified by
    # a user explicitly (so technically there is no enforced coupling to google
    # cloud domain, it serves only as a convenient default value).
    if tokens[0] == "google":
        new_tokens.append("cloud")
        if tokens[1] == "cloud" or tokens[1] == "privacy":
            api_tokens = tokens[2:]
        elif tokens[1] == "devtools":
            new_tokens.append("devtools")
            api_tokens = tokens[2:]

    if len(api_tokens) <= 1:
        api_tokens.insert(0, new_tokens[-1])
    new_tokens.append("_".join(api_tokens))
    new_tokens.append("proto")
    return "/".join(new_tokens)
