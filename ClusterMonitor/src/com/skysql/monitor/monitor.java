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
 * The base monitor class, it is from this class that all monitors are derived. 
 * The monitor class provides an SQL monitor that executes a SQL statement on 
 * the monitored database, this statement returns a single row and single column
 * result set that is the value of the monitor.
 * 
 * @author Mark Riddoch
 *
 */
public class monitor {
	/**
	 * The configuration database
	 */
	protected mondata		m_confdb;
	/**
	 * The ID of the monitor
	 */
	protected int			m_monitor_id;
	/**
	 * The Monitor key
	 */
	protected String		m_monitor_key;
	/**
	 * The node being monitored
	 */
	protected node			m_node;
	/**
	 * The SQL to execute
	 */
	protected String		m_sql;
	/**
	 * The last value returned by the probe
	 */
	protected String		m_lastValue;
	/**
	 * Is this a system averaged monitor
	 */
	protected boolean 		m_systemAverage;
	/**
	 * The monitor interval
	 */
	protected int			m_interval;
	
	/**
	 * The monitor constructor
	 * 
	 * @param db		The database handle for the monitoring database
	 * @param id		The ID of the monitor
	 * @param mon_node	The node being monitored
	 */
	public monitor(mondata db, int id, node mon_node)
	{
		m_confdb = db;
		m_monitor_id = id;
		m_monitor_key = m_confdb.getMonitorKey(m_monitor_id);
		m_node = mon_node;
		m_sql = db.getMonitorSQL(id);
		m_lastValue = null;
		m_systemAverage = db.isMonitorSystemAverage(id);
		m_interval = m_confdb.getMonitorClassInterval(m_monitor_key);
	}
	
	/**
	 * The probe function, called once per probe cycle.
	 * This is the method that get overridden by the custom monitor classes
	 * 
	 * @param verbose	Logging level
	 */
	public void probe(boolean verbose)
	{
		if (m_sql.isEmpty())
			return;
		String value = m_node.execute(m_sql);
		if (verbose)
		{
			System.out.println("probe: " + m_sql + " Last value " + m_lastValue + " new value " + value);
		}
		if (value == null)
		{
			value = "0";
		}
		saveObservation(value);
		m_lastValue = value;
	
	}
	
	/**
	 * Save an observed value for the monitor
	 * 
	 * @param observation	The observed value
	 * @return	True if updated
	 */
	protected boolean saveObservation(String observation)
	{
//		return m_confdb.monitorData(m_node.getSystemID(), m_node.getID(), m_monitor_id, observation);
		return m_node.saveObservation(m_monitor_id, observation);
	}
	
	/**
	 * Return the previous value the monitor reported
	 * 
	 * @return The previous value
	 */
	public String getValue()
	{
		return m_lastValue;
	}
	
	/**
	 * Return the ID of the monitor
	 * 
	 * @return The monitorID
	 */
	public int getID()
	{
		return m_monitor_id;
	}
	
	/**
	 * @param m_node the m_node to set
	 */
	public void setNode(node node) {
		this.m_node = node;
	}

	/**
	 * The monitor has a system value as well as individual node values
	 * 
	 * @return True if there is a system value
	 */
	public boolean hasSystemValue()
	{
		return true;
	}
	
	/**
	 * The system value may be either the sum of all the nodes in the system or
	 * the average of the nodes in the system
	 * 
	 * @return True if the system value of the monitor is an average of all the nodes
	 */
	public boolean isSystemAverage()
	{
		return m_systemAverage;
	}
}
