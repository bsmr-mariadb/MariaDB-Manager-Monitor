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

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the fields for the node API call.
 * 
 * @author Massimo Siani
 *
 */
public class GsonNodeStates extends GsonErrors {
	private NodeStates nodestate;
	private List<NodeStates> nodestates;
	
	/**
	 * @return the nodestate
	 */
	public NodeStates getNodeState(int index) {
		return getNodeStates().get(0);
	}

	/**
	 * @return the nodestates
	 */
	public List<NodeStates> getNodeStates() {
		if (nodestates != null)	return nodestates;
		if (nodestate != null) {
			List<NodeStates> listNodeStates = new ArrayList<NodeStates>(1);
			listNodeStates.add(nodestate);
			return listNodeStates;
		}
		return null;
	}

	public static class NodeStates {
		private String state;
		private int stateid;
		private String description;
		private String icon;
		
		/**
		 * @return the state
		 */
		public String getState() {
			return state;
		}
		
		/**
		 * @return the state id
		 */
		public int getStateId() {
			return stateid;
		}

		/**
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}

		/**
		 * @return the icon
		 */
		public String getIcon() {
			return icon;
		}
	}
	
	public GsonNodeStates() {}
	
	/**
	 * The full list of valid node state names.
	 * 
	 * @return the list of states, null if no found
	 */
	public List<String> getStateList() {
		if (this.getNodeStates() != null) {
			List<String> result = new ArrayList<String>();
			for (NodeStates oneNodeState : this.getNodeStates()) {
				result.add(oneNodeState.getState());
			}
			return result;
		}
		return null;
	}
	/**
	 * The full list of valid node state names.
	 * 
	 * @return the list of states, null if no found
	 */
	public List<String> getDescriptionList() {
		if (this.getNodeStates() != null) {
			List<String> result = new ArrayList<String>();
			for (NodeStates oneNodeState : this.getNodeStates()) {
				result.add(oneNodeState.getDescription());
			}
			return result;
		}
		return null;
	}
	/**
	 * Translate a state id into a state name.
	 * 
	 * @param stateId The id of the state to translate
	 * @return The corresponding state name, null if no id corresponds.
	 */
	public String getStateFromId(int stateId) {
		if (this.getNodeStates() != null) {
			for (NodeStates nodeState : this.getNodeStates()) {
				if (nodeState.getStateId() == stateId) return nodeState.getState();
			}
		}
		if (this.getNodeState(0) != null) {
			if (getNodeState(0).getStateId() == stateId) return getNodeState(0).getState();
		}
		return null;
	}
}