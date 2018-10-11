#! /bin/bash
#
# Helper functions for storing and displaying statistics

function show-stats()
{
    local database="$1"

    # Minimum statistics
	$DOOP_HOME/bin/stats-simple $database

    # Additional Statistics
    if flag-isset stats; then
		$DOOP_HOME/bin/stats-full $database
	fi
}

function run-stats()
{
    local database="$1"

	echo -n "loading statistics (simple) "
	preprocess $DOOP_HOME/logic/library/statistics-simple.logic $DOOP_HOME/tmp/statistics-simple.logic

	timing $bloxbatch -db $database -addBlock -file $DOOP_HOME/tmp/statistics-simple.logic
	$bloxbatch -db $database -execute "+Stats:Runtime($elapsedTime, \"loading statistics (simple) time\")."

    if flag-isset stats; then
		echo -n "loading statistics "
		preprocess $DOOP_HOME/logic/library/statistics.logic $DOOP_HOME/tmp/statistics.logic
		timing $bloxbatch -db $database -addBlock -file $DOOP_HOME/tmp/statistics.logic
		$bloxbatch -db $database -execute "+Stats:Runtime($elapsedTime, \"loading statistics time\")."
		echo "sorting predicates ..."
		local start=`date +%s`
		$bloxbatch -db $database -addBlock -file $DOOP_HOME/logic/library/statistics-sort.logic

		sortPredicate $database Stats:VarCount
		sortPredicate $database Stats:InsensHeapVarCount
		sortPredicate $database Stats:InsensBaseVarCount
		sortPredicate $database Stats:InsensVarCount

		sortPredicate $database Stats:ArrayCount
		sortPredicate $database Stats:InsensHeapArrayCount
		sortPredicate $database Stats:InsensBaseHeapArrayCount
		sortPredicate $database Stats:InsensArrayCount

		sortPredicate $database Stats:FieldCount
		sortPredicate $database Stats:InsensHeapFieldCount
		sortPredicate $database Stats:InsensBaseHeapFieldCount
		sortPredicate $database Stats:InsensFieldCount

		#sortPredicate $database Stats:ThrowsPerMethodCount
		#sortPredicate $database Stats:InsensHeapThrowsPerMethodCount
		#sortPredicate $database Stats:InsensMethodThrowsPerMethodCount
		#sortPredicate $database Stats:InsensThrowsPerMethodCount

		sortPredicate $database Stats:MethodContextCount
		sortPredicate $database Stats:MethodContextCount SORT_BASE
		#sortPredicate $database Stats:InsensMethodVarCount
		#sortPredicate $database Stats:MethodVarCount

		local end=`date +%s`
		let elapsedTime="$end - $start"
		echo "elapsed time: ${elapsedTime}s"
		$bloxbatch -db $database -execute "+Stats:Runtime($elapsedTime, \"sorting statistics time\")."
	fi
}

function sortPredicate()
{
    local database="$1"
	local input="$2"
	local output=${input}Sorted
	local sortFlag=""

	if [[ $# -eq 3 ]]; then
		if [ "x$2" = "xSORT_BASE" ]; then
			output=${output}Base
			sortFlag=$2
		fi
	fi

	$bloxbatch -db $database -exportCsv $output -overwrite -exportDataDir $DOOP_HOME/tmp -exportScriptDir $DOOP_HOME/tmp -keepDerivedPreds

	output=`echo $output | sed -r 's/:/_/g'`

	if test "x$sortFlag" = "xSORT_BASE"; then
		$bloxbatch -db $database -query $input | awk 'BEGIN { FS = "," } ; { print $NF " @ " $0 }' | sort -n | sed -r 's/^.*@[ ](.*)\,[0-9]+$/\1/' | awk '{ print FNR","$0 }' >> $DOOP_HOME/tmp/$output.csv
	else
		$bloxbatch -db $database -query $input | awk 'BEGIN { FS = "," } ; { print $NF }' | sort -n | awk '{ print FNR","$0 }' >> $DOOP_HOME/tmp/$output.csv
	fi

	$bloxbatch -db $database -import $DOOP_HOME/tmp/$output.import
}
