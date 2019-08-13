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

load("@io_bazel_rules_go//go:def.bzl", "GoLibrary", "GoSource")
load("//rules_gapic:gapic_pkg.bzl", "construct_package_dir_paths")

def _go_gapic_src_pkg_impl(ctx):
    srcjars = []
    srcs = []
    src_strs = []
    for dep in ctx.attr.deps:
        if GoSource in dep:
            for dep_src in dep[GoSource].srcs:
                if dep_src.extension == "srcjar":
                    srcjars.append(dep_src)
                else:
                    srcs.append(dep_src)
                    src_strs.append("%s:%s" % (dep[GoLibrary].importpath, dep_src.path))
        elif hasattr(dep, "files"):
            for dep_file in dep.files.to_list():
                if dep_file.extension == "srcjar":
                    srcjars.append(dep_file)

    paths = construct_package_dir_paths(ctx.attr.package_dir, ctx.outputs.pkg, ctx.label.name)
    script = """
    for srcjar in {srcjars}; do
        mkdir -p {package_dir_path}
        unzip -q -o $srcjar -d {package_dir_path}
    done
    for src_str in {srcs_strs}; do
        # Split the `src_str` string in format '<dest_dir_path>:<full_file_path>' by ':' delimiter
        # dest_dir_path:  ${{src_str%:*}}
        # full_file_path: ${{src_str#*:}}
        if [ -d "${{src_str#*:}}" ]; then
            mkdir -p {package_dir_path}
            cp -R -L ${{src_str#*:}}/* {package_dir_path}/
        else
            mkdir -p {package_dir_path}/${{src_str%:*}}
            cp -f ${{src_str#*:}} {package_dir_path}/${{src_str%:*}}
        fi
        chmod 644 {package_dir_path}/${{src_str%:*}}/*
    done
    cd {package_dir_path}
    tar -zchpf {package_dir}.tar.gz {package_dir_expr}
    cd -
    mv {package_dir_path}/{package_dir}.tar.gz {pkg}
    """.format(
        srcjars = " ".join(["'%s'" % f.path for f in srcjars]),
        srcs_strs = " ".join(["'%s'" % s for s in src_strs]),
        package_dir_path = paths.package_dir_path,
        package_dir = paths.package_dir,
        pkg = ctx.outputs.pkg.path,
        package_dir_expr = paths.package_dir_expr,
    )

    ctx.actions.run_shell(
        inputs = srcjars + srcs,
        command = script,
        outputs = [ctx.outputs.pkg],
    )

_go_gapic_src_pkg = rule(
    attrs = {
        "deps": attr.label_list(allow_files = True, mandatory = True),
        "package_dir": attr.string(mandatory = True),
    },
    outputs = {"pkg": "%{name}.tar.gz"},
    implementation = _go_gapic_src_pkg_impl,
)

def go_gapic_assembly_pkg(name, deps):
    _go_gapic_src_pkg(
        name = name,
        deps = deps,
        package_dir = name,
    )
