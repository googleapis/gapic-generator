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

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

_BLACK_PY_BUILD_FILE = """
py_binary(
    name = "black",
    srcs = glob(["**/*.py"]),
    visibility = ["//visibility:public"],
)
"""

def py_gapic_repositories():
    _maybe(
        http_archive,
        name = "pypi_black",
        strip_prefix = "black-19.3b0",
        urls = ["https://files.pythonhosted.org/packages/89/07/aebb10fb8f2ffbac672dfbebffa724643bc84cf012a57737a622d1dabddb/black-19.3b0.tar.gz"],
        build_file_content = _BLACK_PY_BUILD_FILE,
      )

def _maybe(repo_rule, name, strip_repo_prefix = "", **kwargs):
    if not name.startswith(strip_repo_prefix):
        return
    repo_name = name[len(strip_repo_prefix):]
    if repo_name in native.existing_rules():
        return
    repo_rule(name = repo_name, **kwargs)
