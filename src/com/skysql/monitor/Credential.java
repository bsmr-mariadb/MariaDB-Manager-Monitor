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
 * A class to bundle username and password information into a single
 * class.
 * 
 * @author Mark Riddoch
 *
 */
public class Credential {
	private String	m_username, m_passwd;
	
	/**
	 * Constructor for the Credential class
	 * 
	 * @param uname			Username
	 * @param passwd		Password
	 */
	public Credential(String uname, String passwd) 
	{
		m_username = uname;
		m_passwd = passwd;
	}
	
	/**
	 * Return the username from the credentials class
	 * @return The username
	 */
	public String getUsername()
	{
		return m_username;
	}
	
	/**
	 * The password in the Credentials class
	 * @return The password
	 */
	public String getPassword()
	{
		return m_passwd;
	}
}
