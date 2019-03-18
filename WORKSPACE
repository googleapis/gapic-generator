workspace(name = "com_google_api_codegen")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("//:repository_rules.bzl", "com_google_api_codegen_properties")

com_google_api_codegen_properties(
    name = "com_google_api_codegen_properties",
    file = "//:dependencies.properties",
)

load("//:repositories.bzl", "com_google_api_codegen_repositories")

com_google_api_codegen_repositories()

load(
    "@com_google_protoc_java_resource_names_plugin//:repositories.bzl",
    "com_google_protoc_java_resource_names_plugin_repositories",
)

com_google_protoc_java_resource_names_plugin_repositories(omit_com_google_protobuf = True)
