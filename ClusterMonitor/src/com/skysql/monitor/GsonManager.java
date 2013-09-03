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
 * Copyright 2012, 2013 SkySQL Ab
 */

package com.skysql.monitor;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Class to manage Json strings, from and to Java objects.
 * This class requires the definitions of appropriate classes
 * that reflect the Json structure.
 * 
 * @author Massimo Siani
 *
 */
public class GsonManager {

	/**
	 * Gson object.
	 */
	private static Gson			gson = new Gson();
	private static JsonParser	jsonParser = new JsonParser();
	private static JSONParser	parser = new JSONParser();

	/**
	 * Generate Json from an object.
	 * 
	 * @param obj the object.
	 * @return the Json string.
	 */
	public static String toString(Object obj) {
		String result = gson.toJson(obj);
		return result;
	}

	/**
	 * Convert a Json into a Java object.
	 * 
	 * @param inJson the Json as a string.
	 * @param objClass the class of the object.
	 * @return the deserialized Json as a Java object.
	 * @throws ParseException 
	 */
	public static <T> T fromJson(String inJson, Class<T> objClass) {
		try {
			JSONObject jsonObj = (JSONObject)parser.parse(inJson);
			String mainClass = (String)jsonObj.keySet().toArray()[0];
			JsonObject object1 = jsonParser.parse(inJson).getAsJsonObject().getAsJsonObject(mainClass);
			T resultObj = gson.fromJson(object1, objClass);
			return resultObj;
		} catch (Exception e) {
			return null;
		}
	}

}
