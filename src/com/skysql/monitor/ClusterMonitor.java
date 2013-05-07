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

import java.text.DecimalFormat;
import java.util.*;


public class ClusterMonitor extends Thread {
	private int					m_systemID;
	private mondata 			m_confdb;
	private List<node> 			m_nodeList;
	private List<List<monitor>> m_monitorList;
	private boolean				m_verbose;
	private int					m_interval;
	private String				m_dbfile;
	
	public static void main( String[] args )
	{
		if (args.length != 2 && args.length != 3)
		{
			System.err.println("Usage: ClusterMonitor [-v]  <System ID> <dbfile>");
			System.exit(1);
		}
		int off = 0;
		
		boolean verbose = false;
		if (args.length == 3)
		{
			off = 1;
			verbose = true;
		}

		System.err.println("Starting ClusterMonitor v1.4.0");
		System.err.println("==============================");
		
		if (args[off].equalsIgnoreCase("all"))
		{
			mondata db = new mondata(args[off+1]);
			List<Integer> systems = db.getSystemList();
			Iterator<Integer> it = systems.iterator();
			ClusterMonitor monitor = null;
			while (it.hasNext())
			{
				Integer i = it.next();
				monitor = new ClusterMonitor(i.intValue(), args[off + 1], verbose);
				monitor.initialise();
				
				monitor.start();
			}
			if (monitor != null)	
			{
				try {
					monitor.join();
				} catch (Exception ex) {
					// Nothing to do
				}
			}
		}
		else
		{
			int targetSystem = new Integer(args[off]).intValue();
			mondata db = new mondata(args[off+1]);
			List<Integer> systems = db.getSystemList();
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
				System.err.println("Unable to find the target system, " + targetSystem + " in your database.");
				System.exit(1);
			}
			
			ClusterMonitor monitor = new ClusterMonitor(targetSystem, args[off + 1], verbose);
			monitor.initialise();
		
			monitor.execute();
		}
	}
	
	/*
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
	 * @param dbfile	String	Name of the SQLite database file
	 * @param verbose	boolean	Log debugging information
	 */
	public ClusterMonitor(int systemID, String dbfile, boolean verbose)
	{
		m_dbfile = dbfile;
		m_verbose = verbose;
		m_systemID = systemID;
		m_confdb = new mondata(m_systemID, m_dbfile);
		m_interval = m_confdb.monitorInterval();
	}
	
	/**
	 * initialise
	 * 
	 * Initialise the monitoring system, this means getting the list of nodes
	 * from the database, configure and create the monitor classes and run the
	 * IP Address Monitor if it is required.
	 */
	public void initialise()
	{
		refreshconfig();
		if (m_confdb.IPMonitor())
		{
			mondata confdb = new mondata(m_systemID, m_dbfile);
			try {
				PublicIPMonitor ipmon = new PublicIPMonitor(confdb, m_verbose);
				ipmon.start();
			} catch (NoClassDefFoundError ex) {
				System.err.println("Unable to run IPMonitor: Class " + ex.getLocalizedMessage() + " is not available.");
				System.err.println("IP Monitoring functionality has been suspended.");
			}
		}
	}
	
	/*
	 * refreshconfig 
	 *
	 * Read the configuration data from the SQLite database, this was moved out of the initialise
	 * routine to allow the monitor to re-read the configuration periodically and hence take note
	 * of new monitors.
	 */
	private void refreshconfig()
	{
		if (m_verbose)
			System.out.println("Reading configuration data");
		List<Integer> nodeIDList = m_confdb.getNodeList();
		if (nodeIDList == null)
		{
			System.err.println("No nodes configured to monitor.");
			System.exit(1);
		}
		if (m_verbose)
			System.out.println(nodeIDList.size() + " nodes to monitor");
		if (m_nodeList != null)
		{
			Iterator<node> node_it = m_nodeList.iterator();
			while (node_it.hasNext())
			{
				node n = node_it.next();
				n.close();
			}
		}
		m_nodeList = new ArrayList<node>();
		Iterator<Integer> it = nodeIDList.iterator();
		while (it.hasNext())
		{
			Integer i = it.next();
			m_nodeList.add(new node(m_confdb, m_systemID, i.intValue()));
		}
		List<Integer> monitorIDList = m_confdb.getMonitorList();
		if (monitorIDList == null)
		{
			System.err.println("No monitors configured to run.");
			System.exit(1);
		}
		if (m_verbose)
			System.out.println(monitorIDList.size() + " distinct monitors");
		m_monitorList = new ArrayList<List<monitor>>();
		it = monitorIDList.iterator();
		while (it.hasNext())
		{
			Integer i = (Integer)it.next();
			List<monitor> mlist = new ArrayList<monitor>();
			m_monitorList.add(mlist);
			Iterator<node> node_it = m_nodeList.iterator();
			while (node_it.hasNext())
			{
				node n = node_it.next();
				int monid = i.intValue();
				String type = m_confdb.getMonitorType(monid);
				if (type == null || type.equals("SQL"))
				{
					if (m_confdb.monitorIsDelta(monid))
						mlist.add(new deltaMonitor(m_confdb, monid, n));
					else
						mlist.add(new monitor(m_confdb, monid, n));
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
				else
				{
					System.err.println("Unsupported monitor type: " + type);
				}
			}
		}
	}

	/**
	 * execute
	 * 
	 * Run the actual monitors in a loop. This function never returns, it loops
	 * running each monitor in turn, for each of the hosts, and sleeps once a complete
	 * cycle has been completed. The sleep time is controlled by a SystemProperty entry
	 * in the database.
	 * 
	 * In addition to recording values for each of the nodes, certain probes are also
	 * accumulated across all nodes and stored as a system probe value.
	 */
	public void execute()
	{
		while (true)
		{
			try {
				// Every 10th time we reread the config, so do 10 probes
				// here and then read the config
				for (int i = 0; i < 10; i++)
				{
					if (m_verbose)
						System.out.println("Probe");
					
					// Ping all the nodes before we do a real probe
					Iterator<node> node_it = m_nodeList.iterator();
					while (node_it.hasNext())
					{
						node n = node_it.next();
						n.execute("select 1");
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
									System.err.println("Exception converting probe value '" + value + "' for monitor ID " + id);
								}
								if (m_verbose)
									System.out.println("    Probe " + id + " returns value " + m.getValue());
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
							m_confdb.monitorData(m_systemID, id, fmt.format(system_value));
							// m_confdb.updateSystemMonitorData(id, fmt.format(system_value));
							if (m_verbose)
								System.out.println("    Probe system value " + system_value);
						}
					}
					try {
						Thread.sleep(m_interval * 1000);	// Sleep for 30 seconds
					}
					catch (Exception ex)
					{
						// Ignore exception
					}
				}
			} catch (Exception ex) {
				System.err.println("Probe exception: " + ex.getMessage());
				ex.printStackTrace();
			}
			refreshconfig();
		}
			
	}
}
