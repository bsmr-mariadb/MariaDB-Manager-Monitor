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

import com.google.gson.Gson;

/**
 * Class to manage Json strings, from and to Java objects.
 * This class requires the definitions of appropriate classes
 * that reflect the Json structure.
 * 
 * @author Massimo Siani
 *
 */
public class GsonManager {

	private static Gson gson = new Gson();

	public static String toString(Object obj) {
		String result = gson.toJson(obj);
		return result;
	}

	public static <T> T fromJson(String inJson, Class<T> objClass) {
		T resultObj = gson.fromJson(inJson, objClass);
		return resultObj;
	}

}
