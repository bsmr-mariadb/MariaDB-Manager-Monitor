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
public class GsonNode {
	private List<Nodes> nodes;
	
	/**
	 * @return the nodes
	 */
	public List<Nodes> getNodes() {
		return nodes;
	}

	public static class Nodes {
		String systemid;
		private String nodeid;
		private String name;
		private String state;
		private String hostname;
		private String publicIP;
		private String privateIP;
		private String port;
		private int instanceID;
		private String dbusername;
		private String dbpassword;
		private List<Commands> commands;
		private GsonSharedMonitorLatest monitorlatest;
		private String command;
		private String taskid;
		
		/**
		 * @return the systemid
		 */
		public String getSystemid() {
			return systemid;
		}
		/**
		 * @return the nodeid
		 */
		public String getNodeid() {
			return nodeid;
		}
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @return the state
		 */
		public String getState() {
			return state;
		}
		/**
		 * @return the hostname
		 */
		public String getHostname() {
			return hostname;
		}
		/**
		 * @return the publicIP
		 */
		public String getPublicIP() {
			return publicIP;
		}
		/**
		 * @return the privateIP
		 */
		public String getPrivateIP() {
			return privateIP;
		}
		/**
		 * @return the port
		 */
		public String getPort() {
			return port;
		}
		/**
		 * @return the instanceID
		 */
		public int getInstanceID() {
			return instanceID;
		}
		/**
		 * @return the dbusername
		 */
		public String getDbusername() {
			return dbusername;
		}
		/**
		 * @return the dbpassword
		 */
		public String getDbpassword() {
			return dbpassword;
		}
		/**
		 * @return the commands
		 */
		public List<Commands> getCommands() {
			return commands;
		}
		/**
		 * @return the monitorlatest
		 */
		public GsonSharedMonitorLatest getMonitorlatest() {
			return monitorlatest;
		}
		/**
		 * @return the command
		 */
		public String getCommand() {
			return command;
		}
		/**
		 * @return the taskid
		 */
		public String getTaskid() {
			return taskid;
		}
	}
	
	public static class Commands {
		String command;
		String description;
		String icon;
		String steps;
		
		/**
		 * @return the command
		 */
		public String getCommand() {
			return command;
		}
		/**
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}
		/**
		 * @return the icon
		 */
		public String getIcon() {
			return icon;
		}
		/**
		 * @return the steps
		 */
		public String getSteps() {
			return steps;
		}
	}

	/**
	 * Constructor.
	 */
	public GsonNode() {}
	
}
