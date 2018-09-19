workspace(name = "com_google_api_codegen")

load(
    "//:repositories.bzl",
    "com_google_api_codegen_repositories",
    "com_google_api_codegen_test_repositories",
    "com_google_api_codegen_tools_repositories",
)

com_google_api_codegen_repositories()

com_google_api_codegen_test_repositories()

com_google_api_codegen_tools_repositories()

#
# protoc-java-resource-names-plugin repository dependencies (required to support resource names
# feature in gapic generator)
#
local_repository(
    name = "com_google_protoc_java_resource_names_plugin_repositories",
    path = "/usr/local/google/home/vam/_/projects/github/vam-google/protoc-java-resource-names-plugin",
)

load(
    "@com_google_protoc_java_resource_names_plugin_repositories//:repositories.bzl",
    "com_google_protoc_java_resource_names_plugin_repositories",
)

com_google_protoc_java_resource_names_plugin_repositories(omit_com_google_protobuf = True)
