#! /bin/bash

function usage()
{
	local analyses=`ls "${DOOP_HOME}/logic" | grep sensitive | \
        grep -v '.logic' | awk '{ printf "%s  ", $1 }' | \
        fold -s | awk '{ print "   " $0 }'`
	cat <<EOF
Usage: doop [OPTION]... ANALYSIS JAR
       doop [OPTION]... ANALYSIS JAR -- [BLOXOPTION]...

Analysis:
$analyses

Options:
  -main CLASS        Specify the main class
  -c, --cache        The analysis will use the cached input relations, if such exist
  -ssa               Use ssa transformation for input.
  --transform-input  Transform input by removing redundant instructions.
  -jre VERSION       One of 1.3, 1.4, 1.5, 1.6 (default: system)
  -jre1.3            Use jre1.3 (default: system)
  -jre1.4            Use jre1.4 (default: system)
  -jre1.5            Use jre1.5 (default: system)
  -jre1.6            Use jre1.6 (default: system)
  --color            Enable colors in script output
  -i, --interactive  Enable interactive mode
  --allow-phantom    Allow non-existent referenced jars
  --solo-run         Perform checks to ensure no instance of bloxbatch is already running
  --log-mem-stats    Log virtual memory statistics (currently Linux only, uses vmstat)
  --full-stats       Load additional logic for collecting statistics
  --sanity           Load additional logic for sanity checks
  --averroes         Use averroes tool to create a placeholder library
  -tamiflex FILE     File with tamiflex data (multiple occurences disallowed)
  -dynamic FILE      File with tab-separated data for Config:DynamicClass (multiple occurences allowed)
  -client PATH       Additional directory/file of client analysis to include
  -h, --help         Display help and exit

Additional Options:
  --distinguish-class-string-constants (default)
  --distinguish-all-string-constants
  --distinguish-no-string-constants
  --merge-string-buffers
  --no-context-repeat
  --field-based-static
  --field-based-dynamic
  --paddle-compat
  --enable-imprecise-exceptions
  --disable-merge-exceptions
  --disable-reflective-methods
  --disable-reflection
  --enable-exception-flow

Common Bloxbatch Options:
  -logicProfile N   Profile the execution of logic, show the top N predicates
  -logLevel LEVEL   Log the execution of logic at level LEVEL (for example: all)

See man page for more info.

Report issues to martin.bravenboer@acm.org
EOF
}
