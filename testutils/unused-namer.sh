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
# NOTE(pongad): This misses overloads and helper methods used by namers themselves.
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
