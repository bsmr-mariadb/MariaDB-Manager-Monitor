#!/bin/sh
. /usr/local/skysql/skysql_aws/skysql.config
cd /usr/local/skysql/monitor

PATH=${PATH}:/usr/sbin

nohup java -classpath ClusterMonitor.jar com.skysql.monitor.ClusterMonitor $system_id /usr/local/skysql/SQLite/AdminConsole/admin > /var/log/SkySQL-ClusterMonitor.log 2>&1 &
