#! /bin/bash

set -e

source ./env.sh


if test "$OVERRIDE_DOOP_HOME" != ""; then
  DOOP_HOME="$OVERRIDE_DOOP_HOME"
fi

top=$(pwd)

rm -rf build-externals/jsglr
mkdir -p build-externals/jsglr
cd build-externals/jsglr
jsglr-export
./compile

cp build/jsglr.jar $DOOP_HOME/lib
mv *-LICENSE $DOOP_HOME/lib/
