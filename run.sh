#!/usr/bin/env bash

if [[ $# -eq 0 ]];then
    echo "No parameters provided,the mo-tester will run with parameters defined in the run.yml file. "

fi
while getopts ":p:m:t:r:i:e:s:gnch" opt
do
    case $opt in
        p)
        PATHC="path=${OPTARG}"
        echo -e "The path of the cases that need to be executed by mo-tester  : ${OPTARG}"
        ;;
        m)
        METHOD="method=${OPTARG}"
        echo -e "The method that mo-tester will run with :${OPTARG}"
        ;;
        t)
        TYPE="type=${OPTARG}"
        echo -e "The type of the format that mo-tester execute the sqlcommand in : ${OPTARG}"
        ;;
        r)
        RATE="rate=${OPTARG}"
        echo -e "The success rate that test cases should reach : ${OPTARG}"
        ;;
        i)
        INCLUDE="include=${OPTARG}"
        echo -e "Only script files in the path which name contain one of the : {${OPTARG}} will be executed"
        ;;
        e)
        EXCLUDE="exclude=${OPTARG}"
        echo -e "Script files in the path which name contain one of the : {${OPTARG}} will be not executed"
        ;;
        s)
        RESOURCE="resource=${OPTARG}"
        echo -e "Script files in the path which name contain one of the : {${OPTARG}} will be not executed"
        ;;
        g)
        IGNORE="ignore"
        echo -e "SQL commands which is marked with ignore-flag will not be executed"
        ;;
        n)
        NOMETA="nometa"
        echo -e "The meta data of the resultset will be ignored when comparing the resut"
        ;;
        c)
        CHECK="check"
        echo -e "The meta data of the resultset will be ignored when comparing the resut"
        ;;
        h)
        echo -e "Usage:　bash run.sh [option] [param] ...\nExcute test cases task"
        echo -e "   -p  set the path of test cases needed to be executed by mo-tester"
        echo -e "   -m  set the method that mo-tester will run with"
        echo -e "   -t  set the type of the format that mo-tester execute the sqlcommand in"
        echo -e "   -r  set The success rate that test cases should reach"
        echo -e "   -i  set the including list, and only script files in the path which name contain one of the list will be excuted,if more than one,seperated by ,"
        echo -e "   -e  set the excluding list, and script files in the path which name contain one of the list will not be excuted,if more than one,seperated by ,"
        echo -e "   -s  set the resource path that mo-tester use to store resources, and can be refered to $resources in test file"
        echo -e "   -g  means SQL commands which is marked with [bvt:issue] flag will not be executed,this flag starts with [-- @bvt:issue#{issueNO.}],and ends with [-- @bvt:issue],eg:"
        echo -e "       -- @bvt:issue#3236"
        echo -e "       select date_add("1997-12-31 23:59:59",INTERVAL "-10000:1" HOUR_MINUTE);"
        echo -e "       select date_add("1997-12-31 23:59:59",INTERVAL "-100 1" YEAR_MONTH);"
        echo -e "       -- @bvt:issue"
        echo -e "       Those two sql commands are associated with the issue#3236,and they will not been executed in bvt test,until the flag is removed when the issue#3236 is fixed."
        echo -e "   -n  means the meta data of the resultset will be ignored when comparing the resut"
        echo -e "   -c  check whether the case scripts match the result file"
        echo -e "Examples:"
        echo "   bash run.sh -p case -m run -t script -r 100 -i select,subquery -e substring -g"
        echo "For more support,please email to dong.su@matrixorigin.io"
        exit 1
        ;;
        ?)
        echo "Unkown parameter,please use -h to get help."
        exit 1;;
    esac
done

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
        io.mo.Tester ${PATHC} ${METHOD} ${TYPE} ${RATE} ${INCLUDE} ${EXCLUDE} ${IGNORE} ${NOMETA} ${CHECK} ${RESOURCE}
}

boot
