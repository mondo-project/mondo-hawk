#!/bin/bash

MODELIO_MM_LIB=ModelioMetamodelLib-1.0.1.jar
EPSILON_LIB=epsilon-1.3-core.jar
BPMN2_LIB=bpmn2-1.0.0.201407022025.jar

curl -L -o "$MODELIO_MM_LIB" \
     https://github.com/aabherve/modelio-metamodel-lib/releases/download/v1.0.1/ModelioMetamodelLib-1.0.1.jar

curl -L -o "$EPSILON_LIB" \
     http://ftp.halifax.rwth-aachen.de/eclipse//epsilon/interim-jars/epsilon-1.3-core.jar

curl -L -o "$BPMN2_LIB" \
     http://download.eclipse.org/modeling/mdt/bpmn2/updates/mars/1.0.0/plugins/org.eclipse.bpmn2_1.0.0.201407022025.jar

mvn -f pom-plain.xml install:install-file \
    "-Dfile=$MODELIO_MM_LIB" -DgroupId=org.modelio \
    -DartifactId=modelio-metamodel-lib -Dversion=1.0.1 -Dpackaging=jar

mvn -f pom-plain.xml install:install-file \
    "-Dfile=$EPSILON_LIB" -DgroupId=org.eclipse.epsilon \
    -DartifactId=epsilon-core -Dversion=1.3 -Dpackaging=jar

mvn -f pom-plain.xml install:install-file \
    "-Dfile=$BPMN2_LIB" -DgroupId=org.eclipse.mdt \
    -DartifactId=bpmn2 -Dversion=1.0.0.201407022025 -Dpackaging=jar
