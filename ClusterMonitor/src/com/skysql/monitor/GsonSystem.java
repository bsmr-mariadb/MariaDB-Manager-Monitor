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
public class GsonSystem {
	/** A single system */
	private Systems system;
	/** List of systems */
	private List<Systems> systems;
	
	/**
	 * Constructor.
	 */
	public GsonSystem() {}
	
	/**
	 * @return a single system
	 */
	public Systems getSystem() {
		return system;
	}

	/**
	 * @return the list of systems
	 */
	public List<Systems> getSystems() {
		return systems;
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
		private String state;
		private List<String> nodes;
		private String lastBackup;
		private Properties properties;
		private GsonSharedMonitorLatest monitorlatest;
		
		/**
		 * @return the systemid
		 */
		public String getSystemId() {
			return systemid;
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
	}
	
	public static class Properties {
		
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
			result.add(Integer.parseInt(getSystem().getSystemId()));
		} else if (this.getSystems() != null) {
			Iterator<GsonSystem.Systems> it = getSystems().iterator();
			while (it.hasNext()) {
				result.add(Integer.parseInt(it.next().getSystemId()));
			}
		} else return null;
		return result;
	}
	
}