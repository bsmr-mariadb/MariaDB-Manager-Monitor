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
import java.text.DecimalFormat;

import com.skysql.java.Logging;
import com.skysql.java.MonData;

/**
 * The DeltaMonitor is an Monitor class that monitors SQL values, the recorded
 * value is the difference from the previous value monitored and the current
 * Monitor.
 * 
 * @author Mark Riddoch
 *
 */
public class DeltaMonitor extends Monitor {
	/**
	 * The last value that the Monitor monitored
	 */
//	private		Long		lastAbsValue = null;
	
	/**
	 * Monitor constructor - all the work is done in the super class
	 * @param db		Handle on the database
	 * @param id		The ID of the Monitor
	 * @param mon_node	The Node beign monitored
	 */
	public DeltaMonitor(MonData db, int id, Node mon_node)
	{
		super(db, id, mon_node);
	}
	
	/**
	 * The probe function for the Monitor
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
			Logging.debug("probe: " + m_sql + " Last value " + m_lastValue + " new value " + value);
		}
		if (value != null)
		{
			if (m_lastAbsValue != null)
			{
				Float absValue = new Float(value);
				Long delta = (long) (absValue - m_lastAbsValue);
				if (delta < 0)
				{
					Logging.debug("Negative delta value for probe, absolute value is " + absValue + " last absolute value " + m_lastAbsValue);
					delta = new Long(0);
				}
				DecimalFormat format = new DecimalFormat("###############0");
				String deltaStr = format.format(delta.longValue());
				saveObservation(deltaStr);
				m_lastValue = deltaStr;
				m_lastAbsValue = absValue;
			}
			else
			{
				m_lastAbsValue = new Float(value);
			}
		}
	}

}
