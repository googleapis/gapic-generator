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
    root_path = ctx.path(".")

    build_bazel = """
exports_files(glob(include = ["bin/*", "lib/**"], exclude_directories = 0))
     """.format(
    )

    os_name = ctx.os.name

    # First try using the prebuilt version
    for prebuilt_php in ctx.attr.prebuilt_phps:
        if prebuilt_php.name.find(os_name) < 0:
            continue
        tmp = "php_tmp"
        _execute_and_check_result(ctx, ["mkdir", tmp], quiet = False)
        ctx.extract(archive = prebuilt_php, stripPrefix = ctx.attr.strip_prefix, output = tmp)
        res = ctx.execute(["bin/php", "--version"], working_directory = tmp)
        _execute_and_check_result(ctx, ["rm", "-rf", tmp], quiet = False)
        if res.return_code == 0:
            ctx.extract(archive = prebuilt_php, stripPrefix = ctx.attr.strip_prefix)
            ctx.file("BUILD.bazel", build_bazel)
            return

    # If none of the prebuilt versions worked, fallback to building the php
    # interpreter from sources
    srcs_dir = "srcs"
    ctx.download_and_extract(
        url = ctx.attr.urls,
        stripPrefix = ctx.attr.strip_prefix,
        output = srcs_dir,
    )
    _execute_and_check_result(
        ctx,
        ["./configure", "--enable-static", "--without-pear", "--without-iconv", "--prefix=%s" % root_path.realpath],
        working_directory = srcs_dir,
        quiet = False,
    )
    _execute_and_check_result(ctx, ["make", "-j10"], working_directory = srcs_dir, quiet = False)
    _execute_and_check_result(ctx, ["make", "install"], working_directory = srcs_dir, quiet = False)
    _execute_and_check_result(ctx, ["rm", "-rf", srcs_dir], quiet = False)

    ctx.file("BUILD.bazel", build_bazel)

php = repository_rule(
    implementation = _php_impl,
    attrs = {
        "urls": attr.string_list(),
        "strip_prefix": attr.string(),
        "prebuilt_phps": attr.label_list(allow_files = True, mandatory = False),
    },
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

def _execute_and_check_result(ctx, command, **kwargs):
    res = ctx.execute(command, **kwargs)
    if res.return_code != 0:
        fail("""
Failed to execute command: `{command}`
Exit Code: {code}
STDERR: {stderr}
        """.format(
            command = command,
            code = res.return_code,
            stderr = res.stderr,
        ))
    return res

def _maybe(repo_rule, name, strip_repo_prefix = "", **kwargs):
    if not name.startswith(strip_repo_prefix):
        return
    repo_name = name[len(strip_repo_prefix):]
    if repo_name in native.existing_rules():
        return
    repo_rule(name = repo_name, **kwargs)
