#! /bin/bash
#
# Helper functions to create and access meta data.

function meta()
{
    if [ $# -ne 1 ]; then
        echo "Usage: meta {db}" >&2; exit 1
    fi

    echo "$1/.doop-meta"
}

function meta-which()
{
    if [ $# -ne 1 ]; then
        echo "Usage: meta-which {attribute}" >&2; exit 1
    fi

    local filename

    case $1 in
        cmd)
            filename="CMDLINE";;
        analysis)
            filename="ANALYSIS";;
        jre)
            filename="JRE";;
        injar)
            filename="INJAR";;
        deps)
            filename="DEPENDENCIES";;
        main)
            filename="MAINCLASS";;
        appregex)
            filename="APPLICATION_CLASSES";;
        *)
            echo "invalid attribute: $1" >&2; exit 1;;
    esac
    
    echo "$DOOP_HOME/tmp/$filename"
}

function meta-store()
{
    if [ $# -ne 2 ]; then
        echo "Usage: meta-store {attribute} {value}" >&2; exit 1
    fi

    echo "$2" > $( meta-which $1 )
}

function meta-load()
{
    if [ $# -ne 1 ]; then
        echo "Usage: meta-load {attribute}" >&2; exit 1
    fi

    cat $( meta-which $1 )
}

function meta-flush()
{
    local metadir="$(meta $1)"
    mkdir -p $metadir
    
    # Analysis type
    meta-load analysis > $metadir/analysis

    # JRE
    meta-load jre > $metadir/classlib

    # Main class
    meta-load main > $metadir/mainclass

    # Input Jar
    meta-load injar > $metadir/jars

    # External dependency jars
    meta-load deps > $metadir/deps

    # Original command-line arguments
	# TODO: check if existing file is same
    meta-load cmd > $metadir/command-line

    # Application classes
    meta-load appregex > $metadir/application-classes

    # Proprocessor flags
    preprocess-flags > $metadir/CPPFLAGS
}

# Export functions
export -f meta meta-which meta-load meta-store 
