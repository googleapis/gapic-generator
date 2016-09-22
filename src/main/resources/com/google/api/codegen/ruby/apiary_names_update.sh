#!/usr/bin/env bash

# BEFORE RUNNING --- see installation instructions at:
# https://g3doc.corp.google.com/apiserving/vkit/tools/snippetgen/update/g3doc/index.md?cl=head#ruby
# https://github.com/google/google-api-ruby-client/blob/master/README.md

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
APIARY_NAMES=$DIR/apiary_names.yaml
mv $APIARY_NAMES $APIARY_NAMES.old

ROOT=`git rev-parse --show-toplevel`
DISCO_DIR=$ROOT/src/test/java/com/google/api/codegen/testdata/discoveries

TMPDIR=$(mktemp -d)
DISCO_FILES=($(ls $DISCO_DIR/*.json))
generate-api gen $TMPDIR --names_out=$APIARY_NAMES --file=${DISCO_FILES[*]}
rm -r $TMPDIR
