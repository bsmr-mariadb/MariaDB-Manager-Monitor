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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;


public class mondata {
	private int			m_systemID;
	private String		m_dbfile;
	static private int	CONNECT_TRIES = 20;	
	
	/*
	 * Constructor for the monitor data class
	 */
	public mondata(int systemID, String dbfile)
	{
		m_systemID = systemID;
		m_dbfile = dbfile;
		try {
			  Class.forName("org.sqlite.JDBC");
		}
		catch (ClassNotFoundException cnf)
		{
			  System.err.println("Unable to load SQLite JDBC driver.");
			  System.exit(1);
		}
	}
	
	/*
	 * Constructor used when the monitor is being used for when the system id is not known.
	 */
	public mondata(String dbfile)
	{
		m_systemID = -1;
		m_dbfile = dbfile;
		try {
			  Class.forName("org.sqlite.JDBC");
		}
		catch (ClassNotFoundException cnf)
		{
			  System.err.println("Unable to load SQLite JDBC driver.");
			  System.exit(1);
		}
	}
	
	/*
	 * Connect to the SQLite database.
	 * 
	 * This routine connects to a SQLite database, returning the new connection to the caller.
	 * Multiple attempts are made to connect to the database, with a back-off algorithm, in order
	 * to allow for problems connecting due to resource locking issues in SQLite.
	 */
	public Connection connect() throws SQLException
	{
		Connection connection;
		connection = null;
		int retries = 0;
		while (connection == null && retries++ < CONNECT_TRIES)
		{
			retries++;
			try {
				connection = DriverManager.getConnection("jdbc:sqlite:" + m_dbfile);
				connection.setAutoCommit(true);
			}
			catch (SQLException sqlex)
			{
				System.err.println("Failed to connect to SQLite database: " + sqlex.getMessage());
				try {
					Thread.sleep(10 * retries);	// Backoff trying to connect
				} catch (Exception slex) {
					// ignore
				}
			}
		}
		if (connection == null)
		{
			throw new SQLException("Repeated attempts to connect to jdbc:sqlite:" + m_dbfile + " failed");
		}
		return connection;
	}

	/*
	 * Return the list of System ID's to monitor
	 */
	public List<Integer> getSystemList()
	{
		String query = "select systemID from System;";
		try {
			  Connection connection = connect();
			  Statement statement = connection.createStatement();
			  statement.setQueryTimeout(30);
			  ResultSet results = statement.executeQuery(query);
			  List<Integer> ilist = new ArrayList<Integer>();
			  while (results.next())
			  {
			  	ilist.add(results.getInt(1));
			  }
			  results.close();
			  statement.close();
			  connection.close();
			  return ilist;
		  }
		  catch (SQLException sqlex)
		  {
			  System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			  return null;
		  }
	}
	
	/*
	 * Return the list of node numbers to monitor
	 */
	public List<Integer> getNodeList()
	{
		String query = "select nodeID from Node;";
		try {
			  Connection connection = connect();
			  Statement statement = connection.createStatement();
			  statement.setQueryTimeout(30);
			  ResultSet results = statement.executeQuery(query);
			  List<Integer> ilist = new ArrayList<Integer>();
			  while (results.next())
			  {
			  	ilist.add(results.getInt(1));
			  }
			  results.close();
			  statement.close();
			  connection.close();
			  return ilist;
		  }
		  catch (SQLException sqlex)
		  {
			  System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			  return null;
		  }
	}
	
	/*
	 * Return the list of monitors
	 */
	public List<Integer> getMonitorList()
	{
		String query = "select MonitorID from Monitors;";
		try {
			  Connection connection = connect();
			  Statement statement = connection.createStatement();
			  statement.setQueryTimeout(30);
			  ResultSet results = statement.executeQuery(query);
			  List<Integer> ilist = new ArrayList<Integer>();
			  while (results.next())
			  {
			  	ilist.add(results.getInt(1));
			  }
			  results.close();
			  statement.close();
			  connection.close();
			  return ilist;
		  }
		  catch (SQLException sqlex)
		  {
			  System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			  return null;
		  }
	}
	
	/*
	 * Get the private IP address of the specified node
	 */
	public String getNodePrivateIP(int NodeNo)
	{
		String query = "select PrivateIP from NodeData where NodeID = " + NodeNo + " and SystemID = " + m_systemID;
		
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			String res = null;
			if (results.next())
				res = results.getString(1);
			results.close();
			statement.close();
			connection.close();
			return res;
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			return null;
		}
	}
	
	/*
	 * Get the private IP address of the specified node
	 */
	public Credential getNodeMonitorCredentials(int NodeNo)
	{
		String query = "select username, passwd from NodeData where NodeID = " + NodeNo+ " and SystemID = " + m_systemID;
		
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			Credential cred;
			if (results.next())
				cred = new Credential(results.getString(1), results.getString(2));
			else
				cred = new Credential("repluser", "repw");
			results.close();
			statement.close();
			connection.close();
			return cred;
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			return null;
		}
	}
	
	/*
	 * Get the SQL command (or command string) associated with a particular monitor
	 */
	public String getMonitorSQL(int monitor_id)
	{
		String query = "select SQL from Monitors where monitorID = " + monitor_id;
		
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			String res = null;
			if (results.next())
				res = results.getString(1);
			results.close();
			statement.close();
			connection.close();
			return res;
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			return null;
		}
	}

	/*
	 * Is the monitored value a cumulative number or a snapshot value. This allows monitors
	 * to return values that are either the value in the database or the difference between
	 * the current value and the previous value.
	 */
	public Boolean monitorIsDelta(int monitor_id)
	{
		String query = "select delta from Monitors where monitorID = " + monitor_id;
		
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			boolean res = false;
			if (results.next())
			{
				res = results.getBoolean(1);
			}
			results.close();
			statement.close();
			connection.close();
			return res;
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			return false;
		}
	}
	
	/*
	 * Return the most recently value in the database for a particular monitor
	 */
	public String getLatestMonitorData(int monitor_id, int node_id)
	{
		String query = "select Value from MonitorData where monitorID = " + monitor_id;
		query += " and NodeID = " + node_id
				+ " and SystemID = " + m_systemID
				+ " and Start = (select Max(Start) from MonitorData where"
				+ " SystemID = " + m_systemID
				+ " and MonitorID =" + monitor_id
				+ " and NodeID = " + node_id + ")";
		
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			String res = null;
			if (results.next())
				res = results.getString(1);
			statement.close();
			connection.close();
			return res;
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			return null;
		}
	}
	
	/*
	 * Update the value of a monitor in the database. This is used if the value fetched is the same
	 * as the previous, so only the timestamp of the last entry needs to be updated.
	 */
	public void updateMonitorData(int node_id, int monitor_id, String value)
	{
		String query = "update MonitorData set Latest = datetime('now') where SystemID = " + m_systemID
							+ " and MonitorID =" + monitor_id
							+ " and NodeID = " + node_id
							+ " and Value = " + value
							+ " and Start = (select Max(Start) from MonitorData where"
							+ " SystemID = " + m_systemID
							+ " and MonitorID =" + monitor_id
							+ " and NodeID = " + node_id
							+ " and Value = " + value + ")";
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			int cnt = statement.executeUpdate(query);
			switch (cnt)
			{
			case 0:
				System.out.println("Failed to update monitor value: " + query);
				break;
			case 1:
				// Ignore this case, we expect to update exactly 1 row
				break;
			default:
				System.out.println("Updated multiple (" + cnt + ") monitor values: " + query);
				break;
			}
			statement.close();
			connection.close();
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": "+ sqlex.getMessage());
		}
	}
	
	
	/*
	 * Insert a new monitor value in the database
	 */
	public void insertMonitorData(int node_id, int monitor_id, String value)
	{
		String query = "insert into MonitorData values (" + m_systemID + ", " + monitor_id + ", "
				+ node_id + ", '" + value + "', datetime('now'), datetime('now'))";
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			if (statement.executeUpdate(query) == 0)
			{
				System.out.println("Failed to insert monitor value: " + query);
			}
			statement.close();
			connection.close();
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
		}
	}
	
	/*
	 * Update the total system value of the monitor value. This merely alters the timestamp for
	 * the monitor rather than inserting a new row if the value is the same. Otherwise a new row is
	 * inserted.
	 */
	public void updateSystemMonitorData(int monitor_id, String value)
	{
		String lastValue = this.getLatestMonitorData(monitor_id, 0);
		if (lastValue != null && lastValue.equals(value))
		{
			updateMonitorData(0, monitor_id, lastValue);
		}
		else
		{
			if (lastValue != null)
			{
				updateMonitorData(0, monitor_id, lastValue);
			}
			insertMonitorData(0, monitor_id, value);
		}
	}
	
	/*
	 * Fetch the monitor probe interval
	 */
	public int monitorInterval()
	{
		String query = "select Value from SystemProperties where SystemID = " + m_systemID;
		query += " and Property = 'MonitorInterval'";
		
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			int rval = 30;
			if (results.next())
				rval = (new Integer(results.getString(1))).intValue();
			results.close();
			statement.close();
			connection.close();
			return rval;
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			return 30;
		}
	}
	
	/*
	 * Fetch the name of a particular monitor
	 */
	public int getNamedMonitor(String name)
	{
		String query = "select MonitorID from Monitors where Name = '" + name + "'";
		
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			int	rval = -1;
			if (results.next())
				rval = (new Integer(results.getString(1))).intValue();
			results.close();
			statement.close();
			connection.close();
			return rval;
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			return -1;
		}
	}
	
	/*
	 * Return the type, and hence monitor class, of a particular monitor.
	 */
	public String getMonitorType(int id)
	{
		String query = "select MonitorType from Monitors where MonitorID = '" + id + "'";
		
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			String rval = null;
			if (results.next())
				rval = results.getString(1);
			results.close();
			statement.close();
			connection.close();
			return rval;
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			return null;
		}
	}
	
	/*
	 * Is the system monitor value cumulative or an average of all the nodes in the system
	 */
	public boolean getMonitorSystemAverage(int id)
	{
		String query = "select SystemAverage from Monitors where MonitorID = '" + id + "'";
		
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			boolean	 rval = false;
			if (results.next())
				rval = results.getBoolean(1);
			results.close();
			statement.close();
			connection.close();
			return rval;
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			return false;
		}
	}
	
	/*
	 * Return the current state of a node
	 */
	public int getStateValue(String Name)
	{
		String query = "select State from NodeStates where Description = '" + Name + "'";
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			int rval = -1;
			if (results.next())
				rval = (new Integer(results.getString(1))).intValue();
			results.close();
			statement.close();
			connection.close();
			return rval;
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			return -1;
		}
	}
	
	/*
	 * Return the list of valid node states
	 */
	public List<String> getValidStates()
	{
		String query = "select Description from NodeStates";
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			List<String> states = new ArrayList<String>();
			while (results.next())
			{
				states.add(results.getString(1));
			}
			results.close();
			statement.close();
			connection.close();
			return states;
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			return null;
		}
	}
	
	/*
	 * Set the state of a node
	 */
	public void setNodeState(int nodeid, int stateid)
	{
		String query = "update Node set State = " + stateid + " where nodeid = " + nodeid + " and SystemID = " + m_systemID;
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			int cnt = statement.executeUpdate(query);
			switch (cnt)
			{
			case 0:
				System.out.println("Failed to update node state: " + query);
				break;
			case 1:
				// Ignore this case, we expect to update exactly 1 row
				break;
			default:
				System.out.println("Updated multiple (" + cnt + ") node state: " + query);
				break;
			}
			statement.close();
			connection.close();
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": "+ sqlex.getMessage());
		}
	}
	
	/*
	 * Map a CRM state string to a valid node state.
	 */
	public String mapCRMStatus(String state)
	{
		String query = "select State from CRMStateMap crm where crmState = '" + state + "'";
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			String rval = null;
			if (results.next())
				rval = results.getString(1);
			results.close();
			statement.close();
			connection.close();
			return rval;
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			return null;
		}
	}
	
	/*
	 * Set the status of the system
	 */
	public void setSystemStatus()
	{
		String query = "select State from Node where SystemID = " + m_systemID;
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			int rval;
			String systemState = "System Stopped";	// Stopped
			while (results.next())
			{
				rval = results.getInt(1);
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
			results.close();
			statement.close();
			statement = connection.createStatement();
			statement.setQueryTimeout(30);
			query = "update System set State = (select State from NodeStates where Description = '"
						+ systemState + "') where SystemID = " + m_systemID;
			statement.executeUpdate(query);
			statement.close();
			connection.close();
		}
		catch (SQLException sqlex)
		{
			System.err.println("Update System State SQL Failed: " + query + ": " + sqlex.getMessage());
		}
		
	}	
	
	/*
	 * Get the instance ID list for this cluster
	 */
	public List<String> getInstances()
	{
		String query = "select instanceID from NodeData where SystemID = " + m_systemID;
		
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			List<String> res = new ArrayList<String>();
			while (results.next())
				res.add(results.getString(1));
			results.close();
			statement.close();
			connection.close();
			return res;
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			return null;
		}
	}
	
	/*
	 * Update the public IP address of a node if it has changed
	 */
	public boolean setPublicIP(String instanceID, String publicIP)
	{
		String query = "select PublicIP from NodeData where InstanceID = '" + instanceID + "'";
				
		
		
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			if (results.next())
			{
				String current = results.getString(1);
				if (current != null && current.equals(publicIP))
				{
					statement.close();
					connection.close();
					return false;
				}
			}
			query = "update NodeData set PublicIP = '" + publicIP + "' where InstanceID = '" + instanceID + "'";
			statement.executeUpdate(query);
			statement.close();
			connection.close();
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
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
		String query = "select PrivateIP from NodeData where InstanceID = '" + instanceID + "'";
				
		
		
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			if (results.next())
			{
				String current = results.getString(1);
				if (current != null && current.equals(privateIP))
				{
					statement.close();
					connection.close();
					return false;
				}
			}
			query = "update NodeData set PrivateIP = '" + privateIP + "' where InstanceID = '" + instanceID + "'";
			statement.executeUpdate(query);
			statement.close();
			connection.close();
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
		}
		return true;
	}


	/*
	 * IPMonitor
	 * 
	 * Get the system property IPMonitor - this controls the running of the IPMonitor for
	 * EC2 Cloud based deployments. The default is true for reasons of backward compatibility
	 * 
	 * @return	boolean		True if the IP Monitor should be run
	 */
	public boolean IPMonitor()
	{
		String query = "select Value from SystemProperties where SystemID = " + m_systemID;
		query += " and Property = 'IPMonitor'";
		
		try {
			Connection connection = connect();
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery(query);
			boolean rval = true;
			if (results.next())
				rval = (new Boolean(results.getString(1))).booleanValue();
			results.close();
			statement.close();
			connection.close();
			return rval;
		}
		catch (SQLException sqlex)
		{
			System.err.println("SQL Failed: " + query + ": " + sqlex.getMessage());
			return true;
		}
	}
	
}
