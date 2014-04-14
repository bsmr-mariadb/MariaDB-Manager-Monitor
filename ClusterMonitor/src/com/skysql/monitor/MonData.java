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
 * Copyright 2012-2014 SkySQL Corporation Ab
 */

package com.skysql.monitor;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.skysql.java.GsonLatestObservations;
import com.skysql.java.GsonManager;
import com.skysql.java.GsonMonitorClasses;
import com.skysql.java.GsonNode;
import com.skysql.java.GsonNodeStates;
import com.skysql.java.GsonProvisionedNode;
import com.skysql.java.GsonSystem;
import com.skysql.java.GsonUpdatedAPI;
import com.skysql.java.Logging;
import com.skysql.java.MonAPI;


/**
 * Interface to the monitoring database, this is the database that holds
 * the definition of what to Monitor and into which the monitored values
 * are written.
 * 
 * @author Mark Riddoch, Massimo Siani
 *
 */
public class MonData {
	private int							m_systemID;
	private MonAPI						m_api;
	private String						m_systemType;
	private GsonLatestObservations 		m_dataChanged;
	private final int					m_monitorApiID = 3;
	
	/**
	 * Constructor for the Monitor data class.
	 * 
	 * @param systemID	The System ID being monitored
	 */
	public MonData(int systemID)
	{
		m_systemID = systemID;
		m_api = new MonAPI(m_monitorApiID);
		m_systemType = "galera";
		m_dataChanged = new GsonLatestObservations();
	}
	/**
	 * Constructor used when the system id is not known.
	 */
	public MonData()
	{
		this(0);
	}
	
	/**
	 * Fetch the Java object that corresponds to an API URI with GET method.
	 * 
	 * @param apiRequest		the API URI
	 * @param objectClass		the class of the Java object, e.g. MyClass.class
	 * @return					the Java object
	 */
	private <T> T getObjectFromAPI(String apiRequest, Class<T> objectClass) {
		String getJson = m_api.getReturnedJson(apiRequest, null, null);
		T object = GsonManager.fromJson(getJson, objectClass);
		return object;
	}
	/**
	 * Fetch the Java object that corresponds to an API URI with GET method.
	 * Allows to specify the date that will be specified in the If-Modified-Since.
	 * 
	 * @param apiRequest		the API URI
	 * @param objectClass		the class of the Java object, e.g. MyClass.class
	 * @param lastUpdate		the date to set in the If-Modified-Since header, may be null or empty
	 * @return					the Java object
	 */
	private <T> T getObjectFromAPI(String apiRequest, Class<T> objectClass, String lastUpdate) {
		String getJson = m_api.getReturnedJson(apiRequest, null, null, lastUpdate);
		T object = GsonManager.fromJson(getJson, objectClass);
		return object;
	}
	
	/**
	 * Fetch the Java object that corresponds to an API URI with GET method.
	 * Allows to specify some parameters to reduce network traffic.
	 * 
	 * @param apiRequest		the API URI
	 * @param pName				an array of the names of the parameters
	 * @param pValue			an array of the values of the parameters
	 * @param objectClass		the class of the Java object, e.g. MyClass.class
	 * @return					the Java object
	 */
	private <T> T getObjectFromAPI(String apiRequest, String[] pName, String[] pValue, Class<T> objectClass) {
		String getJson = m_api.getReturnedJson(apiRequest, pName, pValue);
		T object = GsonManager.fromJson(getJson, objectClass);
		return object;
	}
	
	/**
	 * Return the Node object that the current instance stored in the cache.
	 * 
	 * @param nodeID	the Node id
	 * @return			the Node object, may be null
	 */
	private GsonNode getNodeCached(int nodeID) {
		return m_dataChanged.getNode(m_systemID, nodeID);
	}
	
	/**
	 * Ask the API to update or create an object, and return an object which encodes
	 * information on the modified object. Sends a PUT request.
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
	
	/**
	 * Registers some information about a component in the API.
	 * If a parameter is <code>null</code>, it is not registered nor updated, but it is
	 * not deleted if already registered in the API.
	 * 
	 * @param component	the name of the component to register in the API
	 * @param version	the version number. Typical format is Major.minor-build
	 * @param release	the release number
	 * @param date		the build date of the current version
	 */
	public void registerAPI(String component, String version, String release, String date) {
		String apiRequest = "system/0/node/0/component/"+ component.toLowerCase() + "/property/";
		String property = "";
		if (component != null) {
			property = "name";
			m_api.updateValue(apiRequest + property, new String[]{"value"}, new String[]{component});
		}
		if (version != null) {
			property = "version";
			m_api.updateValue(apiRequest + property, new String[]{"value"}, new String[]{version});
		}
		if (release != null) {
			property = "release";
			m_api.updateValue(apiRequest + property, new String[]{"value"}, new String[]{release});
		}
		if (date != null) {
			property = "date";
			m_api.updateValue(apiRequest + property, new String[]{"value"}, new String[]{date});
		}
	}
	
	
	/********************************************************
	 * GET
	 ********************************************************/
	/********************************************************
	 * System
	 ********************************************************/
	/**
	 * Fetch the list of System ID's to Monitor.
	 * 
	 * @return The list of SystemIDs defined in the database
	 */
	public List<Integer> getSystemList() {
		String apiRequest = "system";
		String[] fields = new String[] {"fields"};
		String[] values = new String[] {"systemid"};
		GsonSystem gsonSystem = getObjectFromAPI(apiRequest, fields, values, GsonSystem.class);
		return gsonSystem == null ? null : gsonSystem.getSystemIdList();
	}
	/**
	 * Fetch the Monitor probe interval.
	 * 
	 * @return The Monitor interval in seconds
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
	 * Compare the date when the current instance last updated the objects in the
	 * current system with the last update date retrieved from the API. If necessary,
	 * the updated objects are saved in place of the older ones. If this happens,
	 * a return value of true is returned.
	 * 
	 * @return		true if the objects have been updated, false otherwise
	 */
	public boolean getProvisionedNodes() {
		Iterator<Integer> nodeIt = getNodeList().iterator();
		boolean isChanged = true;
		while (nodeIt.hasNext()) {
			Integer nodeID = nodeIt.next();
			String now = m_dataChanged.getNodeUpdateDate(m_systemID, nodeID);
			GsonProvisionedNode gsonProvisionedNode = getObjectFromAPI("provisionednode", GsonProvisionedNode.class, now);
			isChanged = (gsonProvisionedNode == null || gsonProvisionedNode.getProvisionedNodes() == null
					|| gsonProvisionedNode.getProvisionedNodes().isEmpty() ? false : true);
			if (gsonProvisionedNode != null && gsonProvisionedNode.getProvisionedNodes() != null &&
					gsonProvisionedNode.getProvisionedNodes().isEmpty()) {
				m_dataChanged.clearAllNodes(m_systemID);
			}
			if (isChanged) {
				m_dataChanged.clearAllNodes(m_systemID);
				GaleraStatusMonitor.removeSystem((Integer)m_systemID);
				Iterator<GsonProvisionedNode.ProvisionedNodes> it = gsonProvisionedNode.getProvisionedNodes().iterator();
				while (it.hasNext()) {
					GsonProvisionedNode.ProvisionedNodes provisionedNode = it.next();
					String ser = "{ \"node\": " + GsonManager.toJson(provisionedNode) + "}";
					GsonNode gsonNode = GsonManager.fromJson(ser, GsonNode.class);
					m_dataChanged.setLastNode(gsonNode);
				}
				break;
			}
		}
		return isChanged;
	}
	/**
	 * Return the list of Node id's to Monitor.
	 * 
	 * @return The list of nodes in the database
	 */
	public List<Integer> getNodeList()
	{
		String apiRequest = "system/" + m_systemID + "/node";
		String[] fields = {"fields"};
		String[] values = {"nodeid"};
		GsonNode gsonNode = getObjectFromAPI(apiRequest, fields, values, GsonNode.class);
		return gsonNode == null ? null : gsonNode.getNodeIdList();
	}
	/**
	 * Return the list of Node id's to Monitor as saved by the current instance.
	 * 
	 * @return The list of nodes in the instance cache
	 */
	public List<Integer> getNodeListCached() {
		GsonNode gsonNode = m_dataChanged.getAllNodes(m_systemID);
		return gsonNode == null ? null : gsonNode.getNodeIdList();
	}
	/**
	 * Get the private IP address of the specified Node.
	 * 
	 * @param NodeNo	The Node number
	 * @return The private IP address as a string
	 */
	public String getNodePrivateIP(int NodeNo)
	{
		GsonNode gsonNode = getNodeCached(NodeNo);
		return gsonNode == null ? null : gsonNode.getNode(0).getPrivateIP();
	}
	/**
	 * Get the credentials for the specified Node.
	 * 
	 * @param NodeNo The Node number to return the credentials of
	 * @return The Credentials for the Node
	 */
	public Credential getNodeMonitorCredentials(int NodeNo)
	{
		Credential cred;
		try {
			GsonNode gsonNode = getNodeCached(NodeNo);
			if (gsonNode != null && (! gsonNode.getNode(0).getDbUserName().equalsIgnoreCase(""))
					&& gsonNode.getNode(0).getDbUserName() != null) {
				cred = new Credential(gsonNode.getNode(0).getDbUserName(),
						gsonNode.getNode(0).getDbPassword());
			} else {
				cred = new Credential("repluser", "replpassword");
			}
			return cred;
		} catch (Exception ex) {
			Logging.error("Failed to retrieve node credentials for Node ID " + NodeNo);
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
	 * Return the list of the states of the nodes in the system.
	 * 
	 * @return	The list of states
	 */
	public List<String> getNodeStates() {
		String apiRequest = "system/" + m_systemID + "/node";
		String[] fields = {"fields"};
		String[] values = {"state"};
		GsonNode gsonNode = getObjectFromAPI(apiRequest, fields, values, GsonNode.class);
		List<String> result = new ArrayList<String>();
		for (int i=0; i<gsonNode.getNodes().size(); i++) {
			result.add(gsonNode.getNodes().get(i).getState());
		}
		return result;
	}
	
	/**
	 * Fetch the Node state.
	 * 
	 * @param nodeID		the Node ID
	 * @return				a string representing the state
	 */
	public String getNodeState(int nodeID) {
		String apiRequest = "system/" + m_systemID + "/node/" + nodeID;
		String[] fields = {"fields"};
		String[] values = {"state"};
		String result;
		GsonNode gsonNode = getObjectFromAPI(apiRequest, fields, values, GsonNode.class);
		if (gsonNode == null) {
			result = "down";
		} else {
			result = gsonNode.getNode(0).getState();
		}
		return result;
	}
	/**
	 * Get the name of the Node. If the name has not been set, returns
	 * the ID of the Node.
	 * 
	 * @param NodeNo	the Node number
	 * @return			the name or the ID of the Node
	 */
	public String getNodeName(int NodeNo) {
		GsonNode gsonNode = getNodeCached(NodeNo);
		if (gsonNode != null) {
			if (gsonNode.getNode(0).getName() != null) return gsonNode.getNode(0).getName();
			else return Integer.toString(gsonNode.getNode(0).getNodeId());
		}
		return null;
	}
	/**
	 * Get the host name of the Node. If the host name has not been set, returns
	 * the IP of the Node.
	 * 
	 * @param NodeNo	the Node number
	 * @return			the hostname or the IP of the Node
	 */
	public String getNodeHostName(int NodeNo) {
		GsonNode gsonNode = getNodeCached(NodeNo);
		String result = null;
		if (gsonNode != null) {
			if (( result =  gsonNode.getNode(0).getHostname()) != null && ! result.isEmpty()) {
			} else if (( result = gsonNode.getNode(0).getPrivateIP() ) != null && ! result.isEmpty()) {
			} else if (( result = gsonNode.getNode(0).getPublicIP() ) != null && ! result.isEmpty()) {
			} else {
				result = null;
			}
		}
		return result;
	}
	/********************************************************
	 * Node States
	 ********************************************************/
	/**
	 * Return the list of valid Node states.
	 * 
	 * @return The set of defined Node states.
	 */
	public List<String> getNodeValidStates()
	{
		String apiRequest = "nodestate/" + m_systemType;
		GsonNodeStates gsonNodeStates = getObjectFromAPI(apiRequest, GsonNodeStates.class);
		if (gsonNodeStates == null) return null;
		return gsonNodeStates.getDescriptionList();
	}
	/**
	 * Map a Node state string to a state id.
	 * 
	 * @param Name The name of the Node state
	 * @return The Node State
	 */
	public int getNodeStateId(String Name)
	{
		String apiRequest = "nodestate/" + m_systemType;
		GsonNodeStates gsonNodeStates = getObjectFromAPI(apiRequest, GsonNodeStates.class);
		return gsonNodeStates == null ? null : gsonNodeStates.getIdFromState(Name);
	}
	/**
	 * Map a Node state id to a state string.
	 * 
	 * @param stateId The id of the Node state
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
	 * Find if the list of monitors has changed since the last time it has been retrieved.
	 * If yes, the new list is saved in an appropriate instance.
	 * 
	 * @return		true if the list of monitors has changed, false otherwise
	 */
	public boolean saveMonitorChanges() {
		String apiRequest = "monitorclass/" + m_systemType;
		String now = m_dataChanged.getMonitorUpdateDate();
		GsonMonitorClasses gsonMonitorClasses = getObjectFromAPI(apiRequest, GsonMonitorClasses.class, now);
		boolean isChanged = (gsonMonitorClasses == null || gsonMonitorClasses.getMonitorClass(0) == null ? false : true);
		if (isChanged) {
			m_dataChanged.setLastMonitor(gsonMonitorClasses);
		}
		return isChanged;
	}
	/**
	 * Get the object with the list of all cached Monitor classes.
	 * This method does not call the API, but looks in the current cache.
	 * @see <code>saveMonitorChanges</code> to store the Monitor metadata
	 * in the local cache.
	 * 
	 * @return		the Monitor class object
	 */
	private GsonMonitorClasses getMonitorClassesCached() {
		return m_dataChanged.getAllMonitorClasses();
	}
	/**
	 * Get the object corresponding to the given Monitor ID.
	 * This method does not call the API, but looks in the current cache.
	 * @see <code>saveMonitorChanges</code> to store the Monitor metadata
	 * in the local cache.
	 * 
	 * @param monitorID		the Monitor ID
	 * @return				the Monitor class object
	 */
	private GsonMonitorClasses getMonitorClassesCached(Integer monitorID) {
		return m_dataChanged.getMonitorClasses(monitorID);
	}
	/**
	 * Retrieve the id of a Monitor from the key of the Monitor itself.
	 * This method does not call the API, but looks in the current cache.
	 * @see <code>saveMonitorChanges</code> to store the Monitor metadata
	 * in the local cache.
	 * 
	 * @param key		the Monitor key
	 * @return			the Monitor id, or -1 if the key is not found
	 */
	private int getMonitorId (String key) {
		int result = -1;
		GsonMonitorClasses gsonMonitorClasses = getMonitorClassesCached();
		Iterator<GsonMonitorClasses.MonitorClasses> it = gsonMonitorClasses.getMonitorClasses().iterator();
		while (it.hasNext()) {
			GsonMonitorClasses.MonitorClasses monitorClass = it.next();
			if (monitorClass.getMonitor().equalsIgnoreCase(key)) {
				result = monitorClass.getMonitorId();
				break;
			}
		}
		return result;
	}
	/**
	 * Return the list of all available Monitor Id's for the given system type.
	 * This method does not call the API, but looks in the current cache.
	 * @see <code>saveMonitorChanges</code> to store the Monitor metadata
	 * in the local cache.
	 * 
	 * @return The list of monitorID's defined in the database
	 */
	public List<Integer> getMonitorIdList()
	{
		GsonMonitorClasses gsonMonitorClasses = getMonitorClassesCached();
		return gsonMonitorClasses == null ? null : gsonMonitorClasses.getMonitorIdList();
	}
	/**
	 * Get the SQL command (or command string) associated with a particular Monitor.
	 * 
	 * Although originally a simple SQL string for a Monitor to execute, other
	 * Monitor types have reused the string to contain Monitor specific data.
	 * This method does not call the API, but looks in the current cache.
	 * @see <code>saveMonitorChanges</code> to store the Monitor metadata
	 * in the local cache.
	 * 
	 * @param monitor_id The Monitor ID to return the SQL data string for
	 * @return The "SQL" field of the Monitor
	 */
	public String getMonitorSQL(int monitor_id)
	{
		GsonMonitorClasses gsonMonitorClasses = getMonitorClassesCached(monitor_id);
		if (gsonMonitorClasses == null || gsonMonitorClasses.getMonitorClass(0) == null) return null;
		return gsonMonitorClasses.getMonitorClass(0).getSql();
	}
	/**
	 * Fetch the Monitor probe interval.
	 * This method does not call the API, but looks in the current cache.
	 * @see <code>saveMonitorChanges</code> to store the Monitor metadata
	 * in the local cache.
	 * 
	 * @return The Monitor interval in seconds
	 */
	public int getMonitorClassInterval(String monitorKey)
	{
		int monitor_id = getMonitorId(monitorKey);
		GsonMonitorClasses gsonMonitorClasses = getMonitorClassesCached(monitor_id);
		return gsonMonitorClasses == null ? null : gsonMonitorClasses.getMonitorClass(0).getInterval();
	}
	/**
	 * Fetch the id of a particular Monitor.
	 * 
	 * @param monitorKey	The Monitor name
	 * @return The monitor_id of the named Monitor or -1 if the Monitor was not found
	 */
	public int getNamedMonitor(String monitorKey)
	{
		String apiRequest = "monitorclass/" + m_systemType + "/key/" + monitorKey;
		GsonMonitorClasses gsonMonitorClasses = getObjectFromAPI(apiRequest, GsonMonitorClasses.class);
		return gsonMonitorClasses == null ? null : gsonMonitorClasses.getMonitorClass(0).getMonitorId();
	}
	/**
	 * Return the type, and hence Monitor class, of a particular Monitor.
	 * This method does not call the API, but looks in the current cache.
	 * @see <code>saveMonitorChanges</code> to store the Monitor metadata
	 * in the local cache.
	 * 
	 * @param id	The Monitor ID
	 * @return The type field for the Monitor, e.g. SQL, CMD, CRM etc.
	 */
	public String getMonitorType(int monitor_id)
	{
		GsonMonitorClasses gsonMonitorClasses = getMonitorClassesCached(monitor_id);
		if (gsonMonitorClasses == null || gsonMonitorClasses.getMonitorClass(0) == null) return null;
		return gsonMonitorClasses == null ? null : gsonMonitorClasses.getMonitorClass(0).getMonitorType();
	}
	/**
	 * Is the system Monitor value cumulative or an average of all the nodes in the system?
	 * This method does not call the API, but looks in the current cache.
	 * @see <code>saveMonitorChanges</code> to store the Monitor metadata
	 * in the local cache.
	 * 
	 * @param id	The Monitor ID
	 * @return		True if the system value of a Monitor is an average of all the nodes in the system
	 */
	public boolean isMonitorSystemAverage(int monitor_id)
	{
		GsonMonitorClasses gsonMonitorClasses = getMonitorClassesCached(monitor_id);
		Integer result = (gsonMonitorClasses == null ? 0 : gsonMonitorClasses.getMonitorClass(0).getSystemAverage());
		if (result == 1) return true;
		return false;
	}
	/**
	 * Is the monitored value a cumulative number or a snapshot value. This allows monitors
	 * to return values that are either the value in the database or the difference between
	 * the current value and the previous value.
	 * This method does not call the API, but looks in the current cache.
	 * @see <code>saveMonitorChanges</code> to store the Monitor metadata
	 * in the local cache.
	 * 
	 * @param monitor_id	The Monitor ID to check
	 * @return True of the Monitor is a delta of observed values
	 */
	public Boolean isMonitorDelta(int monitor_id)
	{
		GsonMonitorClasses gsonMonitorClasses = getMonitorClassesCached(monitor_id);
		Integer result = (gsonMonitorClasses == null ? 0 : gsonMonitorClasses.getMonitorClass(0).getDelta());
		if (result == 0) {
			return false;
		}
		else return true;
	}
	/**
	 * Retrieve the Monitor key from the Monitor id.
	 * This method does not call the API, but looks in the current cache.
	 * @see <code>saveMonitorChanges</code> to store the Monitor metadata
	 * in the local cache.
	 * 
	 * @param id
	 * @return
	 */
	public String getMonitorKey(int monitor_id) {
		GsonMonitorClasses gsonMonitorClasses = getMonitorClassesCached(monitor_id);
		if (gsonMonitorClasses == null || gsonMonitorClasses.getMonitorClass(0) == null) return null;
		return gsonMonitorClasses.getMonitorClass(0).getMonitor();
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
			// get the state of the Node
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
			m_api.updateValue(apiRequest, "state", systemState);
		} catch (Exception e) {
			Logging.error("Update System State Failed: " + e.getMessage());
		}
	}
	/**
	 * Set the state of the system.
	 * 
	 * @param state		the state to which the system must be set
	 */
	public void setSystemState(String state) {
		String apiRequest = "system/" + m_systemID;
		String[] pName = new String[] {"systemtype", "state"};
		String[] pValue = new String[] {m_systemType, state};
		try {
			GsonUpdatedAPI gsonUpdatedAPI = updateValue(apiRequest, GsonUpdatedAPI.class, pName, pValue);
			if (gsonUpdatedAPI != null) {
				if (gsonUpdatedAPI.getUpdateCount() == 0)
					Logging.error("Failed to update system " + m_systemID + " to state " + state);
			}
			if (gsonUpdatedAPI.getErrors() != null) throw new RuntimeException(gsonUpdatedAPI.getErrors().get(0));
			if (gsonUpdatedAPI.getWarnings() != null) throw new RuntimeException(gsonUpdatedAPI.getWarnings().get(0));
		} catch (Exception e) {
			Logging.error("API Failed: " + apiRequest + ": cannot update system state to " + state);
		}
	}
	/********************************************************
	 * Node
	 ********************************************************/
	/**
	 * Set the state of a Node.
	 * 
	 * @param nodeid	The Node to set the state of
	 * @param stateid	The state to set for the Node
	 */
	public void setNodeState(int nodeid, int stateid)
	{
		String apiRequest = "system/" + m_systemID + "/node/" + nodeid;
		String[] pName = new String[] {"stateid"};
		String[] pValue = new String[] {Integer.toString(stateid)};
		try {
			GsonUpdatedAPI gsonUpdatedAPI = updateValue(apiRequest, GsonUpdatedAPI.class, pName, pValue);
			if (gsonUpdatedAPI != null) {
				if (gsonUpdatedAPI.getUpdateCount() == 0)
					Logging.error("Failed to update Node " + nodeid + " of system " + m_systemID + " to state " + stateid);
			}
			if (gsonUpdatedAPI.getErrors() != null) throw new RuntimeException(gsonUpdatedAPI.getErrors().get(0));
			if (gsonUpdatedAPI.getWarnings() != null) throw new RuntimeException(gsonUpdatedAPI.getWarnings().get(0));
		} catch (Exception e) {
			Logging.error("API Failed: " + apiRequest + ": cannot set Node state to " + stateid);
		}
	}
	/**
	 * Sets the database properties.
	 * 
	 * @param nodeid		The Node to set the state of
	 * @param dbType		The database type
	 * @param dbVersion		The database version
	 */
	public void setNodeDatabaseProperties(int nodeid, String dbType, String dbVersion) {
		String apiRequest = "system/" + m_systemID + "/node/" + nodeid;
		String[] pName = new String[] {"dbtype", "dbversion"};
		String[] pValue = new String[] {dbType, dbVersion};
		try {
			GsonUpdatedAPI gsonUpdatedAPI = updateValue(apiRequest, GsonUpdatedAPI.class, pName, pValue);
			if (gsonUpdatedAPI != null) {
				if (gsonUpdatedAPI.getUpdateCount() == 0)
					Logging.error("Failed to update Node database properties on Node " + nodeid + " of system " + m_systemID);
			}
			if (gsonUpdatedAPI.getErrors() != null) throw new RuntimeException(gsonUpdatedAPI.getErrors().get(0));
			if (gsonUpdatedAPI.getWarnings() != null) throw new RuntimeException(gsonUpdatedAPI.getWarnings().get(0));
		} catch (Exception e) {
			Logging.error("API Failed: " + apiRequest + ": cannot update Node database properties");
		}
	}
	/**
	 * Update the public IP address of a Node if it has changed.
	 * 
	 * @param	nodeID The Node ID
	 * @param	publicIP 	The public IP address of the instance
	 * @return	True if the IP address was updated
	 */
	public boolean setNodePublicIP(int nodeID, String publicIP) {
		GsonNode gsonNode = getNodeCached(nodeID);
		if (gsonNode != null && gsonNode.getNode(0).getPublicIP() != null
				&& gsonNode.getNode(0).getPublicIP().equalsIgnoreCase(publicIP)) {
			return false;
		}
		String apiRequest = "system/" + m_systemID + "/node/" + nodeID;
		return m_api.updateValue(apiRequest, "publicip", publicIP);
	}
	/**
	 * setPrivateIP - Update the private IP of an instance. Only update the database
	 * if the new value differs from that already stored.
	 * 
	 * @param nodeID		The Node ID as a string
	 * @param privateIP		The current private IP address
	 * @return	boolean 	True if the IP address changed
	 */
	public boolean setNodePrivateIP(int nodeID, String privateIP) {
		try {
			if (getNodeCached(nodeID).getNode(0).getPrivateIP().equalsIgnoreCase(privateIP)) {
				return false;
			} else {
				String apiRequest = "system/" + m_systemID + "/node/" + nodeID;
				return m_api.updateValue(apiRequest, "privateip", privateIP);
			}
		} catch (Exception e) {
			return false;
		}
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
	 * Map a CRM state string to a valid Node state.
	 * 
	 * @param state	The CRM state
	 * @return The Node state
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
	 * @return True if the Monitor observation was written
	 */
	public boolean monitorData(int systemID, int monitorID, String observation)
	{
		return m_api.MonitorValue(systemID, getMonitorKey(monitorID), observation);
	}
	/**
	 * Batch request to the API. Adds the current timestamp to the data being sent, despite
	 * it is an optional parameter for the API. 
	 * 
	 * @param fields: the names of the variables to be passed to the API
	 * @param values: the values to the passed to the API
	 */
	public boolean bulkMonitorData(List<Integer> monitorIDs, Integer systemID, Integer nodeID, List<String> values) {
		String apiRequest = "monitordata";
		if ( !(monitorIDs.size() == values.size()) ) {
			Logging.error("Bulk data failed: arrays must be of the same size: got "
					+ monitorIDs.size() + " Monitor IDs, " + values.size() + " values.");
			return false;
		}
		List<String> fi = new ArrayList<String>();
		List<String> va = new ArrayList<String>();
		fi.add("systemid");
		va.add(Integer.toString(systemID));
		fi.add("nodeid");
		va.add(Integer.toString(nodeID));
		fi.add("timestamp");
		va.add(Long.toString((new Date()).getTime() / 1000));
		for (int i=0; i<monitorIDs.size(); i++) {
			fi.add("m[" + Integer.toString(i) + "]");
			va.add(Integer.toString(monitorIDs.get(i)));
			fi.add("v[" + Integer.toString(i) + "]");
			va.add(values.get(i));
		}
		String[] fields = fi.toArray(new String[0]);
		String[] parameters = va.toArray(new String[0]);
		return m_api.bulkMonitorValue(apiRequest, fields, parameters);
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
					+ monitorIDs.size() + " Monitor IDs, " + systemIDs.size() + " systems, "
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