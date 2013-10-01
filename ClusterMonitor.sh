#!/bin/bash
cd /usr/local/skysql/monitor
cd $(dirname $0)

PATH=${PATH}:/usr/sbin

#jars_path=`find /usr/local/skysql/skysql_aws/*.jar | tr '\n' ':' `

java -classpath ./ClusterMonitor.jar com.skysql.monitor.ClusterMonitor -v all &
echo $! > ClusterMonitor.pid
