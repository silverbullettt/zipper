#! /bin/bash
#
# Helper function to check required environment variables

function pathmunge () {
    if ! echo $PATH | /bin/egrep -q "(^|:)$1($|:)" ; then
        PATH=$1:$PATH
    fi
}

function check-env()
{
    if [[ -z $LOGICBLOX_HOME ]]; then
	    printf "ERROR: please set the environment variable LOGICBLOX_HOME\n" >&2
	    exit 1
    fi

    LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$LOGICBLOX_HOME/bin
    bloxbatch="$LOGICBLOX_HOME/bin/bloxbatch"

    if [[ ! -e $bloxbatch ]]; then
	    printf "ERROR: \$LOGICBLOX_HOME/bin/bloxbatch does not exist. \$LOGICBLOX_HOME = %s\n" "$LOGICBLOX_HOME" >&2
	    exit 1
    fi
    
    pathmunge $LOGICBLOX_HOME/bin

    # Export commonly used variables
    export PATH LD_LIBRARY_PATH bloxbatch
}
