Discovery Tests
===============

This directory contains all discovery files that we want to test.
Sub-directories contain baseline files for generated code samples we create, one sub-directory per language.

Each baseline file has the same name as the discovery file it baselines for with a language-name prefix and ".baseline" suffix added.
The suffix is added for convenience.
The prefix might seem redundant, but it is necessary. If baseline test fails, the test framework copies the output of the generation into
directory `/tmp/com.google.api.codegen_testdata/`. The language prefix prevents tests for the same discovery doc in different languages from clashing.
This situation is rather unfortunate; hopefully this deficiency with framework code can be fixed.

The content of each baseline file is simply a concatenation of code samples for methods defined in the corresponding discovery file.
