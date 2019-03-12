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

# Generated libraries direct dependencies
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def java_gapic_repositories():
    _gax_java_version = "8c9f9a5a6e90cb6b94f06a23217c2a4dfe214b80"
    _maybe(
        http_archive,
        name = "com_google_api_gax_java",
        urls = ["https://github.com/googleapis/gax-java/archive/%s.zip" % _gax_java_version],
        strip_prefix = "gax-java-%s" % _gax_java_version,
    )

def _maybe(repo_rule, name, strip_repo_prefix = "", **kwargs):
    if not name.startswith(strip_repo_prefix):
        return
    repo_name = name[len(strip_repo_prefix):]
    if repo_name in native.existing_rules():
        return
    repo_rule(name = repo_name, **kwargs)
