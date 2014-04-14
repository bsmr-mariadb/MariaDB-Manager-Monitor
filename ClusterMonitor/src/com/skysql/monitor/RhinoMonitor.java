/*
 * This file is distributed as part of the MariaDB Manager.  It is free
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
 * Copyright 2012-2014 SkySQL Corporation Ab
 */

package com.skysql.monitor;

import java.text.DecimalFormat;
import java.util.HashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import com.skysql.java.Logging;
import com.skysql.java.MonData;

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
public class RhinoMonitor extends Monitor {
	/**
     * The singleton class associated with this Node that manages the
     * collection and storage of global variables and global status
     * data from the database server being monitored.
     */
	private GlobalStatusObject		m_global;

	/**
	 * Constructor for the RhinoMonitor class. Set the Monitor and the
	 * GlobalStatusObject object.
	 * 
	 * @param db		the API interface
	 * @param id		the Monitor id
	 * @param mon_node	the Node object
	 */
	public RhinoMonitor(MonData db, int id, Node mon_node) {
		super(db, id, mon_node);
		m_sql = m_sql.replace("\\", "");
		m_global = GlobalStatusObject.getInstance(mon_node);
	}
	
	/**
	 * The probe function, called once per probe cycle.
	 * 
	 * @param verbose	The logging level
	 */
	public void probe (boolean verbose) {
		if (m_sql.isEmpty()) {
			Logging.warn("    Empty SQL field, Monitor will not execute.");
			return;
		}
		String value = runJavaScriptString();
		if (value == null) {
			value = "0";
		}
		if (m_delta) {
			if (m_lastAbsValue != null && value != null) {
				Float absValue = new Float(value);
				Float delta = absValue - m_lastAbsValue;
				if (delta < 0) {
					Logging.debug("Negative delta value for probe, absolute value is " + absValue + " last absolute value " + m_lastAbsValue);
					delta = new Float(0);
				}
				DecimalFormat format = new DecimalFormat("###############.##");
				String deltaStr = format.format(delta);
				saveObservation(deltaStr);
				m_lastValue = deltaStr;
				m_lastAbsValue = absValue;
			} else if (value != null) {
				m_lastAbsValue = new Float(value);
			} else {
				m_lastAbsValue = null;
			}
		} else {
			saveObservation(value);
			m_lastValue = value;
		}
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
			HashMap<String, GlobalStatusObject> jsBindings= new HashMap<String, GlobalStatusObject>(1);
			jsBindings.put("globals", m_global);
			bindings.putAll(jsBindings);

			if (engine instanceof Compilable) {
				Compilable compEngine = (Compilable)engine;
				CompiledScript cs = compEngine.compile(m_sql);
				return ( (Double) cs.eval(bindings) ).toString();
			} else {
				return ( (Double) engine.eval(m_sql, bindings) ).toString();
			}
		} catch (Exception e) {
			if (m_confdb.getNodeState(m_node.getID()).equalsIgnoreCase("down")) {
				Logging.error("Cannot execute this Monitor: Node "
						+ m_confdb.getNodeName(m_node.getID()) + " is down.");
			} else {
				Logging.error("Error in JavaScript: " + e.getMessage());
			}
			return null;
		}
	}
	

}
