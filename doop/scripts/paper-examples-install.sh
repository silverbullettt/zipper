#! /bin/bash

set -e

source ./env.sh
cd ..

topdir="$(pwd)"


function download-deps()
{
  mkdir -p lib
  cd lib
  rm -f *.jar
  wget http://plg.uwaterloo.ca/~olhotak/build/polyglot-1.3.3.jar
  wget http://plg.uwaterloo.ca/~olhotak/build/jedd-runtime.jar
  wget http://plg.uwaterloo.ca/~olhotak/build/jedd-translator.jar
  cd ..
}

function generate-settings()
{
cat > ant.settings <<EOF
## Location of Soot classes
soot.loc=$PA_ROOT/soot/sootclasses.jar

release.loc=.
soot.version=$1

## Location of Polyglot classes jar file to run Jedd with
polyglot.jedd.jar=lib/polyglot-1.3.3.jar

## Location of Jedd runtime classes jar file
jedd.runtime.jar=lib/jedd-runtime.jar

## Location of Jedd translator classes jar file
jedd.translator.jar=lib/jedd-translator.jar

sat.solver.cmd=$ZCHAFF_HOME/zchaff-script
EOF
}

function main()
{
  cd $topdir/paper-examples/trunk/jedd/simple
  download-deps
  generate-settings
}

main
