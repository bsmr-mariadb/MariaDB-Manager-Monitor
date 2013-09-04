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
 * Subclass shared among other Gsons.
 * 
 * @author Massimo Siani
 *
 */
public class GsonSharedMonitorLatest {
	private String connections;
	private String traffic;
	private String availability;
	String nodestate;
	String capacity;
	String hoststate;
	String clustersize;
	String reppaused;
	String parallelism;
	String recvqueue;
	String flowcontrol;
	String sendqueue;

	/**
	 * @return the connections
	 */
	public String getConnections() {
		return connections;
	}
	/**
	 * @return the traffic
	 */
	public String getTraffic() {
		return traffic;
	}
	/**
	 * @return the availability
	 */
	public String getAvailability() {
		return availability;
	}
	/**
	 * @return the nodestate
	 */
	public String getNodestate() {
		return nodestate;
	}
	/**
	 * @return the capacity
	 */
	public String getCapacity() {
		return capacity;
	}
	/**
	 * @return the hoststate
	 */
	public String getHoststate() {
		return hoststate;
	}
	/**
	 * @return the clustersize
	 */
	public String getClustersize() {
		return clustersize;
	}
	/**
	 * @return the reppaused
	 */
	public String getReppaused() {
		return reppaused;
	}
	/**
	 * @return the parallelism
	 */
	public String getParallelism() {
		return parallelism;
	}
	/**
	 * @return the recvqueue
	 */
	public String getRecvqueue() {
		return recvqueue;
	}
	/**
	 * @return the flowcontrol
	 */
	public String getFlowcontrol() {
		return flowcontrol;
	}
	/**
	 * @return the sendqueue
	 */
	public String getSendqueue() {
		return sendqueue;
	}
}
