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

load("//rules_gapic:gapic.bzl", "GapicInfo", "gapic_srcjar", "unzipped_srcjar")

def _py_gapic_postprocessed_srcjar_impl(ctx):
    gapic_srcjar = ctx.file.gapic_srcjar
    formatter = ctx.executable.formatter
    output_dir_name = ctx.label.name

    output_main = ctx.outputs.main
    output_test = ctx.outputs.test
    output_smoke_test = ctx.outputs.smoke_test
    output_pkg = ctx.outputs.pkg
    outputs = [output_main, output_test, output_smoke_test, output_pkg]

    output_dir_path = "%s/%s" % (output_main.dirname, output_dir_name)

    # Note the script is more complicated than it intuitively should be because of limitations
    # inherent to bazel execution environment: no absolute paths allowed, the generated artifacts
    # must ensure uniqueness within a build.
    script = """
    unzip -q {gapic_srcjar} -d {output_dir_path}
    {formatter} -q {output_dir_path}
    pushd {output_dir_path}
    zip -q -r {output_dir_name}-pkg.srcjar nox.py setup.py setup.cfg docs MANIFEST.in README.rst LICENSE
    rm -rf nox.py setup.py docs
    zip -q -r {output_dir_name}-test.srcjar tests/unit
    rm -rf tests/unit
    zip -q -r {output_dir_name}-smoke-test.srcjar tests/system
    rm -rf tests/system
    zip -q -r {output_dir_name}.srcjar . -i \*.py
    popd
    mv {output_dir_path}/{output_dir_name}.srcjar {output_main}
    mv {output_dir_path}/{output_dir_name}-test.srcjar {output_test}
    mv {output_dir_path}/{output_dir_name}-smoke-test.srcjar {output_smoke_test}
    mv {output_dir_path}/{output_dir_name}-pkg.srcjar {output_pkg}
    rm -rf {output_dir_path}
    """.format(
        gapic_srcjar = gapic_srcjar.path,
        output_dir_name = output_dir_name,
        output_dir_path = output_dir_path,
        formatter = formatter.path,
        output_main = output_main.path,
        output_test = output_test.path,
        output_smoke_test = output_smoke_test.path,
        output_pkg = output_pkg.path,
    )

    ctx.actions.run_shell(
        inputs = [gapic_srcjar],
        command = script,
        outputs = outputs,
    )

    return [
        DefaultInfo(
            files = depset(direct = outputs)
        ),
        GapicInfo(
            main = output_main,
            test = output_test,
            smoke_test = output_smoke_test,
            pkg = output_pkg,
        ),
    ]

_py_gapic_postprocessed_srcjar = rule(
    implementation = _py_gapic_postprocessed_srcjar_impl,
    attrs = {
        "gapic_srcjar": attr.label(
            doc = "The srcjar of the generated GAPIC client.",
            mandatory = True,
            allow_single_file = True,
        ),
        "formatter": attr.label(
            doc = "Formats the output to a Python-idiomatic style.",
            default = Label("@pypi_black//:black"),
            executable = True,
            cfg = "host",
        ),
    },
    outputs = {
      "main": "%{name}.srcjar",
      "test": "%{name}-test.srcjar",
      "smoke_test": "%{name}-smoke-test.srcjar",
      "pkg": "%{name}-pkg.srcjar",
    },
    doc = """Runs Python-specific post-processing for the generated GAPIC
    client.
    Post-processing includes running the formatter and splitting the main and
    test packages.
    """,
)

def py_gapic_srcjar(
        name,
        src,
        gapic_yaml,
        service_yaml,
        **kwargs):
    """Generates the Python GAPIC client, performing Python-specific
    postprocessing on the generated client.
    """
    raw_srcjar_name = "%s_raw" % name

    gapic_srcjar(
        name = raw_srcjar_name,
        src = src,
        gapic_yaml = gapic_yaml,
        service_yaml = service_yaml,
        artifact_type = "GAPIC_CODE",
        language = "python",
        **kwargs
    )

    _py_gapic_postprocessed_srcjar(
        name = name,
        gapic_srcjar = ":%s" % raw_srcjar_name,
        **kwargs
    )

def py_gapic_library(
        name,
        src,
        gapic_yaml,
        service_yaml,
        deps = [],
        **kwargs):
    """Generates the Python GAPIC client into a Python library for the main src
    and the tests.
    """
    srcjar_name = "%s_srcjar" % name

    py_gapic_srcjar(
        name = srcjar_name,
        src = src,
        gapic_yaml = gapic_yaml,
        service_yaml = service_yaml,
        **kwargs
    )

    main_file = ":%s.srcjar" % srcjar_name
    main_dir = "%s_main" % srcjar_name

    unzipped_srcjar(
        name = main_dir,
        srcjar = main_file,
        extension = ".py",
    )

    native.py_library(
        name = name,
        srcs = [":%s" % main_dir],
        deps = deps,
        **kwargs
    )

    test_file = ":%s-test.srcjar" % srcjar_name
    test_dir = "%s_test" % srcjar_name

    unzipped_srcjar(
        name = test_dir,
        srcjar = test_file,
        extension = ".py",
    )

    smoke_test_file = ":%s-smoke-test.srcjar" % srcjar_name
    smoke_test_dir = "%s_smoke_test" % srcjar_name

    unzipped_srcjar(
        name = smoke_test_dir,
        srcjar = smoke_test_file,
        extension = ".py",
    )

    pkg_file = ":%s-pkg.srcjar" % srcjar_name
    pkg_dir = "%s_pkg" % srcjar_name

    unzipped_srcjar(
        name = pkg_dir,
        srcjar = pkg_file,
        extension = ".py",
    )
