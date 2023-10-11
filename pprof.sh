#!/usr/bin/env bash
if [ $# -eq 0 ] ; then
    echo "No parameters provided,please use -H to get help. " >&2
    exit 2
fi

PORT=6060
SERVER="127.0.0.1"
DURATION=30
SLEEP=60
WORKSPACE=$(cd `dirname $0`; pwd)

while getopts ":h:p:d:s:H" opt
do
    case $opt in
        h)
        SERVER="${OPTARG}"
        echo -e "`date +'%Y-%m-%d %H:%M:%S'` The server addr is : ${OPTARG}"
        ;;
        p)
        expr ${OPTARG} "+" 10 &> /dev/null
        if [ $? -ne 0 ]; then
          echo "`date +'%Y-%m-%d %H:%M:%S'` The port [${OPTARG}] is not a number"
          exit 1
        fi
        PORT=${OPTARG}
        echo -e "`date +'%Y-%m-%d %H:%M:%S'` The debug port is : ${OPTARG}"
        ;;
        d)
        DURATION=${OPTARG}
        echo -e "`date +'%Y-%m-%d %H:%M:%S'` The collection duration is : ${OPTARG} s"
        ;;
        s)
        SLEEP=${OPTARG}
        echo -e "`date +'%Y-%m-%d %H:%M:%S'` The sleep time for each turn is : ${OPTARG} s"
        ;;
        H)
        echo -e "Usage:　bash pprof.sh [option] [param] ...\nExcute pprof task"
        echo -e "   -h  server address, default value is 127.0.0.1"
        echo -e "   -p  server port, default value is 6060"
        echo -e "   -d  the duration that pprof collection will last once, unit s"
        echo -e "   -s  the sleeptime for each turn, unit s"
        echo -e "Examples:"
        echo "   bash pprof.sh -h 127.0.0.1 -p 6060 -d 10"
	      echo "For more support,please email to sudong@matrixorigin.cn"
        exit 1
        ;;
        ?)
        echo "Unkown parameter,please use -H to get help."
        exit 1;;
    esac
done

if [ ! -d ${WORKSPACE}/report/prof/${SERVER} ] ; then
    mkdir ${WORKSPACE}/report/prof/${SERVER}
fi

time=`date +'%Y-%m-%d_%H-%M-%S'`
mkdir -p ${WORKSPACE}/report/prof/${SERVER}/${time}
curl http://${SERVER}:${PORT}/debug/pprof/goroutine?debug=2 -o ${WORKSPACE}/report/prof/${SERVER}/${time}/goroutine.log
curl http://${SERVER}:${PORT}/debug/pprof/trace?seconds=30 -o ${WORKSPACE}/report/prof/${SERVER}/${time}/trace.out
if [ $? -ne 0 ];then
  echo -e "`date +'%Y-%m-%d_%H:%M:%S'` The MO debug service can not be reached, the pprof operation was failed."
  exit 1
fi

