#!/bin/bash
cd /usr/local/skysql/monitor
cd $(dirname $0)

PATH=${PATH}:/usr/sbin

#jars_path=`find /usr/local/skysql/skysql_aws/*.jar | tr '\n' ':' `

nohup java -classpath ./ClusterMonitor.jar com.skysql.monitor.ClusterMonitor -v all > /var/log/SkySQL-ClusterMonitor.log 2>&1 &
echo $! > ClusterMonitor.pid
