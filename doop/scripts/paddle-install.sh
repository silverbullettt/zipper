#! /bin/bash

set -e

source ./env.sh
cd ..

topdir="$(pwd)"

function checkout()
{
  svn checkout https://svn.sable.mcgill.ca/soot/paddle/trunk/ paddle-$1
}

function download-deps()
{
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

function install()
{
cat > install <<EOF
#! /bin/sh

mkdir -p $PADDLE_HOME
echo "copying jar archive to $PADDLE_HOME"
cp paddle-$1.jar $PADDLE_HOME
echo "copying jedd-runtime support to $PADDLE_HOME"
cp lib/jedd-runtime.jar $PADDLE_HOME
EOF

  chmod u+x install
  ./install
}

function paddle-driver()
{
  ./compile
  ./install
}

function zchaff()
{
  rm -rf $ZCHAFF_HOME
  mkdir -p $ZCHAFF_HOME
  cd $ZCHAFF_HOME
  
  machine_name=$(uname -m | tr 'A-Z ' 'a-z_')
  case $machine_name in
    i*86)
        machine_name=i686
        ;;
    x86_64)
        machine_name=x86_64
        ;;
    *)
	echo "error: unknown CPU $machine_name"
	exit 1
        ;;
  esac

  if test "$machine_name" = "x86_64"; then
    wget http://www.ee.princeton.edu/~chaff/zchaff/zchaff.64bit.2007.3.12.zip
    unzip zchaff.64bit.2007.3.12.zip
    mv zchaff64 zchaff
    cd zchaff
    make
  else
    wget http://www.ee.princeton.edu/~chaff/zchaff/zchaff.2008.10.12.zip
    unzip zchaff.2008.10.12.zip
    cd zchaff
    make
  fi

  cd ..
  cat > zchaff-script <<EOF
#! /bin/sh

\$ZCHAFF_HOME/zchaff/zchaff \$* | sed 's/Random.*//g'
EOF

  chmod u+x zchaff-script
}

function buddy()
{
  rm -rf build-externals/buddy
  mkdir -p build-externals/buddy
  cd build-externals/buddy

  wget http://internap.dl.sourceforge.net/sourceforge/buddy/buddy-2.4.tar.gz
  tar zxf buddy-2.4.tar.gz
  cd buddy-2.4
  ./configure
  cd ..

  wget http://www.sable.mcgill.ca/software/jedd-0.4.tar.gz
  tar zxf jedd-0.4.tar.gz

  rm -rf build
  mkdir build

  cp jedd-0.4/runtime/generated/jbuddy_wrap.c build
  cp jedd-0.4/runtime/csrc/jbuddy.c build
  cp jedd-0.4/runtime/csrc/jbuddy.h build

  cp buddy-2.4/config.h build
  cp buddy-2.4/src/*.c build
  cp buddy-2.4/src/*.h build

  cd build
  for file in *.c; do
    libtool --mode=compile gcc -c -O3 -I$JDK_HOME/include -I$JDK_HOME/include/linux -I. $file
  done
  libtool --mode=link gcc -avoid-version -rpath $PADDLE_HOME -o libjeddbuddy.la *.lo
  libtool --mode=install cp libjeddbuddy.la $PADDLE_HOME/libjeddbuddy.la
}

function main()
{
  zchaff

  cd $topdir/paddle-modified
  download-deps
  generate-settings modified
  ant classesjar
  install modified

  cd $topdir/paddle-minimal
  download-deps
  generate-settings minimal
  ant classesjar
  install minimal

  cd $topdir/scripts
  buddy

  cd $topdir/paddle-driver/trunk
  ./compile
  ./install
}

main
