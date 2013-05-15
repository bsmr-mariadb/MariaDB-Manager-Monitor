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
/*
 * Monitors the output of crm status bynode
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
 */
public class crmMonitor extends monitor {
	
	public crmMonitor(mondata db, int id, node mon_node)
	{
		super(db, id, mon_node);
	}
	public void probe(boolean verbose)
	{
		if (m_node.getID() != 1)
		{
			return;
		}
		if (verbose)
			System.out.println("Parse crm status bynode");
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
					System.out.println(line);
				if (line.matches("Node.*"))
				{
					int beginIndex = line.indexOf("node");
					int endIndex = line.indexOf(":");
					Integer node = new Integer(line.substring(beginIndex + 4, endIndex));
					nodeNo = node.intValue();
					if (verbose)
						System.out.println(">>> Node is: " + nodeNo);
					if (line.matches(".*OFFLINE.*"))
					{
						if (verbose)
							System.out.println(">>> Set Node Status: " + nodeNo + " OFFLINE");
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
							System.out.println(">>> Node is: " + nodeNo +". State is: " + state);
					}
					else
					{
						int beginIndex = line.lastIndexOf(") ");
						state = line.substring(beginIndex + 2).trim();
						if (verbose)
							System.out.println(">>> Node is: " + nodeNo +". State is: " + state);
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
				System.out.println("process.exitValue: " + pex.getLocalizedMessage());
			}
		} catch (Exception ex) {
			System.err.println("CRM Probe exception: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	private void crmSetState(int nodeNo, String state, boolean verbose)
	{
		String value = m_confdb.mapCRMStatus(state);
		
		if (value == null)
		{
			System.out.println("Unable to map state " + state);
			return;
		}
		if (verbose)
			System.out.println("Set Node State: Node: " + nodeNo + " State: " + value + "(mapped from " + state + ")");

		m_confdb.monitorData(nodeNo, m_monitor_id, value);
		try {
			m_confdb.setNodeState(nodeNo, (new Integer(value)).intValue());
		} catch (Exception ex) {
			System.err.println("Can not set node state of " + value + " or node " + nodeNo);
		}
	}

	
	public boolean hasSystemValue()
	{
		return false;
	}
}
