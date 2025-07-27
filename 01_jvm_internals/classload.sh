#!/bin/bash

javac classloading_mechanism/ClassLoadingDemo.java classloading_mechanism/MyClass.java classloading_mechanism/MyClassNoInline.java

# ClassLoadingDemo is an entry point of the program
java -verbose:class -cp . classloading_mechanism.ClassLoadingDemo