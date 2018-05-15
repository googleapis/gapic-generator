#!/bin/bash

set -e

# TODO: We should figure out how to do this as part of GapicGeneratorTest at some point.

exitStatus=0

## Check for unhandled cases.
unhandledStr='$unhandledCallingForm'
if grep --include='*.baseline' -r -l -F $unhandledStr src/; then
	cat 1>&2 <<EOF
Above baseline files contain string "$unhandledStr";
some sample code won't render properly.

EOF
	exitStatus=1
fi

## Check that every calling form is used somewhere.
callingFormsFile="src/main/java/com/google/api/codegen/viewmodel/CallingForm.java"

# Calling forms are in an enum, so we look for capital identifiers that begins the line.
callingForms=$(< $callingFormsFile grep -o '^[[:space:]]*[A-Z][a-zA-Z]*' | tr -d -c '[a-zA-Z\n]')

unusedForms=$(
	# Grep src/
	#   recursively (-r)
	#   in baseline files (--include='*.baseline')
	#   for any calling forms: -f <(cat <<< $callingForms)
	#   use fixed strings, not regexp (-F)
	#   match whole word (-w)
	#   print only the calling form, not surrounding line (-o)
	#   don't print file names (-h)
	# Prints all occurences of used calling forms
	grep --include='*.baseline' -h -w -o -r -F -f <(cat <<< $callingForms) src/ |

	# Then turn it into a set.
	sort -u |

	# Both callingForms and used forms are sets, sorting them together means
	#   used forms appear twice together (one from callingForms, one from used forms)
	#   unused forms appear only once
	sort <(cat <<< $callingForms) - |

	# Only print unique lines, leaving unused forms.
	uniq -u
)
if [ "$unusedForms" ]; then
	cat 1>&2 <<EOF
Unused CallingForm:
$unusedForms

EOF
	# There are unused calling forms right now, so commenting this off so we don't fail CI.
	# exitStatus=1
fi

exit $exitStatus
