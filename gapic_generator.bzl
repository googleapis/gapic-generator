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

def google_java_format_srcs(name, srcs, formatter):
    native.genrule(
        name = name,
        outs = ["%s.sh" % name],
        srcs = srcs,
        # TODO: this may fail is list of files is too long (exceeds max command line limit in shell).
        #       Split the command into multiple executions if this ever fails (good enough for now)
        cmd = "echo ' $(location %s) --replace $(SRCS)' > $@" % formatter,
        executable = True,
        tools = [formatter],
        local = 1,
    )