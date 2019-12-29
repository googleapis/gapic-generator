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

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_jar")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:jvm.bzl", "jvm_maven_import_external")
load("@com_google_api_codegen_properties//:dependencies.properties.bzl", "PROPERTIES")

def com_google_api_codegen_repositories():
    # Import dependencies shared between Gradle and Bazel (i.e. maven dependencies)
    for name, artifact in PROPERTIES.items():
        _maybe(
            jvm_maven_import_external,
            name = name,
            strip_repo_prefix = "maven.",
            artifact = _fix_bazel_artifact_format(artifact),
            server_urls = ["https://repo.maven.apache.org/maven2/", "http://repo1.maven.org/maven2/"],
            licenses = ["notice", "reciprocal"]
        )

    # Import Bazel-only dependencies (Gradle version will import maven artifacts of same
    # version, while Bazel will depend on Bazel workspaces). The versions are shared in the
    # properties file.

    _protobuf_version = PROPERTIES["version.com_google_protobuf"]

    _maybe(
        http_archive,
        name = "com_google_protobuf",
        urls = ["https://github.com/protocolbuffers/protobuf/archive/v%s.zip" % _protobuf_version],
        strip_prefix = "protobuf-%s" % _protobuf_version,
    )

    _maybe(
        jvm_maven_import_external,
        name = "google_java_format_all_deps",
        artifact = "com.google.googlejavaformat:google-java-format:jar:all-deps:%s" % PROPERTIES["version.google_java_format"],
        server_urls = ["https://repo.maven.apache.org/maven2/", "http://repo1.maven.org/maven2/"],
        licenses = ["notice", "reciprocal"]
    )

    _maybe(
        http_archive,
        name = "com_google_protoc_java_resource_names_plugin",
        strip_prefix = "protoc-java-resource-names-plugin-3fb2ec9b778f62646c05a7b960c893464c7791c0",
        urls = ["https://github.com/googleapis/protoc-java-resource-names-plugin/archive/3fb2ec9b778f62646c05a7b960c893464c7791c0.zip"],
    )

    _maybe(
        http_archive,
        name = "bazel_skylib",
        sha256 = "bbccf674aa441c266df9894182d80de104cabd19be98be002f6d478aaa31574d",
        strip_prefix = "bazel-skylib-2169ae1c374aab4a09aa90e65efe1a3aad4e279b",
        urls = ["https://github.com/bazelbuild/bazel-skylib/archive/2169ae1c374aab4a09aa90e65efe1a3aad4e279b.tar.gz"],
    )

    _maybe(
        http_archive,
        name = "com_google_googleapis",
        strip_prefix = "googleapis-84c8ad4e52f8eec8f08a60636cfa597b86969b5c",
        urls = [
            "https://github.com/googleapis/googleapis/archive/84c8ad4e52f8eec8f08a60636cfa597b86969b5c.zip",
        ],
    )

    _maybe(
        native.bind,
        name = "guava",
        actual = "@com_google_guava_guava__com_google_api_codegen//jar",
    )

    _maybe(
        native.bind,
        name = "gson",
        actual = "@com_google_code_gson_gson//jar",
    )

    _maybe(
        jvm_maven_import_external,
        name = "error_prone_annotations_maven",
        artifact = "com.google.errorprone:error_prone_annotations:2.3.2",
        server_urls = ["https://repo.maven.apache.org/maven2/", "http://repo1.maven.org/maven2/"],
        licenses = ["notice", "reciprocal"]
    )

    _maybe(
        native.bind,
        name = "error_prone_annotations",
        actual = "@error_prone_annotations_maven//jar",
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
