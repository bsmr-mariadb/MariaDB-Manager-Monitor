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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;
import java.util.Date;
import java.util.TimeZone;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * The interface to the SkySQL Manager API, a REST interface to the monitor database.
 * This is the API that should be used to store all monitor observations and to retrieve 
 * system configuration information rather than access the SQLite monitoring database directly.
 * 
 * @author Mark Riddoch
 *
 */
public class monAPI {
	/**
	 * The host on which the API is running
	 */
	String		m_apiHost;
	/**
	 * The API Key to use to access the API
	 */
	String		m_apiKey;
	/**
	 * The API key ID as assigned by the API provider
	 */
	String		m_apiKeyID;
	
	/**
	 * Construct the monAPI instance. This consists of obtaining the information required
	 * to contact the API. This information is available via Java System Properties.
	 */
	public monAPI()
	{
		Properties props = System.getProperties();
		m_apiHost = props.getProperty("SKYSQL_API_HOST", "localhost");
		m_apiKey = props.getProperty("SKYSQL_API_KEY", "1f8d9e040e65d7b105538b1ed0231770");
		m_apiKeyID = props.getProperty("SKYSQL_API_KEYID", "1");
	}
	
	/**
	 * Populate a monitor value for a given node.
	 * 
	 * @param systemID	The ID of the System
	 * @param nodeID	The ID of the node within the system that the data refers to
	 * @param monitorID	The ID of the monitor itself
	 * @param value		The observed value
	 * @return True if the update was performed to the API
	 */
	public boolean MonitorValue(int systemID, int nodeID, int monitorID, String value)
	{
		
		return restPost("system/" + systemID + "/node/" + nodeID + "/monitor/" + monitorID + "/data", "value", value);
	}
	
	/**
	 * Populate a monitor value for the system
	 * 
	 * @param systemID	The ID of the System
	 * @param monitorID	The ID of the monitor itself
	 * @param value		The observed value
	 * @return True if the update was performed to the API
	 */
	public boolean MonitorValue(int systemID, int monitorID, String value)
	{
		return restPost("system/" + systemID + "/monitor/" + monitorID + "/data", "value", value);
	}
	
	public boolean UpdateValue(String restRequest, String pName, String pValue) {
		String[] newpName = {pName};
		String[] newpValue = {pValue};
		return UpdateValue(restRequest, newpName, newpValue);
	}
	public boolean UpdateValue(String restRequest, String[] pName, String[] pValue) {
		return restPut(restRequest, pName, pValue);
	}
	
	/**
	 * Reads a value
	 * 
	 * @param restRequest	The URL, excluding the fixed stem
	 * @param pName			The parameter name for the GET request
	 * @param pValue		The parameter value for the GET request
	 * @return
	 */
	public List<String> SystemValue(String restRequest, String pName, String pValue) {
		String[] newpName = new String[] {pName};
		String[] newpValue = new String[] {pValue};
		return SystemValue(restRequest, newpName, newpValue);
	}
	
	/**
	 * Calls the API and parses the resulting JSON
	 * 
	 * @param restRequest	The URL, excluding the fixed stem
	 * @param pName[]		The parameter names for the GET request
	 * @param pValue[]		The parameter values for the GET request
	 * @return
	 */
	public List<String> SystemValue(String restRequest, String[] pName, String[] pValue) {
		String outJson = restGet(restRequest, pName, pValue);
		return json.getStringField(outJson, pValue[0]);
	}
	
	/**
	 * Send a GET request to the API
	 * 
	 * @param restRequest	The URL, excluding the fixed stem
	 * @param pName[]		The parameter names for the GET request
	 * @param pValue[]		The parameter values for the GET request
	 * @return				The output of the API (a JSON string)
	 */
	private String restGet(String restRequest, String[] pName, String[] pValue) {
		String result = "";
		String[] _params_name = {"_rfcdate", "_authorization", "_method", "_accept"};
		String[] _params = new String[_params_name.length];
		String value = "";
		for (int i=0; i < pName.length; i++) {
			value += "&" + pName[i] + "=" + pValue[i];
		}
		
		try {
			// set up authorization for the redirected webpage (ie, $_POST variable)
			String reqString = "http://" + m_apiHost + "/consoleAPI/api/" + restRequest;
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
			sdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));
		    String rfcdate = sdf.format(new Date());
		    String sb = this.setAuth(restRequest, rfcdate);
		    _params[0] = encodeURIComponent(rfcdate);
			_params[1] = "api-auth-" + m_apiKeyID + "-" + sb;
			_params[2] = "GET";
			_params[3] = "application/json";
			for (int i = 0; i < _params.length; i++) {
				value += "&" + _params_name[i] + "=" + _params[i];
			}
			value = value.substring(1);
			if (value.substring(0,1).matches("=")) {
				value = value.substring(2);
			}
	        
			// set up connection
		    URL postURL = new URL(reqString);
			HttpURLConnection apiConn = (HttpURLConnection) postURL.openConnection();
	        apiConn.setRequestMethod("POST");
			apiConn.setRequestProperty("Accept", "application/json");
			apiConn.setRequestProperty("Authorization", "api-auth-" + m_apiKeyID + "-" + sb);
			apiConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			apiConn.setRequestProperty("charset", "utf-8");
			apiConn.setRequestProperty("Date", rfcdate);
			apiConn.setRequestProperty("Content-Length", "" + Integer.toString(value.getBytes().length));
			apiConn.setDoOutput(true);
			apiConn.setUseCaches(false);
			OutputStreamWriter out = new OutputStreamWriter(apiConn.getOutputStream());
			out.write(value);
			out.flush();
			out.close();
			
			// get output
			BufferedReader in = new BufferedReader(new InputStreamReader(apiConn.getInputStream()));
			String tmp;
			while ((tmp = in.readLine()) != null) {
				result += tmp + "\n";
			}
			in.close();

			if (apiConn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error : "
					+ apiConn.getResponseMessage() + ": returned data: " + result);
			}
			
		} catch (IOException e) {
			e.printStackTrace();			
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}
	
	/**
	 * Send a PUT request to the API
	 * 
	 * @param restRequest	The URL, excluding the fixed stem
	 * @param pName[]		The parameter names for the GET request
	 * @param pValue[]		The parameter values for the GET request
	 * @return				The output of the API (a JSON string)
	 */
	private boolean restPut(String restRequest, String[] pName, String[] pValue) {
		String result = "";
		String[] _params_name = {"_rfcdate", "_authorization", "_method", "_accept"};
		String[] _params = new String[_params_name.length];
		String value = "";
		for (int i=0; i < pName.length; i++) {
			value += "&" + pName[i] + "=" + pValue[i];
		}

		try {
			// set up authorization for the redirected webpage (ie, $_POST variable)
			String reqString = "http://" + m_apiHost + "/consoleAPI/api/" + restRequest;
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
			sdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));
			String rfcdate = sdf.format(new Date());
			String sb = this.setAuth(restRequest, rfcdate);
			_params[0] = encodeURIComponent(rfcdate);
			_params[1] = "api-auth-" + m_apiKeyID + "-" + sb;
			_params[2] = "PUT";
			_params[3] = "application/json";
			for (int i = 0; i < _params.length; i++) {
				value += "&" + _params_name[i] + "=" + _params[i];
			}
			value = value.substring(1);
			if (value.substring(0,1).matches("=")) {
				value = value.substring(2);
			}

			// set up connection
			URL postURL = new URL(reqString);
			HttpURLConnection apiConn = (HttpURLConnection) postURL.openConnection();
			apiConn.setRequestMethod("POST");
			apiConn.setRequestProperty("Accept", "application/json");
			apiConn.setRequestProperty("Authorization", "api-auth-" + m_apiKeyID + "-" + sb);
			apiConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			apiConn.setRequestProperty("charset", "utf-8");
			apiConn.setRequestProperty("Date", rfcdate);
			apiConn.setRequestProperty("Content-Length", "" + Integer.toString(value.getBytes().length));
			apiConn.setDoOutput(true);
			apiConn.setUseCaches(false);
			OutputStreamWriter out = new OutputStreamWriter(apiConn.getOutputStream());
			out.write(value);
			out.flush();
			out.close();

			// get output
			BufferedReader in = new BufferedReader(new InputStreamReader(apiConn.getInputStream()));
			String tmp;
			while ((tmp = in.readLine()) != null) {
				result += tmp + "\n";
			}
			in.close();

			if (apiConn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error : "
						+ apiConn.getResponseMessage() + ": returned data: " + result);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Send a POST request to the API
	 * 
	 * @param restRequest	The URL, excluding the fixed stem
	 * @param pName			The parameter name for the post request
	 * @param pValue		The parameter value for the port request
	 */
	private boolean restPost(String restRequest, String pName, String pValue)
	{
		String[] newpName = {pName};
		String[] newpValue = {pValue};
		return restPost(restRequest, newpName, newpValue);
	}
	
	/**
	 * Send a POST request to the API
	 * 
	 * @param restRequest	The URL, excluding the fixed stem
	 * @param pName			The parameter names for the post request
	 * @param pValue		The parameter values for the port request
	 */
	private boolean restPost(String restRequest, String[] pName, String[] pValue) {
		String result = "";
		String[] _params_name = {"_rfcdate", "_authorization", "_method", "_accept"};
		String[] _params = new String[_params_name.length];
		String value = "";
		for (int i=0; i < pName.length; i++) {
			value += "&" + pName[i] + "=" + pValue[i];
		}

		try {
			// set up authorization for the redirected webpage (ie, $_POST variable)
			String reqString = "http://" + m_apiHost + "/consoleAPI/api/" + restRequest;
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
			sdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));
			String rfcdate = sdf.format(new Date());
			String sb = this.setAuth(restRequest, rfcdate);
			_params[0] = encodeURIComponent(rfcdate);
			_params[1] = "api-auth-" + m_apiKeyID + "-" + sb;
			_params[2] = "POST";
			_params[3] = "application/json";
			for (int i = 0; i < _params.length; i++) {
				value += "&" + _params_name[i] + "=" + _params[i];
			}
			value = value.substring(1);
			if (value.substring(0,1).matches("=")) {
				value = value.substring(2);
			}

			// set up connection
			URL postURL = new URL(reqString);
			HttpURLConnection apiConn = (HttpURLConnection) postURL.openConnection();
			apiConn.setRequestMethod("POST");
			apiConn.setRequestProperty("Accept", "application/json");
			apiConn.setRequestProperty("Authorization", "api-auth-" + m_apiKeyID + "-" + sb);
			apiConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			apiConn.setRequestProperty("charset", "utf-8");
			apiConn.setRequestProperty("Date", rfcdate);
			apiConn.setRequestProperty("Content-Length", "" + Integer.toString(value.getBytes().length));
			apiConn.setDoOutput(true);
			apiConn.setUseCaches(false);
			OutputStreamWriter out = new OutputStreamWriter(apiConn.getOutputStream());
			out.write(value);
			out.flush();
			out.close();

			// get output
			BufferedReader in = new BufferedReader(new InputStreamReader(apiConn.getInputStream()));
			String tmp;
			while ((tmp = in.readLine()) != null) {
				result += tmp + "\n";
			}
			in.close();

			if (apiConn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error : "
						+ apiConn.getResponseMessage() + ": returned data: " + result);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	
	
	
	private String setAuth(String restRequest, String rfcdate) throws NoSuchAlgorithmException {
		String fullkey = (restRequest.substring(0, 1).matches("/")) ? restRequest.substring(1) : restRequest;
		fullkey += m_apiKey + rfcdate;
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(fullkey.getBytes());
		byte byteData[] = md.digest();
		//convert the byte to hex format method 1
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < byteData.length; i++) {
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}
	
	
	public static String encodeURIComponent(String component)   {     
		String result = null;      
		
		try {       
			result = URLEncoder.encode(component, "UTF-8")   
				   .replaceAll("\\%28", "(")
				   .replaceAll("\\%29", ")")
				   .replaceAll("\\%27", "'")
				   .replaceAll("\\%21", "!")
				   .replaceAll("\\%7E", "~");
		} catch (Exception e) {       
			result = component;     
		}
		
		return result;   
	}  
		
}
