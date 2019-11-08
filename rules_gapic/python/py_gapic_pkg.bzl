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

load("//rules_gapic:gapic_pkg.bzl", "construct_package_dir_paths", "put_dep_in_a_bucket")
load("//rules_gapic:gapic.bzl", "path_ignoring_repository")

def _py_gapic_src_pkg_impl(ctx):
    strs_srcs = []
    srcs = []
    dir_srcs = []
    srcjar_srcs = []

    for dep in ctx.attr.deps:
        for f in dep.files.to_list():
            if hasattr(f, "is_directory") and f.is_directory:
                dir_srcs.append(f)
                continue
            if f.extension in ("srcjar", "jar", "zip"):
                srcjar_srcs.append(f)
                continue
            srcs.append(f)
            short_path = path_ignoring_repository(f)
            strs_srcs.append("%s:%s" % (short_path.rsplit("/", 1)[0], f.path))

    paths = construct_package_dir_paths(ctx.attr.package_dir, ctx.outputs.pkg, ctx.label.name)

    script = """
    mkdir -p {package_dir_path}
    for str_src in {strs_srcs}; do
        # Split the `src_str` string in format '<dest_dir_path>:<full_file_path>' by ':' delimiter
        # dest_dir_path:  ${{str_src%:*}}
        # full_file_path: ${{str_src#*:}}
        mkdir -p {package_dir_path}/${{str_src%:*}}
        touch {package_dir_path}/${{str_src%:*}}/__init__.py
        cp -f ${{str_src#*:}} {package_dir_path}/${{str_src%:*}}
    done
    for dir_src in {dir_srcs}; do
        cp -R -L $dir_src/* {package_dir_path}/
    done

    # Replace 555 (forced by Bazel) permissions with 644
    find {package_dir_path} -type f -exec chmod 644 {{}} \;

    for srcjar_src in {srcjar_srcs}; do
        unzip -q -o $srcjar_src -d {package_dir_path}
    done
    cd {package_dir_path}
    tar -zchpf {package_dir}.tar.gz {package_dir_expr}*
    cd -
    mv {package_dir_path}/{package_dir}.tar.gz {pkg}
    rm -rf {package_dir_path}
    """.format(
        strs_srcs = " ".join(["'%s'" % s for s in strs_srcs]),
        dir_srcs = " ".join(["'%s'" % f.path for f in dir_srcs]),
        srcjar_srcs = " ".join(["'%s'" % f.path for f in srcjar_srcs]),
        package_dir_path = paths.package_dir_path,
        package_dir = paths.package_dir,
        pkg = ctx.outputs.pkg.path,
        package_dir_expr = paths.package_dir_expr,
    )

    ctx.actions.run_shell(
        inputs = srcs + dir_srcs + srcjar_srcs,
        command = script,
        outputs = [ctx.outputs.pkg],
    )

_py_gapic_src_pkg = rule(
    attrs = {
        "deps": attr.label_list(allow_files = True, mandatory = True),
        "package_dir": attr.string(mandatory = True),
    },
    outputs = {"pkg": "%{name}.tar.gz"},
    implementation = _py_gapic_src_pkg_impl,
)

def py_gapic_assembly_pkg(name, deps, **kwargs):
    actual_deps = []
    processed_deps = {}
    for dep in deps:
        put_dep_in_a_bucket(dep, actual_deps, processed_deps)
        if dep.endswith("_py_gapic"):
            put_dep_in_a_bucket("%s_test" % dep, actual_deps, processed_deps)
            put_dep_in_a_bucket("%s_srcjar-test.srcjar" % dep, actual_deps, processed_deps)
            put_dep_in_a_bucket("%s_srcjar-smoke-test.srcjar" % dep, actual_deps, processed_deps)
            put_dep_in_a_bucket("%s_srcjar-pkg.srcjar" % dep, actual_deps, processed_deps)

    _py_gapic_src_pkg(
        name = name,
        deps = actual_deps,
        package_dir = name,
        **kwargs
    )


