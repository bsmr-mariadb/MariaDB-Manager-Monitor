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
 * Contains the fields for the node API call.
 * 
 * @author Massimo Siani
 *
 */
public class GsonMonitorClasses {
	private MonitorClasses monitorclass;
	private List<MonitorClasses> monitorclasses;
	
	/**
	 * @return the monitorclass
	 */
	public MonitorClasses getMonitorclass() {
		return monitorclass;
	}

	/**
	 * @return the monitorclasses
	 */
	public List<MonitorClasses> getMonitorclasses() {
		return monitorclasses;
	}

	public static class MonitorClasses {
		private String systemtype;
		String monitor;
		private String name;
		private String sql;
		private String description;
		private String charttype;
		private int delta;
		private String monitortype;
		private int systemaverage;
		private int interval;
		private String unit;
		String monitorid;
		
		/**
		 * @return the systemtype
		 */
		public String getSystemtype() {
			return systemtype;
		}
		/**
		 * @return the monitor
		 */
		public String getMonitor() {
			return monitor;
		}
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @return the sql
		 */
		public String getSql() {
			return sql;
		}
		/**
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}
		/**
		 * @return the charttype
		 */
		public String getCharttype() {
			return charttype;
		}
		/**
		 * @return the delta
		 */
		public int getDelta() {
			return delta;
		}
		/**
		 * @return the monitortype
		 */
		public String getMonitortype() {
			return monitortype;
		}
		/**
		 * @return the systemaverage
		 */
		public int getSystemaverage() {
			return systemaverage;
		}
		/**
		 * @return the interval
		 */
		public int getInterval() {
			return interval;
		}
		/**
		 * @return the unit
		 */
		public String getUnit() {
			return unit;
		}
		/**
		 * @return the monitorid
		 */
		public String getMonitorid() {
			return monitorid;
		}
	}
	
	/**
	 * Constructor.
	 */
	public GsonMonitorClasses() {}

}