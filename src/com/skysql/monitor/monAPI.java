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
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.Date;



import java.net.HttpURLConnection;
import java.net.URL;

public class monAPI {
	String		m_apiHost;
	String		m_apiKey;
	String		m_apiKeyID;
	
	/*
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
	
	/*
	 * Populate a monitor value for a given node.
	 */
	public boolean MonitorValue(int systemID, int nodeID, int monitorID, String value)
	{
		
		return restPost("system/" + systemID + "/node/" + nodeID + "/monitor/" + monitorID + "/data", "value", value);
	}
	
	/*
	 * Populate a monitor value for the system
	 */
	public boolean MonitorValue(int systemID, int monitorID, String value)
	{
		return restPost("system/" + systemID + "/monitor/" + monitorID + "/data", "value", value);
	}


	/* 
	 * Send a POST request to the API
	 */
	private boolean restPost(String restRequest, String pName, String pValue)
	{
		String result = null;
		
		String value = pName + "=" + pValue;
		
		
		try {
	 
			String reqString = "http://" + m_apiHost + "/consoleAPI/api/" + restRequest;
			URL postURL = new URL(reqString);
			HttpURLConnection apiConn = (HttpURLConnection)postURL.openConnection();
			apiConn.setRequestMethod("POST");
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
		    String rfcdate = sdf.format(new Date());
			
			String fullkey = restRequest + m_apiKey + rfcdate;
			MessageDigest md = MessageDigest.getInstance("MD5");

	        md.update(fullkey.getBytes());
	        byte byteData[] = md.digest();
	        //convert the byte to hex format method 1
	        StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < byteData.length; i++) {
				sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
	        }

			apiConn.addRequestProperty("Date", rfcdate);
			apiConn.setRequestProperty("Authorization", "api-auth-" + m_apiKeyID + "-" + sb.toString());
			apiConn.setRequestProperty("Accept", "application/json");
			apiConn.setDoOutput(true);
			apiConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			apiConn.setRequestProperty("charset", "utf-8");
			apiConn.setRequestProperty("Content-Length", "" + Integer.toString(value.getBytes().length));
			apiConn.setUseCaches(false);
			apiConn.setRequestMethod("POST");
			OutputStreamWriter out = new OutputStreamWriter(apiConn.getOutputStream());
			out.write(value);
			out.close();
			
			BufferedReader in = new BufferedReader(new InputStreamReader(apiConn.getInputStream()));
			result = in.readLine();
			in.close();
			
			if (apiConn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error : "
					+ apiConn.getResponseMessage() + ": returned data: " + result);
			}
		  } catch (IOException e) {
					e.printStackTrace();
		  } catch (Exception e) {
			e.printStackTrace();
		  }
		return true;
	}
}
