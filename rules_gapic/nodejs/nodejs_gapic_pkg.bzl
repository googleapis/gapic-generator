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

load("//rules_gapic:gapic_pkg.bzl", "construct_package_dir_paths")

def _nodejs_gapic_src_pkg_impl(ctx):
    proto_srcs = []
    gapic_srcs = []

    for dep in ctx.attr.deps:
        if ProtoInfo in dep:
            proto_srcs.extend(dep[ProtoInfo].check_deps_sources.to_list())
        elif hasattr(dep, "proto"):
            proto_srcs.extend(dep.proto.check_deps_sources.to_list())
        else:
            gapic_srcs.extend(dep.files.to_list())

    paths = construct_package_dir_paths(ctx.attr.package_dir, ctx.outputs.pkg, ctx.label.name)

    script = """
    for gapic_src in {gapic_srcs}; do
        mkdir -p {package_dir_path}
        unzip -q -o $gapic_src -d {package_dir_path}
    done
    for proto_src in {proto_srcs}; do
        mkdir -p {package_dir_path}/protos
        cp -f --parents $proto_src {package_dir_path}/protos
    done
    cd {package_dir_path}
    tar -zchpf {package_dir}.tar.gz {package_dir_expr}
    cd -
    mv {package_dir_path}/{package_dir}.tar.gz {pkg}
    rm -rf {package_dir_path}
    """.format(
        gapic_srcs = " ".join(["'%s'" % f.path for f in gapic_srcs]),
        proto_srcs = " ".join(["'%s'" % f.path for f in proto_srcs]),
        package_dir_path = paths.package_dir_path,
        package_dir = paths.package_dir,
        pkg = ctx.outputs.pkg.path,
        package_dir_expr = paths.package_dir_expr,
    )

    ctx.actions.run_shell(
        inputs = proto_srcs + gapic_srcs,
        command = script,
        outputs = [ctx.outputs.pkg],
    )

_nodejs_gapic_src_pkg = rule(
    attrs = {
        "deps": attr.label_list(allow_files = True, mandatory = True),
        "package_dir": attr.string(mandatory = True),
    },
    outputs = {"pkg": "%{name}.tar.gz"},
    implementation = _nodejs_gapic_src_pkg_impl,
)

def nodejs_gapic_assembly_pkg(name, deps):
    _nodejs_gapic_src_pkg(
        name = name,
        deps = deps,
        package_dir = name,
    )
