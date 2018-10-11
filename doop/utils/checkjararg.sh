#! /bin/bash
#
# Helper function to check jar argument

function check-injar()
{
    # process the jar file argument
    if [[ -d $1 ]]; then
        printf "ERROR: directories are currently not supported\n" >&2
        exit 1
    fi
    if [[ ! -e $1 ]]; then
        printf "ERROR: jar file not found \"$1\"\n" >&2
        exit 1
    fi
    if [[ ! -r $1 ]]; then
        printf "ERROR: no read permission for \"$1\"\n" >&2
        exit 1
    fi

    if [ $# -ne 3 ]; then
        printf "Usage: check-injar <JAR> <MAIN> <DYN>" >&2
        exit 1
    fi

    local injar="$1"
    local main="$2"
    # local dynamics="$3"

    if [[ $(basename $(dirname $(readlink -f $injar))) = "dacapo" ]]; then
        # for dacapo we need to link with the deps: some of the deps
        # themselves do not have all deps (e.g. pmd).
        
        local benchmark=$(basename $injar .jar)
        printf "${C_WHITE}running dacapo benchmark: ${C_GREEN}%s${C_RESET}\n" "$benchmark"
        
        local deps="${injar/%.jar/-deps.jar}"
        local dynamic="${injar/%.jar/.dynamic}"

        # Try different suffix
        if [[ ! -e $dynamic ]]; then
            dynamic="${injar/%.jar/.dyn}"
        fi

        # Append dynamic file to outer dynamics
        if [[ -r $dynamic ]]; then
            eval "$3=\"$dynamic \$$3\""
        fi

        if [[ -z $main ]]; then
            main="dacapo.$benchmark.Main"
        fi
    else
        # Try extracting the main class from the manifest file
        if [[ -z $main ]]; then
            jar xf $injar META-INF/MANIFEST.MF

            main=`grep '^Main-Class: ' META-INF/MANIFEST.MF | \
                cut -d: -f2 | \
                sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//'`

            rm -rf META-INF/
        fi

        if [[ -z $main ]]; then
            local klass=$(basename $injar .jar)

            jar xf $injar $klass

            if [[ -e ${klass}.class ]]; then
                main=$klass
                rm ${klass}.class
            fi
        fi
    fi

    # Store main class, injar, dependency jar, as metadata
    meta-store main  "${main:?no main class has been specified}"
    meta-store injar "$injar"
    meta-store deps  "${deps:-${EMPTY_JAR}}"
}
