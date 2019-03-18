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

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_jar")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@com_google_api_codegen_properties//:dependencies.properties.bzl", "PROPERTIES")

def com_google_api_codegen_repositories():
    # Import dependencies shared between Gradle and Bazel (i.e. maven dependencies)
    for name, artifact in PROPERTIES.items():
        _maybe(
            native.maven_jar,
            name = name,
            strip_repo_prefix = "maven.",
            artifact = _fix_bazel_artifact_format(artifact),
        )

    # Import Bazel-only dependencies (Gradle version will import maven artifacts of same
    # version, while Bazel will depend on Bazel workspaces). The versions are shared in the
    # properties file.

    # A temporary hack, to fix incompatibility of protobuf 3.6.1 with new Bazel versions.
    # We have to do it, because version 3.6.1.3 exists only as a Bazel workspace, but not as
    # a maven artifact.
    # Remove once upgraded to 3.7.0
    _protobuf_version = PROPERTIES["version.com_google_protobuf"]
    if _protobuf_version == "3.6.1":
        _protobuf_version = "3.6.1.3"

    _maybe(
        http_archive,
        name = "com_google_protobuf",
        urls = ["https://github.com/protocolbuffers/protobuf/archive/v%s.zip" % _protobuf_version],
        strip_prefix = "protobuf-%s" % _protobuf_version,
    )

    _maybe(
        native.maven_jar,
        name = "google_java_format_all_deps",
        artifact = "com.google.googlejavaformat:google-java-format:jar:all-deps:%s" % PROPERTIES["version.google_java_format"],
    )

    _maybe(
        http_archive,
        name = "com_google_protoc_java_resource_names_plugin",
        strip_prefix = "protoc-java-resource-names-plugin-46d8662701a9ce9a7afcf16c2262f686f9dbe279",
        urls = ["https://github.com/googleapis/protoc-java-resource-names-plugin/archive/46d8662701a9ce9a7afcf16c2262f686f9dbe279.zip"],
    )

def _maybe(repo_rule, name, strip_repo_prefix = "", **kwargs):
    if not name.startswith(strip_repo_prefix):
        return
    repo_name = name[len(strip_repo_prefix):]
    if repo_name in native.existing_rules():
        return
    repo_rule(name = repo_name, **kwargs)

def _fix_bazel_artifact_format(artifact_id):
    # Fix the artifact id format discrepancy between Bazel & Gradle.
    # This is relevant only when classifier is specified explicitly.
    # Bazel format:  groupId:artifactId:jar:classifier:version
    # Gradle format: groupId:artifactId:version:classifier
    ids = artifact_id.split(":")
    if len(ids) != 4:
        return artifact_id
    return "%s:%s:%s:%s:%s" % (ids[0], ids[1], "jar", ids[3], ids[2])
