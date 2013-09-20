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
 * Contains the fields for the system API call.
 * Also implements methods to return the fields
 * of greater interest.
 * 
 * @author Massimo Siani
 *
 */
public class GsonSystem extends GsonErrors {
	/** A single system */
	private Systems					system;
	/** List of systems */
	private List<Systems>			systems;
	
	/**
	 * Constructor. Accepts a single system, not a list, and builds the
	 * corresponding object. After the system object has been set, it
	 * cannot be modified.
	 */
	public GsonSystem(GsonSystem.Systems system) {
		this.system = system;
	}
	
	/**
	 * @return a single system
	 */
	private Systems getSystem() {
		return system;
	}
	/**
	 * @return the list of systems
	 */
	public List<Systems> getSystems() {
		if (systems != null) return systems;
		else if (getSystem() != null) {
			List<Systems> listSystems = new ArrayList<Systems>(1);
			listSystems.add(getSystem());
			return listSystems;
		}
		else return null;
	}
	
	/**
	 * Retrieve a single Systems object, at the given position from the list.
	 * The first position has index zero.
	 * If you expect the list of System objects to contain only one element,
	 * you may call getSystem(0) to get it.
	 * 
	 * @param index		the position of the object to retrieve
	 * @return			a Systems object
	 */
	public Systems getSystem(int index) {
		try {
			return getSystems().get(index);
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * The system API main subclass. The corresponding Json object
	 * is called either systems (if it's a list) or system if it's
	 * a single object.
	 * 
	 * @author Massimo Siani
	 *
	 */
	public static class Systems {
		private String systemid;
		private String systemtype;
		private String name;
		private String started;
		private String lastaccess;
		private String updated;
		private String state;
		private String dbusername;
		private String dbpassword;
		private String repusername;
		private String reppassword;
		private List<String> nodes;
		private String lastBackup;
		private Properties properties;
		private GsonSharedMonitorLatest monitorlatest;
		
		/**
		 * @return the systemid
		 */
		public int getSystemId() {
			return Integer.parseInt(systemid);
		}
		/**
		 * @return the systemtype
		 */
		public String getSystemtype() {
			return systemtype;
		}
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @return the started
		 */
		public String getStarted() {
			return started;
		}
		/**
		 * @return the lastaccess
		 */
		public String getLastaccess() {
			return lastaccess;
		}
		/**
		 * @return the state
		 */
		public String getState() {
			return state;
		}
		/**
		 * @return the nodes
		 */
		public List<String> getNodes() {
			return nodes;
		}
		/**
		 * @return the lastBackup
		 */
		public String getLastBackup() {
			return lastBackup;
		}
		/**
		 * @return the properties
		 */
		public Properties getProperties() {
			return properties;
		}
		/**
		 * @return the monitorlatest
		 */
		public GsonSharedMonitorLatest getMonitorLatest() {
			return monitorlatest;
		}
		/**
		 * The default value, valid for every node in the system.
		 * 
		 * @return the dbusername
		 */
		public String getDbUserName() {
			return dbusername;
		}
		/**
		 * The default value, valid for every node in the system.
		 * 
		 * @return the dbpassword
		 */
		public String getDbPassword() {
			return dbpassword;
		}
		/**
		 * The default value that applies to every node in the System.
		 * 
		 * @return the repusername
		 */
		public String getRepUserName() {
			return repusername;
		}
		/**
		 * The default value that applies to every node in the System.
		 * 
		 * @return the reppassword
		 */
		public String getRepPassword() {
			return reppassword;
		}
		/**
		 * The timestamp for the last change. 
		 * 
		 * @return the string representing the last update time
		 */
		public String getLastUpdateTime() {
			return updated;
		}
	}
	
	public static class Properties {
		private String MonitorInterval;
		private String IPMonitor;

		/**
		 * @return the monitorInterval
		 */
		public Integer getMonitorInterval() {
			try {
				return Integer.parseInt(MonitorInterval);
			} catch (Exception e) {
				return null;
			}
		}
		/**
		 * @return the iPMonitor
		 */
		public String getIPMonitor() {
			return IPMonitor;
		}
		
	}
	
	/**
	 * Get the list of system id's.
	 * First check if a single system exists, in case wrap it
	 * in a list. This excludes getting the id's from the
	 * multiple systems list. Then check if multiple systems
	 * exist, and fill a list with the values of their id's.
	 * 
	 * @return a list as described above, or null if no system
	 * if defined.
	 */
	public List<Integer> getSystemIdList() {
		List<Integer> result = new ArrayList<Integer>();
		if (this.getSystem() != null) {
			result.add(getSystem().getSystemId());
		} else if (this.getSystems() != null) {
			Iterator<GsonSystem.Systems> it = getSystems().iterator();
			while (it.hasNext()) {
				result.add(it.next().getSystemId());
			}
		} else return null;
		return result;
	}
	
}