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
public class GsonNode extends GsonErrors {
	private Nodes node;
	private List<Nodes> nodes;
	
	/**
	 * @return the node
	 */
	public Nodes getNode() {
		return node;
	}

	/**
	 * @return the nodes
	 */
	public List<Nodes> getNodes() {
		return nodes;
	}

	public static class Nodes {
		private String systemid;
		private String nodeid;
		private String name;
		private String state;
		private String hostname;
		private String publicip;
		private String privateip;
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
		public String getSystemId() {
			return systemid;
		}
		/**
		 * @return the nodeid
		 */
		public String getNodeId() {
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
			return publicip;
		}
		/**
		 * @return the privateIP
		 */
		public String getPrivateIP() {
			return privateip;
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
		public GsonSharedMonitorLatest getMonitorLatest() {
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
		public String getTaskId() {
			return taskid;
		}
	}
	
	public static class Commands {
		private String command;
		private String description;
		private String icon;
		private String steps;
		
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
	
	/**
	 * Get the list of node id's.
	 * 
	 * @return a list of available nodes, or null if no node is found.
	 */
	public List<Integer> getNodeIdList() {
		List<Integer> result = new ArrayList<Integer>();
		if (this.getNodes() != null) {
			Iterator<GsonNode.Nodes> it = getNodes().iterator();
			while (it.hasNext()) {
				result.add(Integer.parseInt(it.next().getNodeId()));
			}
		} else return null;
		return result;
	}
	/**
	 * Fetch the states of the available nodes.
	 * 
	 * @return the list of states, null if no node is found
	 */
	public List<String> getNodeStateList() {
		List<String> result = new ArrayList<String>();
		if (this.getNodes() != null) {
			Iterator<GsonNode.Nodes> it = getNodes().iterator();
			while (it.hasNext()) {
				result.add(it.next().getState());
			}
		} else if (this.getNode() != null) {
			result.add(this.getNode().getState());
		} else result = null;
		return result;
	}
	
}
