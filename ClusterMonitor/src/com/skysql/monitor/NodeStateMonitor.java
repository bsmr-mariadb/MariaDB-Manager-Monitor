/*
 * This file is distributed as part of the MariaDB Manager.  It is free
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
 * Copyright 2012-2014 SkySQL Corporation Ab
 */


package com.skysql.monitor;

import com.skysql.java.Logging;

/**
 * NodeStateMonitor - a Monitor class that implements a SQL based method to update the state of a Node within
 * the monitored set of systems. The result set will contain a single value which is the state to use for the
 * particular Node.
 * 
 * @author Mark Riddoch
 */
public class NodeStateMonitor extends Monitor {
	
	/**
	 * Constructor for the Node state Monitor, all the work is done in the
	 * super class 
	 * 
	 * @param db		The monitoring database
	 * @param id		The ID of the Monitor
	 * @param mon_node	The Node being monitored
	 */
	public NodeStateMonitor(MonData db, int id, Node mon_node)
	{
		super(db, id, mon_node);
	}
	
	/**
	 * The probe entry for the Monitor
	 * 
	 * @param verbose	The logging verbosity level
	 */
	public void probe(boolean verbose)
	{
		if (m_sql.isEmpty())
			return;
		String value = m_node.execute(m_sql);
		int nodeNo = m_node.getID();
		if (value == null) 	/* Failed to get status return from Node */
		{
			value = "100";	/* Temporary kludge to have a stopped state - need to get this from config */
		}
		if (verbose)
		{
			Logging.debug("probe: " + m_sql + " new value " + value);
		}
		try {
			m_confdb.setNodeState(nodeNo, (new Integer(value)).intValue());
		} catch (Exception ex) {
			Logging.error("Can not set Node state of " + value + " or Node " + nodeNo);
		}
		saveObservation(value);
		m_lastValue = value;
	}
	
	/**
	 * The Node state has no corresponding system value
	 * 
	 * @return false
	 */
	public boolean hasSystemValue()
	{
		return false;
	}
}
