#! /bin/bash

BASH_COMPLETION_DIR="${BASH_COMPLETION_DIR:-/etc/bash_completion.d}"
EXTERNAL_MAN_PATH="/usr/local/share/man/man1/"

pushd $(dirname $0)/..
mkdir -p ${EXTERNAL_MAN_PATH}
cp man/man1/doop.1.gz ${EXTERNAL_MAN_PATH}
cp completion/doop ${BASH_COMPLETION_DIR}/
popd
