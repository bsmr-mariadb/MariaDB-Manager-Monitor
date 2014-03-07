/*
 * This file is distributed as part of the MariaDB Enterprise.  It is free
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

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.*;

import com.skysql.java.Logging;

/**
 * The main class of the query router, this comprises the main function itself,
 * the handling of the configuration and creation of the monitor probes and
 * the monitoring main loop.
 * 
 * The cluster monitor class is designed to monitor one system, a system being a 
 * collection of one or more nodes. When multiple systems must be monitored new
 * threads are created to monitor each system. These threads each have a unique
 * instance of the ClusterMonitor class.
 * 
 * @author Mark Riddoch, Massimo Siani
 *
 */
public class ClusterMonitor extends Thread {
	/** The Monitor version number. */
	private final static String		MONITOR_VERSION = "1.7-121";
	/**
	 * The ID of the system we are monitoring. This is
	 * read from the arguments list.
	 */
	private int					m_systemID; 
	/**
	 *  A handle on the configuration class that handles
	 *  interaction with the database.
	 */
	private mondata 			m_confdb;	
	/** The list of nodes in the system to monitor. */
	private List<node> 			m_nodeList;	
	/**
	 * The list of the lists of monitors.
	 * The outer list is characterized by the monitor id,
	 * the inner list by the node id. Thus, this can be
	 * seen as a table (monitor id, node id) where each
	 * element is an instance of the monitor class (or its
	 * extensions) that knows which node it's monitoring.
	 */
	private List<List<monitor>> m_monitorList;
	/** Verbose logging flag, read from the arguments list. */
	private boolean				m_verbose;
	/** The default polling interval to use. */
	private int					m_interval;
	/** The GCD amongst the monitor intervals. */
	private int					m_gcdMonitorInterval;
	/** The system observed values, for bulk updates. */
	private LinkedHashMap<Integer, String>	m_observedValues;
	/**
	 * The table to save all the currently thread instances, so
	 * that they can be retrieved and terminated if no longer
	 * necessary.
	 */
	private static volatile LinkedHashMap<Integer, ClusterMonitor>	m_threadMap = new LinkedHashMap<Integer, ClusterMonitor>(3);
	/** The list of systems that are currently running. */
	private static volatile List<Integer>	m_systems_old = new ArrayList<Integer>();
	
	public static void main( String[] args )
	{
		Logging.setComponent("Monitor");
		if (args.length != 1 && args.length != 2)
		{
			Logging.error("Usage: ClusterMonitor [-v]  <System ID>");
			System.exit(1);
		}
		int off = 0;
		
		boolean verbose = false;
		if (args.length == 2 && args[0].equals("-v"))
		{
			off = 1;
			verbose = true;
		}

		Logging.info("Starting ClusterMonitor v" + MONITOR_VERSION);
		Logging.info("==============================");
		mondata monitorData = new mondata();
		monitorData.registerAPI(MONITOR_VERSION);
		
		if (args[off].equalsIgnoreCase("all"))
		{
			List<Integer> systems;
			while (true)
			{
				systems = monitorData.getSystemList();
				if (systems == null) systems = new ArrayList<Integer>();
				systems.removeAll(m_systems_old);		// start only new threads (ie new systems)
				Iterator<Integer> it = systems.iterator();
				ClusterMonitor monitor = null;
				while (it.hasNext())
				{
					Integer i = it.next();
					monitor = new ClusterMonitor(i.intValue(), verbose);
					if (monitor != null) m_systems_old.add(i);
					monitor.initialise();
					monitor.start();
					m_threadMap.put(i, monitor);
				}
				monitorData.registerAPI(MONITOR_VERSION);
				if (systems.isEmpty() && m_systems_old.isEmpty()) {
					Logging.warn("No systems found to monitor, waiting for systems to be deployed.");
					try {
						Thread.sleep(10000);
					} catch (Exception e) {
						Logging.error("Sleep on current thread failed: " + e.getLocalizedMessage());
					}
				} else {
					try {
						Thread.sleep(30000);
					} catch (Exception ex) {
						// Nothing to do
					}
				}
			}
		}
		else
		{
			int targetSystem = new Integer(args[off]).intValue();
			List<Integer> systems = monitorData.getSystemList();
			Iterator<Integer> it = systems.iterator();
			boolean found = false;
			while (it.hasNext())
			{
				Integer i = it.next();
				if (targetSystem == i.intValue())
				{
					found = true;
				}
			}
			if (! found)
			{
				Logging.error("Unable to find the target system, " + targetSystem + " in your database.");
				System.exit(1);
			}
			ClusterMonitor monitor = new ClusterMonitor(targetSystem, verbose);
			monitor.initialise();
			monitor.execute();
		}
	}
	
	/**
	 * Allow the ClusterMonitor to be run in a thread in order to facilitate the monitoring of
	 * more than one System.
	 *  
	 * @see java.lang.Thread#run()
	 */
	public void run()
	{
		this.execute();
	}
	
	/**
	 * ClusterMonitor constructor
	 * 
	 * @param systemID	int		Unique ID of the system
	 * @param verbose	boolean	Log debugging information
	 */
	public ClusterMonitor(int systemID, boolean verbose)
	{
		m_verbose = verbose;
		m_systemID = systemID;
		m_confdb = new mondata(m_systemID);
		m_interval = 30;
		m_gcdMonitorInterval = m_interval;
		m_observedValues = new LinkedHashMap<Integer, String>();
		m_nodeList = new ArrayList<node>();
	}
	
	/**
	 * Initialise the monitoring system, this means getting the list of nodes
	 * from the database, configure and create the monitor classes and run the
	 * IP Address Monitor if it is required.
	 */
	public void initialise()
	{
//		refreshconfig();
//		try {
//			Class.forName("PublicIPMonitor");
//			if (m_confdb.IPMonitor())
//			{
//				mondata confdb = new mondata(m_systemID, m_dbfile);
//				try {
//					PublicIPMonitor ipmon = new PublicIPMonitor(confdb, m_verbose);
//					ipmon.start();
//				} catch (NoClassDefFoundError ex) {
//					Logging.error("Unable to run IPMonitor: Class " + ex.getLocalizedMessage() + " is not available.");
//					Logging.error("IP Monitoring functionality has been suspended.");
//				}
//			}
//		} catch (Exception ex) {
//			// IGNORE
//		}
	}
	
	/**
	 * Read the configuration data from the SQLite database, this was moved out of the initialise
	 * routine to allow the monitor to re-read the configuration periodically and hence take note
	 * of new monitors.
	 */
	private boolean refreshconfig()
	{
		if (m_verbose) {
			Logging.info("Reading configuration data for system " + m_systemID + ".");
		}
		List<Integer> nodeIDList = m_confdb.getNodeListCached();
		int countNodeFail = 0;
		while (nodeIDList == null || nodeIDList.isEmpty())
		{
			Logging.warn("No nodes configured in system " + m_systemID + ".");
			m_confdb.setSystemState("created");
			try {
				if (++countNodeFail > 3) {
					int index = m_systems_old.indexOf(m_systemID);
					m_systems_old.remove(index);
					m_threadMap.get((Integer) m_systemID).interrupt();
					return false;
				}
				Thread.sleep(10000);
			} catch (Exception e) {
				Logging.warn("Failed while waiting for nodes: " + e.getLocalizedMessage());
			}
			nodeIDList = m_confdb.getNodeList();
		}
		if (m_verbose) {
			Logging.info(nodeIDList.size() + " node(s) to monitor");
		}
		if (m_nodeList != null && ! m_nodeList.isEmpty())	{
			closeNodes();
		}
		refreshNodeList(nodeIDList);
		m_confdb.saveMonitorChanges();
		refreshMonitorList();
		return true;
	}

	/**
	 * Run the actual monitors in a loop. This function never returns, it loops
	 * running each monitor in turn, for each of the hosts, and sleeps once a complete
	 * cycle has been completed.
	 * 
	 * In addition to recording values for each of the nodes, certain probes are also
	 * accumulated across all nodes and stored as a system probe value.
	 */
	public void execute()
	{
		long cycleCount = -1L;
		while (true)
		{
			cycleCount++;
			try {
				if (m_confdb.getProvisionedNodes()) {
					if ((! refreshconfig()) || Thread.interrupted()) {
						throw new InterruptedException();
					}
				} else if (m_confdb.saveMonitorChanges()) {
					refreshMonitorList();
				}

				// Ping all the nodes before we do a real probe
				Iterator<node> node_it = m_nodeList.iterator();
				while (node_it.hasNext())
				{
					node n = node_it.next();
					n.execute("show status like 'wsrep_local_state'");
				}
				// Iterate on the monitors
				Iterator<List<monitor>> mit = m_monitorList.iterator();
				while (mit.hasNext())
				{
					List<monitor> mlist = mit.next();
					Iterator<monitor> it = mlist.iterator();
					double system_value = 0.0;
					boolean validSystemProbe = false;
					boolean systemAverage = false;
					int id = 0;

					// Iterate on the instances of the monitors, ie probe the machines
					while (it.hasNext())
					{
						monitor m = it.next();
						id = m.getID();
						if ((m_gcdMonitorInterval * cycleCount) % m.m_interval != 0) continue;
						m.probe(m_verbose);
						systemAverage = m.isSystemAverage();
						if (m.hasSystemValue())
						{
							validSystemProbe = true;
							String value = m.getValue();
							try {
								if (value != null)
									system_value += (new Double(value)).doubleValue();
							} catch (Exception ex) {
								Logging.error("Exception converting probe value '" + value + "' for monitor ID " + id);
							}
							if (m_verbose)
								Logging.info("    Probe " + id + " " + m_confdb.getMonitorKey(id)
										+ " on node " + m_confdb.getNodeName(m.m_node.getID()) + " of system " + m.m_node.getSystemID()
										+ " returns value " + m.getValue());
						}
					}

					// This monitor is valid for the system as well
					if (validSystemProbe)
					{
						String format;
						if (systemAverage)
						{
							system_value = system_value / m_nodeList.size();
							format = "############.##";
						}
						else if (system_value > 100)
							format = "#############";
						else if (system_value > 10)
							format = "##.#";
						else
							format = "#.##";
						DecimalFormat fmt = new DecimalFormat(format);
						m_observedValues.put(id, fmt.format(system_value));
						if (m_verbose)
							Logging.info("        Probe system value " + system_value);
					}
				}
//				if ((m_gcdMonitorInterval * cycleCount) % m_interval == 0) {
					updateFullObservations();
//				}
			} catch (InterruptedException e) {
				return;
			} catch (Exception ex) {
				Logging.error("Probe exception: " + ex.getMessage());
				updateFullObservations();
			} finally {
				try {
					Thread.sleep(m_gcdMonitorInterval * 1000);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
	}
	
	/**
	 * Send all the buffered observations about the system to the API in one shot.
	 * 
	 * @return True if the update is performed
	 */
	private boolean updateObservations() {
		if (m_observedValues.isEmpty()) return false;
		List<Integer> monitorIDs = new ArrayList<Integer>(m_observedValues.size());
		List<String> values = new ArrayList<String>(m_observedValues.size());
		for (Integer key : m_observedValues.keySet()) {
			monitorIDs.add(key);
			values.add(m_observedValues.get(key));
		}
		m_observedValues.clear();
		return m_confdb.bulkMonitorData(monitorIDs, m_systemID, 0, values);
	}
	
	/**
	 * Update the API information about the system and all its nodes. 
	 */
	private void updateFullObservations() {
		// Update the observations: system
		if (updateObservations())
			Logging.info("System " + m_systemID + " monitor data updated.");
		// Update the observations: nodes in this system
		Iterator<node> node_it = m_nodeList.iterator();
		while (node_it.hasNext())
		{
			node n = node_it.next();
			if(n.updateObservations())
				Logging.info("Node " + m_confdb.getNodeName(n.getID()) + " of system " + n.getSystemID() + " monitor data updated.");
		}
	}
	
	/**
	 * Close any active connection to every node in the system.
	 */
	private void closeNodes() {
		Iterator<node> node_it = m_nodeList.iterator();
		while (node_it.hasNext()) {
			node n = node_it.next();
			n.close();
		}
	}
	
	/**
	 * Refresh the list of nodes. Changes the field m_nodeList according
	 * to the ID in the list passed as parameter.
	 * 
	 * @param nodeIDList	a list of the ID of the available nodes
	 */
	private void refreshNodeList(List<Integer> nodeIDList) {
		m_nodeList.clear();
		Iterator<Integer> it = nodeIDList.iterator();
		while (it.hasNext()) {
			Integer i = it.next();
			m_nodeList.add(new node(m_confdb, m_systemID, i.intValue()));
		}
	}
	
	/**
	 * Refreshes the list of monitors available for the system.
	 * Changes the field m_monitorList.
	 */
	private void refreshMonitorList() {
		List<Integer> monitorIDList = m_confdb.getMonitorIdList();
		if (monitorIDList == null) {
			Logging.warn("No monitors configured to run.");
			return;
		}
		if (m_verbose)
			Logging.info(monitorIDList.size() + " distinct monitor(s)");
		m_interval = 30;
		m_monitorList = new ArrayList<List<monitor>>();
		Iterator<Integer> it = monitorIDList.iterator();
		while (it.hasNext())
		{
			int monid = it.next().intValue();
			String type = m_confdb.getMonitorType(monid);
			Boolean monIsDelta = m_confdb.isMonitorDelta(monid);
			List<monitor> mlist = new ArrayList<monitor>();
			m_monitorList.add(mlist);
			Iterator<node> node_it = m_nodeList.iterator();
			while (node_it.hasNext())
			{
				node n = node_it.next();
				if (type == null || type.equals("SQL"))
				{
					if (monIsDelta) {
						mlist.add(new deltaMonitor(m_confdb, monid, n));
					} else {
						mlist.add(new monitor(m_confdb, monid, n));
					}
				}
				else if (type.equals("CRM"))
				{
					mlist.add(new crmMonitor(m_confdb, monid, n));
				}
				else if (type.equals("PING"))
				{
					mlist.add(new pingMonitor(m_confdb, monid, n));
				}
				else if (type.equals("COMMAND"))
				{
					mlist.add(new commandMonitor(m_confdb, monid, n));
				}
				else if (type.equals("SQL_NODE_STATE"))
				{
					mlist.add(new nodeStateMonitor(m_confdb, monid, n));
				}
				else if (type.equals("GLOBAL"))
				{
					mlist.add(new globalMonitor(m_confdb, monid, n, monIsDelta));
				} else if (type.equals("JS")) {
					mlist.add(new RhinoMonitor(m_confdb, monid, n));
				} else if (type.equals("GALERA_STATUS")) {
					mlist.add(new GaleraStatusMonitor(m_confdb, monid, n));
				}
				else
				{
					Logging.warn("Unsupported monitor type: " + type);
				}
				if (! mlist.isEmpty()) {
					m_gcdMonitorInterval = BigInteger.valueOf(m_gcdMonitorInterval)
					.gcd(BigInteger.valueOf(mlist.get(mlist.size() -1).m_interval)).intValue();
				}
				// m_gcdMonitorInterval = m_interval;   // uncomment this line to disable polling functionality
			}
		}
	}

}
