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

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import com.skysql.java.AboutMe;
import com.skysql.java.Configuration;
import com.skysql.java.Logging;
import com.skysql.java.MonData;
import com.skysql.java.Configuration.DEFAULT_SECTION;

/**
 * The main class of the query router, this comprises the main function itself,
 * the handling of the configuration and creation of the Monitor probes and
 * the monitoring main loop.
 * 
 * The cluster Monitor class is designed to Monitor one system, a system being a 
 * collection of one or more nodes. When multiple systems must be monitored new
 * threads are created to Monitor each system. These threads each have a unique
 * instance of the ClusterMonitor class.
 * 
 * @author Mark Riddoch
 * @author Massimo Siani
 *
 */
public class ClusterMonitor extends Thread {
	/** The Monitor component name */
	private final static String		MONITOR_NAME = "MariaDB-Manager-Monitor";
	/**
	 * The Monitor build number.
	 */
	private final static String		MONITOR_VERSION = "1.7-129";
	/**
	 * The Monitor release number as part of the MariaDB-Manager package.
	 */
	private final static String		MONITOR_RELEASE = "1.0.2";
	/**
	 * The Monitor last change date.
	 */
	private final static String		MONITOR_DATE = "Fri, 25 Apr 2014 04:40:33 -0400";
	/**
	 * The ID of the system we are monitoring. This is
	 * read from the arguments list.
	 */
	private int					m_systemID;
	/**
	 *  A handle on the configuration class that handles
	 *  interaction with the database.
	 */
	private MonData 			m_confdb;	
	/** The list of nodes in the system to Monitor. */
	private List<Node> 			m_nodeList;	
	/**
	 * The list of the lists of monitors.
	 * The outer list is characterized by the Monitor id,
	 * the inner list by the Node id. Thus, this can be
	 * seen as a table (Monitor id, Node id) where each
	 * element is an instance of the Monitor class (or its
	 * extensions) that knows which Node it's monitoring.
	 */
	private List<List<Monitor>> m_monitorList;
	/** Verbose logging flag, read from the arguments list. */
	private boolean				m_verbose;
	/** The default polling interval to use. */
	private int					m_interval;
	/** The GCD amongst the Monitor intervals. */
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
		Configuration.setApplication(DEFAULT_SECTION.MONITOR);
		if (args.length != 1 && args.length != 2)
		{
			Logging.error("Usage: ClusterMonitor [-v]  <System ID>");
			System.exit(1);
		}
		int off = 0;
		if (args[off].equalsIgnoreCase("-v")) {
			off = 1;
		}
		
		boolean verbose = false;
		Configuration config = new Configuration();
		try {
			verbose = Boolean.parseBoolean(config.getConfig(Configuration.DEFAULT_SECTION.MONITOR).get("verbose"));
		} catch (Exception e) {
			Logging.error(e.getMessage());
		}

		Logging.info("Starting ClusterMonitor v" + MONITOR_VERSION);
		Logging.info("================================");
		MonData monitorData = new MonData();
		monitorData.registerAPI(MONITOR_NAME, MONITOR_VERSION, MONITOR_RELEASE, MONITOR_DATE);
		monitorData.registerAPI(AboutMe.NAME, AboutMe.VERSION, AboutMe.RELEASE, AboutMe.DATE);
		
		if (args[off].equalsIgnoreCase("all"))
		{
			List<Integer> systems;
			int cycles = 0;
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
				cycles++;
				if (cycles == 10) {
					monitorData.registerAPI(MONITOR_NAME, MONITOR_VERSION, MONITOR_RELEASE, MONITOR_DATE);
					monitorData.registerAPI(AboutMe.NAME, AboutMe.VERSION, AboutMe.RELEASE, AboutMe.DATE);
					cycles = 0;
				}
				if (systems.isEmpty() && m_systems_old.isEmpty()) {
					Logging.warn("No systems found to Monitor, waiting for systems to be deployed.");
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
		m_confdb = new MonData(m_systemID);
		m_interval = 30;
		m_gcdMonitorInterval = m_interval;
		m_observedValues = new LinkedHashMap<Integer, String>();
		m_nodeList = new ArrayList<Node>();
	}
	
	/**
	 * Initialise the monitoring system, this means getting the list of nodes
	 * from the database, configure and create the Monitor classes and run the
	 * IP Address Monitor if it is required.
	 */
	public void initialise()
	{
//		refreshconfig();
//		try {
//			Class.forName("PublicIPMonitor");
//			if (m_confdb.IPMonitor())
//			{
//				MonData confdb = new MonData(m_systemID, m_dbfile);
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
	 * routine to allow the Monitor to re-read the configuration periodically and hence take note
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
			Logging.info(nodeIDList.size() + " Node(s) to Monitor");
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
	 * running each Monitor in turn, for each of the hosts, and sleeps once a complete
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
					GaleraStatusMonitor.removeSystem((Integer)m_systemID);
					if ((! refreshconfig()) || Thread.interrupted()) {
						throw new InterruptedException();
					}
				} else if (m_confdb.saveMonitorChanges()) {
					refreshMonitorList();
				}

				// Ping all the nodes before we do a real probe
				Iterator<Node> node_it = m_nodeList.iterator();
				while (node_it.hasNext())
				{
					Node n = node_it.next();
					n.execute("show status like 'wsrep_local_state'");
				}
				// Iterate on the monitors
				Iterator<List<Monitor>> mit = m_monitorList.iterator();
				while (mit.hasNext())
				{
					List<Monitor> mlist = mit.next();
					Iterator<Monitor> it = mlist.iterator();
					double system_value = 0.0;
					boolean validSystemProbe = false;
					boolean systemAverage = false;
					int id = 0;

					// Iterate on the instances of the monitors, ie probe the machines
					while (it.hasNext())
					{
						Monitor m = it.next();
						id = m.getID();
						if ((m_gcdMonitorInterval * cycleCount) % m.getInterval() != 0) continue;
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
								Logging.error("Exception converting probe value '" + value + "' for Monitor ID " + id);
							}
							if (m_verbose)
								Logging.info("    Probe " + id + " " + m_confdb.getMonitorKey(id)
										+ " on Node " + m_confdb.getNodeName(m.m_node.getID()) + " of system " + m.m_node.getSystemID()
										+ " returns value " + m.getValue());
						}
					}

					// This Monitor is valid for the system as well
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
		Iterator<Node> node_it = m_nodeList.iterator();
		while (node_it.hasNext())
		{
			Node n = node_it.next();
			if(n.updateObservations())
				Logging.info("Node " + m_confdb.getNodeName(n.getID()) + " of system " + n.getSystemID() + " monitor data updated.");
		}
	}
	
	/**
	 * Close any active connection to every Node in the system.
	 */
	private void closeNodes() {
		Iterator<Node> node_it = m_nodeList.iterator();
		while (node_it.hasNext()) {
			Node n = node_it.next();
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
			m_nodeList.add(new Node(m_confdb, m_systemID, i.intValue()));
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
			Logging.info(monitorIDList.size() + " distinct Monitor(s)");
		m_interval = 30;
		m_monitorList = new ArrayList<List<Monitor>>();
		Iterator<Integer> it = monitorIDList.iterator();
		while (it.hasNext())
		{
			int monid = it.next().intValue();
			String type = m_confdb.getMonitorType(monid);
			Boolean monIsDelta = m_confdb.isMonitorDelta(monid);
			List<Monitor> mlist = new ArrayList<Monitor>();
			m_monitorList.add(mlist);
			Iterator<Node> node_it = m_nodeList.iterator();
			while (node_it.hasNext())
			{
				Node n = node_it.next();
				if (type == null || type.equals("SQL"))
				{
					if (monIsDelta) {
						mlist.add(new DeltaMonitor(m_confdb, monid, n));
					} else {
						mlist.add(new Monitor(m_confdb, monid, n));
					}
				}
				else if (type.equals("CRM"))
				{
					mlist.add(new crmMonitor(m_confdb, monid, n));
				}
				else if (type.equals("PING"))
				{
					mlist.add(new PingMonitor(m_confdb, monid, n));
				}
				else if (type.equals("COMMAND"))
				{
					mlist.add(new commandMonitor(m_confdb, monid, n));
				}
				else if (type.equals("SQL_NODE_STATE"))
				{
					mlist.add(new NodeStateMonitor(m_confdb, monid, n));
				}
				else if (type.equals("GLOBAL"))
				{
					mlist.add(new GlobalMonitor(m_confdb, monid, n, monIsDelta));
				} else if (type.equals("JS")) {
					mlist.add(new RhinoMonitor(m_confdb, monid, n));
				} else if (type.equals("GALERA_STATUS")) {
					mlist.add(new GaleraStatusMonitor(m_confdb, monid, n));
				}
				else
				{
					Logging.warn("Unsupported Monitor type: " + type);
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
