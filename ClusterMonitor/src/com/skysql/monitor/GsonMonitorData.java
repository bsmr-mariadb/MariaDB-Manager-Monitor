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

import java.util.List;

/**
 * Monitor data.
 * 
 * @author Massimo Siani
 *
 */
public class GsonMonitorData extends GsonErrors {
	private MonitorData monitor_data;
	private MonitorData monitor_rawdata;
	
	/**
	 * @return the monitor_data
	 */
	private MonitorData getMonitor_data() {
		return monitor_data;
	}
	/**
	 * @return the monitor_rawdata
	 */
	private MonitorData getMonitor_rawdata() {
		return monitor_rawdata;
	}
	
	/**
	 * Get the instance.
	 * 
	 * @return	the MonitorData object, null if not set
	 */
	public MonitorData getMonitorData() {
		if (getMonitor_data() != null) return getMonitor_data();
		else if (getMonitor_rawdata() != null) return getMonitor_rawdata();
		else return null;
	}

	public static class MonitorData {
		private List<String> timestamp;
		private List<String> value;
		private List<String> repeats;
		
		/**
		 * @return the timestamp
		 */
		public List<String> getTimestamp() {
			return timestamp;
		}
		/**
		 * @return the value
		 */
		public List<String> getValue() {
			return value;
		}
		/**
		 * @return the repeats
		 */
		public List<String> getRepeats() {
			return repeats;
		}
	}
}
