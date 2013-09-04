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
import com.skysql.monitor.mondata;

/**
 * Standalone program for setting the node state. No longer used.
 * 
 * @author Mark Riddoch
 *
 */
public class SetNodeState {
	
	mondata		m_confdb;
	int			m_systemID;
	int			m_nodeid;
	
	public static void main( String[] args )
	{
		if (args.length != 4)
		{
			System.err.println("Usage: SetNodeState <System ID> <node ID> <dbfile> <state>");
			System.exit(1);
		}
		SetNodeState obj = new SetNodeState(new Integer(args[0]).intValue(), new Integer(args[1]).intValue(), args[2]);
		obj.setState(args[3]);
	}
	
	public SetNodeState(int systemID, int nodeID, String dbfile)
	{
		m_confdb = new mondata(systemID, dbfile);
		m_nodeid = nodeID;
		m_systemID = systemID;
	}
	
	public void setState(String state)
	{
		int monid = m_confdb.getNamedMonitor("Node State");
		if (monid == -1)
		{
			System.err.println("Can't find monitor \"Node State\".");
			System.exit(1);
		}
		int stateid = m_confdb.getStateValue(state);
		if (stateid == -1)
		{
			System.err.println("Unknown node state " + state);
			System.err.println("Valid states are:");
			List<String> states = m_confdb.getValidStates();
			Iterator<String> it = states.iterator();
			while (it.hasNext())
			{
				System.err.println("    " + it.next());
			}
			System.exit(1);
		}
		String newValue = (new Integer(stateid)).toString();
//		m_confdb.monitorData(m_nodeid, monid, newValue);
		m_confdb.bulkMonitorData(new Integer[]{monid}, new Integer[]{m_systemID}, new Integer[]{m_nodeid}, new String[]{newValue});
		m_confdb.setNodeState(m_nodeid, stateid);
	}
}
