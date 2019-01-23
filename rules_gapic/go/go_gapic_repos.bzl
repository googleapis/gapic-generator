load("@bazel_gazelle//:deps.bzl", "go_repository")

def go_gapic_repositories(
        omit_com_github_googleapis_gax_go = False,
        omit_org_golang_google_api = False,
        omit_org_golang_x_oauth2 = False,
        omit_com_github_google_go_cmp = False,
        omit_com_google_cloud_go = False,
        omit_io_opencensus_go = False):
    if not omit_com_github_googleapis_gax_go:
        com_github_googleapis_gax_go()
    if not omit_org_golang_google_api:
        org_golang_google_api()
    if not omit_org_golang_x_oauth2:
        org_golang_x_oauth2()
    if not omit_com_github_google_go_cmp:
        com_github_google_go_cmp()
    if not omit_com_google_cloud_go:
        com_google_cloud_go()
    if not omit_io_opencensus_go:
        io_opencensus_go()

def com_github_googleapis_gax_go():
    go_repository(
        name = "com_github_googleapis_gax_go",
        importpath = "github.com/googleapis/gax-go",
        type = "zip",
        strip_prefix = "gax-go-1.0.1",
        urls = ["https://github.com/googleapis/gax-go/archive/v1.0.1.zip"]
    )

def org_golang_google_api():
    go_repository(
        name = "org_golang_google_api",
        importpath = "google.golang.org/api",
        type = "zip",
        strip_prefix = "google-api-go-client-0a71a4356c3f4bcbdd16294c78ca2a31fda36cca",
        urls = ["https://github.com/googleapis/google-api-go-client/archive/0a71a4356c3f4bcbdd16294c78ca2a31fda36cca.zip"]

    )

def org_golang_x_oauth2():
    go_repository(
        name = "org_golang_x_oauth2",
        importpath = "golang.org/x/oauth2",
        type = "zip",
        strip_prefix = "oauth2-8f65e3013ebad444f13bc19536f7865efc793816",
        urls = ["https://github.com/golang/oauth2/archive/8f65e3013ebad444f13bc19536f7865efc793816.zip"]
    )

def com_github_google_go_cmp():
    go_repository(
        name = "com_github_google_go_cmp",
        importpath = "github.com/google/go-cmp/cmp",
        type = "zip",
        strip_prefix = "go-cmp-0.2.0",
        urls = ["https://github.com/google/go-cmp/archive/v0.2.0.zip"]
    )

#TODO: this must be removed (otherwise googleapis becomes dependenty on google-cloud-go)
#      we have to add it as a temporary workaroudn, because the generated go clients transitively
#      depend on `cloud.google.com/go/compute/metadata` which is a part of google-cloud-go
def com_google_cloud_go():
    go_repository(
        name = "com_google_cloud_go",
        importpath = "cloud.google.com/go",
        type = "zip",
        strip_prefix = "google-cloud-go-0.33.1",
        urls = ["https://github.com/GoogleCloudPlatform/google-cloud-go/archive/v0.33.1.zip"]
    )

def io_opencensus_go():
    go_repository(
        name = "io_opencensus_go",
        importpath = "go.opencensus.io",
        type = "zip",
        strip_prefix = "opencensus-go-0.18.0",
        urls = ["https://github.com/census-instrumentation/opencensus-go/archive/v0.18.0.zip"]
    )

