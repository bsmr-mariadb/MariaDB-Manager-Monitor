#!/bin/sh

cp SkySQL-monitor-syslog.conf /etc/rsyslog.d/SkySQL-monitor-syslog.conf
sed -i 's/#$ModLoad imudp/$ModLoad imudp/' /etc/rsyslog.conf
sed -i 's/#$UDPServerRun 514/$UDPServerRun 514/' /etc/rsyslog.conf
