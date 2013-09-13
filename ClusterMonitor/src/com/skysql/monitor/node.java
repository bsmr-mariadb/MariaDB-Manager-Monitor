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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The node interface in the monitor. Each instance of a node represents a database that 
 * is being monitored. This class provides a mechanism to connect to the monitored database,
 * a means to execute SQL on that database and basic reachability tests for the node.
 * A Node instance retrieves the list of monitors to be run, stores the results, and
 * uses the bulk update API to minimize the overhead.
 * 
 * Node connections are implemented as threads in order not to delay the execution of the
 * main monitor. Connections are closed if the thread is stuck.
 * 
 * @author Mark Riddoch, Massimo Siani
 *
 */
public class node implements Runnable {
	/**
	 * The database URL to connect to the node
	 */
	private String		m_URL;
	/**
	 * System ID of the node
	 */
	private int			m_systemID;
	/**
	 * The nodes node number
	 */
	private int			m_nodeNo;
	/**
	 * The JDBC connection to the monitored database
	 */
	private Connection 	m_mondb;
	/**
	 * True if a connection exists
	 */
	private boolean 	m_connected;
	/**
	 * The thread being used to create the connection
	 */
	private Thread		m_conthread;
	/**
	 * True if a connection attempt is in progress
	 */
	private boolean		m_connecting;
	/**
	 * The SQLite monitoring database
	 */
	private mondata		m_confdb;
	/**
	 * Attempts to connect to the database. If more are necessary, reset the connection
	 */
	private int			m_tempts;
	/**
	 * Store observed values, and send them to the API only when all the monitors
	 * have returned their result. Use bulk updates for this.
	 * Integer numbers are the monitor id's, Strings are the values.
	 */
	private LinkedHashMap<Integer, String>	m_observedValues;
	
	/**
	 * Node constructor
	 * 
	 * @param confDB	The SQLite monitoring database
	 * @param systemID	The System ID
	 * @param nodeNo	The Node ID
	 */
	public node(mondata confDB, int systemID, int nodeNo)
	{
		m_connected = false;
		m_connecting = false;
		m_systemID = systemID;
		m_nodeNo = nodeNo;
		m_confdb = confDB;
		m_tempts = 1;
		m_observedValues = new LinkedHashMap<Integer, String>();
		String address = confDB.getNodePrivateIP(nodeNo);
		if (address == null)
		{
			System.err.println("Unable to obtain address for node " + nodeNo);
		}
		m_URL = "jdbc:mysql://" + address + "/information_schema";
		connect();
		System.out.println("Created node: " + this);
	}

	/**
	 * Close the connection to the monitored database
	 */
	public synchronized void close()
	{
		System.out.println("Disconnect from monitored database " + m_URL);
		try {
			if (m_connected)
				m_mondb.close();
		} catch (SQLException sqlex)
		{
			System.out.println("Close failed: " + sqlex.getMessage());
			System.out.println("ErrorCode: " + sqlex.getErrorCode() + ": SQLState: " + sqlex.getSQLState());
		}
		m_connected = false;
	}
	
	/**
	 * Connect to the monitored database. A new thread will be created to establish the connection,
	 * if a connection already exists, or a connection thread is running, then another
	 * connection will not be made to the database.
	 */
	private synchronized void connect() 
	{
		System.out.println("Try to connect to monitored database " + m_URL);
		if (m_connecting)
		{
			m_tempts++;
			System.out.println("\n    Already running connection thread - do not run another."
					+ "\n	Attempt number " + m_tempts);
			if (m_tempts >= 10) {
				System.out.println("	Limit reached: reset this connection.");
				m_connecting = false;
			}
			System.out.println();
			return;
		}
		m_tempts = 1;
		if (m_connected)
		{
			System.out.println("    Still appear to be connected, disconnect first");
			this.close();
		}
		m_connecting = true;
		String address = m_confdb.getNodePrivateIP(m_nodeNo);
		if (address == null)
		{
			System.err.println("Unable to obtain address for node " + m_nodeNo);
		}
		m_URL = "jdbc:mysql://" + address + "/information_schema";
		m_conthread = new Thread(this);
		m_conthread.start();
	}
	
	/**
	 * The connection thread entry point. This method gets called on the connection thread
	 * and will create the connection and then terminate the thread
	 */
	public void run()
	{
		try {
			  Class.forName("org.mariadb.jdbc.Driver").newInstance();
			  Credential cred = m_confdb.getNodeMonitorCredentials(m_nodeNo);
			  m_mondb = DriverManager.getConnection(m_URL + "?socketTimeout=60000", cred.getUsername(), cred.getPassword());
			  m_connected = true;
			  System.out.println("Connected");
		}
		catch (SQLException sqlex)
		{
			  m_connected = false;
			  System.out.println("Failed: " + sqlex.getMessage());
		}
		catch (Exception ex)
		{
			  m_connected = false;
			  System.out.println("Failed: " + ex.getMessage());
		}
		m_connecting = false;
	}
	
	/**
	 * Execute an SQL statement on the monitored database
	 * 
	 * @param sql	The SQL statement to execute
	 * @return	The single row/column result of the query
	 */
	public String execute(String sql)
	{
		if (! m_connected)
		{
			connect();
			return null;
		}
		try {
			Statement statement = m_mondb.createStatement();
			statement.setQueryTimeout(60);
			ResultSet result = statement.executeQuery(sql);
			if (!result.first())
				return null;
			return result.getString(1);
		}
		catch (SQLException sqlex)
		{
			System.out.println("Probe failed: " + sql + ": " + sqlex.getMessage());
			System.out.println("ErrorCode: " + sqlex.getErrorCode() + ": SQLState: " + sqlex.getSQLState());
			try {
				this.close();
			} catch (Exception ex) {
				// Ignore failures
			}
		}
		return null;	// If we can't probe return null
	}
	
	/**
	 * Get the node ID of the node
	 * 
	 * @return The node ID
	 */
	public int getID()
	{
		return m_nodeNo;
	}
	
	/**
	 * Get the System ID of the node
	 * 
	 * @return The System ID
	 */
	public int getSystemID()
	{
		return m_systemID;
	}
	
	/**
	 * Perform a basic ICMP Ping reachability test on the node
	 * @return True if the node is reachable
	 */
	public boolean isReachable()
	{
		try {
			return InetAddress.getByName(m_confdb.getNodePrivateIP(m_nodeNo)).isReachable(4000);
		} catch (Exception ex) {
			return false;
		}
	}
	
	/**
	 * Execute a SQL statement that will return a result set with two columns 
	 * and multiple rows. The result set is mapped into a Java HashMap with the
	 * first column treated as the key and the second the corresponding values 
	 * in the hash map. This is designed to fetch bulk data in the form of key
	 * value pairs. 
	 * 
	 * The primary use of this method is to fetch all the rows in the global_status
	 * and global_variables tables.
	 * 
	 * @param sql	The SQL to execute
	 * @return	A hashmap of string pairs for the table
	 */
	public HashMap<String, String> fetchTable(String sql)
	{
		HashMap<String, String> rval = new HashMap<String, String>();
		
		if (! m_connected)
		{
			connect();
			return null;
		}
		try {
			Statement statement = m_mondb.createStatement();
			statement.setQueryTimeout(60);
			ResultSet result = statement.executeQuery(sql);
			while (result.next())
			{
				rval.put(result.getString(1), result.getString(2));
			}
			return rval;
		}
		catch (SQLException sqlex)
		{
			System.out.println("Probe failed: " + sql + ": " + sqlex.getMessage());
			System.out.println("ErrorCode: " + sqlex.getErrorCode() + ": SQLState: " + sqlex.getSQLState());
			try {
				this.close();
			} catch (Exception ex) {
				// Ignore failures
			}
		}
		return null;	// If we can't probe return null
	}
	
	/**
	 * Save an observed value for a monitor in a local buffer.
	 * 
	 * @param observation	The observed value
	 * @return	True if the value is correctly buffered
	 */
	protected boolean saveObservation(Integer monitorId, String observation)
	{
		try {
			m_observedValues.put(monitorId, observation);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Send all the buffered observations about this node to the API in one shot.
	 * 
	 * @return True if the update is performed
	 */
	public boolean updateObservations() {
		List<Integer> monitorIDs = new ArrayList<Integer>(m_observedValues.size());
		List<Integer> systemIDs = new ArrayList<Integer>(m_observedValues.size());
		List<Integer> nodeIDs = new ArrayList<Integer>(m_observedValues.size());
		List<String> values = new ArrayList<String>(m_observedValues.size());
		for (Integer key : m_observedValues.keySet()) {
			monitorIDs.add(key);
			systemIDs.add(m_systemID);
			nodeIDs.add(m_nodeNo);
			values.add(m_observedValues.get(key));
		}
		m_observedValues.clear();
		return m_confdb.bulkMonitorData(monitorIDs, systemIDs, nodeIDs, values);
	}
}
