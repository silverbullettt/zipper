#! /bin/bash
#
# Helper functions that serve as an interface to our preprocessor

export CPPFLAGS=""
export CPPFLAGS_CONSTANTS=""
export CPPFLAGS_EXCEPTIONS=""

function preprocess()
{
    cpp -CC -P `preprocess-flags` $*
}

function preprocess-append-flags()
{
    for flag in $@; do
        CPPFLAGS="$CPPFLAGS -D$flag"
    done
}

function preprocess-remove-flags()
{
    local TMPFLAGS

    # Can receive multiple patterns
    # such as: *FIELD_BASED*
    for pat in $@; do

        TMPFLAGS=""

        for flag in $CPPFLAGS; do
            if [[ ${flag#-D} != $pat ]]; then
                TMPFLAGS="$TMPFLAGS $flag"
            fi
        done

        CPPFLAGS="$TMPFLAGS"
    done
    unset TMPFLAGS
}

function preprocess-set-constant-flag()
{
    CPPFLAGS_CONSTANTS="-D$1"
}

function preprocess-set-exception-flags()
{
    CPPFLAGS_EXCEPTIONS=""

    for flag in $@; do
        CPPFLAGS_EXCEPTIONS="$CPPFLAGS_EXCEPTIONS -D$flag"
    done
}

function preprocess-flags()
{
    ## TODO: sort them in lexicographic order
    echo "$CPPFLAGS $CPPFLAGS_CONSTANTS $CPPFLAGS_EXCEPTIONS"
}
