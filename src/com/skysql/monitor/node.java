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


public class node implements Runnable {
	private String		m_URL;
	private int			m_nodeNo;
	private Connection 	m_mondb;		// The database being monitored
	private boolean 	m_connected;
	private Thread		m_conthread;
	private boolean		m_connecting;
	private mondata		m_confdb;
	
	public node(mondata confDB, int nodeNo)
	{
		m_connected = false;
		m_connecting = false;
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
	
	public int getID()
	{
		return m_nodeNo;
	}
	
	public boolean isReachable()
	{
		try {
			return InetAddress.getByName(m_confdb.getNodePrivateIP(m_nodeNo)).isReachable(4000);
		} catch (Exception ex) {
			return false;
		}
	}
}
