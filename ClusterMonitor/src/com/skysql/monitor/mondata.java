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
	private int							m_systemID;
	private monAPI						m_api;
	private String						m_systemType;
	private GsonLatestObservations 		m_dataChanged;
	
	/**
	 * Constructor for the monitor data class.
	 * 
	 * @param systemID	The System ID being monitored
	 */
	public mondata(int systemID)
	{
		m_systemID = systemID;
		m_api = new monAPI();
		m_systemType = "galera";
		m_dataChanged = new GsonLatestObservations();
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
	
	/**
	 * Ask the API to update or create an object, and return an object which encodes
	 * information on the modified object.
	 * 
	 * @param apiRequest
	 * @param objectClass
	 * @param pName
	 * @param pValue
	 * @return		an object which contains information on the modified elements
	 */
	private <T> T updateValue(String apiRequest, Class<T> objectClass, String[] pName, String[] pValue) {
		String getJson = m_api.updateValue(apiRequest, pName, pValue);
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
		return gsonSystem.getSystems().get(0).getProperties().getMonitorInterval();
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
		String IP = gsonSystem.getSystems().get(0).getProperties().getIPMonitor();
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
		return gsonNode == null ? null : gsonNode.getNode(0).getPrivateIP();
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
			if (gsonNode != null && gsonNode.getNode(0).getDbUserName() != null) {
				cred = new Credential(gsonNode.getNode(0).getDbUserName(),
						gsonNode.getNode(0).getDbPassword());
			} else {
				apiRequest = "system/" + m_systemID;
				GsonSystem gsonSystem = getObjectFromAPI(apiRequest, GsonSystem.class);
				if (gsonSystem != null) {
					cred = new Credential(gsonSystem.getSystem(0).getDbUserName(),
							gsonSystem.getSystem(0).getDbPassword());
				}
				else cred = new Credential("repluser", "replpassword");
			}
			return cred;
		} catch (Exception ex) {
			Logging.error("API Failed: " + apiRequest + ": " + ex.getMessage());
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
	/**
	 * Get the name of the node. If the name has not been set, returns
	 * the ID of the node.
	 * 
	 * @param NodeNo	the node number
	 * @return	the name or the ID of the node
	 */
	public String getNodeName(int NodeNo) {
		String apiRequest = "system/" + m_systemID + "/node/" + NodeNo;
		GsonNode gsonNode = getObjectFromAPI(apiRequest, GsonNode.class);
		if (gsonNode != null) {
			if (gsonNode.getNode(0).getName() != null) return gsonNode.getNode(0).getName();
			else return Integer.toString(gsonNode.getNode(0).getNodeId());
		}
		return null;
	}
	/**
	 * Get the host name of the node. If the host name has not been set, returns
	 * the ID of the node.
	 * 
	 * @param NodeNo	the node number
	 * @return	the hostname or the ID of the node
	 */
	public String getNodeHostName(int NodeNo) {
		String apiRequest = "system/" + m_systemID + "/node/" + NodeNo;
		GsonNode gsonNode = getObjectFromAPI(apiRequest, GsonNode.class);
		if (gsonNode != null) {
			if (gsonNode.getNode(0).getHostname() != null) return gsonNode.getNode(0).getHostname();
			else return Integer.toString(gsonNode.getNode(0).getNodeId());
		}
		return null;
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
		String apiRequest = "nodestate/" + m_systemType;
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
		String apiRequest = "nodestate/" + m_systemType;
		GsonNodeStates gsonNodeStates = getObjectFromAPI(apiRequest, GsonNodeStates.class);
		return gsonNodeStates == null ? null : gsonNodeStates.getIdFromState(Name);
	}
	/**
	 * Map a node state id to a state string.
	 * 
	 * @param stateId The id of the node state
	 * @return The Node State
	 */
	public String getNodeStateFromId(int stateId)
	{
		String apiRequest = "nodestate/" + m_systemType;
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
			Logging.error("Update System State Failed: " + e.getMessage());
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
			GsonUpdatedAPI gsonUpdatedAPI = updateValue(apiRequest, GsonUpdatedAPI.class, new String[]{"stateid"}, new String[]{Integer.toString(stateid)});
			if (gsonUpdatedAPI != null) {
				Logging.info(gsonUpdatedAPI.getUpdateCount() + " row(s) updated, " + gsonUpdatedAPI.getInsertedKey() + " new keys added.");
				if (gsonUpdatedAPI.getUpdateCount() == 0)
					Logging.error("Failed to update node state: " + apiRequest + " to state " + stateid);
			}
			if (gsonUpdatedAPI.getErrors() != null) throw new RuntimeException(gsonUpdatedAPI.getErrors().get(0));
			if (gsonUpdatedAPI.getWarnings() != null) throw new RuntimeException(gsonUpdatedAPI.getWarnings().get(0));
		} catch (Exception e) {
			Logging.error("API Failed: " + apiRequest + ": "+ e.getMessage());
		}
	}
	/**
	 * Update the public IP address of a node if it has changed.
	 * 
	 * @param	nodeID The node ID
	 * @param	publicIP 	The public IP address of the instance
	 * @return	True if the IP address was updated
	 */
	public boolean setNodePublicIP(int nodeID, String publicIP) {
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
	public boolean setNodePrivateIP(int nodeID, String privateIP) {
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
	/**
	 * Batch request to the API.
	 * 
	 * @param fields: the names of the variables to be passed to the API
	 * @param values: the values to the passed to the API
	 */
	public boolean bulkMonitorData(List<Integer> monitorIDs, List<Integer> systemIDs, List<Integer> nodeIDs, List<String> values) {
		String apiRequest = "monitordata";
		if ( !(monitorIDs.size() == systemIDs.size() && monitorIDs.size() == nodeIDs.size() && monitorIDs.size() == values.size()) ) {
			Logging.error("Bulk data failed: arrays must be of the same size: got "
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
	/**
	 * Compare the date when the current instance last updated the objects in the
	 * current system with the last update date retrieved from the API. If necessary,
	 * the updated objects are saved in place of the older ones. If this happens,
	 * a return value of true is returned.
	 * 
	 * @return		true if the objects have been updated, false otherwise
	 */
	public boolean testChanges() {
		String now = m_dataChanged.getSystemUpdateDate(m_systemID);
		String json = m_api.restModified("system/" + m_systemID, null, null, now);
		boolean isChanged = (json == "" ? false : true);
		if (isChanged) {
			GsonSystem gsonSystem = GsonManager.fromJson(json, GsonSystem.class);
			m_dataChanged.setLastSystem(gsonSystem);
		}
		boolean toUpdate = isChanged;
		Iterator<Integer> it = getNodeList().iterator();
		while (it.hasNext()) {
			Integer nodeid = it.next();
			now = m_dataChanged.getNodeUpdateDate(m_systemID, nodeid);
			json = m_api.restModified("system/" + m_systemID + "/node/" + nodeid, null, null, now);
			isChanged = (json == "" ? false : true);
			if (isChanged) {
				GsonNode gsonNode = GsonManager.fromJson(json, GsonNode.class);
				m_dataChanged.setLastNode(gsonNode);
			}
			toUpdate = toUpdate || isChanged;
		}
		return toUpdate;
	}
}