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
import com.nesscomputing.syslog4j.SyslogFacility;
import com.nesscomputing.syslog4j.SyslogIF;

/**
 * Class to handle the centralized monitor logging.
 * 
 * @author Massimo Siani
 *
 */
public class Logging {
	/**
	 * Instance to handle the logging mechanism.
	 */
	private SyslogIF				m_syslog;
	/**
	 * Instance of this singleton.
	 */
	private static volatile Logging		INSTANCE = null;
	private String						m_host = "127.0.0.1";
	private String						m_protocol = "udp";
	private int							m_port = 514;
	
	/**
	 * @return	this instance
	 */
	private static Logging getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new Logging();
		}
		return INSTANCE;
	}
	/**
	 * @return	the syslog interface
	 */
	private static SyslogIF getSyslog() {
		return getInstance().m_syslog;
	}
	
	/**
	 * Set a host different from 127.0.0.1
	 * 
	 * @param host the log host
	 */
	public static Logging setHost(String host) {
		getInstance().m_host = host;
		getSyslog().getConfig().setHost(getInstance().m_host);
		return getInstance();
	}

	/**
	 * Set a protocol different from udp.
	 * 
	 * @param protocol the protocol to set
	 */
	public static Logging setProtocol(String protocol) {
		if (! Syslog.exists(protocol)) {
			return getInstance();
		}
		getInstance().m_protocol = protocol;
		INSTANCE = null;
		INSTANCE = new Logging();
		setHost(getInstance().m_host);
		return setPort(getInstance().m_port);
	}

	/**
	 * Set a port different from 514.
	 * 
	 * @param port the port to set
	 */
	public static Logging setPort(int port) {
		getInstance().m_port = port;
		getSyslog().getConfig().setPort(getInstance().m_port);
		return getInstance();
	}

	/**
	 * Constructor for the class.
	 */
	private Logging() {
		m_syslog = Syslog.getInstance(m_protocol);
		m_syslog.getConfig().setFacility(SyslogFacility.local6);
	}
	
	/**
	 * Log an info.
	 * 
	 * @param message		the string that will appear in the log
	 */
	public static void info(String message) {
		try {
			getSyslog().info(message);
		} catch (Exception e) {}
	}
	
	/**
	 * Log with error level.
	 * 
	 * @param message
	 */
	public static void error(String message) {
		try {
			getSyslog().error(message);
		} catch (Exception e) {}
	}
	
	/**
	 * Log with warning level.
	 * 
	 * @param message
	 */
	public static void warn(String message) {
		try {
			getSyslog().warn(message);
		} catch (Exception e) {}
	}
	/**
	 * Log with debug level.
	 * 
	 * @param message
	 */
	public static void debug(String message) {
		try {
			getSyslog().debug(message);
		} catch (Exception e) {}
	}

}
