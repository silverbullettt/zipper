#! /bin/bash
#
# Helper function to check jre compatibility

function check-jre()
{
    local JRE=""
    case $1 in
        "jre1.3")
            JRE="JRE13";;
        "jre1.4")
            JRE="JRE14";;
        "jre1.5")
            JRE="JRE15";;
        "jre1.6")
            JRE="JRE16";;
        "jre1.7")
            JRE="JRE17";;
        "system")
            JRE="JRE16";;
        *)
            echo "invalid class library: $1" >&2; usage
            exit 1;;
    esac

    preprocess-append-flags "$JRE"
    meta-store jre "$1"
    
    # Set the default jar to be analyzed (only useful 
    # for incremental analysis)

    meta-store injar "$INCR_EVAL_EMPTY"
}
