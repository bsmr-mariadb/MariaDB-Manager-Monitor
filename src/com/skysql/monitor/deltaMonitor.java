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
import java.text.DecimalFormat;

public class deltaMonitor extends monitor {
	private		Long		lastAbsValue = null;
	
	public deltaMonitor(mondata db, int id, node mon_node)
	{
		super(db, id, mon_node);
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
		if (value != null)
		{
			if (lastAbsValue != null)
			{
				Long	absValue = new Long(value);
				Long delta = absValue - lastAbsValue;
				if (delta < 0)
				{
					System.out.println("Negative delta value for probe, absolute value is " + absValue + " last absolute value " + lastAbsValue);
					delta = new Long(0);
				}
				DecimalFormat format = new DecimalFormat("###############0");
				String deltaStr = format.format(delta.longValue());
				
				if (m_lastValue != null && m_lastValue.equals(deltaStr))
				{
					m_confdb.updateMonitorData(m_node.getID(), m_monitor_id, deltaStr);
				}
				else
				{
					if (m_lastValue != null)
						m_confdb.updateMonitorData(m_node.getID(), m_monitor_id, m_lastValue);
					m_confdb.insertMonitorData(m_node.getID(), m_monitor_id, deltaStr);
					m_lastValue = deltaStr;
					lastAbsValue = absValue;
				}
			}
			else
			{
				lastAbsValue = new Long(value);
			}
		}
	}
}
