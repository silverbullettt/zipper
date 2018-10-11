#! /bin/bash
#
# Helper function to measure disk footprint of database

function pollFootprint()
{
	local monitorDir="$1/"
	local pollingInterval=10

	local result=`du -csh $monitorDir | tail -n 1 | awk '{ print $1 }'`
	local length=${#result}
	local size=`expr substr $result 1 $((length - 1))`
	local magn=`expr substr $result $length 1`

	if [[ $magn = "M" ]]; then size=`echo "$size * 1024" | bc -q`
	elif [[ $magn = "G" ]]; then size=`echo "$size * 1024 * 1024" | bc -q` ; fi

	echo $size
}
