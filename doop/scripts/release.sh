#! /bin/bash

#
# Latest release for LogicBlox 3.4.1 : 956
#
# Current release for LogicBlox 3.7.3 : 958
#

set -e

source ./env.sh

top=$(pwd)

function svn-revision()
{
  local url=$1
  echo "958" # $(svn info $url | awk -f $top/export-revision.awk)"
}

function simple-src()
{
  local package=$1
  local url=$2

  cd $top/release
  revision=$(svn-revision $url)
  
  rm -rf ${package}-r${revision}-src
  svn export -r $revision $url ${package}-r${revision}-src > /dev/null
  tar -zcf ${package}-r${revision}-src.tar.gz ${package}-r${revision}-src
  cd $top

  cp release/${package}-r${revision}-src.tar.gz ../website/wwwroot/software
  echo "${package}-r${revision}"
}

function soot-fact-generation()
{
  local base=$(simple-src soot-fact-generation "https://svn.strategoxt.org/repos/pointer-analysis/soot-fact-generation/trunk")

  cd $top/release
  rm -rf $base-src
  tar zxf $base-src.tar.gz
  cd $base-src
  ./compile
  ./install
  cd $top
}

function jsglr-source-and-binary() {
  local revision=$(svn-revision "https://svn.strategoxt.org/repos/StrategoXT/spoofax/trunk/spoofax/org.spoofax.jsglr")

  rm -rf release/jsglr-r${revision}-src
  rm -rf release/jsglr-r${revision}-bin

  mkdir -p release/jsglr-r${revision}-src
  mkdir -p release/jsglr-r${revision}-bin

  # source
  cd release/jsglr-r${revision}-src
  jsglr-export
  cd ..
  tar -zcf jsglr-r${revision}-src.tar.gz jsglr-r${revision}-src

  # binary
  rm -rf release/jsglr-r${revision}-src
  tar -zxf jsglr-r${revision}-src.tar.gz
  cd jsglr-r${revision}-src
  ./compile
  cp build/jsglr.jar ../jsglr-r${revision}-bin
  cp *-LICENSE ../jsglr-r${revision}-bin
  cd ..
  tar -zcf jsglr-r${revision}-bin.tar.gz jsglr-r${revision}-bin
  cd $top

  cp release/jsglr-r${revision}-???.tar.gz ../website/wwwroot/software
}


function logicblox-unit()
{
  local base=$(simple-src logicblox-unit "https://svn.strategoxt.org/repos/pointer-analysis/logicblox-unit/trunk")

  cd $top/release
  rm -rf $base-src
  tar zxf $base-src.tar.gz
  cd $base-src
  ./compile
  ./install
  cd $top
}

function prepare-doop-binary()
{
  base=$1

  rm -rf $top/release/$base-bin
  mkdir -p $top/release/$base-bin
  cd $top/release/$base-bin
  tar zxf $top/release/$base-src.tar.gz
  mv $base-src/* .
  rmdir $base-src
}

function finish-doop-binary()
{
  base=$1

  cd $top/release
  tar -zcf $base-bin.tar.gz $base-bin
  cd $top

  cp release/$base-bin.tar.gz ../website/wwwroot/software
}

rm -rf release/*

doop=$(simple-src doop https://svn.strategoxt.org/repos/pointer-analysis/doop/trunk)

export DOOP_HOME=$top/release/$doop-bin
export OVERRIDE_DOOP_HOME=$top/release/$doop-bin
export JSGLR_HOME=$top/release/$doop-bin/lib
export JSGLR=$top/release/$doop-bin/lib/jsglr.jar

prepare-doop-binary $doop
cd $top
./soot-install.sh
soot-fact-generation
# TODO - we are using a pre-compiled jsglr.jar because the source code changed
# ./jsglr-install.sh
cp ../doop/trunk/lib/jsglr.jar $DOOP_HOME/lib
logicblox-unit
set -x
finish-doop-binary $doop

# jsglr-source-and-binary

# simple-src paddle-driver https://svn.strategoxt.org/repos/pointer-analysis/paddle-driver/trunk
# simple-src paddle-modified https://svn.strategoxt.org/repos/pointer-analysis/paddle-minimal
# simple-src doop-benchmark https://svn.strategoxt.org/repos/pointer-analysis/doop-benchmark/trunk
# simple-src doop-paddle-compat https://svn.strategoxt.org/repos/pointer-analysis/doop-paddle-compat/trunk

