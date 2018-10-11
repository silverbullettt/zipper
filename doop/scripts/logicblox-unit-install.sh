#! /bin/bash

set -e

source ./env.sh

export JSGLR=$(pwd)/../doop/trunk/lib/jsglr.jar

cd ../logicblox-unit/trunk
./compile
./install
