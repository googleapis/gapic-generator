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
