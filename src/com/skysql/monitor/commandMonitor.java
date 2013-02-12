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

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;

public class commandMonitor extends monitor {
	
	private int			m_id;
	private int 		m_rate;
	static	private		int POLERATIO = 5;
	private String		m_nodeIP;
	
	public commandMonitor(mondata db, int id, node mon_node)
	{
		super(db, id, mon_node);
		m_id = id;
		m_rate = POLERATIO;
		m_nodeIP = db.getNodePrivateIP(mon_node.getID());
	}
	
	public void probe(boolean verbose)
	{
		if (m_sql.isEmpty())
			return;
		if (m_rate++ < POLERATIO)
			return;
		m_rate = 0;
		
		String value = null;
		try {
			Process proc = Runtime.getRuntime().exec(m_sql + " + " + m_nodeIP);
			BufferedInputStream buffer = new BufferedInputStream(proc.getInputStream());
			BufferedReader commandOutput= new BufferedReader(new InputStreamReader(buffer));
	
			value = commandOutput.readLine().trim();
			commandOutput.close();
		}
		catch (Exception ex)
		{
			System.err.println("Command monitor exception: " + ex.getMessage() + " in monitor " + m_id);
		}
		if (value != null)
		{
			if (verbose)
			{
				System.out.println("probe: " + m_sql + " Last value " + m_lastValue + " new value " + value);
			}
	
			if (m_lastValue != null && m_lastValue.equals(value))
			{
				m_confdb.updateMonitorData(m_node.getID(), m_monitor_id, value);
			}
			else
			{
				if (m_lastValue != null)
					m_confdb.updateMonitorData(m_node.getID(), m_monitor_id, m_lastValue);
				m_confdb.insertMonitorData(m_node.getID(), m_monitor_id, value);
				m_lastValue = value;
			}
		}
	}
}
