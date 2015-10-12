#!/bin/sh

# Script to compare differences of versions of code in git with
# google3. Performs file name mapping and excludes package and
# import declarations as they do likely differ.

# Can be run for a file a directory tree. For a tree, its intended
# to be used in the src root (e.g. src/main/java).

G3SRC=/google/src/head/depot/google3

if [ "$1" == "-n" ]
then
  shift
  NO_KDIFF="yes"
fi


# Helper functions

function strip_file() {
  this=$1
  strip=$2
  sed -e '/^import .*;/ d' -e '/^package .*;/ d' -e '/^$/ d' <$1 >$2
}

function diff_files() {
  dot_slash="./"
  this_file=${1/${dot_slash}}
  this_path=$2
  that_path=$3
  this_pkg=$4
  that_pkg=$5

  if [ ! -z "${this_path}" ]
  then
    that_file="${G3SRC}/${file/${this_path}/${that_path}}"
  else
    that_file="${G3SRC}/${file}"
  fi

  if [ -f "${that_file}" ]
  then
    strip_this_file=`mktemp`
    strip_that_file=`mktemp`
    strip_file ${this_file} ${strip_this_file}
    strip_file ${that_file} ${strip_that_file}
    if [ "`diff ${strip_this_file} ${strip_that_file}`" != "" ]
    then
      if [ -z "${NO_KDIFF}" ]
      then
        read -p \
          "file ../${this_file/${this_path}} differs in g3; show diff? [yN] " \
          yn
        case ${yn} in
          [Yy]*)
            show="yes"
            ;;
          *)
            show=""
        esac
      else
        show=""
      fi
      if [ ! -z "${show}" ]
      then
        kdiff3 --fname "G3 ${that_file}" --fname "G3 ${this_file}" \
          ${strip_that_file} ${strip_this_file}
      else
        echo "file ../${this_file/${this_path}} differs in g3"
      fi
    fi
  else
    echo "file ../${this_file/${this_path}} not in g3"
  fi
}

# Determine whether to check a file or a directory
SUBJECT=$1
if [ -z "${SUBJECT}" ]
then
  SUBJECT=.
fi

if [ -d "${SUBJECT}" ]
then
  FILES="`find ${SUBJECT} -type f -print`"
else
  FILES=${SUBJECT}
fi

for file in ${FILES}
do
  case $file in
    *io/gapi/fx*)
      diff_files $file io/gapi/fx/ java/com/google/api/tools/framework/
      ;;
    *io/gapi/gax*)
      diff_files $file io/gapi/gax/ java/com/google/api/common/
      ;;
    *)
      diff_files $file
  esac
done
