#!/bin/bash

# TODO: We should figure out how to do this as part of GapicGeneratorTest at some point.

unhandledStr='$unhandledCallingForm'
if git ls-files | grep 'baseline$' | xargs grep -l -F $unhandledStr; then
	cat 1>&2 <<EOF
Above baseline files contain string "$unhandledStr";
some sample code won't render properly.
EOF
	exit 1
fi
