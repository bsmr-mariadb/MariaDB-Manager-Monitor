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

/**
 * nodeStateMonitor - a monitor class that implements a SQL based method to update the state of a node within
 * the monitored set of systems. The result set will contain a single value which is the state to use for the
 * particular node.
 * 
 * @author Mark Riddoch
 */
public class nodeStateMonitor extends monitor {
	
	/**
	 * Constructor for the node state monitor, all the work is done in the
	 * super class 
	 * 
	 * @param db		The monitoring database
	 * @param id		The ID of the monitor
	 * @param mon_node	The node being monitored
	 */
	public nodeStateMonitor(mondata db, int id, node mon_node)
	{
		super(db, id, mon_node);
	}
	
	/**
	 * The probe entry for the monitor
	 * 
	 * @param verbose	The logging verbosity level
	 */
	public void probe(boolean verbose)
	{
		if (m_sql.isEmpty())
			return;
		String value = m_node.execute(m_sql);
		int nodeNo = m_node.getID();
		if (value == null) 	/* Failed to get status return from node */
		{
			value = "107";	/* Temporary kludge to have a stopped state - need to get this from config */
		}
		if (verbose)
		{
			System.out.println("probe: " + m_sql + " new value " + value);
		}
		try {
			m_confdb.setNodeState(nodeNo, (new Integer(value)).intValue());
		} catch (Exception ex) {
			System.err.println("Can not set node state of " + value + " or node " + nodeNo);
		}
		saveObservation(value);
		m_lastValue = value;
	}
	
	/**
	 * The node state has no corresponding system value
	 * 
	 * @return false
	 */
	public boolean hasSystemValue()
	{
		return false;
	}
}
