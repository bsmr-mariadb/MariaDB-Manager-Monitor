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

/**
 * Contains the fields for the system API call.
 * 
 * @author Massimo Siani
 *
 */
public class GsonSystem {
	
	private static class system {
	private static int system;
	private String name;
	private String startDate;
	private String lastAccess;
	private int state;
	private int[] nodes;
	private String lastBackup;
	private String[] properties;
	private String[] commands;
	private String[] connections;
	private String[] packets;
	private String[] health;
	}
	
	public GsonSystem() {}

	/**
	 * @return the system
	 */
	public int getSystem() {
		return system.system;
	}
//
//	/**
//	 * @return the name
//	 */
//	public String getName() {
//		return name;
//	}
//
//	/**
//	 * @return the startDate
//	 */
//	public String getStartDate() {
//		return startDate;
//	}
//
//	/**
//	 * @return the lastAccess
//	 */
//	public String getLastAccess() {
//		return lastAccess;
//	}
//
//	/**
//	 * @return the state
//	 */
//	public int getState() {
//		return state;
//	}
//
//	/**
//	 * @return the nodes
//	 */
//	public int[] getNodes() {
//		return nodes;
//	}
//
//	/**
//	 * @return the lastBackup
//	 */
//	public String getLastBackup() {
//		return lastBackup;
//	}
//
//	/**
//	 * @return the properties
//	 */
//	public String[] getProperties() {
//		return properties;
//	}
//
//	/**
//	 * @return the commands
//	 */
//	public String[] getCommands() {
//		return commands;
//	}
//
//	/**
//	 * @return the connections
//	 */
//	public String[] getConnections() {
//		return connections;
//	}
//
//	/**
//	 * @return the packets
//	 */
//	public String[] getPackets() {
//		return packets;
//	}
//
//	/**
//	 * @return the health
//	 */
//	public String[] getHealth() {
//		return health;
//	}
	
}
