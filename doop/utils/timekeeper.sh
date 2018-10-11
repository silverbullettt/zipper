#! /bin/bash
#
# Helper functions for counting time
# Exported variable <elapsedTime> will contain the value of the last timing.

export elapsedTime=""

function opt-timeout()
{
    if hash timeout 2>/dev/null; then
        timeout "$@"
    else
        echo >&2 \
            "Coreutils command 'timeout' is not installed." \
            "Running command without timeout..."
        shift; "$@"
    fi
}

function benchmark()
{
	local timeLimit=5400
    local errstream="$DOOP_HOME/tmp/TIMER"
    local outstream="$DOOP_HOME/tmp/OUTPUT"
	echo "..."
	printf "${C_YELLOW}Pointer analysis START${C_RESET}\n"

	set +e

    # Bash cannot interept signals while running a
    # subprocess synchronously. So, we run it asynchronously
    # and then wait for it to finish, after having set a trap
    # to kill it if SIGINT or SIGQUIT signal is received.

    trap 'echo you hit Ctrl-C/Ctrl-\, now exiting..; kill $! && exit' SIGINT SIGQUIT

    # Run command while recording its execution time, but force a timeout
	# opt-timeout $timeLimit /usr/bin/time -f "%e" "$@" 2> $errstream &       
    opt-timeout $timeLimit /usr/bin/time -f "%e" "$@" 2> $errstream 1> $outstream &
    wait $!

    # Get exit status of the last command executed in opt-timeout
    local rc=$?

    # Reset signal traps
    trap - SIGINT SIGQUIT 

    # Check if command was timed out
    if [[ $rc == 124 ]]; then
        printf >&2 "${C_RED}Timeout after %ss${C_RESET}\n" $timeLimit
    fi

    # Check exit status
    [[ $rc != 0 ]] && cat $errstream && exit $rc

	set -e

    # Read elapsed time as the last line of stderr
    elapsedTime=$( tail -n 1 $errstream)

    # Print error stream only inside verbose mode
    flag-isset verbose && head -n -1 $errstream

	printf "${C_WHITE}analysis time: ${C_GREEN}%ss${C_RESET}\n" "$elapsedTime"
	printf "${C_YELLOW}Pointer analysis FINISH${C_RESET}\n"
}


function timing()
{
    local errstream="$DOOP_HOME/tmp/TIMER"
	echo "..."
    set +e

    # Run command while recording its execution time
    /usr/bin/time -f "%e" "$@" 2> $errstream

    # Get exit status of time
    local rc=$?
    
    # Check exit status. If the program exited normally, the return value of time 
    # is the return value of the program it executed and
    # measured. Otherwise, the return value is 128 plus the number of
    # the signal which caused the program to stop or terminate.

    [[ $rc != 0 ]] && cat $errstream && exit $rc

    set -e

    elapsedTime=$( tail -n 1 $errstream)

    # Print error stream only inside verbose mode
    flag-isset verbose && head -n -1 $errstream

    echo "elapsed time: ${elapsedTime}s"
}
