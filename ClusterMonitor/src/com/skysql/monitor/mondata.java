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

import java.util.*;
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
	
	/**
	 * Constructor for the monitor data class
	 * 
	 * @param systemID	The System ID being monitored
	 * @param dbfile	The SQLite database file
	 */
	public mondata(int systemID, String dbfile)
	{
		m_systemID = systemID;
		m_api = new monAPI();
	}
	/**
	 * Constructor used when the monitor is being used for when the system id is not known.
	 * 
	 * @param dbfile	The SQLite database file
	 */
	public mondata(String dbfile)
	{
		this(-1, dbfile);
	}
	
	/**
	 * Returns the result of the query in a list.
	 * 
	 * @param query to send
	 * @return a list of the results, null on errors
	 */
	private List<Integer> getIntegerFromQuery(String request, String parName, String parValue) {
		String[] newparName = {parName};
		String[] newparValue = {parValue};
		return getIntegerFromQuery(request, newparName, newparValue);
	}
	private List<Integer> getIntegerFromQuery(String request, String parName[], String parValue[]) {
		try {
			List<Integer> ilist = new ArrayList<Integer>();
			Iterator<String> results = m_api.SystemValue(request, parName, parValue).iterator();
			while (results.hasNext())
			{
				ilist.add(Integer.parseInt(results.next()));
			}
			return ilist;
		}
		catch (Exception ex)
		{
			System.err.println("Failed: " + request + ": " + ex.getMessage());
			return null;
		}
	}
	
	/**
	 * Returns the result of the query in a list.
	 * 
	 * @param query to send
	 * @return a list of the results, null on errors
	 */
	private List<String> getStringFromQuery(String request, String parName, String parValue) {
		String[] newparName = {parName};
		String[] newparValue = {parValue};
		return getStringFromQuery(request, newparName, newparValue);
	}
	private List<String> getStringFromQuery(String request, String parName[], String parValue[]) {
		try {
			List<String> slist = new ArrayList<String>();
			Iterator<String> results = m_api.SystemValue(request, parName, parValue).iterator();
			while (results.hasNext())
			{
				slist.add(results.next());
			}
			return slist;
		}
		catch (Exception ex)
		{
			System.err.println("Failed: " + request + ": " + ex.getMessage());
			return null;
		}
	}
	
	/**
	 * Returns the first element of a list of String.
	 * 
	 * @param list
	 * @return the first element
	 */
	private String ListStringToString(List<String> strlist) {
		try {
			String[] result = new String[strlist.size()];
			strlist.toArray(result);
			return result[0];
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Returns the first element of a list of Integer.
	 * 
	 * @param list
	 * @return the first element
	 */
	private Integer ListIntegerToInteger(List<Integer> ilist) {
		try {
			Integer[] result = new Integer[ilist.size()];
			ilist.toArray(result);
			return result[0];
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Return the list of System ID's to monitor
	 * 
	 * @return The list of SystemIDs defined in the database
	 */
	public List<Integer> getSystemList()
	{
		String apiRequest = "system";
		return getIntegerFromQuery(apiRequest, "fields", "system");
	}
	
	/**
	 * Return the list of node numbers to monitor
	 * 
	 * @return The list of nodes in the database
	 */
	public List<Integer> getNodeList()
	{
		String apiRequest = "system/" + m_systemID + "/node";
		return getIntegerFromQuery(apiRequest, "fields", "id");
	}
	
	/**
	 * Return the list of monitors
	 * 
	 * @return The list of monitorID's defined in the database
	 */
	public List<Integer> getMonitorList()
	{
		String apiRequest = "monitorclass";
		return getIntegerFromQuery(apiRequest, "fields", "id");
	}
	
	/**
	 * Get the private IP address of the specified node
	 * 
	 * @param NodeNo	The node number
	 * @return The private IP address as a string
	 */
	public String getNodePrivateIP(int NodeNo)
	{
		String apiRequest = "system/" + m_systemID + "/node/" + NodeNo;
		return ListStringToString(getStringFromQuery(apiRequest, "fields", "privateIP"));
	}
	
	/**
	 * Get the credentials for the specified node
	 * 
	 * @param NodeNo The node number to return the credentials of
	 * @return The Credentials for the node
	 */
	public Credential getNodeMonitorCredentials(int NodeNo)
	{
		String apiRequest = "system/" + m_systemID + "/node/" + NodeNo;
		String fields = "fields";
		String values = "username";
		try {
			// we have to call the API twice: json are unordered
			List<String> result = new ArrayList<String>();
			result.add(ListStringToString(getStringFromQuery(apiRequest, fields, values)));
			values = "passwd";
			result.add(ListStringToString(getStringFromQuery(apiRequest, fields, values))); 
			Iterator<String> iresults = result.iterator();
			Credential cred;
			if (iresults.hasNext())
				cred = new Credential(iresults.next(), iresults.next());
			else
				cred = new Credential("repluser", "repw");
			return cred;
		} catch (Exception ex) {
			System.err.println("API Failed: " + apiRequest + ": " + ex.getMessage());
			return null;
		}
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
		String apiRequest = "monitorclass/" + monitor_id;
		return ListStringToString(getStringFromQuery(apiRequest, "fields", "sql"));
	}

	/**
	 * Is the monitored value a cumulative number or a snapshot value. This allows monitors
	 * to return values that are either the value in the database or the difference between
	 * the current value and the previous value.
	 * 
	 * @param monitor_id	The monitor ID to check
	 * @return True of the monitor is a delta of observed values
	 */
	public Boolean monitorIsDelta(int monitor_id)
	{
		String apiRequest = "monitorclass/" + monitor_id;
		String result = ListStringToString(getStringFromQuery(apiRequest, "fields", "delta"));
		if (result.equalsIgnoreCase("0")) {
			return false;
		}
		else return true;
	}
	
	/**
	 * Fetch the monitor probe interval
	 * 
	 * @return The monitor interval in seconds
	 */
	public int monitorInterval()
	{
		String apiRequest = "system/" + m_systemID + "/property/MonitorInterval";
		String[] fields = {"system", "propertyname"};
		String[] values = {Integer.toString(m_systemID), "MonitorInterval"};
		Integer result = ListIntegerToInteger(getIntegerFromQuery(apiRequest, fields, values));
		if (result == null) {
			return 30;
		}
		return result;
	}
	
	/**
	 * Fetch the name of a particular monitor
	 * 
	 * @param name	The monitor name
	 * @return The monitor_id of the named monitor or -1 if the monitor was not found
	 */
	public int getNamedMonitor(String name)
	{
		String apiRequest = "monitorclass/" + name;
		return ListIntegerToInteger(getIntegerFromQuery(apiRequest, "fields", "id"));
	}
	
	/**
	 * Return the type, and hence monitor class, of a particular monitor.
	 * 
	 * @param id	The monitor ID
	 * @return The type field for the monitor, e.g. SQL, CMD, CRM etc.
	 */
	public String getMonitorType(int id)
	{
		String apiRequest = "monitorclass/" + id;
		return ListStringToString(getStringFromQuery(apiRequest, "fields", "monitortype"));
	}
	
	/**
	 * Is the system monitor value cumulative or an average of all the nodes in the system
	 * 
	 * @param id	The Monitor ID
	 * @return		True if the system value of a monitor is an average of all the nodes in the system
	 */
	public boolean getMonitorSystemAverage(int id)
	{
		String apiRequest = "monitorclass/" + id;
		String result = ListStringToString(getStringFromQuery(apiRequest, "fields", "systemaverage"));
		if (result.equalsIgnoreCase("1")) return true;
		return false;
	}
	
	/**
	 * Map a node state string to a state value
	 * 
	 * @param Name The name of the node state
	 * @return The Node State
	 */
	public int getStateValue(String Name)
	{
		String apiRequest = "nodestate/" + Name;
		return ListIntegerToInteger(getIntegerFromQuery(apiRequest, "fields", "state"));
	}
	
	/**
	 * Return the list of valid node states
	 * 
	 * @return The set of defined node states
	 */
	public List<String> getValidStates()
	{
		String apiRequest = "nodestate";
		return getStringFromQuery(apiRequest, "fields", "description");
	}
	
	/**
	 * Set the state of a node
	 * 
	 * @param nodeid	The node to set the state of
	 * @param stateid	The state to set for the node
	 */
	public void setNodeState(int nodeid, int stateid)
	{
		String query = "update Node set State = " + stateid + " where nodeid = " + nodeid + " and SystemID = " + m_systemID;
		String apiRequest = "system/" + m_systemID + "/node/" + nodeid;
		try {
			boolean results = m_api.UpdateValue(apiRequest, "state", Integer.toString(stateid));
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
	 * Map a CRM state string to a valid node state.
	 * 
	 * @param state	The CRM state
	 * @return The node state
	 */
	public String mapCRMStatus(String state)
	{
		String query = "select State from CRMStateMap crm where crmState = '" + state + "'";
		String apiRequest = "";
		return ListStringToString(getStringFromQuery(apiRequest, "fields", ""));
	}
	
	/**
	 * Set the status of the system
	 */
	public void setSystemStatus()
	{
		String query = "select State from Node where SystemID = " + m_systemID;
		String apiRequest = "system/" + m_systemID + "/node";
		try {
			// get the state of the node
			Iterator<Integer> results = getIntegerFromQuery(apiRequest, "fields", "state").iterator();
			String systemState = "System Stopped";	// Stopped
			while (results.hasNext()) {
				int rval = results.next();
				if (rval == 1) 		// We have a master
				{
					systemState = "System Started";		// Running
					break;
				}
				if (rval < 12) 		// We have a master
				{
					systemState = "System Starting";		// Running
				}
			}
			query = "update System set State = (select State from NodeStates where Description = '"
				+ systemState + "') where SystemID = " + m_systemID;
			// before update, get the state corresponding to the description
			apiRequest = "nodestate/" + systemState;
			int newSystemStateID = ListIntegerToInteger(getIntegerFromQuery(apiRequest, "fields", "state"));
			// now update
			apiRequest = "system/" + m_systemID;
			m_api.UpdateValue(apiRequest, "state", String.valueOf(newSystemStateID));
		} catch (Exception e) {
			System.err.println("Update System State SQL Failed: " + query + ": " + e.getMessage());
		}
	}	
	
	/**
	 * Get the list of instance ID for this cluster
	 * 
	 * @return The lsit of instance IDs
	 */
	public List<String> getInstances()
	{
		String apiRequest = "system/" + m_systemID + "/node";
		return getStringFromQuery(apiRequest, "fields", "instanceID");
	}
	
	/**
	 * Update the public IP address of a node if it has changed
	 * 
	 * @param	instanceID The instance ID
	 * @param	publicIP 	The public IP addres of the instance
	 * @return	True if the IP address was updated
	 */
	public boolean setPublicIP(String instanceID, String publicIP)
	{
		String apiRequest = "system/" + m_systemID + "/node";
		String fields = "fields";
		String values = "id, instanceID, publicIP";
		String nodeID = new String();
		try {
			List<String> secondRequestL = getStringFromQuery(apiRequest, fields, values);
			for (int i = 0; i < secondRequestL.size(); i++) {
				if (secondRequestL.get(i) != instanceID) continue;
				if (secondRequestL.get(i) == instanceID) {
					if (secondRequestL.get(i+1) != null && secondRequestL.get(i+1).equals(publicIP)) {
						return false;
					}
					nodeID = secondRequestL.get(i-1);
				}
			}
			apiRequest = "system/" + m_systemID + "/node/" + nodeID;
			fields = "publicip";
			values = publicIP;
			return m_api.UpdateValue(apiRequest, fields, values);
		} catch (Exception e) {
			System.err.println("Failed: set public IP : " + apiRequest);
			System.err.println("       " + e.getMessage());
		}
		return true;
	}
	
	/**
	 * setPrivateIP - Update the private IP of an instance. Only update the database
	 * if the new value differs from that already stored.
	 * 
	 * @param instanceID	The instanceID as a string
	 * @param privateIP		The current private IP address
	 * @return	boolean 	True if the IP address changed
	 */
	public boolean setPrivateIP(String instanceID, String privateIP)
	{
		String apiRequest = "system/" + m_systemID + "/node";
		String fields = "fields";
		String values = "id, instanceID, privateIP";
		String nodeID = new String();
		try {
			List<String> secondRequestL = getStringFromQuery(apiRequest, fields, values);
			for (int i = 0; i < secondRequestL.size(); i++) {
				if (secondRequestL.get(i) != instanceID) continue;
				if (secondRequestL.get(i) == instanceID) {
					if (secondRequestL.get(i+1) != null && secondRequestL.get(i+1).equals(privateIP)) {
						return false;
					}
					nodeID = secondRequestL.get(i-1);
				}
			}
			apiRequest = "system/" + m_systemID + "/node/" + nodeID;
			fields = "privateip";
			values = privateIP;
			return m_api.UpdateValue(apiRequest, fields, values);
		} catch (Exception e) {
			System.err.println("Failed: set private IP : " + apiRequest);
			System.err.println("       " + e.getMessage());
		}
		return true;
	}


	/**
	 * IPMonitor
	 * 
	 * Get the system property IPMonitor - this controls the running of the IPMonitor for
	 * EC2 Cloud based deployments. The default is true for reasons of backward compatibility
	 * 
	 * @return	boolean		True if the IP Monitor should be run
	 */
	public boolean IPMonitor()
	{
		String apiRequest = "system/" + m_systemID + "/property/IPMonitor";
		String IP = ListStringToString(getStringFromQuery(apiRequest, "", ""));
		if (IP == null) return false;
		Pattern pattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
		Matcher matcher = pattern.matcher(IP);
		if (matcher.matches()) {
			return true;
		}
		return false;
	}
	
	/**
	 * bulkMonitorData: batch request to the API.
	 * 
	 * @param fields: the names of the variables to be passed to the API
	 * @param values: the values to the passed to the API
	 */
	public void bulkMonitorData(String[] fields, String[] values) {
		String apiRequest = "monitordata";
		getStringFromQuery(apiRequest, fields, values);
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
		return m_api.MonitorValue(systemID, nodeID, monitorID, observation);
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
		return m_api.MonitorValue(systemID, monitorID, observation);
	}
	
}
