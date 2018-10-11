#! /bin/bash

set -e

source ./env.sh

cd ../soot-fact-generation/trunk
./compile
./install
