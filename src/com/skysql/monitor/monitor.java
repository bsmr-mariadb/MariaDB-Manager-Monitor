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

public class monitor {
	protected mondata		m_confdb;
	protected	int			m_monitor_id;
	protected node			m_node;
	protected String		m_sql;
	protected String		m_lastValue;
	protected boolean 		m_systemAverage;
	
	public monitor(mondata db, int id, node mon_node)
	{
		m_confdb = db;
		m_monitor_id = id;
		m_node = mon_node;
		m_sql = db.getMonitorSQL(id);
		m_lastValue = null;
		m_systemAverage = db.getMonitorSystemAverage(id);
	}
	
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
	
	protected boolean saveObservation(String observation)
	{
		return m_confdb.monitorData(m_node.getSystemID(), m_node.getID(), m_monitor_id, observation);
	}
	
	public String getValue()
	{
		return m_lastValue;
	}
	
	public int getID()
	{
		return m_monitor_id;
	}
	
	public boolean hasSystemValue()
	{
		return true;
	}
	
	public boolean isSystemAverage()
	{
		return m_systemAverage;
	}

}
