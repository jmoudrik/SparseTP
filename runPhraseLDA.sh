#!/bin/bash
# run bash2snippet from bash
set -e
lib="lib"
src="src"
classes="out/production"

LibPath=${lib}/hppc-0.7.1.jar:${lib}/org.json-20120521.jar
javac -classpath ${classes}:$LibPath -sourcepath ${src} -d ${classes} ${src}/topicmodel/PhraseLDA.java

java -classpath ${classes}:$LibPath topicmodel/PhraseLDA