#! /bin/bash

set -e

source ./env.sh

cd ../soot-tools/trunk
./compile
./install
