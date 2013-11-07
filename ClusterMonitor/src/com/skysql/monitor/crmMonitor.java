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

import java.io.*;

import com.skysql.java.Logging;
/**
 * Monitors the output of crm status bynode. This is a specialist monitor
 * class designed for use with the SkySQL Data Suite environment with a master
 * slave MySQL cluster under the control of pacemaker.
 * 
 * [root@node1 skysql_aws]# crm status bynode
 * ============
 * Last updated: Tue Sep 18 05:06:12 2012
 * Last change: Mon Sep 17 21:33:41 2012 via cibadmin on node2
 * Stack: openais
 * Current DC: node2 - partition with quorum
 * Version: 1.1.7-6.el6-148fccfd5985c5590cc601123c6c16e966b85d14
 * 3 Nodes configured, 3 expected votes
 * 12 Resources configured.
 * ============
 * 
 * Node node2: online
 * 	resMySQL:0	(ocf::custom:mysql) Master 
 * 	AmazonEIP	(lsb:amazon_master_eip) Started 
 * 	ApachePhpMyAdmin:0	(ocf::heartbeat:apache) Started 
 * 	monyog-service	(lsb:MONyogd) Started 
 * 	monyog-fs	(ocf::custom:Filesystem) Started 
 * Node node3: online
 * 	ApachePhpMyAdmin:1	(ocf::heartbeat:apache) Started 
 * 	resMySQL:1	(ocf::custom:mysql) Started 
 * Node node1: online
 * 	resMySQL:2	(ocf::custom:mysql) Started 
 * 	ApachePhpMyAdmin:2	(ocf::heartbeat:apache) Started 
 * 	SkySQL-monitor	(lsb:skysql_monitor) Started 
 * 	console-fs	(ocf::custom:Filesystem) Started 
 * 	Tomcat7	(ocf::heartbeat:tomcat) Started 
 * 
 * Fix to account for different output from pacemaker 1.1.9
 * 
 * @author Mark Riddoch
 */
public class crmMonitor extends monitor {
	
	/**
	 * Constructor - all work is done by the superclass
	 * @param db		The database handler
	 * @param id		The ID of the monitor
	 * @param mon_node	The node to monitor
	 */
	public crmMonitor(mondata db, int id, node mon_node)
	{
		super(db, id, mon_node);
	}
	
	/**
	 * The probe method, called once per probe cycle.
	 * 
	 * This is a system only probe, so only runs if the node number is -1
	 * 
	 * @param verbose The verbosity to log with
	 */
	public void probe(boolean verbose)
	{
		if (m_node.getID() != 1)
		{
			return;
		}
		if (verbose)
			Logging.info("Parse crm status bynode");
		try {
			Process process = Runtime.getRuntime().exec(m_sql);
			InputStream probe = process.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(probe));
			String line;
			int		nodeNo = -1;
			boolean failures = false;
			while ((line = in.readLine()) != null)
			{
				if (verbose)
					Logging.info(line);
				if (line.matches("Node.*"))
				{
					int beginIndex = line.indexOf("node");
					int endIndex = line.indexOf(":");
					Integer node = new Integer(line.substring(beginIndex + 4, endIndex));
					nodeNo = node.intValue();
					if (verbose)
						Logging.info(">>> Node is: " + nodeNo);
					if (line.matches(".*OFFLINE.*"))
					{
						if (verbose)
							Logging.info(">>> Set Node Status: " + nodeNo + " OFFLINE");
						crmSetState(nodeNo, "OFFLINE", verbose);
					}
				}
				if (line.matches(".*resMySQL.*"))
				{

					String state;
					if (failures)
					{
						if (line.matches(".*demote.*"))	// Ignore failed demotes
							continue;
						if (line.matches(".*monitor.*"))	// Ignore failed monitors
							continue;
						int beginIndex = line.lastIndexOf(": ");
						state = line.substring(beginIndex + 2);
						beginIndex = line.indexOf("node=node");
						int endIndex = line.indexOf(",");
						Integer node = new Integer(line.substring(beginIndex + 9, endIndex));
						nodeNo = node.intValue();
						if (verbose)
							Logging.info(">>> Node is: " + nodeNo +". State is: " + state);
					}
					else
					{
						String	tline = line.trim();
						String	words[] = tline.split("\\s");
						state = words[words.length-1];

						if (verbose)
							Logging.info(">>> Node is: " + nodeNo +". State is: " + state);
					}
					crmSetState(nodeNo, state, verbose);
				}
				if (line.matches("Failed actions:"))
				{
					failures = true;
				}
			}
			in.close();
			m_confdb.setSystemStatus();
			try {
				process.exitValue();
			} catch (Exception pex)
			{
				Logging.error("process.exitValue: " + pex.getLocalizedMessage());
			}
		} catch (Exception ex) {
			Logging.error("CRM Probe exception: " + ex.getMessage());
		}
	}
	
	/**
	 * Set the node state by mapping the state parsed from the crm command
	 * into an internal state
	 *  
	 * @param nodeNo	The node number to set state for
	 * @param state		CRM state string
	 * @param verbose	Logging verbosity
	 */
	private void crmSetState(int nodeNo, String state, boolean verbose)
	{
		String value = m_confdb.mapCRMStatus(state);
		
		if (value == null)
		{
			Logging.warn("Unable to map state " + state);
			return;
		}
		if (verbose)
			Logging.info("Set Node State: Node: " + nodeNo + " State: " + value + "(mapped from " + state + ")");

		m_confdb.monitorData(nodeNo, m_monitor_id, value);
		try {
			m_confdb.setNodeState(nodeNo, (new Integer(value)).intValue());
		} catch (Exception ex) {
			Logging.error("Can not set node state of " + value + " or node " + nodeNo);
		}
	}

	/**
	 * Disables system values for this monitor
	 */
	public boolean hasSystemValue()
	{
		return false;
	}
}
