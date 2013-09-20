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
	 * Constructor.
	 */
	public GsonNode(Nodes node) {
		this.node = node;
	}
	
	/**
	 * @return the node
	 */
	private Nodes getNode() {
		return node;
	}

	/**
	 * @return the nodes
	 */
	public List<Nodes> getNodes() {
		if (nodes != null) return nodes;
		else if (getNode() != null) {
			List<Nodes> listNodes = new ArrayList<Nodes>(1);
			listNodes.add(getNode());
			return listNodes;
		}
		return null;
	}
	/**
	 * Return a single Node object at a given position.
	 * The first element is at position 0.
	 * 
	 * @param index		the position 
	 * @return			the corresponding MonitorClass object
	 */
	public Nodes getNode(int index) {
		if (index < 0) return null;
		List<Nodes> dummy = getNodes();
		if (dummy == null || dummy.size() < index+1) return null;
		else return dummy.get(index);
	}

	public static class Nodes {
		private String systemid;
		private String nodeid;
		private String name;
		private String state;
		private String updated;
		private String hostname;
		private String publicip;
		private String privateip;
		private String port;
		private String instanceid;
		private String dbusername;
		private String dbpassword;
		private String repusername;
		private String reppassword;
		private List<Commands> commands;
		private GsonSharedMonitorLatest monitorlatest;
		private String command;
		private String taskid;
		
		/**
		 * @return the systemid
		 */
		public int getSystemId() {
			return Integer.parseInt(systemid);
		}
		/**
		 * @return the nodeid
		 */
		public int getNodeId() {
			return Integer.parseInt(nodeid);
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
			if (instanceid.isEmpty()) return 0;
			return Integer.parseInt(instanceid);
		}
		/**
		 * @return the dbusername
		 */
		public String getDbUserName() {
			return dbusername;
		}
		/**
		 * @return the dbpassword
		 */
		public String getDbPassword() {
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
		/**
		 * If set, this value overrides the corresponding
		 * one in the System class.
		 * 
		 * @return the repusername
		 */
		public String getRepUserName() {
			return repusername;
		}
		/**
		 * If set, this value overrides the corresponding
		 * one in the System class.
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
	 * Get the list of node id's.
	 * 
	 * @return a list of available nodes, or null if no node is found.
	 */
	public List<Integer> getNodeIdList() {
		List<Integer> result = new ArrayList<Integer>();
		if (this.getNodes() != null) {
			Iterator<GsonNode.Nodes> it = getNodes().iterator();
			while (it.hasNext()) {
				result.add(it.next().getNodeId());
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
