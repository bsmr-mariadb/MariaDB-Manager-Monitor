/**
 * 
 */
package com.skysql.monitor;

import java.util.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Parse a JSON string.
 * 
 * @author Massimo Siani
 *
 */
public class json {
	
	protected static String sObject = "org.json.simple.JSONObject";
	protected static String sArray = "org.json.simple.JSONArray";

	
	/**
	 * Extract the values from a json string.
	 * 
	 * @param inJson The input string
	 * @param field For future use
	 * @return The list of all values
	 */
	public static List<String> getStringField(String inJson, String field) {
		List<String> result = new ArrayList<String>();
		JSONParser parser = new JSONParser();
		try {
			Object parsedJson = parser.parse(inJson);
			JSONObject jsonObj = (JSONObject)parsedJson;
			String mainClass = (String)jsonObj.keySet().toArray()[0];
			
			// json string does not contain arrays: { "something": {"id": 1, "other": "othvalue"} }
			if (jsonObj.get(mainClass).getClass().getName() == sObject) {
				// jsonObjw is the "sub-json"
				JSONObject jsonObjw = (JSONObject)jsonObj.get(mainClass);
				Object[] keySetObj = jsonObjw.keySet().toArray();
				for (int i = 0; i < keySetObj.length; i++) {
					result.add(jsonObjw.get(keySetObj[i].toString().replaceFirst(" ", "")).toString());
				}
			}
			// json contains an array: { "something": [ {"id": 1, "oth": "othval1"}, {"id": 2, "oth": "othval2"} ] }
			else if (jsonObj.get(mainClass).getClass().getName() == sArray) {
				// "sub-array"
				JSONArray jsonArray = (JSONArray)jsonObj.get(mainClass);
				for (int j = 0; j < jsonArray.size(); j++) {
					// each element of the array is a "sub-json"
					JSONObject jsonObjw = (JSONObject) jsonArray.get(j);
					Object[] keySetObj = jsonObjw.keySet().toArray();
					for (int i = 0; i < keySetObj.length; i++) {
						result.add(jsonObjw.get(keySetObj[i].toString().replaceFirst(" ", "")).toString());
					}
				}
			}
			
			return result;
		} catch (NullPointerException e) {
		} catch (Exception e) {
			System.err.println("Unexpected message returned from API: first line is:\n\t\t"
					+ inJson.substring(1, inJson.indexOf("\n", 1)));
		}
		return null;
	}
	
}
