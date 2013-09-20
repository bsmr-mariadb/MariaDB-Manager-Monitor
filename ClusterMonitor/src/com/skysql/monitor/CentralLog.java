/*
 * This file is distributed as part of the SkySQL Cloud Data Suite.  It is free
 * software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation,
 * version 2.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Copyright SkySQL Ab
 */

package com.skysql.monitor;

import com.nesscomputing.syslog4j.Syslog;
import com.nesscomputing.syslog4j.SyslogIF;

/**
 * Class to handle the centralized monitor logging.
 * 
 * @author Massimo Siani
 *
 */
public class CentralLog {
	private SyslogIF		m_syslog;
	
	public CentralLog(String host, String protocol, int port) {
		if (! Syslog.exists(protocol)) {
			m_syslog = null;
			return;
		}
		m_syslog = Syslog.getInstance(protocol);
		m_syslog.getConfig().setHost(host);
		m_syslog.getConfig().setPort(port);
	}
	
	public CentralLog(String protocol) {
		this("127.0.0.1", protocol, 514);
	}

	public CentralLog() {
		this("127.0.0.1", "udp", 514);
	}
	
	

}
