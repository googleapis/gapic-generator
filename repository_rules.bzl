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

def _com_google_api_codegen_properties_impl(ctx):
    props_path = ctx.path(ctx.attr.file)
    result = ctx.execute(["cat", props_path])

    if result.return_code != 0:
        fail("Could not load dependencies from properties file, error_code %s" + result.return_code)

    props = result.stdout.splitlines()
    props_as_map = {}

    for prop in props:
        p = prop.strip()
        if len(p) <= 0 or p.startswith("#"):
            continue
        key_value = p.split("=", maxsplit = 1)
        props_as_map[key_value[0]] = key_value[1]

    props_name = ctx.attr.file.name
    dependencies_bzl = """
# DO NOT EDIT. This file was generated from {properties_file}.
PROPERTIES = {props_as_map}
     """.format(
        properties_file = props_name,
        props_as_map = str(props_as_map),
     )
    ctx.file("BUILD.bazel", "")
    ctx.file("%s.bzl" % props_name, dependencies_bzl)


com_google_api_codegen_properties = repository_rule(
    implementation = _com_google_api_codegen_properties_impl,
    attrs = {
        "file": attr.label(),
    },
    local = True,
)
