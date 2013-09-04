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
 * Class for systemtypes API.
 * 
 * @author Massimo Siani
 *
 */
public class GsonSystemTypes {
	
	private SystemTypes systemtypes;
	
	/**
	 * @return the systemtypes
	 */
	public SystemTypes getSystemtypes() {
		return systemtypes;
	}

	public static class SystemTypes {
		private String aws, galera;
		/**
		 * @return the aws
		 */
		public String getAws() {
			return aws;
		}

		/**
		 * @return the galera
		 */
		public String getGalera() {
			return galera;
		}
	}
}
