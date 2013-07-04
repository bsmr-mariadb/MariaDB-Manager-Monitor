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

/**
 * The deltaMonitor is an monitor class that monitors SQL values, the recorded
 * value is the difference from the previous value monitored and the current
 * monitor.
 * 
 * @author Mark Riddoch
 *
 */
public class deltaMonitor extends monitor {
	/**
	 * The last value that the monitor monitored
	 */
	private		Long		lastAbsValue = null;
	
	/**
	 * Monitor constructor - all the work is done in the super class
	 * @param db		Handle on the database
	 * @param id		The ID of the monitor
	 * @param mon_node	The node beign monitored
	 */
	public deltaMonitor(mondata db, int id, node mon_node)
	{
		super(db, id, mon_node);
	}
	
	/**
	 * The probe function for the monitor
	 * 
	 * @param verbose 	Enable verbose logging of true
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
				saveObservation(deltaStr);
				m_lastValue = deltaStr;
				lastAbsValue = absValue;
			}
			else
			{
				lastAbsValue = new Long(value);
			}
		}
	}
}
