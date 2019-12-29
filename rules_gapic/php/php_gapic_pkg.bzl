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

load("//rules_gapic:gapic.bzl", "CustomProtoInfo", "gapic_srcjar", "proto_custom_library")
load("//rules_gapic:gapic_pkg.bzl", "construct_package_dir_paths")

def _php_gapic_src_pkg_impl(ctx):
    proto_grpc_srcs = []
    gapic_srcs = []

    for dep in ctx.attr.deps:
        if ProtoInfo in dep or CustomProtoInfo in dep:
            proto_grpc_srcs.extend(dep.files.to_list())
        else:
            gapic_srcs.extend(dep.files.to_list())

    paths = construct_package_dir_paths(ctx.attr.package_dir, ctx.outputs.pkg, ctx.label.name)

    script = """
    for proto_grpc_src in {proto_grpc_srcs}; do
        mkdir -p {package_dir_path}/proto/src
        unzip -q -o $proto_grpc_src -d {package_dir_path}/proto/src
    done
    for gapic_src in {gapic_srcs}; do
        mkdir -p {package_dir_path}/proto
        unzip -q -o $gapic_src -d {package_dir_path}
    done
    cd {package_dir_path}
    tar -zchpf {package_dir}.tar.gz {package_dir_expr}*
    cd -
    mv {package_dir_path}/{package_dir}.tar.gz {pkg}
    rm -rf {package_dir_path}
    """.format(
        proto_grpc_srcs = " ".join(["'%s'" % f.path for f in proto_grpc_srcs]),
        gapic_srcs = " ".join(["'%s'" % f.path for f in gapic_srcs]),
        package_dir_path = paths.package_dir_path,
        package_dir = paths.package_dir,
        pkg = ctx.outputs.pkg.path,
        package_dir_expr = paths.package_dir_expr,
    )

    ctx.actions.run_shell(
        inputs = proto_grpc_srcs + gapic_srcs,
        command = script,
        outputs = [ctx.outputs.pkg],
    )

_php_gapic_src_pkg = rule(
    attrs = {
        "deps": attr.label_list(allow_files = True, mandatory = True),
        "package_dir": attr.string(mandatory = True),
    },
    outputs = {"pkg": "%{name}.tar.gz"},
    implementation = _php_gapic_src_pkg_impl,
)

def php_gapic_assembly_pkg(name, deps, **kwargs):
    _php_gapic_src_pkg(
        name = name,
        deps = deps,
        package_dir = name,
        **kwargs
    )

