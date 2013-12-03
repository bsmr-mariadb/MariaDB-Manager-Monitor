/*
 * This file is distributed as part of the MariaDB Enterprise.  It is free
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
 * Copyright 2012-2014 SkySQL Ab
 */
package com.skysql.monitor;

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;

import com.skysql.java.Logging;

/**
 * The commandMonitor is an instance of a monitor class designed to execute
 * external programs as sources of monitoring data.
 * 
 * @author Mark Riddoch
 *
 */
public class commandMonitor extends monitor {
	
	/** The monitor ID */
	private int			m_id;	
	/** The rate to poll, i.e. number of cycles
	 *  between polls
	 */
	private int 		m_rate;	
	/** Number of polls between command execution */
	static	private		int POLERATIO = 5;	
	// private String		m_nodeIP;
	
	/**
	 * The constructor for the command monitor. Must of the work is
	 * down by the super class, monitor, we only need to set a couple
	 * of local member variables.
	 * 
	 * @param db		The Monitor database
	 * @param id		The ID of the monitor 
	 * @param mon_node	The node we are monitoring
	 */
	public commandMonitor(mondata db, int id, node mon_node)
	{
		super(db, id, mon_node);
		m_id = id;
		m_rate = POLERATIO;
		// m_nodeIP = db.getNodePrivateIP(mon_node.getID());
	}
	
	/**
	 * The probe function, called once per probe cycle.
	 * 
	 * We only actually execute the command once per POLERATIO calls,
	 * this allows the command execution to be throttled back as it is
	 * relatively expensive.
	 * 
	 * TODO: The command is run locally currently, it should be run on
	 * the node m_node
	 * 
	 * @param verbose	Verbose or normal logging required
	 */
	public void probe(boolean verbose)
	{
		if (m_sql.isEmpty())
			return;
		if (m_rate++ < POLERATIO)
			return;
		m_rate = 0;
		
		String value = null;
		try {
			Process proc = Runtime.getRuntime().exec(m_sql + " " + m_node);
			BufferedInputStream buffer = new BufferedInputStream(proc.getInputStream());
			BufferedReader commandOutput= new BufferedReader(new InputStreamReader(buffer));
	
			value = commandOutput.readLine().trim();
			commandOutput.close();
		}
		catch (Exception ex)
		{
			Logging.error("Command monitor exception: " + ex.getMessage() + " in monitor " + m_id);
		}
		saveObservation(value);
		m_lastValue = value;
	}
}
