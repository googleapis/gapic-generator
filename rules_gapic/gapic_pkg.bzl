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

def construct_package_dir_paths(attr_package_dir, out_pkg, label_name):
    if attr_package_dir:
        package_dir = attr_package_dir
        package_dir_expr = "../{}/".format(package_dir)
        tar_cd_suffix = ".."
        tar_prefix = attr_package_dir
    else:
        package_dir = label_name
        package_dir_expr = "./"
        tar_cd_suffix = "."
        tar_prefix = "."

    # We need to include label in the path to eliminate possible output files duplicates
    # (labels are guaranteed to be unique by bazel itself)
    package_dir_path = "%s/%s/%s" % (out_pkg.dirname, label_name, package_dir)
    return struct(
        package_dir = package_dir,
        package_dir_expr = package_dir_expr,
        package_dir_path = package_dir_path,
        package_dir_sibling_parent = out_pkg,
        package_dir_sibling_basename = label_name,
        tar_cd_suffix = tar_cd_suffix,
        tar_prefix = tar_prefix
    )

def put_dep_in_a_bucket(dep, dep_bucket, processed_deps):
    if processed_deps.get(dep):
        return
    dep_bucket.append(dep)
    processed_deps[dep] = True

def _pkg_tar_impl(ctx):
    deps = []
    for dep in ctx.attr.deps:
        for f in dep.files.to_list():
            deps.append(f)

    paths = construct_package_dir_paths(
        ctx.attr.package_dir,
        ctx.outputs.pkg,
        ctx.label.name,
    )

    script = """
    mkdir -p {package_dir_path}
    for dep in {deps}; do
        tar -xzpf $dep -C {package_dir_path}
    done
    cd {package_dir_path}/{tar_cd_suffix}
    tar -zchpf {tar_prefix}/{package_dir}.tar.gz {tar_prefix}/*
    cd -
    mv {package_dir_path}/{package_dir}.tar.gz {pkg}
    rm -rf {package_dir_path}
    """.format(
        deps = " ".join(["'%s'" % d.path for d in deps]),
        package_dir_path = paths.package_dir_path,
        package_dir = paths.package_dir,
        pkg = ctx.outputs.pkg.path,
        tar_cd_suffix = paths.tar_cd_suffix,
        tar_prefix = paths.tar_prefix,
    )

    ctx.actions.run_shell(
        inputs = deps,
        command = script,
        outputs = [ctx.outputs.pkg],
    )

# The Bazel's native pkg_tar rule behaves weirdly when package_dir parameter
# is specified (at least on some Linux machines it does not put all the files
# under the package_dir). As a workaround for that bug we provide the custom
# implementation of the pkg_tar rule.
pkg_tar = rule(
    attrs = {
        "deps": attr.label_list(mandatory = True),
        "package_dir": attr.string(mandatory = False, default = ""),
        "extension": attr.string(mandatory = False, default = "tar.gz"),
    },
    outputs = {"pkg": "%{name}.%{extension}"},
    implementation = _pkg_tar_impl,
)
