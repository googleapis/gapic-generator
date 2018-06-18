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

## Check for unhandled cases.
unhandledStr='$unhandledCallingForm'
if grep --include='*.baseline' -r -l -F $unhandledStr src/; then
	cat 1>&2 <<EOF
Above baseline files contain string "$unhandledStr";
some sample code won't render properly.

EOF
	exit 1
fi
