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

def construct_package_dir_paths(attr_package_dir, out_pkg, label_name):
    if attr_package_dir:
        package_dir = attr_package_dir
        package_dir_expr = "../{}/".format(package_dir)
    else:
        package_dir = label_name
        package_dir_expr = "./"

    # We need to include label in the path to eliminate possible output files duplicates
    # (labels are guaranteed to be unique by bazel itself)
    package_dir_path = "%s/%s/%s" % (out_pkg.dirname, label_name, package_dir)
    return struct(
        package_dir = package_dir,
        package_dir_expr = package_dir_expr,
        package_dir_path = package_dir_path,
        package_dir_sibling_parent = out_pkg,
        package_dir_sibling_basename = label_name,
    )

def put_dep_in_a_bucket(dep, dep_bucket, processed_deps):
    if processed_deps.get(dep):
        return
    dep_bucket.append(dep)
    processed_deps[dep] = True
