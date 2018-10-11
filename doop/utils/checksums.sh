#! /bin/bash
#
# Helper functions to handle checksums.

function init-checksums()
{
    inputfiles="$DOOP_HOME/tmp/INPUTFILES"
    logicfiles="$DOOP_HOME/tmp/LOGICFILES"

    > $inputfiles
    > $logicfiles
}

function append-files-inputsum()
{
    for i in "$@"; do
        if [[ ! -r $i ]]; then
            echo >&2 "ERROR: file $i does not exist"
            exit 1
        fi
        cat "$i" >> $inputfiles
    done
}

function append-files-logicsum()
{
    for i in "$@"; do
        if [[ ! -r $i ]]; then
            echo >&2 "ERROR: file $i does not exist"
            exit 1
        fi
        cat "$i" >> $logicfiles
    done
}

function compute-inputsum()
{
    local injar="$( meta-load injar )"
    local deps="$( meta-load deps )"
    local main="$( meta-load main )"
    local apps="$( meta-load appregex )"

    echo "$main $apps" | cat $inputfiles - $injar $deps | sha256sum | awk '{print $1}'
}

function compute-lib-inputsum()
{
    local injar="$INCR_EVAL_EMPTY"
    local deps="$EMPTY_JAR"
    local main="INCREMENTAL_BUILD_NO_MAIN"
    local apps=$( application-regex "$injar" "$deps")

    echo "$main $apps" | cat $inputfiles - $injar $deps | sha256sum | awk '{print $1}'
}

function compute-logicsum()
{
    local analysis="$( meta-load analysis )"

    # Create temporary file that holds client extension logic
    local logicexternal="$DOOP_HOME/tmp/external.logic"
    > "$logicexternal"

    if flag-isset client; then
        for i in `find "$DOOP_HOME/tmp/extensions/" -type f -name '*.logic'`; do
            cat "$i" >> "$logicexternal"
        done
    fi

    cat $logicexternal $logicfiles | sha256sum | awk '{print $1}'
    rm "$logicexternal"
}
