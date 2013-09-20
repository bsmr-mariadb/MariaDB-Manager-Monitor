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
 * @author Massimo Siani, Mark Riddoch
 *
 */
public class RhinoMonitor extends monitor {
	/**
     * The singleton class associated with this node that manages the
     * collection and storage of global variables and global status
     * data from the database server being monitored.
     */
	private globalStatusObject		m_global;

	/**
	 * Constructor for the RhinoMonitor class. Set the monitor and the
	 * globalStatusObject object.
	 * 
	 * @param db		the API interface
	 * @param id		the monitor id
	 * @param mon_node	the node object
	 */
	public RhinoMonitor(mondata db, int id, node mon_node) {
		super(db, id, mon_node);
		m_sql = m_sql.replace("\\", "");
		m_global = globalStatusObject.getInstance(mon_node);
	}
	
	/**
	 * The probe function, called once per probe cycle.
	 * 
	 * @param verbose	The logging level
	 */
	public void probe (boolean verbose) {
		if (m_sql.isEmpty())
			return;
		String value = runJavaScriptString();
		if (value == null)
		{
			value = "0";
		}
		saveObservation(value);
		m_lastValue = value;
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
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
			Bindings bindings = new SimpleBindings();
			HashMap<String, globalStatusObject> jsBindings= new HashMap<String, globalStatusObject>(1);
			jsBindings.put("globals", m_global);
			bindings.putAll(jsBindings);

			if (engine instanceof Compilable) {
				Compilable compEngine = (Compilable)engine;
				CompiledScript cs = compEngine.compile(m_sql);
				return ( (Double) cs.eval(bindings) ).toString();
			} else return ( (Double) engine.eval(m_sql, bindings) ).toString();
		} catch (Exception e) {
			System.err.println("Error in JavaScript: " + e.getMessage());
			return null;
		}
	}
	

}
