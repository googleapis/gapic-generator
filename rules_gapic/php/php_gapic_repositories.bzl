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

# Generated libraries direct dependencies
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_file")

def _php_impl(ctx):
    srcs_dir = "srcs";
    ctx.download_and_extract(
        url = ["https://www.php.net/distributions/php-%s.tar.gz" % ctx.attr.version],
        stripPrefix = "php-%s" % ctx.attr.version,
        output = srcs_dir,
    )
    root_path = ctx.path(".")

    ctx.execute(
        ["./configure", "--prefix=%s" % root_path.realpath],
        working_directory = srcs_dir,
        quiet = False,
    )
    ctx.execute(["make", "-j10"], working_directory = srcs_dir, quiet = False)
    ctx.execute(["make", "install"], working_directory = srcs_dir, quiet = False)
    ctx.execute(["rm", "-rf", srcs_dir], quiet = False)

    build_bazel = """
exports_files(glob(include = ["bin/*", "lib/**", "etc/*"], exclude_directories = 0))
     """.format(
    )

    ctx.file("BUILD.bazel", build_bazel)

php = repository_rule(
    implementation = _php_impl,
    attrs = {
        "version": attr.string(),
    },
#    local = True,
)

def php_gapic_repositories():
    _maybe(
        http_file,
        name = "php_cs_fixer",
        urls = ["https://github.com/FriendsOfPHP/PHP-CS-Fixer/releases/download/v2.9.1/php-cs-fixer.phar"],
    )

    _maybe(
        http_file,
        name = "php_code_sniffer",
        urls = ["https://github.com/squizlabs/PHP_CodeSniffer/releases/download/3.4.2/phpcbf.phar"],
    )

def _maybe(repo_rule, name, strip_repo_prefix = "", **kwargs):
    if not name.startswith(strip_repo_prefix):
        return
    repo_name = name[len(strip_repo_prefix):]
    if repo_name in native.existing_rules():
        return
    repo_rule(name = repo_name, **kwargs)

