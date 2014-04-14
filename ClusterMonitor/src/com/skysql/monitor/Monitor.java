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
 * The base Monitor class, it is from this class that all monitors are derived. 
 * The Monitor class provides an SQL Monitor that executes a SQL statement on 
 * the monitored database, this statement returns a single row and single column
 * result set that is the value of the Monitor.
 * 
 * @author Mark Riddoch
 *
 */
public class Monitor {
	/**
	 * The configuration database
	 */
	protected MonData		m_confdb;
	/**
	 * The ID of the Monitor
	 */
	protected int			m_monitor_id;
	/**
	 * The Monitor key
	 */
	protected String		m_monitor_key;
	/**
	 * The Node being monitored
	 */
	protected Node			m_node;
	/**
	 * The SQL to execute
	 */
	protected String		m_sql;
	/**
	 * The last value returned by the probe
	 */
	protected String		m_lastValue;
	/**
	 * Is this a system averaged Monitor
	 */
	protected boolean 		m_systemAverage;
	/**
	 * The Monitor interval
	 */
	protected Integer		m_interval;
	/**
	 * The Monitor should record differences between the consecutive
	 * probe cycles.
	 */
	protected boolean		m_delta;
	/**
	 * The last value probed if this is a delta Monitor
	 */
	protected Float			m_lastAbsValue = null;
	
	/**
	 * The Monitor constructor
	 * 
	 * @param db		The database handle for the monitoring database
	 * @param id		The ID of the Monitor
	 * @param mon_node	The Node being monitored
	 */
	public Monitor(MonData db, int id, Node mon_node)
	{
		m_confdb = db;
		m_monitor_id = id;
		m_monitor_key = m_confdb.getMonitorKey(m_monitor_id);
		m_node = mon_node;
		m_sql = m_confdb.getMonitorSQL(id);
		m_lastValue = null;
		m_systemAverage = m_confdb.isMonitorSystemAverage(id);
		m_interval = m_confdb.getMonitorClassInterval(m_monitor_key);
		if (m_interval == null)	m_interval = 30;
		m_delta = m_confdb.isMonitorDelta(m_monitor_id);
	}
	
	/**
	 * Gets the Monitor interval.
	 * 
	 * @return		the Monitor interval
	 */
	public int getInterval() {
		return m_interval;
	}
	
	/**
	 * The probe function, called once per probe cycle.
	 * This is the method that get overridden by the custom Monitor classes
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
			Logging.debug("probe: " + m_sql + " Last value " + m_lastValue + " new value " + value);
		}
		if (value == null)
		{
			value = "0";
		}
		saveObservation(value);
		m_lastValue = value;
	}
	
	/**
	 * Save an observed value for the Monitor
	 * 
	 * @param observation	The observed value
	 * @return	True if updated
	 */
	protected boolean saveObservation(String observation)
	{
		return m_node.saveObservation(m_monitor_id, observation);
	}
	
	/**
	 * Return the previous value the Monitor reported
	 * 
	 * @return The previous value
	 */
	public String getValue()
	{
		return m_lastValue;
	}
	
	/**
	 * Return the ID of the Monitor
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
	public void setNode(Node node) {
		this.m_node = node;
	}

	/**
	 * The Monitor has a system value as well as individual Node values
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
	 * @return True if the system value of the Monitor is an average of all the nodes
	 */
	public boolean isSystemAverage()
	{
		return m_systemAverage;
	}
}
