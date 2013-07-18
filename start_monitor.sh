#!/bin/bash

java -DSKYSQL_APIKEYID=1 -DSKYSQL_APIKEY=1f8d9e040e65d7b105538b1ed0231770 -classpath ClusterMonitor.jar com.skysql.monitor.ClusterMonitor -v 1 /usr/local/skysql/SQLite/AdminConsole/admin
