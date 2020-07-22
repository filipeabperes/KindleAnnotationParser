#!/bin/bash

javac Main.java
jar cvfe ../dist/KindleAnnotationParser.jar Main *.class
