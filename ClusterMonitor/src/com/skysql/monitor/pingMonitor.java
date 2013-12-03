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

/**
 * A basic monitor class that implements ICMP pings as a monitor mechanism
 *
 * The only action of this monitor is to set the state of the node to Stopped
 * if it does not respond to 2 or more succesive pings
 * 
 * @author Mark Riddoch
 */
public class pingMonitor extends monitor {

	/**
	 * The number of ping failures
	 */
	int	m_failcnt;

	/**
	 * Constructor for the class
	 * 
	 * @param db		The monitoring database handle
	 * @param id		The ID of the monitor
	 * @param mon_node	The node being monitored
	 */
	public pingMonitor(mondata db, int id, node mon_node)
	{
		super(db, id, mon_node);
		m_failcnt = 0;
	}

	/**
	 * Execute a probe. Note that this monitor does not update the state after
	 * a single failure, 2 or more successive failures are required.
	 * 
	 * @param verbose The log level
	 */	
	public void probe(boolean verbose)
	{
		String value = "0";
		if (m_node.isReachable())
		{
			value = "1";
			m_failcnt = 0;
		}
		else
		{
			m_failcnt++;
		}
		if (m_failcnt > 1)
		{
			try {
				int state = m_confdb.getNodeStateId("machine-down");
				m_confdb.setNodeState(m_node.getID(), state);
			} catch (Exception e) {
				// IGNORE
			}
		}
	
		/*
		** Update the history of the probe results. Note that we only
		** insert new rows on change of state, not for every probe. If
		** the state has remained the same we merely update the last 
		** observed timestamp for the state.
		*/
		saveObservation(value);
		m_lastValue = value;
	}
}
