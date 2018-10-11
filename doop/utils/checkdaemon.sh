#! /bin/bash
#
# Helper function to check if only one instance of bloxbatch is running

function check-daemon()
{
	bloxbatchRunning=`ps -ef | grep bloxbatch | { grep -v grep || true; }`
	if [ "x${bloxbatchRunning}" != "x" ]; then
		echo -n 'WARNING: Bloxbatch is already running. ' >&2
		read -p "Continue (y/n)? "
		if [[ $REPLY != "y" ]]; then
			echo "Terminating..."
			exit
		fi
		echo
	fi
}
