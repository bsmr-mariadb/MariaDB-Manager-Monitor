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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Contains the fields for the node API call.
 * 
 * @author Massimo Siani
 *
 */
public class GsonMonitorClasses extends GsonErrors {
	private MonitorClasses monitorclass;
	private List<MonitorClasses> monitorclasses;
	
	/**
	 * Deprecated. Do not use in new programs.
	 * 
	 * @return the monitorclass
	 */
//	public MonitorClasses getMonitorClass() {
//		return monitorclass;
//	}

	public MonitorClasses getMonitorClass(int index) {
		return getMonitorClasses().get(index);
	}
	/**
	 * @return the monitorclasses
	 */
	public List<MonitorClasses> getMonitorClasses() {
		if (monitorclasses != null) return monitorclasses;
		List<MonitorClasses> listMonitorClasses = new ArrayList<MonitorClasses>(1);
		listMonitorClasses.add(monitorclass);
		return listMonitorClasses;
	}

	public static class MonitorClasses {
		private String systemtype;
		private String monitor;
		private String name;
		private String sql;
		private String description;
		private String charttype;
		private String delta;
		private String monitortype;
		private String systemaverage;
		private String interval;
		private String unit;
		private String monitorid;
		
		/**
		 * @return the systemtype
		 */
		public String getSystemType() {
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
		public String getChartType() {
			return charttype;
		}
		/**
		 * @return the delta
		 */
		public int getDelta() {
			return Integer.parseInt(delta);
		}
		/**
		 * @return the monitortype
		 */
		public String getMonitorType() {
			return monitortype;
		}
		/**
		 * @return the systemaverage
		 */
		public int getSystemAverage() {
			return Integer.parseInt(systemaverage);
		}
		/**
		 * @return the interval
		 */
		public int getInterval() {
			return Integer.parseInt(interval);
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
		public int getMonitorId() {
			return Integer.parseInt(monitorid);
		}
	}
	
	/**
	 * Constructor.
	 */
	public GsonMonitorClasses() {}
	
	/**
	 * Get the list of monitor id's.
	 * 
	 * @return a list as described above, or null if no system
	 * if defined.
	 */
	public List<Integer> getMonitorIdList() {
		List<Integer> result = new ArrayList<Integer>();
//		if (this.getMonitorClass() != null) {
//			result.add(getMonitorClass().getMonitorId());
//		} else
			if (this.getMonitorClasses() != null) {
			Iterator<GsonMonitorClasses.MonitorClasses> it = getMonitorClasses().iterator();
			while (it.hasNext()) {
				result.add(it.next().getMonitorId());
			}
		} else return null;
		return result;
	}

}