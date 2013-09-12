#!/bin/bash

cd $(dirname $0)
read < ClusterMonitor.pid
kill -9 $REPLY
rm ClusterMonitor.pid
