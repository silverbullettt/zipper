#! /bin/bash
#
# Helper functions to set/get flags

export DOOP_FLAGS=""

function flag-isset()
{
    if [ $# -ne 1 ]; then
        echo "Usage: flag-isset {name}" >&2; exit 1
    fi

    local flag="$(checked-flag $1)"
    local i

    for i in ${DOOP_FLAGS//:/' '}; do
        if [[ $i == $flag ]]; then
            return 0
        fi
    done
    return 1
}

function flag-set()
{
    if [ $# -ne 1 ]; then
        echo "Usage: flag-set {name}" >&2; exit 1
    fi

    local flag="$(checked-flag $1)"

    if [[ -n $DOOP_FLAGS ]]; then
        DOOP_FLAGS="$DOOP_FLAGS:$flag"
    else
        DOOP_FLAGS="$flag"
    fi
}

function checked-flag()
{
    local valid_flags="solo averroes stats verbose color \
        sanity ssa phantom cache memlog setbased \
        refine client csv interactive incremental libonly"

    if [ $# -ne 1 ]; then
        echo "Usage: checked-flag {name}" >&2; exit 1
    fi

    local i

    # Here are where all valid flags are listed
    for i in $valid_flags
    do
        if [[ $i == $1 ]]; then
            echo "$1"
            return 0
        fi
    done

    # Invalid flag
    echo "ERROR: invalid flag $1" >&2; exit 1
}

function annotate-db-path()
{
    # Annotate paths with options that have an effect on fact generation
    suffix=""
    for i in ssa setbased averroes phantom; do
        if flag-isset $i; then
            suffix="${suffix}-$i"
        fi
    done

    echo "${1}${suffix}"
}

# Export functions
export -f flag-isset flag-set checked-flag annotate-db-path
