#!/bin/bash

set -e

# By default, comm prints 3 columns:
#   lines only in arg1
#   lines only in arg2
#   common lines
# -2 -3 suppresses the last two.
#
# arg1 is names of methods in SurfaceNamers
# arg2 is all identifiers used outside of namers
#
# so we end up with methods defined in SurfaceNamer that
# we don't use anywhere.
#
# This script has some false-negatives:
#   - For overloads, this will flag when none are used.
#     If only some are used, it won't determine which ones are or aren't used.
#   - If a namer identifier is not used anywhere, but some other class also happens to define the same identifier,
#     this will incorrectly flag the namer identifier as being used.
# and some false-positives:
#   - For helper methods used only in the namer classes,
#     this may incorrectly say they are not used at all,
#     when in reality they are not used outside the namers but may be used in the namers.
#
# The false-negatives are safe. We could miss some methods, but we won't break anything.
# The false-positives are not safe. If we delete something we shouldn't, we'll get compile-error.
# It shouldn't be possible to silently break anything.

comm -2 -3 \
<(find src/main -name '*SurfaceNamer.java' |
  xargs cat |
  grep 'public .*(' |
  grep -o '[a-zA-Z0-9]*(' |
  tr -d '(' |
  sort -u) \
<(find src/main -name '*.java' -not -name '*SurfaceNamer.java' |
  xargs cat |
  tr -c 'a-zA-Z0-9' '\n' |
  sort -u)
