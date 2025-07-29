#!/bin/bash

javac ClassWithNoPackage.java -d external_classes
javac CustomClassLoader.java ClassLoaderTest.java

echo "Compilation complete."

java -cp . ClassLoaderTest
