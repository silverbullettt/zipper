#! /bin/sh

hostname="$(hostname)"

if test "${OVERRIDE_DOOP_HOME}x" = "x"; then
  export DOOP_HOME="$(pwd)/../doop/trunk"
else
  export DOOP_HOME="${OVERRIDE_DOOP_HOME}"
fi

case $hostname in
  "martin-laptop")
    export JDK_HOME=/usr/lib/jvm/java-6-sun
    ;;

  "scheme.logicblox.local")
    export JDK_HOME=/usr/java/latest
    ;;

  "codemonkey.cs.umass.edu")
    export JDK_HOME=/usr/java/latest
    ;;

  *)
    echo "hostname not recognized."
    export JDK_HOME="$(dirname $(dirname $(which java)))"
    echo "inferred JDK_HOME as $JDK_HOME"
    ;;
esac

function jsglr-export() {
  mkdir -p src
  svn export https://svn.strategoxt.org/repos/StrategoXT/spoofax/trunk/spoofax/org.spoofax.jsglr/src/ src/jsglr
  svn export https://svn.strategoxt.org/repos/StrategoXT/spoofax/trunk/spoofax/org.spoofax.aterm/src/ src/aterm
  svn export https://svn.strategoxt.org/repos/StrategoXT/spoofax/trunk/spoofax/org.spoofax.aterm/lib/ lib
  svn export https://svn.strategoxt.org/repos/StrategoXT/spoofax/trunk/spoofax/org.spoofax.jsglr/LICENSE
  mv LICENSE JSGLR-LICENSE
  svn export https://svn.strategoxt.org/repos/StrategoXT/spoofax/trunk/spoofax/org.spoofax.aterm/COPYING
  mv COPYING JSGLR-ATERM-LICENSE
  
  rm -rf src/jsglr/org/spoofax/server
  rm -rf src/jsglr/org/spoofax/shared
  
  cat >compile <<EOF
#! /bin/sh

set -e
jsglr=\$(find src/jsglr -name '*.java' | grep -v tests)
aterm=\$(find src/aterm -name '*.java')

rm -rf build
mkdir -p build/classes

javac -d build/classes -cp 'lib/*' \$jsglr \$aterm
(cd build/classes; jar xvf '../../lib/jjtraveler-0.4.3.jar')
(cd build/classes; jar xvf '../../lib/shared-objects-1.4.jar')

jar cvf build/jsglr.jar -C build/classes .

javadoc -d build/docs \$jsglr \$aterm
EOF

  chmod u+x compile
}
