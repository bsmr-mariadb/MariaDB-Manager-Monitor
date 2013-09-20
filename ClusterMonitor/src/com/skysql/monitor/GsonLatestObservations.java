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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Handle the latest observations for all the Gson objects.
 * 
 * @author Massimo Siani
 *
 */
public class GsonLatestObservations {
	private String								m_standardDate;
	private LinkedHashMap<Integer, GsonSystem.Systems>								m_system;
	private LinkedHashMap<Integer, LinkedHashMap<Integer, GsonNode.Nodes>>			m_node;
	private LinkedHashMap<Integer, String>				m_systemDates;
	private LinkedHashMap<Integer, LinkedHashMap<Integer, String>>				m_nodeDates;


	public GsonLatestObservations() {
		m_system = new LinkedHashMap<Integer, GsonSystem.Systems>();
		m_node = new LinkedHashMap<Integer, LinkedHashMap<Integer, GsonNode.Nodes>>();
		m_systemDates = new LinkedHashMap<Integer, String>();
		m_nodeDates = new LinkedHashMap<Integer, LinkedHashMap<Integer,String>>();
		m_standardDate = "Thu, 01 Jan 1970 00:00:01 +0100";
	}

	public GsonSystem getLastSystem (int systemID) {
		try {
			GsonSystem gsonSystem = new GsonSystem(m_system.get(systemID));
			return gsonSystem;
		} catch (Exception e) {
			return null;
		}
	}

	public GsonNode getLastNode (int systemID, int nodeID) {
		try {
			GsonNode gsonNode = new GsonNode(m_node.get(systemID).get(nodeID));
			return gsonNode;
		} catch (Exception e) {
			return null;
		}
	}
	
	public String getSystemUpdateDate (int systemID) {
		try {
			if (! m_systemDates.containsKey(systemID)) throw new RuntimeException();
			return m_systemDates.get(systemID);
		} catch (Exception e) {
			return m_standardDate;
		}
	}
	
	public String getNodeUpdateDate (int systemID, int nodeID) {
		try {
			if (! m_nodeDates.containsKey(systemID)) throw new RuntimeException();
			if (! m_nodeDates.get(systemID).containsKey(nodeID)) throw new RuntimeException();
			return m_nodeDates.get(systemID).get(nodeID);
		} catch (Exception e) {
			return m_standardDate;
		}
	}

	public void setLastSystem (int systemID, GsonSystem systemObj) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
			String now = sdf.format(new Date());
			Iterator<GsonSystem.Systems> it = systemObj.getSystems().iterator();
			while (it.hasNext()) {
				GsonSystem.Systems systemTmp = it.next();
				m_system.put(systemTmp.getSystemId(), systemTmp);
				m_systemDates.put(systemID, now);
			}
		} catch (Exception e) {
			//
		}
	}

	public void setLastNode (int systemID, int nodeID, GsonNode nodeObj) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
			String now = sdf.format(new Date());
			Iterator<GsonNode.Nodes> it = nodeObj.getNodes().iterator();
			while (it.hasNext()) {
				GsonNode.Nodes nodeTmp = it.next();
				LinkedHashMap<Integer, GsonNode.Nodes> lhm = new LinkedHashMap<Integer, GsonNode.Nodes>();
				lhm.put(nodeTmp.getNodeId(), nodeTmp);
				m_node.put(nodeTmp.getSystemId(), lhm);
				LinkedHashMap<Integer, String> lhmDate = new LinkedHashMap<Integer, String>();
				lhmDate.put(nodeID, now);
				m_nodeDates.put(systemID, lhmDate);
			}
		} catch (Exception e) {
			//
		}
	}

//	public <T extends GsonErrors> void setLastObserved(T object) {
//		if (object.getClass() == GsonSystem.class) {
//			GsonSystem gsonSystem = (GsonSystem) object;
//			Iterator<Integer> it = gsonSystem.getSystemIdList().iterator();
//			while (it.hasNext()) {
//				if (system.containsKey(it.next())) break;
//			}
//		}
//		if (object.getClass() == GsonNode.class)
//			node = (GsonNode) object;
//	}

}
