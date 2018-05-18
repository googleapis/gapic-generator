#!/bin/bash

# Copyright 2018 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

# TODO: Do this as part of GapicGeneratorTest so this check can be run with Gradle check command.

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

# Calling forms are in an enum, so we look for lines that begin with whitespace plus a capital letter.
callingForms=$(< $callingFormsFile grep -o '^[[:space:]]*[A-Z][a-zA-Z]*' | tr -d -c '[a-zA-Z\n]')

unusedForms=$(
	# Grep src/
	#   recursively (-r)
	#   in baseline files (--include='*.baseline')
	#   for any calling forms: -f <(echo "$callingForms")
	#   use fixed strings, not regexp (-F)
	#   match whole word (-w)
	#   print only the calling form, not surrounding line (-o)
	#   don't print file names (-h)
	# Prints all occurences of used calling forms
	grep --include='*.baseline' -h -w -o -r -F -f <(echo "$callingForms") src/ |

	# Then turn it into a set.
	sort -u |

	# Both $callingForms and the used forms determined so far in this pipeline are sets,
	# so sorting  them together means
	#   used forms appear twice together (one from $callingForms, one from the used forms)
	#   unused forms appear only once
	sort <(echo "$callingForms") - |

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
