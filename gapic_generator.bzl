def gapic_generator_tests(name, srcs, runtime_deps, size):
    classNames = []
    for src in srcs:
        # convert .java file path to fully qualified class name
        className = src[(src.index("/com/") + 1):-5].replace("/", ".")
        classNames.append(className)
        native.java_test(
            name = className,
            test_class = className,
            runtime_deps = runtime_deps,
            size = size,
        )
    if classNames:
        native.test_suite(
            name = name,
            tests = classNames,
        )

def google_java_format(name, srcs, formatter):
    native.genrule(
        name = name,
        outs = ["%s.sh" % name],
        srcs = srcs,
        # TODO: this may fail if list of files is too long (exceeds max command line limit in shell).
        #       Split the command into multiple executions if this ever fails (good enough for now)
        cmd = "echo ' $(location %s) --replace $(SRCS)' > $@" % formatter,
        executable = True,
        tools = [formatter],
        local = 1,
    )

def _google_java_format_verification_impl(ctx):
    src_files = [src.path for src in ctx.files.srcs]
    output_file = ctx.outputs.output_file
    formatter = ctx.executable.formatter

    ctx.actions.run_shell(
        inputs = ctx.files.srcs,
        arguments = ["--dry-run", "--set-exit-if-changed"] + src_files,
        tools = [formatter],
        command = "%s $@ > %s" % (formatter.path, output_file.path),
        outputs = [output_file],
        progress_message =
            "If this target fails check the list of files that must be formatted in %s" % output_file.path,
    )

google_java_format_verification = rule(
    attrs = {
        "srcs": attr.label_list(allow_files = True),
        "formatter": attr.label(
            default = Label("//:google_java_format_binary"),
            executable = True,
            cfg = "host",
        ),
    },
    outputs = {"output_file": "%{name}.txt"},
    implementation = _google_java_format_verification_impl,
)
