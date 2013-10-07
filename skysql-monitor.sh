#!/bin/sh

sed -i 's/#$ModLoad imudp/$ModLoad imudp/' /etc/rsyslog.conf
sed -i 's/#$UDPServerRun 514/$UDPServerRun 514/' /etc/rsyslog.conf
