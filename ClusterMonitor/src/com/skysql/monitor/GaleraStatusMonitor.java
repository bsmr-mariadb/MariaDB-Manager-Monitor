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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
	private final int					UPDATE_THRESHOLD = 5000;
	/**
	 * A table of the updated systems. (systemID, time of the last update).
	 */
	private static HashMap<Integer, Long>		m_updatedSystems;

	/**
	 * Get the instance.
	 * 
	 * @return
	 */
	private synchronized HashMap<Integer, List<node>> getInstances() {
		if (INSTANCES == null) {
			INSTANCES = new HashMap<Integer, List<node>>();
			m_updatedSystems = new HashMap<Integer, Long>();
		}
		if (INSTANCES.get(m_systemID) == null)
			m_updatedSystems.put(m_systemID, (new Date()).getTime());
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
	 * @param mon_node
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
	 * @param systemID
	 */
	public synchronized void probe(boolean verbose) {
		if ((new Date()).getTime() - m_updatedSystems.get(m_node.getSystemID()) <= UPDATE_THRESHOLD)
			return;
		Iterator<node> nodeIt = getInstances().get(m_node.getSystemID()).iterator();
		HashMap<String, List<node>> hmUUID = new HashMap<String, List<node>>();
		HashMap<node, String> hmIncAddress = new HashMap<node, String>();
		List<node> nodeList = new ArrayList<node>();
		while (nodeIt.hasNext()) {
			node n = nodeIt.next();
			nodeList.clear();
			String monitorState = m_confdb.getNodeStateFromId(Integer.parseInt(n.execute(m_sql)));
			if (! monitorState.equalsIgnoreCase("joined")) {
				n.saveObservation(m_monitor_id, monitorState);
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
		if (hmUUID.keySet().size() == 1) {
			// only one UUID: next check is incoming address
			if (checkIncomingAddress(hmIncAddress)) {
				for (node n : hmIncAddress.keySet()) {
					n.saveObservation(m_monitor_id, "104");
				}
				updateTime();
				return;
			}
		}
		if (isMajority()) {
			// TODO
		}
		nodeIt = hmIncAddress.keySet().iterator();
		while (nodeIt.hasNext()) {
			node n = nodeIt.next();
			n.saveObservation(m_monitor_id, "101");
		}
		updateTime();
		return;
	}
	
	/**
	 * Check whether all the nodes appear in the incoming address variable.
	 * 
	 * @param incomingAddress
	 * @return
	 */
	private boolean checkIncomingAddress(HashMap<node, String> incomingAddress) {
		boolean isCluster = true;
		Set<node> nodeSet = incomingAddress.keySet();
		Set<node> nodeSetb = incomingAddress.keySet();
		for (node n : nodeSet) {
			String hostname = m_confdb.getNodeHostName(n.getID());
			nodeSetb.remove(n);
			for (node m : nodeSetb) {
				isCluster = isCluster && incomingAddress.get(m).contains(hostname);
				if (! isCluster) break;
			}
			if (! isCluster) break;
		}
		return isCluster;
	}
	
	private boolean isMajority() {
		return true;
	}
	
	/**
	 * Update the time when this system has been last updated.
	 */
	private void updateTime() {
		m_updatedSystems.put(m_systemID, (new Date()).getTime());
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