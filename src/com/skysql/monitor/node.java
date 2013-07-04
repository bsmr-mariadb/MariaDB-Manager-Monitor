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
import java.util.HashMap;

/**
 * The node interface in the monitor. Each instance of a node represents a database that 
 * is being monitored. This class provides a mechanism to connect to the monitored database,
 * a means to execute SQL on that database and basic reachability tests for the node.
 * 
 * Node connections are implemented as threads in order not to delay the execution of the
 * main monitor.
 * 
 * @author Mark Riddoch
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
			System.out.println("    Already running connection thread - do not run another.");
			return;
		}
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
			  Class.forName("org.skysql.jdbc.Driver").newInstance();
			  Credential cred = m_confdb.getNodeMonitorCredentials(m_nodeNo);
			  m_mondb = DriverManager.getConnection(m_URL, cred.getUsername(), cred.getPassword());
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
}
