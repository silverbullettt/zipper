#! /bin/bash
#
# Helper functions to run analysis and created the database. It does
# not export anything since it is intended to be invoked only from
# the main script.

# Variables created and passed implicitly (without being exported) 
# are: cachefacts cachedatabase humandatabase database
#
# Variables required: bloxbatch analysis

function init-analysis()
{
    mkdir -p $DOOP_HOME/tmp

    preprocess $DOOP_HOME/logic/$analysis/declarations.logic $DOOP_HOME/tmp/${analysis}-declarations.logic
    preprocess $DOOP_HOME/logic/$analysis/delta.logic $DOOP_HOME/tmp/${analysis}-delta.logic
    preprocess $DOOP_HOME/logic/library/reflection-delta.logic $DOOP_HOME/tmp/reflection-delta.logic
    preprocess $DOOP_HOME/logic/client/exception-flow-delta.logic $DOOP_HOME/tmp/exception-flow-delta.logic
    preprocess $DOOP_HOME/logic/$analysis/analysis.logic $DOOP_HOME/tmp/${analysis}.logic
    preprocess $DOOP_HOME/logic/client/auxiliary-heap-allocations-delta.logic $DOOP_HOME/tmp/auxiliary-heap-allocations-delta.logic

    # Mark as cache dependencies

    append-files-logicsum \
        $DOOP_HOME/tmp/${analysis}-declarations.logic \
        $DOOP_HOME/tmp/$analysis-delta.logic \
        $DOOP_HOME/tmp/reflection-delta.logic \
        $DOOP_HOME/tmp/exception-flow-delta.logic \
        $DOOP_HOME/tmp/$analysis.logic \
        $DOOP_HOME/tmp/auxiliary-heap-allocations-delta.logic

    local injar="$( meta-load injar )"
    local jre="$( meta-load jre )"

    # Compute sums

    local inputsum=$( compute-inputsum )
    local logicsum=$( compute-logicsum )

    cachefacts="`annotate-db-path $DOOP_HOME/cache/input-facts/$jre`/${inputsum}"
    cachedatabase="`annotate-db-path $DOOP_HOME/cache/input-database/$jre`/${inputsum}"
    database="`annotate-db-path $DOOP_HOME/cache/analysis/${logicsum}/$jre`/${inputsum}"
    humandatabase="`annotate-db-path $DOOP_HOME/results/$analysis/$jre`/`basename $injar`"
}

function create-database()
{
    rm -rf $database

    local main="$( meta-load main )"
    local injar="$( meta-load injar )"
    local regex="$( meta-load appregex )"

    for ign in 1
    do
        # Skip if cached database already exists

        if [[ -e $cachedatabase ]] && flag-isset cache; then
            echo "using cached database (${cachedatabase}/)"
            break
        fi

        # Generate facts

        if [[ -e $cachefacts ]] && flag-isset cache; then
            echo "using cached facts (${cachefacts}/)"
        elif flag-isset csv; then
            mkdir -p $cachefacts
            mv $DOOP_HOME/tmp/export/* $cachefacts
        else
            echo -n "generating facts ($injar $( meta-load deps )) in ${cachefacts}/ "
            local linked

            rm -rf $DOOP_HOME/tmp/facts
            mkdir -p $DOOP_HOME/tmp/facts

            # Run jphantom if phantom refs not allowed
            if ! flag-isset phantom; then
                # Ensure that no dependencies were specified
                if [[ $( meta-load deps ) == ${EMPTY_JAR} ]]
                then
                    echo "Running JPhantom..."
                
                    # Create complemented jar
                    complement=$( ${DOOP_HOME}/bin/complement-phantoms ${DOOP_HOME}/tmp )

                    echo "JPhantom finished executing. Complement created: $complement"

                    # new complemented jar location
                    injar="$complement"

                    # Remove complemented jar on exit
                    trap "rm -rf $complement" EXIT
                fi
            fi

            # Run Averroes
            if flag-isset averroes; then

                local properties="averroes.properties"
                local column="input_jar_files"

                # Extract properties file
                jar xf $AVERROES $properties

                # Change input jar in place
                sed -i.bak -e "s/^${column}[[:space:]]*=.*$/${column} = ${injar}/" $properties

                # Renew averroes' properties file
                jar uf $AVERROES $properties

                # Remove temporary files
                rm $properties ${properties}.bak

                # Run averroes
                java -jar $AVERROES

                # Set input jar and dependencies before calling soot
                linked="-l ${AVERROES_OUTDIR}/placeholderLibrary.jar"
                injar="${AVERROES_OUTDIR}/organizedApplication.jar"

                # Remove averroes output jars on exit
                trap "rm -rf ${AVERROES_OUTDIR}" EXIT
            else
                linked="$( ${DOOP_HOME}/bin/jre-link-arguments $( meta-load jre ) ) -l $( meta-load deps )"
            fi

            # Determine Soot's fact generation flags

            local sootFactGenArgs="-full $linked -application-regex $regex "

            if flag-isset ssa; then
                sootFactGenArgs="$sootFactGenArgs -ssa"
            fi

            if flag-isset phantom; then
                sootFactGenArgs="$sootFactGenArgs -allow-phantom"
            fi

            if ! flag-isset libonly; then
                sootFactGenArgs="$sootFactGenArgs -main $main"
            fi

            # Run Soot

            timing java -cp ${SOOT_CLASSES}:${SOOT_FACT_GEN} Main \
                $sootFactGenArgs -d $DOOP_HOME/tmp/facts $injar

            mkdir -p $cachefacts
            mv $DOOP_HOME/tmp/facts/* $cachefacts
        fi

        # Generate database (edb only)

        mkdir -p $(dirname $cachedatabase)

        if flag-isset incremental
        then
            local jre="$( meta-load jre )"
            local inputsum=$( compute-lib-inputsum )
            local logicsum=$( compute-logicsum )

            libdatabase="`annotate-db-path $DOOP_HOME/cache/analysis/${logicsum}/${jre}-phantom`/${inputsum}"

            echo "Searching for $libdatabase"

            if [[ ! -d $libdatabase ]]; then
                echo "Preanalyzed library database is missing! Exiting..." >&2 
                exit 1
            fi
            
            # TODO: more checking .doop_meta of libdatabase
            
            echo -n "copying precomputed database of library from ${libdatabase}/ "
            timing cp -R $libdatabase/* $cachedatabase
        else
            echo -n "creating database in ${cachedatabase}/ "
            timing $bloxbatch -db $cachedatabase -create -overwrite -blocks base

            echo -n "loading fact declarations "
            timing $bloxbatch -db $cachedatabase -addBlock -file $DOOP_HOME/logic/library/fact-declarations.logic
        fi

        echo -n "loading facts "
        rm -rf $(dirname $cachedatabase)/facts
        ln -s $cachefacts $(dirname $cachedatabase)/facts

        touch $(dirname $cachedatabase)/facts/ApplicationClass.facts

        if flag-isset csv; then
            ${DOOP_HOME}/gen-import -csv $DOOP_HOME/tmp/fact-declarations.import
        else
            ${DOOP_HOME}/gen-import -text $DOOP_HOME/tmp/fact-declarations.import
        fi
        timing $bloxbatch -db $cachedatabase -import $DOOP_HOME/tmp/fact-declarations.import

        rm $(dirname $cachedatabase)/facts

        if ! flag-isset libonly; then
            echo "setting main class to $main"
            $bloxbatch -db $cachedatabase -execute "+MainClass(x) <- ClassType(x), Type:Value(x:\"$main\")."
        fi

        # Run set-based logic
        flag-isset setbased && ${DOOP_HOME}/bin/set-based "$cachedatabase" 4
    done

    mkdir -p $(dirname $database)
    cp -R $cachedatabase $database
}

function analyze()
{
    local dynamics="$1"; shift
    local opts="$@"

    for dynamic in $dynamics; do

        local fields=`cat $dynamic | head -n 1 | awk '{ print NF; }'`

        # Skip dynamic files with wrong arity
        if [[ $fields -ne 2 ]]; then
            continue
        fi
        
        cat > $DOOP_HOME/tmp/dynamic.import <<EOF
option,delimiter,"\t"
option,hasColumnNames,false

fromFile,"$(readlink -f $dynamic)",a,inv,b,type
toPredicate,Config:DynamicClass,type,inv
EOF
        $bloxbatch -db $database -import $DOOP_HOME/tmp/dynamic.import
    done

    if ! flag-isset incremental; then
        echo -n "loading $analysis declarations "
        timing $bloxbatch -db $database -addBlock -file $DOOP_HOME/tmp/${analysis}-declarations.logic

        if flag-isset sanity; then
            echo -n "loading sanity rules "
            timing $bloxbatch -db $database -addBlock -file $DOOP_HOME/logic/library/sanity.logic
        fi
    fi

    echo -n "loading $analysis delta rules "
    timing $bloxbatch -db $database -execute -file $DOOP_HOME/tmp/${analysis}-delta.logic
    echo -n "loading reflection delta rules "
    timing $bloxbatch -db $database -execute -file $DOOP_HOME/tmp/reflection-delta.logic
    echo -n "loading client delta rules "
    timing $bloxbatch -db $database -execute -file $DOOP_HOME/tmp/exception-flow-delta.logic
    echo -n "loading auxiliary delta rules "
    timing $bloxbatch -db $database -execute -file $DOOP_HOME/tmp/auxiliary-heap-allocations-delta.logic

    echo -n "loading $analysis rules "

    # Read Zipper context-sensitive methods
	if [ ! "x$zipper" = "x" ]; then
	#if test "$zipper" = "true"; then
		printf "\nloading ${C_YELLOW}Zipper precision-critical methods${C_RESET} from %s ...\n" $zipper
		#echo "loading Zipper precision-critical methods ... "
		cat > tmp/zipper.import <<EOF
option,delimiter,"	"
option,hasColumnNames,false

fromFile,"$(readlink -f $zipper)",a,method
toPredicate,ZipperContextSensitiveMethod,method
EOF
		$bloxbatch -db $database -import $(pwd)/tmp/zipper.import
	fi

    # Read reflection log
	if [ ! "x$tamiflex" = "x" ]; then
        python scripts/reflection.py $tamiflex $cachefacts tmp
		# printf "\nloading ${C_YELLOW}reflection log${C_RESET} from $tamiflex ... "
		
		# Class.newInstance
		cat > tmp/classnewinstance.import <<EOF
option,delimiter,"	"
option,hasColumnNames,false

fromFile,"$(readlink -f tmp/ClassNewInstance.log)",a,invo,b,type
toPredicate,ClassNewInstance:Log,invo,type
EOF
		$bloxbatch -db $database -import $(pwd)/tmp/classnewinstance.import
		
		# Constructor.newInstance
		cat > tmp/constructornewinstance.import <<EOF
option,delimiter,"	"
option,hasColumnNames,false

fromFile,"$(readlink -f tmp/ConstructorNewInstance.log)",a,invo,b,constructor
toPredicate,ConstructorNewInstance:Log,invo,constructor
EOF
		$bloxbatch -db $database -import $(pwd)/tmp/constructornewinstance.import
		
		# Method.invoke
		cat > tmp/methodinvoke.import <<EOF
option,delimiter,"	"
option,hasColumnNames,false

fromFile,"$(readlink -f tmp/MethodInvoke.log)",a,invo,b,method
toPredicate,MethodInvoke:Log,invo,method
EOF
		$bloxbatch -db $database -import $(pwd)/tmp/methodinvoke.import
	fi

    # Log memory statistics
    if flag-isset memlog; then
        vmstat -n 3 > $(meta $database)/vmstat.log &
        vmstatPid="$!"
        trap "kill $vmstatPid" INT TERM
    fi

    # Run refinement logic
    flag-isset refine && ${DOOP_HOME}/bin/refine "$analysis" "$database"

    if ! flag-isset incremental; then
        # Run main analysis logic
        benchmark $bloxbatch -db $database -addBlock -file $DOOP_HOME/tmp/${analysis}.logic $opts

        # Store elapsed time as database metadata
        $bloxbatch -db $database -execute "+Stats:Runtime(${elapsedTime}, \"benchmark time\")."
    fi

    # Loading extensions
    if flag-isset client; then
        echo -n "loading client extensions"

        # Read one filename per line
        while read extension; do
            timing $bloxbatch -db $database -addBlock -file "$extension" $opts
        done < "${DOOP_HOME}/tmp/extensions"
    fi

    # Kill memory logger
    if flag-isset memlog; then
        kill $vmstatPid
        trap - INT TERM
    fi
}

function reanalyze()
{
    # Same arguments as analyze

    echo "loading $analysis refinement-delta rules "
    preprocess $DOOP_HOME/logic/$analysis/refinement-delta.logic $DOOP_HOME/tmp/${analysis}-refinement-delta.logic

    timing $bloxbatch -db $database -execute -file $DOOP_HOME/tmp/${analysis}-refinement-delta.logic
    timing $bloxbatch -db $database -exportCsv TempSiteToRefine -overwrite -exportDataDir $DOOP_HOME/tmp -exportFilePrefix ${analysis}-
    timing $bloxbatch -db $database -exportCsv TempNegativeSiteFilter -overwrite -exportDataDir $DOOP_HOME/tmp -exportFilePrefix ${analysis}-
    timing $bloxbatch -db $database -exportCsv TempObjectToRefine -overwrite -exportDataDir $DOOP_HOME/tmp -exportFilePrefix ${analysis}-
    timing $bloxbatch -db $database -exportCsv TempNegativeObjectFilter -overwrite -exportDataDir $DOOP_HOME/tmp -exportFilePrefix ${analysis}-
    
    create-database
    write-meta
    flag-set refine
    analyze "$@"
}

function link-result()
{
    printf "${C_WHITE}making database available at ${C_GREEN}%s${C_RESET}\n" "$humandatabase"
    mkdir -p $(dirname $humandatabase)
    rm -rf $humandatabase

    ln -s $database $humandatabase

    printf "${C_WHITE}making database available at ${C_GREEN}last-analysis${C_RESET}\n"
    rm -f $DOOP_HOME/last-analysis

    ln -s $(readlink $humandatabase) $DOOP_HOME/last-analysis
}

function write-meta()
{
    meta-flush "$database"
}
