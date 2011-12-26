#!/bin/sh
JAVAFX_RT="$JAVAFX_HOME/rt"

mvn install:install-file \
  -Dfile=$JAVAFX_RT/lib/jfxrt.jar \
  -DgroupId=com.oracle \
  -DartifactId=javafx-runtime \
  -Dpackaging=jar \
  -Dversion=2.0

mkdir bin
cp $JAVAFX_RT/bin/* bin/
#JAVAFX_M2=~/.m2/repository/com/oracle/javafx-runtime
#mkdir $JAVAFX_M2/bin
#cp $JAVAFX_RT/bin/* $JAVAFX_M2/bin

