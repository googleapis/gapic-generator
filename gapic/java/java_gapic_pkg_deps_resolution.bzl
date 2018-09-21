JavaGapicPkg = provider(fields = ["name"])

def construct_dep_strings(deps, test_deps, artifact_group_overrides = {}):
    dep_dict = _reconstruct_artifact_id_strings(deps, artifact_group_overrides)
    test_dep_dict = _reconstruct_artifact_id_strings(test_deps, artifact_group_overrides)

    project_deps = _construct_package_dep_strings(deps)
    project_test_deps = _construct_package_dep_strings(test_deps)

    for dep in dep_dict.items():
        test_dep_dict.pop(dep[0], default = None)

    return struct(
        compile = dep_dict.values(),
        test_compile = test_dep_dict.values(),
        project_compile = project_deps,
        project_test_compile = project_test_deps,
    )

def construct_gradle_build_deps_subs(deps_struct):
    gradle_dep_segments = [
        ("compile '%s'", deps_struct.compile),
        ("testCompile '%s'", deps_struct.test_compile),
        ("compile project(':%s')", deps_struct.project_compile),
        ("testCompile project(':%s')", deps_struct.project_test_compile),
    ]
    gradle_deps = []
    for gradle_dep_segment in gradle_dep_segments:
        for gradle_dep in gradle_dep_segment[1]:
            gradle_deps.append(gradle_dep_segment[0] % gradle_dep)

    return {"{{dependencies}}": "\n  ".join(gradle_deps)}

def construct_gradle_assembly_includes_subs(deps_struct):
    project_deps = deps_struct.project_compile + deps_struct.project_test_compile
    includes = ["include ':%s'" % project_dep for project_dep in project_deps]
    return {"{{includes}}": "\n".join(includes)}

def is_java_dependency(dep):
    return hasattr(dep, "java")

def is_source_dependency(dep):
    return is_java_dependency(dep) and hasattr(dep.java, "source_jars") and dep.label.package != "jar"

def is_gapic_pkg_dependency(dep):
    files_list = dep.files.to_list();
    if not files_list or len(files_list) != 1 or \
        (files_list[0].extension != "gz" and files_list[0].extension != "tgz"):
        return False
    return True


# This is a bit ugly, but there is no way to pass a function object as a rule parameter. As a
# workaround passing a function name as string instead and then resolving the actual function object
# using the following map.
def get_dynamic_subsitution_func(subs_func_name):
    return {
        "construct_gradle_build_deps_subs": construct_gradle_build_deps_subs,
        "construct_gradle_assembly_includes_subs": construct_gradle_assembly_includes_subs,
    }[subs_func_name]

#
# Private helper functions
#
def _construct_package_dep_strings(deps):
    dep_list = []
    for dep in deps:
        if is_gapic_pkg_dependency(dep):
            dep_file = dep.files.to_list()[0]
            dep_name = dep_file.basename
            for ext in (".tar.gz", ".gz", ".tgz"):
                if dep_name.endswith(ext):
                    dep_name = dep_name[:-len(ext)]
                    break;
            dep_list.append(dep_name)
    return dep_list

def _reconstruct_artifact_id_strings(deps, group_overrides = {}):
    dep_dict = {}
    for dep in deps:
        if not is_java_dependency(dep):
            continue
        for f in dep.java.transitive_deps.to_list():
            id = _reconstruct_artifact_id(f, group_overrides)
            if id[0]:
                dep_dict[":".join((id[0], id[1], id[3]))] = id

    sorted_keys = sorted(dep_dict.keys())  # Is sorting appropriate here?
    for sorted_key in sorted_keys:
        dep_dict[sorted_key] = ":".join(dep_dict.pop(sorted_key))
    return dep_dict

def _reconstruct_artifact_id(file, group_overrides = {}):
    """
    This method reconstruct artifact id in the form of `group:name:version:classifier` from the
    corresponding jar dependency file's full path. This implies that maven_jar targets are named
    following the convention documented in bazel's official documentation (a relatively safe
    assumption).

    The aforementioned bazel documentation link: https://docs.bazel.build/versions/master/be/workspace.html#maven_jar

    The implementation approach is dictated by the fact that maven_jar rule and corresponding java_*
    bazel rules drop the original artifact information (after downloading the jar is completely
    "demavenized"). Also there is really no good way of retreiving the value of the "artifact"
    attribute value (passed to maven_jar rule) during analysis phase (note, native.existing_rule()
    is not available during analysis phase).

    There is no support for regular expressions in Starlark, so all the string shenanigans are done
    the "old school" way.

    Args:
        file: (File) The File object, representing the jar dependency. Required.
        group_overrides: {"name": "group"} dictionary to explicitly override calculated group for an
            artifact, for cases, when either convention (maven artifact naming or bazel repository
            target naming) is not followed.

    Returns:
        tuple: (string, string, string, string) The artifact id in the form of a tuple
            (group, name, version, classifier), for example ("com.google.api", "gax-grpc", "1.30.0", "testlib")
    """

    # Reconstructing the `name:version:classifier` portion from basename
    dirname = file.dirname
    extension = file.extension
    basename = file.basename[:-len(extension) - 1]

    if basename.endswith("-ijar") or basename.endswith("-hjar"):
        basename = basename[:-len("-ijar")]

    artifact_id = ["", "", "", ""]  # representing [group, name, version, classifier]
    basename_len = len(basename)
    for i in range(1, basename_len):
        if not artifact_id[1]:
            if basename[i].isdigit() and not basename[i - 1].isalnum():
                artifact_id[1] = basename[0:i - 1]
        elif not artifact_id[2]:
            if i == basename_len - 1:
                artifact_id[2] = basename[len(artifact_id[1]) + 1:i + 1]
            elif not basename[i].isdigit() and not basename[i] == ".":
                artifact_id[2] = basename[len(artifact_id[1]) + 1:i]
        else:
            artifact_id[3] = basename[i:]
            break

    # Reconstruction `group` portion from dirname (full path to dependency jar)
    if artifact_id[1]:
        if group_overrides.get(artifact_id[1]):
            artifact_id[0] = group_overrides.get(artifact_id[1])
        else:
            underscore_artifact_name = artifact_id[1].replace("-", "_")
            art_name_index = dirname.rfind(underscore_artifact_name)
            if art_name_index >= 0:
                left_index = dirname.rfind("/", end = art_name_index) + 1
                right_index = dirname.find("/", start = art_name_index)
                artifact_dir = dirname[left_index:right_index]
                art_name_index = artifact_dir.rfind(underscore_artifact_name)
                artifact_id[0] = artifact_dir[:art_name_index - 1].replace("_", ".")
    return tuple(artifact_id)
