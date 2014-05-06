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
 * Copyright 2013-2014 SkySQL Corporation Ab
 * 
 * Author: Massimo Siani
 * Date: July 2013
 */

package com.skysql.monitor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.skysql.java.Logging;
import com.skysql.java.MonData;

/**
 * Handle the membership of each Node in a system, set the Node
 * and the system states. Use the global variables retrieved
 * from Galera.
 * Since the Node state can only be determined by looking at all
 * the nodes of the system this class is a system singleton class.
 * This singleton gathers the state of each Node, and return the
 * Node state for the calling Node as well as setting the system
 * state.
 * 
 * @author Massimo Siani
 *
 */
public class GaleraStatusMonitor extends Monitor {
	/**
	 * The instances of the class. The key is the systemID.
	 */
	private static volatile HashMap<Integer, List<Node>>	INSTANCES;
	/**
	 * The ID of the system passed to the constructor. Necessary to
	 * add the system to the INSTANCES variable.
	 */
	private int							m_systemID;
	/**
	 * The GlobalStatusObject that contains the global status
	 * and variables from Galera.
	 */
	private GlobalStatusObject			m_globalStatus;
	/**
	 * The lapse before a system can be checked again. It needs
	 * to avoid multiple nodes from the same system to ask to
	 * update the system itself multiple times. Unit: milliseconds. 
	 */
	private final static int					UPDATE_THRESHOLD = 5000;
	/**
	 * A table of the updated systems. (systemID, time of the last update).
	 */
	private static HashMap<Integer, Long>		m_updatedSystems;
	/**
	 * The Node state -> Node state ID map.
	 */
	private static HashMap<NodeStates, Integer>			m_nodeStates = null;
	/**
	 * Node states that are not computed by a query.
	 */
	private enum NodeStates {
		DOWN, MACHINEDOWN, ISOLATED, INCORRECTLYJOINED
		, JOINED
	}

	/**
	 * Get the table (system id's, list of nodes with that system id).
	 * The corresponding object is static.
	 * 
	 * @return	the table (system id, list of nodes)
	 */
	private synchronized HashMap<Integer, List<Node>> getInstances() {
		if (INSTANCES == null) {
			INSTANCES = new HashMap<Integer, List<Node>>();
			m_updatedSystems = new HashMap<Integer, Long>();
		}
		if (INSTANCES.get(m_systemID) == null) {
			m_updatedSystems.put(m_systemID, now() - UPDATE_THRESHOLD);   // next time the Monitor will run
		}
		return INSTANCES;
	}
	
	/**
	 * Constructor for the class.
	 * 
	 * @param db			an instance of the db handling class
	 * @param id			the Monitor id
	 * @param mon_node		the instance of the Node
	 */
	public GaleraStatusMonitor(MonData db, int id, Node mon_node) {
		super(db, id, mon_node);
		m_systemID = mon_node.getSystemID();
		setInstance(mon_node);
		setNodeStates();
	}

	/**
	 * Initialize the system singleton. Add the system id to the
	 * table of known system id's and loads the associated nodes.
	 * 
	 * @param mon_node	the Node to be added to the instance
	 */
	private synchronized void setInstance(Node mon_node) {
		List<Node> nodeList = getInstances().get(mon_node.getSystemID());
		if (nodeList != null) {
			List<Node> nodeListb = new ArrayList<Node>();
			nodeListb.addAll(nodeList);
			Iterator<Node> nodeIt = nodeList.iterator();
			boolean found = false;
			while (nodeIt.hasNext()) {
				Node n = nodeIt.next();
				if (mon_node.getID() == n.getID()) {
					nodeListb.remove(n);
					nodeListb.add(mon_node);
					found = true;
				}
			}
			if (! found) nodeListb.add(mon_node);
			nodeList = nodeListb;
		}
		else {
			nodeList = new ArrayList<Node>();
			nodeList.add(mon_node);
		}
		getInstances().put(mon_node.getSystemID(), nodeList);
	}
	
	/**
	 * Generate a map to the Node states that are not computed from a query.
	 */
	private void setNodeStates() {
		if (m_nodeStates == null) {
			m_nodeStates = new HashMap<NodeStates, Integer>();
			m_nodeStates.put(NodeStates.DOWN, m_confdb.getNodeStateId("down"));
			m_nodeStates.put(NodeStates.MACHINEDOWN, m_confdb.getNodeStateId("machine-down"));
			m_nodeStates.put(NodeStates.ISOLATED, m_confdb.getNodeStateId("isolated"));
			m_nodeStates.put(NodeStates.INCORRECTLYJOINED, m_confdb.getNodeStateId("incorrectly-joined"));
			m_nodeStates.put(NodeStates.JOINED, m_confdb.getNodeStateId("joined"));
		}
	}
	
	/**
	 * Reset the list of nodes that have to be checked to compute
	 * the nodes and system states.
	 * 
	 * @param systemID		the ID of the system to remove
	 */
	public static synchronized void removeSystem (Integer systemID) {
		if (INSTANCES != null) {
			INSTANCES.remove(systemID);
		}
	}
	
	/**
	 * Check the nodes and assign them their state. Also assign the system state,
	 * based on the result of the Node states probe.
	 * Overrides the method in the parent class.
	 * 
	 * @param verbose
	 */
	public synchronized void probe(boolean verbose) {
		if (now() - m_updatedSystems.get(m_node.getSystemID()) <= UPDATE_THRESHOLD) {
			return;
		}
		Iterator<Node> nodeIt = getInstances().get(m_node.getSystemID()).iterator();
		HashMap<String, List<Node>> hmUUID = new HashMap<String, List<Node>>();
		HashMap<Node, String> hmIncAddress = new HashMap<Node, String>();
		while (nodeIt.hasNext()) {
			Node n = nodeIt.next();
			m_globalStatus = GlobalStatusObject.getInstance(n);
			String dbType = getDbType();
			String dbVersion = getDbVersion();
			if (dbType != null && dbVersion != null) {
				m_confdb.setNodeDatabaseProperties(n.getID(), dbType, dbVersion);
			}
			List<Node> nodeList = new ArrayList<Node>();
			try {
				String nodeStateString = m_globalStatus.getStatus("wsrep_local_state");
				Integer nodeStateID;
				if (nodeStateString != null) {
					nodeStateID = Integer.parseInt(nodeStateString) + 100;
				} else {
					String nodeClusterSize = m_globalStatus.getStatus("wsrep_cluster_size");
					if (nodeClusterSize != null && nodeClusterSize.equalsIgnoreCase("0")) {
						nodeStateID = m_nodeStates.get(NodeStates.ISOLATED);
					} else {
						nodeStateID = m_nodeStates.get(NodeStates.DOWN);
					}
				} 
				String monitorState = m_confdb.getNodeStateFromId(nodeStateID);
				if (! monitorState.equalsIgnoreCase("joined")) {
					m_confdb.setNodeState(n.getID(), nodeStateID);
					continue;
				}
			} catch (Exception e) {
				continue;
			}
			String UUID = m_globalStatus.getStatus("wsrep_local_state_uuid");
			String local;
			String wsrepIncomingAddress = ( (local = m_globalStatus.getStatus("wsrep_incoming_addresses")) != null ?
					local.replaceAll(":3306", "") : null );
			if (!(hmUUID.get(UUID) == null)) nodeList.addAll(hmUUID.get(UUID));
			nodeList.add(n);
			hmUUID.put(UUID, nodeList);
			hmIncAddress.put(n, wsrepIncomingAddress);
		}
		boolean notFinished = true;
		if (hmUUID.keySet().size() == 1) {
			// only one UUID: next check is incoming address
			if (checkIncomingAddress(hmIncAddress)) {
				for (Node n : hmIncAddress.keySet()) {
					m_confdb.setNodeState(n.getID(), m_nodeStates.get(NodeStates.JOINED));
				}
				notFinished = false;
			}
		}
		if (notFinished) {
			for (Node n : isMajority(hmUUID, hmIncAddress)) {
				m_confdb.setNodeState(n.getID(), m_nodeStates.get(NodeStates.JOINED));
				hmIncAddress.remove(n);
			}
		}
		if (notFinished) {
			nodeIt = hmIncAddress.keySet().iterator();
			while (nodeIt.hasNext()) {
				Node n = nodeIt.next();
				m_confdb.setNodeState(n.getID(), m_nodeStates.get(NodeStates.INCORRECTLYJOINED));
			}
		}
		setSystemState();
		updateTime();
		Logging.info("    Probe " + getID() + " " + m_confdb.getMonitorKey(getID())
				+ " for system " + m_systemID + " done.");
		return;
	}
	
	/**
	 * Algorithm to assign the state to the system. It is based only on
	 * the list of states returned by the API, so it does not check
	 * if anything has changed.
	 */
	public void setSystemState() {
		List<String> states = m_confdb.getNodeStates();
		String systemState;
		Set<String> statesSet = new HashSet<String>(states);
		int numOfJoined = 0;
		for (String state : states) {
			if (state.equalsIgnoreCase("joined")) numOfJoined++;
		}
		if (states.contains("incorrectly-joined")) {
			systemState = "inconsistent";
		}
		else if (! states.contains("joined")) {
			systemState =  "down";
		}
		else if (states.size() < 3 || numOfJoined < 3) {
			systemState = "limited-availability";
		}
		else if (statesSet.size() == 1) {
			systemState = "running";
		}
		else systemState = "available";
		m_confdb.setSystemState(systemState);
	}
	
	/**
	 * Check whether all the nodes appear in the incoming address variable.
	 * 
	 * @param incomingAddress	a table which contains the INCOMING_ADDRESSESES variable
	 * @return	true if and only if all the nodes appear in all the other nodes'
	 * INCOMING_ADDRESSES variable
	 */
	private boolean checkIncomingAddress(HashMap<Node, String> incomingAddress) {
		boolean isCluster = true;
		Set<Node> nodeSet = incomingAddress.keySet();
		Set<Node> nodeSetb = new HashSet<Node>(incomingAddress.keySet());
		for (Node n : nodeSet) {
			String hostname = m_confdb.getNodeHostName(n.getID());
			String IP = m_confdb.getNodePrivateIP(n.getID());
			try {
				hostname = InetAddress.getByName(IP).getHostName();
				if (hostname == null || hostname.isEmpty()) {
					throw new UnknownHostException();
				}
			} catch (UnknownHostException e) {
				hostname = IP;
			}
			nodeSetb.remove(n);
			for (Node m : nodeSetb) {
				isCluster = isCluster
						&& (incomingAddress.get(m).contains(hostname) || incomingAddress.get(m).contains(IP));
				if (! isCluster) break;
			}
			if (! isCluster) break;
		}
		return isCluster;
	}
	
	/**
	 * If there is a majority of nodes correctly joined.
	 * 
	 * @param hmIncAddress	an HashMap of (Node, incoming_addresses variable)
	 * @param hmUUID		an HashMap of (UUID, list of nodes with that UUID)
	 * 
	 * @return	The list of nodes in the main cluster, an empty list if no main cluster exists
	 */
	private List<Node> isMajority(HashMap<String, List<Node>> hmUUID, HashMap<Node, String> hmIncAddress) {
		HashMap<Integer, List<Node>> nodePartitions = new HashMap<Integer, List<Node>>();
		Integer partitionNo = 1;
		int totalSize = 0;
		for (String UUID : hmUUID.keySet()) {
			List<Node> toIterateOn = hmUUID.get(UUID);
			List<Node> toIterateOnb = new ArrayList<Node>(hmUUID.get(UUID));
			totalSize += toIterateOn.size();
			for (Node n : toIterateOn) {		// do not use toIterateOn here, only below
				toIterateOnb.remove(n);
				String hostname = m_confdb.getNodeHostName(n.getID());
				List<Node> partitionNodes = new ArrayList<Node>();
				partitionNodes.add(n);
				for (Node m : toIterateOnb) {
					if (hmIncAddress.get(m).contains(hostname)) {
						partitionNodes.add(m);
					}
				}
				nodePartitions.put(partitionNo, partitionNodes);
				partitionNo++;
			}
		}
		for (Integer i : nodePartitions.keySet()) {
			List<Node> nl = nodePartitions.get(i);
			if (nl.size() > totalSize/2) {
				return nl;
			}
		}
		return new ArrayList<Node>();
	}
	
	/**
	 * Retrieves the database type.
	 * 
	 * @return		the database type
	 */
	private String getDbType() {
		return m_globalStatus.getVariable("version_comment");
	}
	
	/**
	 * Retrieves the database version.
	 * 
	 * @return		the database version
	 */
	private String getDbVersion() {
		return m_globalStatus.getVariable("version");
	}
	
	/**
	 * Return the current time.
	 * 
	 * @return a number reprsenting the current time
	 */
	private long now() {
		return (new Date()).getTime();
	}
	
	/**
	 * Update the time when this system has been last updated.
	 */
	private void updateTime() {
		m_updatedSystems.put(m_systemID, now());
	}
	
	/**
	 * The Monitor has a system value as well as individual Node values
	 * 
	 * @return True if there is a system value
	 */
	public boolean hasSystemValue()
	{
		return false;
	}
}
