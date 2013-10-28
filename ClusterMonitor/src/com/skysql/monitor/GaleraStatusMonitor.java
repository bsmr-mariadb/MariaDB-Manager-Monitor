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
 * Copyright 2012, 2013 SkySQL Ab
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

/**
 * Handle the membership of each node in a system, set the node
 * and the system states. Use the global variables retrieved
 * from Galera.
 * Since the node state can only be determined by looking at all
 * the nodes of the system this class is a system singleton class.
 * This singleton gathers the state of each node, and return the
 * node state for the calling node as well as setting the system
 * state.
 * 
 * @author Massimo Siani
 *
 */
public class GaleraStatusMonitor extends monitor {
	/**
	 * The instances of the class. The key is the systemID.
	 */
	private static volatile HashMap<Integer, List<node>>	INSTANCES;
	private int							m_systemID;
	/**
	 * The globalStatusObject that contains the global status
	 * and variables from Galera.
	 */
	private globalStatusObject			m_globalStatus;
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
	 * Get the table (system id's, list of nodes with that system id).
	 * The corresponding object is static.
	 * 
	 * @return	the table (system id, list of nodes)
	 */
	private synchronized HashMap<Integer, List<node>> getInstances() {
		if (INSTANCES == null) {
			INSTANCES = new HashMap<Integer, List<node>>();
			m_updatedSystems = new HashMap<Integer, Long>();
		}
		if (INSTANCES.get(m_systemID) == null) {
			m_updatedSystems.put(m_systemID, now() - UPDATE_THRESHOLD);   // next time the monitor will run
		}
		return INSTANCES;
	}
	
	/**
	 * Constructor for the class.
	 * 
	 * @param db			an instance of the db handling class
	 * @param id			the monitor id
	 * @param mon_node		the instance of the node
	 */
	public GaleraStatusMonitor(mondata db, int id, node mon_node) {
		super(db, id, mon_node);
		m_systemID = mon_node.getSystemID();
		setInstance(mon_node);
		m_sql = "select 100 + variable_value from global_status where variable_name = 'WSREP_LOCAL_STATE' union select 99 limit 1;";
	}

	/**
	 * Initialize the system singleton. Add the system id to the
	 * table of known system id's and loads the associated nodes.
	 * 
	 * @param mon_node	the node to be added to the instance
	 */
	private synchronized void setInstance(node mon_node) {
		List<node> nodeList = getInstances().get(mon_node.getSystemID());
		if (nodeList != null) {
			List<node> nodeListb = new ArrayList<node>();
			nodeListb.addAll(nodeList);
			Iterator<node> nodeIt = nodeList.iterator();
			boolean found = false;
			while (nodeIt.hasNext()) {
				node n = nodeIt.next();
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
			nodeList = new ArrayList<node>();
			nodeList.add(mon_node);
		}
		getInstances().put(mon_node.getSystemID(), nodeList);
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
	 * based on the result of the node states probe.
	 * Overrides the method in the parent class.
	 * 
	 * @param verbose
	 */
	public synchronized void probe(boolean verbose) {
		if (now() - m_updatedSystems.get(m_node.getSystemID()) <= UPDATE_THRESHOLD)
			return;
		Iterator<node> nodeIt = getInstances().get(m_node.getSystemID()).iterator();
		HashMap<String, List<node>> hmUUID = new HashMap<String, List<node>>();
		HashMap<node, String> hmIncAddress = new HashMap<node, String>();
		while (nodeIt.hasNext()) {
			node n = nodeIt.next();
			List<node> nodeList = new ArrayList<node>();
			try {
				String nodeStateString = n.execute(m_sql);
				Integer nodeStateID;
				if (nodeStateString != null) {
					nodeStateID = Integer.parseInt(nodeStateString);
				} else {
					nodeStateID = 100;
				} 
				String monitorState = m_confdb.getNodeStateFromId(nodeStateID);
				if (! monitorState.equalsIgnoreCase("joined")) {
					m_confdb.setNodeState(n.getID(), nodeStateID);
					continue;
				}
			} catch (Exception e) {
				continue;
			}
			m_globalStatus = globalStatusObject.getInstance(n);
			String UUID = m_globalStatus.getStatus("WSREP_LOCAL_STATE_UUID");
			String local;
			String wsrepIncomingAddress = ( (local = m_globalStatus.getStatus("WSREP_INCOMING_ADDRESSES")) != null ?
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
				for (node n : hmIncAddress.keySet()) {
					m_confdb.setNodeState(n.getID(), 104);
				}
				notFinished = false;
			}
		}
		if (notFinished) {
			for (node n : isMajority(hmUUID, hmIncAddress)) {
				m_confdb.setNodeState(n.getID(), 104);
				hmIncAddress.remove(n);
			}
		}
		if (notFinished) {
			nodeIt = hmIncAddress.keySet().iterator();
			while (nodeIt.hasNext()) {
				node n = nodeIt.next();
				m_confdb.setNodeState(n.getID(), 98);
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
	private boolean checkIncomingAddress(HashMap<node, String> incomingAddress) {
		boolean isCluster = true;
		Set<node> nodeSet = incomingAddress.keySet();
		Set<node> nodeSetb = new HashSet<node>(incomingAddress.keySet());
		for (node n : nodeSet) {
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
			for (node m : nodeSetb) {
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
	 * @param hmIncAddress	an HashMap of (node, incoming_addresses variable)
	 * @param hmUUID		an HashMap of (UUID, list of nodes with that UUID)
	 * 
	 * @return	The list of nodes in the main cluster, an empty list if no main cluster exists
	 */
	private List<node> isMajority(HashMap<String, List<node>> hmUUID, HashMap<node, String> hmIncAddress) {
		HashMap<Integer, List<node>> nodePartitions = new HashMap<Integer, List<node>>();
		Integer partitionNo = 1;
		int totalSize = 0;
		for (String UUID : hmUUID.keySet()) {
			List<node> toIterateOn = hmUUID.get(UUID);
			List<node> toIterateOnb = new ArrayList<node>(hmUUID.get(UUID));
			totalSize += toIterateOn.size();
			for (node n : toIterateOn) {		// do not use toIterateOn here, only below
				toIterateOnb.remove(n);
				String hostname = m_confdb.getNodeHostName(n.getID());
				List<node> partitionNodes = new ArrayList<node>();
				partitionNodes.add(n);
				for (node m : toIterateOnb) {
					if (hmIncAddress.get(m).contains(hostname)) {
						partitionNodes.add(m);
					}
				}
				nodePartitions.put(partitionNo, partitionNodes);
				partitionNo++;
			}
		}
		for (Integer i : nodePartitions.keySet()) {
			List<node> nl = nodePartitions.get(i);
			if (nl.size() > totalSize/2) {
				return nl;
			}
		}
		return new ArrayList<node>();
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
	 * The monitor has a system value as well as individual node values
	 * 
	 * @return True if there is a system value
	 */
	public boolean hasSystemValue()
	{
		return false;
	}
}
