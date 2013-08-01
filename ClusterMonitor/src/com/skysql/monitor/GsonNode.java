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
 * Contains the fields for the node API call.
 * 
 * @author Massimo Siani
 *
 */
public class GsonNode {
	private int system;
	private int id;
	private String name;
	private int state;
	private String hostname;
	private String publicIP;
	private String privateIP;
	private int instanceID;
	private String username;
	private String passwd;
	private int[] commands;
	private String[] connections;
	private String[] packets;
	private String[] health;
	private String command;
	private String task;
	
	public GsonNode() {}

	/**
	 * @return the system
	 */
	public int getSystem() {
		return system;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the state
	 */
	public int getState() {
		return state;
	}

	/**
	 * @return the hostname
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * @return the publicIP
	 */
	public String getPublicIP() {
		return publicIP;
	}

	/**
	 * @return the privateIP
	 */
	public String getPrivateIP() {
		return privateIP;
	}

	/**
	 * @return the instanceID
	 */
	public int getInstanceID() {
		return instanceID;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @return the passwd
	 */
	public String getPasswd() {
		return passwd;
	}

	/**
	 * @return the commands
	 */
	public int[] getCommands() {
		return commands;
	}

	/**
	 * @return the connections
	 */
	public String[] getConnections() {
		return connections;
	}

	/**
	 * @return the packets
	 */
	public String[] getPackets() {
		return packets;
	}

	/**
	 * @return the health
	 */
	public String[] getHealth() {
		return health;
	}

	/**
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * @return the task
	 */
	public String getTask() {
		return task;
	}
	
}
