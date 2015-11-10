#!/bin/sh
# Script for setting up a GAPI development environment.
# Call in the directory where you want to install the GAPI tree
if [[ `pwd` == $HOME ]]
then
  echo "You probably don't want to enroll in your home dir."
  exit 1
fi
git clone https://github.com/wrwg/protobuf-gradle-plugin.git || exit 1
(cd protobuf-gradle-plugin && ./gradlew install) || exit 1
git clone --recursive sso://gapi/gapi-gax-java || exit 1
(cd gapi-gax-java && ./gradlew build install) || exit 1
git clone --recursive sso://gapi/gapi-tools || exit 1
(cd gapi-tools && ./gradlew build install) || exit 1
git clone --recursive sso://gapi/gapi-core-java || exit 1
(cd gapi-core-java && ./gradlew build install) || exit 1
git clone --recursive sso://gapi/gapi-example-library-java || exit 1
(cd gapi-example-library-java && ./gradlew build)
