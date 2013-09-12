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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interface to the monitoring database, this is the database that holds
 * the definition of what to monitor and into which the monitored values
 * are written.
 * 
 * @author Mark Riddoch, Massimo Siani
 *
 */
public class mondata {
	private int			m_systemID;
	private monAPI		m_api;
	private String		m_systemType = "galera";
	
	/**
	 * Constructor for the monitor data class.
	 * 
	 * @param systemID	The System ID being monitored
	 */
	public mondata(int systemID)
	{
		m_systemID = systemID;
		m_api = new monAPI();
	}
	/**
	 * Constructor used when the system id is not known.
	 */
	public mondata()
	{
		this(-1);
	}
	
	/**
	 * Fetch the Java object that corresponds to an API URI.
	 * 
	 * @param apiRequest
	 * @param objectClass the class of the Java object, e.g. MyClass.class
	 * @return the Java object
	 */
	private <T> T getObjectFromAPI(String apiRequest, Class<T> objectClass) {
		String getJson = m_api.getReturnedJson(apiRequest, new String[]{""}, new String[] {""});
		T object = GsonManager.fromJson(getJson, objectClass);
		return object;
	}
	
	
	/********************************************************
	 * GET
	 ********************************************************/
	/********************************************************
	 * System
	 ********************************************************/
	/**
	 * Fetch the list of System ID's to monitor.
	 * 
	 * @return The list of SystemIDs defined in the database
	 */
	public List<Integer> getSystemList() {
		String apiRequest = "system";
		GsonSystem gsonSystem = getObjectFromAPI(apiRequest, GsonSystem.class);
		return gsonSystem == null ? null : gsonSystem.getSystemIdList();
	}
	/**
	 * Fetch the monitor probe interval.
	 * 
	 * @return The monitor interval in seconds
	 */
	public Integer getSystemMonitorInterval()
	{
		String apiRequest = "system/" + m_systemID + "/property/MonitorInterval";
		GsonSystem gsonSystem = getObjectFromAPI(apiRequest, GsonSystem.class);
		if (gsonSystem == null) return 30;
		return gsonSystem.getSystem().getProperties().getMonitorInterval();
	}
	/**
	 * IPMonitor
	 * 
	 * Get the system property IPMonitor - this controls the running of the IPMonitor for
	 * EC2 Cloud based deployments. The default is true for reasons of backward compatibility.
	 * 
	 * @return	boolean		True if the IP Monitor should be run
	 */
	public boolean IPMonitor()
	{
		String apiRequest = "system/" + m_systemID + "/property/IPMonitor";
		GsonSystem gsonSystem = getObjectFromAPI(apiRequest, GsonSystem.class);
		if (gsonSystem == null) return false;
		String IP = gsonSystem.getSystem().getProperties().getIPMonitor();
		if (IP == null) return false;
		Pattern pattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
		Matcher matcher = pattern.matcher(IP);
		if (matcher.matches()) {
			return true;
		}
		return false;
	}
	/********************************************************
	 * Node
	 ********************************************************/	
	/**
	 * Return the list of node id's to monitor.
	 * 
	 * @return The list of nodes in the database
	 */
	public List<Integer> getNodeList()
	{
		String apiRequest = "system/" + m_systemID + "/node";
		GsonNode gsonNode = getObjectFromAPI(apiRequest, GsonNode.class);
		return gsonNode == null ? null : gsonNode.getNodeIdList();
	}
	/**
	 * Get the private IP address of the specified node.
	 * 
	 * @param NodeNo	The node number
	 * @return The private IP address as a string
	 */
	public String getNodePrivateIP(int NodeNo)
	{
		String apiRequest = "system/" + m_systemID + "/node/" + NodeNo;
		GsonNode gsonNode = getObjectFromAPI(apiRequest, GsonNode.class);
		return gsonNode == null ? null : gsonNode.getNode().getPrivateIP();
	}
	/**
	 * Get the credentials for the specified node.
	 * 
	 * @param NodeNo The node number to return the credentials of
	 * @return The Credentials for the node
	 */
	public Credential getNodeMonitorCredentials(int NodeNo)
	{
		String apiRequest = "system/" + m_systemID + "/node/" + NodeNo;
		Credential cred;
		try {
			GsonNode gsonNode = getObjectFromAPI(apiRequest, GsonNode.class);
			if (gsonNode != null) {
				cred = new Credential(gsonNode.getNode().getDbusername(),
						gsonNode.getNode().getDbpassword());
			} else
				cred = new Credential("repluser", "repw");
			return cred;
		} catch (Exception ex) {
			System.err.println("API Failed: " + apiRequest + ": " + ex.getMessage());
			return null;
		}
	}
	/**
	 * Get the list of instance ID for this cluster.
	 * 
	 * @return The list of instance IDs
	 */
	public List<Integer> getInstances()
	{
		String apiRequest = "system/" + m_systemID + "/node";
		GsonNode gsonNode = getObjectFromAPI(apiRequest, GsonNode.class);
		List<Integer> result = new ArrayList<Integer>();
		for (int i=0; i<gsonNode.getNodes().size(); i++) {
			result.add(gsonNode.getNodes().get(i).getInstanceID());
		}
		return result;
	}
	/********************************************************
	 * Node States
	 ********************************************************/
	/**
	 * Return the list of valid node states.
	 * 
	 * @return The set of defined node states.
	 */
	public List<String> getNodeValidStates()
	{
		String apiRequest = "nodestate";
		GsonNodeStates gsonNodeStates = getObjectFromAPI(apiRequest, GsonNodeStates.class);
		if (gsonNodeStates == null) return null;
		return gsonNodeStates.getDescriptionList();
	}
	/**
	 * Map a node state string to a state id.
	 * 
	 * @param Name The name of the node state
	 * @return The Node State
	 */
	public int getNodeStateId(String Name)
	{
		String apiRequest = "nodestate/" + Name;
		GsonNodeStates gsonNodeStates = getObjectFromAPI(apiRequest, GsonNodeStates.class);
		return gsonNodeStates == null ? null : gsonNodeStates.getNodeState(0).getStateId();
	}
	/**
	 * Map a node state id to a state string.
	 * 
	 * @param stateId The id of the node state
	 * @return The Node State
	 */
	public String getNodeStateFromId(int stateId)
	{
		String apiRequest = "nodestate";
		GsonNodeStates gsonNodeStates = getObjectFromAPI(apiRequest, GsonNodeStates.class);
		return gsonNodeStates.getStateFromId(stateId);
	}
	/********************************************************
	 * Monitor
	 ********************************************************/
	/**
	 * Return the list of all available monitor Id's for the given system type.
	 * 
	 * @return The list of monitorID's defined in the database
	 */
	public List<Integer> getMonitorIdList()
	{
		String apiRequest = "monitorclass/" + m_systemType + "/key";
		GsonMonitorClasses gsonMonitorClasses = getObjectFromAPI(apiRequest, GsonMonitorClasses.class);
		return gsonMonitorClasses == null ? null : gsonMonitorClasses.getMonitorIdList();
	}
	/**
	 * Get the SQL command (or command string) associated with a particular monitor.
	 * 
	 * Although originally a simple SQL string for a monitor to execute, other
	 * monitor types have reused the string to contain monitor specific data.
	 * 
	 * @param monitor_id The monitor ID to return the SQL data string for
	 * @return The "SQL" field of the monitor
	 */
	public String getMonitorSQL(int monitor_id)
	{
		String apiRequest = "monitorclass/" + m_systemType + "/key/" + getMonitorKey(monitor_id);
		GsonMonitorClasses gsonMonitorClasses = getObjectFromAPI(apiRequest, GsonMonitorClasses.class);
		if (gsonMonitorClasses == null || gsonMonitorClasses.getMonitorClass(0) == null) return null;
		return gsonMonitorClasses.getMonitorClass(0).getSql();
	}
	/**
	 * Fetch the monitor probe interval.
	 * 
	 * @return The monitor interval in seconds
	 */
	public int getMonitorClassInterval(String monitorKey)
	{
		String apiRequest = "monitorclass/" + m_systemType + "/key/" + monitorKey;
		GsonMonitorClasses gsonMonitorClasses = getObjectFromAPI(apiRequest, GsonMonitorClasses.class);
		return gsonMonitorClasses == null ? null : gsonMonitorClasses.getMonitorClass(0).getInterval();
	}
	/**
	 * Fetch the id of a particular monitor.
	 * 
	 * @param monitorKey	The monitor name
	 * @return The monitor_id of the named monitor or -1 if the monitor was not found
	 */
	public int getNamedMonitor(String monitorKey)
	{
		String apiRequest = "monitorclass/" + m_systemType + "/key/" + monitorKey;
		GsonMonitorClasses gsonMonitorClasses = getObjectFromAPI(apiRequest, GsonMonitorClasses.class);
		return gsonMonitorClasses == null ? null : gsonMonitorClasses.getMonitorClass(0).getMonitorId();
	}
	/**
	 * Return the type, and hence monitor class, of a particular monitor.
	 * 
	 * @param id	The monitor ID
	 * @return The type field for the monitor, e.g. SQL, CMD, CRM etc.
	 */
	public String getMonitorType(int monitor_id)
	{
		String apiRequest = "monitorclass/" + m_systemType + "/key/" + getMonitorKey(monitor_id);
		GsonMonitorClasses gsonMonitorClasses = getObjectFromAPI(apiRequest, GsonMonitorClasses.class);
		return gsonMonitorClasses == null ? null : gsonMonitorClasses.getMonitorClass(0).getMonitorType();
	}
	/**
	 * Is the system monitor value cumulative or an average of all the nodes in the system?
	 * 
	 * @param id	The Monitor ID
	 * @return		True if the system value of a monitor is an average of all the nodes in the system
	 */
	public boolean isMonitorSystemAverage(int monitor_id)
	{
		String apiRequest = "monitorclass/" + m_systemType + "/key/" + getMonitorKey(monitor_id);
		GsonMonitorClasses gsonMonitorClasses = getObjectFromAPI(apiRequest, GsonMonitorClasses.class);
		Integer result = (gsonMonitorClasses == null ? 0 : gsonMonitorClasses.getMonitorClass(0).getSystemAverage());
		if (result == 1) return true;
		return false;
	}
	/**
	 * Is the monitored value a cumulative number or a snapshot value. This allows monitors
	 * to return values that are either the value in the database or the difference between
	 * the current value and the previous value.
	 * 
	 * @param monitor_id	The monitor ID to check
	 * @return True of the monitor is a delta of observed values
	 */
	public Boolean isMonitorDelta(int monitor_id)
	{
		String apiRequest = "monitorclass/" + m_systemType + "/key/" + getMonitorKey(monitor_id);
		GsonMonitorClasses gsonMonitorClasses = getObjectFromAPI(apiRequest, GsonMonitorClasses.class);
		Integer result = (gsonMonitorClasses == null ? 0 : gsonMonitorClasses.getMonitorClass(0).getDelta());
		if (result == 0) {
			return false;
		}
		else return true;
	}
	/**
	 * Retrieve the Monitor key from the Monitor id.
	 * 
	 * @param id
	 * @return
	 */
	public String getMonitorKey(int monitor_id) {
		String apiRequest = "monitorclass/" + m_systemType + "/key";
		for (GsonMonitorClasses.MonitorClasses monitorClass : getObjectFromAPI(apiRequest, GsonMonitorClasses.class).getMonitorClasses()) {
			if (monitorClass.getMonitorId() == monitor_id)
				return monitorClass.getMonitor();
		}
		return null;
	}
	

	/********************************************************
	 * SET
	 ********************************************************/
	/********************************************************
	 * System
	 ********************************************************/
	/**
	 * Set the status of the system.
	 */
	public void setSystemStatus()
	{
		String apiRequest = "system/" + m_systemID + "/node";
		try {
			// get the state of the node
			GsonNode gsonNode = getObjectFromAPI(apiRequest, GsonNode.class);
			Iterator<String> states = gsonNode.getNodeStateList().iterator();
			String systemState = "stopped";	// Stopped
			while (states.hasNext()) {
				String rval = states.next();
				if (rval == "master") 		// We have a master
				{
					systemState = "running";		// Running
					break;
				}
				if (rval == "slave") 		// We have a master
				{
					systemState = "running";		// Running
				}
			}
			// now update
			apiRequest = "system/" + m_systemID;
			m_api.UpdateValue(apiRequest, "state", systemState);
		} catch (Exception e) {
			System.err.println("Update System State Failed: " + e.getMessage());
		}
	}
	/********************************************************
	 * Node
	 ********************************************************/
	/**
	 * Set the state of a node.
	 * 
	 * @param nodeid	The node to set the state of
	 * @param stateid	The state to set for the node
	 */
	public void setNodeState(int nodeid, int stateid)
	{
		String apiRequest = "system/" + m_systemID + "/node/" + nodeid;
		try {
			boolean results = m_api.UpdateValue(apiRequest, "stateid", Integer.toString(stateid));
			if (! results) {
				System.err.println("Failed to update node state: " + apiRequest + " to state " + stateid);
				return;
			}
			else if (results) {
				// TODO: recognize how many rows have been changed
			}
		} catch (Exception e) {
			System.err.println("API Failed: " + apiRequest + ": "+ e.getMessage());
		}
	}
	/**
	 * Update the public IP address of a node if it has changed.
	 * 
	 * @param	nodeID The node ID
	 * @param	publicIP 	The public IP address of the instance
	 * @return	True if the IP address was updated
	 */
	public boolean setNodePublicIP(String nodeID, String publicIP) {
		String apiRequest = "system/" + m_systemID + "/node";
		GsonNode gsonNode = getObjectFromAPI(apiRequest, GsonNode.class);
		for (GsonNode.Nodes nodes : gsonNode.getNodes()) {
			if (nodes.getNodeId() == nodeID) {
				if (nodes.getPublicIP() != null && nodes.getPublicIP().equalsIgnoreCase(publicIP))
					return false;
				apiRequest = "system/" + m_systemID + "/node/" + nodeID;
				return m_api.UpdateValue(apiRequest, "publicip", publicIP);
			}
		}
		return false;
	}
	/**
	 * setPrivateIP - Update the private IP of an instance. Only update the database
	 * if the new value differs from that already stored.
	 * 
	 * @param nodeID		The node ID as a string
	 * @param privateIP		The current private IP address
	 * @return	boolean 	True if the IP address changed
	 */
	public boolean setNodePrivateIP(String nodeID, String privateIP) {
		String apiRequest = "system/" + m_systemID + "/node";
		GsonNode gsonNode = getObjectFromAPI(apiRequest, GsonNode.class);
		for (GsonNode.Nodes nodes : gsonNode.getNodes()) {
			if (nodes.getNodeId() == nodeID) {
				if (nodes.getPrivateIP() != null && nodes.getPrivateIP().equalsIgnoreCase(privateIP))
					return false;
				apiRequest = "system/" + m_systemID + "/node/" + nodeID;
				return m_api.UpdateValue(apiRequest, "privateip", privateIP);
			}
		}
		return false;
	}
	/********************************************************
	 * Node State
	 ********************************************************/
	/********************************************************
	 * Monitor
	 ********************************************************/


	/********************************************************
	 * OTHER
	 ********************************************************/
	/**
	 * Map a CRM state string to a valid node state.
	 * 
	 * @param state	The CRM state
	 * @return The node state
	 */
	public String mapCRMStatus(String state)
	{
//		String query = "select State from CRMStateMap crm where crmState = '" + state + "'";
//		String apiRequest = "";
		return "";
//		return ListStringToString(getStringFromQuery(apiRequest, "fields", ""));
	}

	/**
	 * Interface to record monitor observed values. This differs from the other 
	 * entry points in that it passes the data onto the API.
	 * 
	 * @param systemID		The SystemID to update
	 * @param nodeID		The NodeID to update
	 * @param monitorID		The moitorID the value is associated with
	 * @param observation	The observed value
	 * @return True if the monitor observation was written
	 */
	public boolean monitorData(int systemID, int nodeID, int monitorID, String observation)
	{
		return m_api.MonitorValue(systemID, nodeID, getMonitorKey(monitorID), observation);
	}
	/**
	 * Interface to record observed values for a system. This differs from the other 
	 * entry points in that it passes the data onto the API.
	 * 
	 * @param systemID		The SystemID to update
	 * @param monitorID		The MonitorID the value is associated with
	 * @param observation	The observed value
	 * @return True if the monitor observation was written
	 */
	public boolean monitorData(int systemID, int monitorID, String observation)
	{
		return m_api.MonitorValue(systemID, getMonitorKey(monitorID), observation);
	}
//	public boolean bulkMonitorData(Integer monitorID, Integer systemID, Integer nodeID, String value) {
//		return bulkMonitorData(new Integer[]{monitorID}, new Integer[]{systemID}, new Integer[]{nodeID}, new String[]{value});
//	}
	/**
	 * Batch request to the API.
	 * 
	 * @param fields: the names of the variables to be passed to the API
	 * @param values: the values to the passed to the API
	 */
	public boolean bulkMonitorData(List<Integer> monitorIDs, List<Integer> systemIDs, List<Integer> nodeIDs, List<String> values) {
		String apiRequest = "monitordata";
		if ( !(monitorIDs.size() == systemIDs.size() && monitorIDs.size() == nodeIDs.size() && monitorIDs.size() == values.size()) ) {
			System.err.println("Bulk data failed: arrays must be of the same size: got "
					+ monitorIDs.size() + " monitors, " + systemIDs.size() + " systems, "
					+ nodeIDs.size() + " nodes and " + values.size() + " values.");
			return false;
		}
		List<String> fi = new ArrayList<String>();
		List<String> va = new ArrayList<String>();
		for (int i=0; i<monitorIDs.size(); i++) {
			fi.add("m[" + Integer.toString(i) + "]");
			va.add(Integer.toString(monitorIDs.get(i)));
			fi.add("s[" + Integer.toString(i) + "]");
			va.add(Integer.toString(systemIDs.get(i)));
			fi.add("n[" + Integer.toString(i) + "]");
			va.add(Integer.toString(nodeIDs.get(i)));
			fi.add("v[" + Integer.toString(i) + "]");
			va.add(values.get(i));
		}
		String[] fields = fi.toArray(new String[0]);
		String[] parameters = va.toArray(new String[0]);
		return m_api.bulkMonitorValue(apiRequest, fields, parameters);
	}
	
}