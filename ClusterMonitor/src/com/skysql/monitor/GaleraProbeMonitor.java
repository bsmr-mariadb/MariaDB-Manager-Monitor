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

import java.util.HashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

/**
 * The Galera Probe Monitor class.
 * 
 * This is used for efficiently catch the Node state by looking at the Galera
 * variables. It decodes the value of a subset of session variables into a
 * single 
 * 
 * @author Massimo Siani
 *
 */
public class GaleraProbeMonitor extends monitor {

	public GaleraProbeMonitor(mondata db, int id, node mon_node) {
		super(db, id, mon_node);
		m_sql = m_confdb.getMonitorSQL(m_monitor_id);
	}
	
	public void probe () {
		String galeraState = runJavaScriptString();
		System.out.println("Global variables: " + galeraState);
	}
	
	/**
	 * Read a JavaScript from the table Monitors and execute it. The JavaScript
	 * must return a String, which is then the return value of this method.
	 * 
	 * @param script the script that identifies the cluster state
	 * @return a string with the state
	 */
	private String runJavaScriptString() {
		try {
//			try {
//				Class.forName("org.mariadb.jdbc.Driver");
//			} catch (ClassNotFoundException e) {
//				System.err.println("ERROR: cannot find MariaDB driver");
//				return null;
//			}
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
			Bindings bindings = new SimpleBindings();
			Credential credential = m_confdb.getNodeMonitorCredentials(m_node.getID());
			HashMap<String, String> jsBindings= new HashMap<String, String>(3);
			jsBindings.put("address", m_confdb.getNodePrivateIP(m_node.getID()));
			jsBindings.put("dbUserName", credential.getUsername());
			jsBindings.put("dbPassword", credential.getPassword());
			bindings.putAll(jsBindings);

			if (engine instanceof Compilable) {
				Compilable compEngine = (Compilable)engine;
				CompiledScript cs = compEngine.compile(m_sql);
				return (String) cs.eval(bindings);
			} else return (String) engine.eval(m_sql, bindings);
		} catch (Exception e) {
			return null;
		}
	}
	

}
