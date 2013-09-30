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

import java.util.List;

public class GsonProvisionedNode extends GsonErrors {
	private List<ProvisionedNodes> provisionednodes;
	
	/**
	 * @return the provisionedNodes
	 */
	public List<ProvisionedNodes> getProvisionedNodes() {
		return provisionednodes;
	}

	public static class ProvisionedNodes {
		private String systemid;
		private String nodeid;
		private String name;
		private String hostname;
		private String privateip;
		private String dbusername;
		private String dbpassword;
		private String systemtype;
		/**
		 * @return the systemid
		 */
		public Integer getSystemid() {
			return Integer.parseInt(systemid);
		}
		/**
		 * @return the nodeid
		 */
		public Integer getNodeid() {
			return Integer.parseInt(nodeid);
		}
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @return the hostname
		 */
		public String getHostname() {
			return hostname;
		}
		/**
		 * @return the privateip
		 */
		public String getPrivateip() {
			return privateip;
		}
		/**
		 * @return the dbusername
		 */
		public String getDbusername() {
			return dbusername;
		}
		/**
		 * @return the dbpassword
		 */
		public String getDbpassword() {
			return dbpassword;
		}
		/**
		 * @return the systemtype
		 */
		public String getSystemtype() {
			return systemtype;
		}
	}

	public GsonProvisionedNode() {}

}
