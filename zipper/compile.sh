if [ ! -d "build" ]; then
 mkdir build
else
 rm -rf build/*
fi

CP="lib/sootclasses-2.5.0.jar:lib/guava-23.0.jar"

javac -classpath $CP $(find src -name "*.java") -d build
jar -cvf zipper.jar -C build .
mv zipper.jar build
