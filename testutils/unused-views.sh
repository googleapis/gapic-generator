#!/bin/bash

# Finds methods in *View classes that are not used by *.snip files.
# Usage: run `./testutils/unused-views.sh` from the root of repository.

# comm writes three columns by default
#   1. lines found only in arg1
#   2. lines found only in arg2
#   3. lines found in both
# comm -2 -3 suppresses the last two, printing lines found only in arg1
#
# arg1 consists of method names found in View classes
# arg2 consists of all identifiers in *.snip files.
#
# Thus, we print names of methods defined in View but not used by any snippets.
#
# There are false negatives:
#   - If the "static text" portion of the snip contains method names
#   - If two classes define methods with the same name, and only one is actually used
#   - If the method is used by Java code but not snippets (we'll get compile-error)
# and false positives:
#   - If a method in View is not used directly but used by another method (arguably we should fix this anyway)

comm -2 -3 \
	<(find -name '*View.java' | xargs cat | grep -o '[a-zA-Z0-9]*();' | tr -d '();' | sort -u) \
	<(find -name '*.snip' | xargs cat | tr -c 'a-zA-Z0-9' '\n' | sort -u)
