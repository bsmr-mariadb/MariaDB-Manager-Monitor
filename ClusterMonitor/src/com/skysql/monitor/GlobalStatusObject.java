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
 * Copyright 2013-2014 SkySQL Corporation Ab
 */

package com.skysql.monitor;
import java.util.Date;
import java.util.HashMap;


/**
 * A modified singleton pattern implementation of a class to fetch and distribute
 * global status and global variables.
 * 
 * An instance is created for each Node that is being monitored, calls are made
 * to retrieve particular values, using the VARIABLE_NAME of the status or
 * variable required. The class will fetch all values from the global_status and
 * global_variables table and cache them for a predefined period of time. This allows
 * multiple monitors to be based upon global_status and global_variables without the
 * overhead of fetching each one individually.
 * 
 * @author Mark Riddoch
 *
 */
public class GlobalStatusObject {
	
	/**
	 * The hashtable of instances of the GlobalStatusObject. The table is indexed by the Node
	 * class of the Node being monitored.
	 */
	private static final HashMap<Node,GlobalStatusObject> instances = new HashMap<Node, GlobalStatusObject>();
	
	/**
	 * The length of time to cache the global_status or global_variables data
	 * for between going back to fetch new data from the database
	 */
	private static int	UPDATE_THRESHOLD = 2000;	// 2 thousand milliseconds
	
	/**
	 * The global_status cache for this instance. The hashmap key is the
	 * VARIABLE_NAME column and the value is the VARIABLE_VALUE
	 */
	private HashMap<String,String> 	m_globalStatus;
	
	/**
	 * The global_variables cache for this instance. The hashmap key is the
	 * VARIABLE_NAME column and the value is the VARIABLE_VALUE
	 */
	private HashMap<String,String> 	m_globalVariables;
	
	/**
	 * The time when the global status and global variables cache was last
	 * updated
	 */
	private long					m_fetchTime;
	
	/**
	 * The Node we are monitoring
	 */
	private Node					m_node;
	
	/**
	 * Private constructor for GlobalStatusObject
	 * 
	 * @param nodeObject The Node we are monitoring
	 */
	private GlobalStatusObject(Node nodeObject) {
		m_globalStatus = new HashMap<String, String>();
		m_globalVariables = new HashMap<String, String>();
		m_fetchTime = 0;
		m_node = nodeObject;
	}
	
	/**
	 * The get instance entry point will return the instance that is monitoring
	 * the Node passed in. If there is no instance for this Node then an 
	 * instance will be created.
	 * 
	 * @param nodeObject	The Node to Monitor
	 * @return The globalStatus Object for this database instance
	 */
	public static GlobalStatusObject getInstance(Node nodeObject) {
		GlobalStatusObject inst;
		
		if ((inst = instances.get(nodeObject)) != null)
			return inst;
		
		int systemId = nodeObject.getSystemID();
		int nodeId = nodeObject.getID();
		for (Node node : instances.keySet()) {
			if (systemId == node.getSystemID() && nodeId == node.getID()) {
				instances.remove(node);
				break;
			}
		}
		inst = new GlobalStatusObject(nodeObject);
		instances.put(nodeObject, inst);
		return inst;
	}
	
	/**
	 * Get a value for a global_status value in the database. The value will be
	 * fetched from the cache, if the cache is older than the configured 
	 * update time then it will be refreshed first.
	 * 
	 * @param name	The VARIABLE_NAME to return
	 * @return	The VARIABLE_VALUE
	 */
	public String getStatus(String name) {
		fetchData();
		return m_globalStatus.get(name.toLowerCase());
	}
	/**
	 * Get a value for a global_variables value in the database. The value will be
	 * fetched from the cache, if the cache is older than the configured 
	 * update time then it will be refreshed first.
	 * 
	 * @param name	The VARIABLE_NAME to return
	 * @return	The VARIABLE_VALUE
	 */
	public String getVariable(String name) {
		fetchData();
		return m_globalVariables.get(name.toLowerCase());
	}
	
	/**
	 * Get a value for a global_status or global variables value in the database.
	 * The value will be fetched from the cache, if the cache is older than the 
	 * configured update time then it will be refreshed first.
	 * 
	 * @param name	The VARIABLE_NAME to return
	 * @return	The VARIABLE_VALUE
	 */
	public String getStatusOrVariable(String name) {
		String rval;
		fetchData();
		try {
			if ((rval = getStatus(name)) == null) {
				rval = getVariable(name);
			}
		} catch (Exception e) {
			rval = null;
		}
		return rval;
	}
	
	/**
	 * Refresh the cache if it is more than UPDATE_THRESHOLD milliseconds
	 * old
	 */
	private void fetchData() {
		Date now = new Date();
		
		if (now.getTime() - m_fetchTime <= UPDATE_THRESHOLD)
		{
			return;
		}
		m_globalStatus.clear();
		m_globalVariables.clear();
		HashMap<String, String> status = m_node.fetchTable("show global status");
		if (status != null) {
			m_globalStatus.putAll(status);
		}
		HashMap<String, String> variables = m_node.fetchTable("show global variables");
		if (variables != null) {
			m_globalVariables.putAll(variables);
		}
		m_fetchTime = now.getTime();
	}
}
