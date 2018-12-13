#!/bin/bash

EPSILON_LIB=epsilon-1.4-core.jar
BPMN2_LIB=bpmn2-1.0.0.201407022025.jar

curl -L -o "$EPSILON_LIB" \
     http://ftp.halifax.rwth-aachen.de/eclipse//epsilon/interim-jars/epsilon-1.4-core.jar

curl -L -o "$BPMN2_LIB" \
     http://download.eclipse.org/modeling/mdt/bpmn2/updates/mars/1.0.0/plugins/org.eclipse.bpmn2_1.0.0.201407022025.jar

mvn -f pom-plain.xml install:install-file \
    "-Dfile=$EPSILON_LIB" -DgroupId=org.eclipse.epsilon \
    -DartifactId=epsilon-core -Dversion=1.4 -Dpackaging=jar

mvn -f pom-plain.xml install:install-file \
    "-Dfile=$BPMN2_LIB" -DgroupId=org.eclipse.mdt \
    -DartifactId=bpmn2 -Dversion=1.0.0.201407022025 -Dpackaging=jar
