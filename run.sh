#!/usr/bin/env bash

WORKSPACE=$(cd `dirname $0`; pwd)
MO_YAML=$WORKSPACE/mo.yml
RUN_YAML=$WORKSPACE/run.yml
LIB_WORKSPACE=$WORKSPACE/lib


function boot {
local libJars libJar
for libJar in `find ${LIB_WORKSPACE} -name "*.jar"`
do
  libJars=${libJars}:${libJar}
done
java -Xms1024M -Xmx1024M -cp ${libJars} \
        -Dconf.yml=${MO_YAML} \
        -Drun.yml=${RUN_YAML} \
        io.mo.Tester
}

boot