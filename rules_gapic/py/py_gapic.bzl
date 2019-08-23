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


def moved_proto_library(name, srcs, import_prefix, strip_import_prefix, deps = []):
    srcs_name = "%s_srcs" % name

    replaced_import_proto_srcs(
        name = srcs_name,
        import_prefix = import_prefix,
        strip_import_prefix = strip_import_prefix,
        deps = srcs,
    )

    native.proto_library(
        name = name,
        srcs = [":%s" % srcs_name],
        deps = deps,
        import_prefix = import_prefix,
        strip_import_prefix = srcs_name
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

