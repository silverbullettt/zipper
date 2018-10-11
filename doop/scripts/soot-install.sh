#! /bin/bash

set -e

source ./env.sh

rm -f sootclasses-2.3.0.jar
wget http://www.sable.mcgill.ca/software/sootclasses-2.3.0.jar

cp sootclasses-2.3.0.jar $DOOP_HOME/lib

svn export https://svn.sable.mcgill.ca/soot/soot/trunk/COPYING-LESSER.txt
mv COPYING-LESSER.txt $DOOP_HOME/lib/SOOT-LICENSE
