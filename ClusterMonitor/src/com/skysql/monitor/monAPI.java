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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The interface to the SkySQL Manager API, a REST interface to the monitor database.
 * This is the API that should be used to store all monitor observations and to retrieve 
 * system configuration information rather than access the SQLite monitoring database directly.
 * 
 * @author Mark Riddoch, Massimo Siani
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
	 * Execute stored batch commands?
	 */
	private boolean		m_bufferingExecution;
	/**
	 * Count the number of calls, to avoid buffering too often
	 */
	private int			m_cycles;
	/**
	 * Timezone read from the php configuration. Assume /etc/php.ini, default to Europe/London.
	 */
	private String		m_timeZone = null;
	
	/**
	 * Construct the monAPI instance. This consists of obtaining the information required
	 * to contact the API. This information is available via Java System Properties.
	 * Set the buffering to true by default.
	 */
	public monAPI()
	{
		this(true);
	}
	/**
	 * Construct the monAPI instance. This consists of obtaining the information required
	 * to contact the API. This information is available via Java System Properties.
	 *
	 * @param buffer		true if buffer is active (default).
	 */
	public monAPI(boolean buffer) {
		Properties props = System.getProperties();
		m_apiHost = props.getProperty("SKYSQL_API_HOST", "localhost");
		m_apiKey = props.getProperty("SKYSQL_API_KEY", "1f8d9e040e65d7b105538b1ed0231770");
		m_apiKeyID = props.getProperty("SKYSQL_API_KEYID", "1");
		m_bufferingExecution = buffer;
		m_cycles = 0;
	}
	/**
	 * Populate a monitor value for the system.
	 * 
	 * @param systemID		The ID of the System
	 * @param monitorKey	The key of the monitor itself
	 * @param value			The observed value
	 * @return True if the update was performed to the API
	 */
	public boolean MonitorValue(int systemID, String monitorKey, String value) {
		String apiRequest = "system/" + systemID + "/monitor/" + monitorKey + "/data";
		return restPost(apiRequest, new String[] {"value"}, new String[] {value});
	}
	/**
	 * Bulk update API.
	 * 
	 * @param apiRequest	the request uri
	 * @param fields
	 * @param values
	 * @return True if the update was performed
	 */
	public boolean bulkMonitorValue(String apiRequest, String[] fields, String[] values) {
		return restPost(apiRequest, fields, values);
	}
	
	/**
	 * DEPRECATED.
	 * API call which requires to modify something.
	 * 
	 * @param restRequest
	 * @param pName
	 * @param pValue
	 * @return
	 */
	public boolean UpdateValue(String restRequest, String pName, String pValue) {
		String[] newpName = {pName};
		String[] newpValue = {pValue};
		String result = updateValue(restRequest, newpName, newpValue);
		if (result == null) return false;
		return true;
	}
	
	/**
	 * API call which requires to modify something.
	 * 
	 * @param restRequest	the API URI
	 * @param pName			an array with the names of the parameters
	 * @param pValue		an array with the values of the parameters
	 * @return				the Json from the API as is, or null if an error occurred.
	 */
	public String updateValue(String restRequest, String[] pName, String[] pValue) {
		String outJson = restPut(restRequest, pName, pValue);
		return outJson;
	}
	
	/**
	 * Bounce the Json that comes from the API. Only for GET requests.
	 * This is a convenient call for getReturnedJson(restRequest, pName, pValue, null).
	 * 
	 * @param restRequest		the API URI
	 * @param pName				an array with the names of the parameters, can be null
	 * @param pValue			an array with the values of the parameters, can be null
	 * @return					the Json from the API as is, or null if an error occurred.
	 */
	public String getReturnedJson(String restRequest, String[] pName, String[] pValue) {
		String outJson = getReturnedJson(restRequest, pName, pValue, null);
		if (outJson == null) {
			Logging.error("Failed: Output Json: " + outJson);
			Logging.debug("        URI request: " + restRequest);
			return null;
		}
		return outJson;
	}
	
	/**
	 * Returns the Json that comes from the API. Only for GET requests.
	 * Sets the If-Modified-Since header, although it does not bounce
	 * the http code 304.
	 * 
	 * @param restRequest		the API URI
	 * @param pName				an array with the names of the parameters, can be null
	 * @param pValue			an array with the values of the parameters, can be null
	 * @param lastUpdate		the date for the If-Modified-Since header in RFC 2822 format, may be null or empty
	 * @return					the output Json, empty string if code 304 is returned
	 */
	public String getReturnedJson(String restRequest, String[] pName, String[] pValue, String lastUpdate) {
		String outJson = restGet(restRequest, pName, pValue, lastUpdate);
		if (outJson == null) {
			Logging.error("Failed: Output Json: " + outJson);
			Logging.debug("        URI request: " + restRequest);
			return null;
		}
		return outJson;
	}
	
	/**
	 * Send a GET request to the API and set the If-Modified-Since header.
	 * The lists of names and values of parameters to be passed to the API
	 * may and has to be null if no parameter is to be passed to the API.
	 * 
	 * @param restRequest	The URL, excluding the fixed stem
	 * @param pName[]		The parameter names for the GET request
	 * @param pValue[]		The parameter values for the GET request
	 * @param lastUpdate	The If-Modified-Since date, in RFC 2822 format
	 * @return				The output of the API (a JSON string)
	 */
	private String restGet(String restRequest, String[] pName, String[] pValue, String lastUpdate) {
		String result = "";
		String value = "";
		if (pName != null && pValue != null && ! pName[0].isEmpty()) {
			for (int i=0; i < pName.length; i++) {
				value += "&" + pName[i] + "=" + pValue[i];
			}
			value = value.substring(1);
		}
		try {
			// set up authorization
			String reqString = "http://" + m_apiHost + "/restfulapi/" + restRequest;
			if (value != "") {
				reqString += "?" + value;
			}
			String rfcdate = setDate();
		    String sb = this.setAuth(restRequest, rfcdate);
	        
			// set up connection
		    URL postURL = new URL(reqString);
			HttpURLConnection apiConn = (HttpURLConnection) postURL.openConnection();
			if (lastUpdate != null && lastUpdate != "") {
				apiConn.setRequestProperty("If-Modified-Since", lastUpdate);
			}
			setUpConn(apiConn, sb, rfcdate, value, "GET");
			
			// get output
			BufferedReader in = new BufferedReader(new InputStreamReader(apiConn.getInputStream()));
			String tmp;
			while ((tmp = in.readLine()) != null)
				result += tmp + "\n";
			in.close();

			if (apiConn.getResponseCode() == 304) {
				result = "";
			}
		} catch (ConnectException e) {
			Logging.error("Cannot connect to the web server.");
			return "";
		} catch (Exception e) {
			Logging.error(e.getMessage());
			return "";
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
	private String restPut(String restRequest, String[] pName, String[] pValue) {
		String result = "";
		String value = "";
		if (pName != null && pValue != null && ! pName[0].isEmpty()) {
			for (int i=0; i < pName.length; i++) {
				value += "&" + pName[i] + "=" + pValue[i];
			}
			value = value.substring(1);
		}
		try {
			// set up authorization for the redirected webpage (ie, $_POST variable)
			String reqString = "http://" + m_apiHost + "/restfulapi/" + restRequest;
			String rfcdate = setDate();
			String sb = this.setAuth(restRequest, rfcdate);

			// set up connection
			URL postURL = new URL(reqString);
			HttpURLConnection apiConn = (HttpURLConnection) postURL.openConnection();
			setUpConn(apiConn, sb, rfcdate, value, "PUT");

			// get output
			BufferedReader in = new BufferedReader(new InputStreamReader(apiConn.getInputStream()));
			String tmp;
			while ((tmp = in.readLine()) != null) {
				result += tmp + "\n";
			}
			in.close();
			
			// run buffer
			runBuffer();

			if (apiConn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error : "
						+ apiConn.getResponseMessage() + ": returned data: " + result);
			}
		} catch (Exception e) {
			pushFailedApi("restPut", restRequest, pName, pValue);
			return null;
		}
		return result;
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
		String value = "";
		if (pName != null && pValue != null && ! pName[0].isEmpty()) {
			for (int i=0; i < pName.length; i++) {
				value += "&" + pName[i] + "=" + pValue[i];
			}
			value = value.substring(1);
		}
		try {
			// set up authorization for the redirected webpage (ie, $_POST variable)
			String reqString = "http://" + m_apiHost + "/restfulapi/" + restRequest;
			String rfcdate = setDate();
			String sb = this.setAuth(restRequest, rfcdate);

			// set up connection
			URL postURL = new URL(reqString);
			HttpURLConnection apiConn = (HttpURLConnection) postURL.openConnection();
			setUpConn(apiConn, sb, rfcdate, value, "POST");

			// get output
			BufferedReader in = new BufferedReader(new InputStreamReader(apiConn.getInputStream()));
			String tmp;
			while ((tmp = in.readLine()) != null) {
				result += tmp + "\n";
			}
			in.close();
			
			// buffer
			runBuffer();

			if (apiConn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error : "
						+ apiConn.getResponseMessage() + ": returned data: " + result);
			}
		} catch (Exception e) {
			pushFailedApi("restPost", restRequest, pName, pValue);
			return false;
		}
		return true;
	}
	
	
	
	
	
	
	
	
	
	
	/**
	 * Read the timezone configuration and set the appropriate date format.
	 * 
	 * @return a string with the appropriate date
	 * @throws IOException
	 */
	private String setDate() throws IOException {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
		if (this.m_timeZone == null) {
			BufferedReader timeZone = new BufferedReader(new FileReader("/etc/php.ini"));
			String tz;
			while ((tz = timeZone.readLine()) != null) {
				Pattern p = Pattern.compile("date\\.timezone\\s*=\\s*(\\w+/\\w+)");
				Matcher m = p.matcher(tz);
				if (m.find()) {
					tz = m.group(1);
					break;
				}
			}
			timeZone.close();
			this.m_timeZone = (tz == null ? "Atlantic/Reykjavik" : tz);
		}
		sdf.setTimeZone(TimeZone.getTimeZone(this.m_timeZone));
		return sdf.format(new Date());
	}
	/**
	 * Compute the authorization string.
	 * 
	 * @param restRequest		the URI
	 * @param rfcdate			the date in rfc format
	 * @return the authorization string
	 * @throws NoSuchAlgorithmException
	 */
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
	/**
	 * Set up the common properties of the connection.
	 * 
	 * @param apiConn
	 * @param sb
	 * @param rfcdate
	 * @param value
	 * @throws IOException
	 */
	private void setUpConn(HttpURLConnection apiConn, String sb, String rfcdate, String value, String method)
	throws IOException {
		apiConn.setRequestMethod(method);
		apiConn.setRequestProperty("Accept", "application/json");
		apiConn.setRequestProperty("Authorization", "api-auth-" + m_apiKeyID + "-" + sb);
		apiConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		apiConn.setRequestProperty("charset", "utf-8");
		apiConn.setRequestProperty("Date", rfcdate);
		apiConn.setRequestProperty("Content-Length", "" + Integer.toString(value.getBytes().length));
		apiConn.setRequestProperty("X-SkySQL-API-Version", "1");
		apiConn.setDoOutput(true);
		apiConn.setUseCaches(false);
		if (method != "GET" && value.length() > 1) {
			OutputStreamWriter out = new OutputStreamWriter(apiConn.getOutputStream());
			out.write(value);
			out.flush();
			out.close();
		}
		return;
	}
	/**
	 * Run the buffer queue.
	 */
	private void runBuffer() {
		if (this.m_bufferingExecution && (this.m_cycles >= 10)) {
			APIBatchExecution.sendAll();
			this.m_cycles = 0;
		}
		this.m_cycles++;
	}
	/**
	 * Save the failed request to the stack.
	 * 
	 * @param typeValue			the name of the method to call
	 * @param restRequest		the request URI
	 * @param pName				the name of the parameters to pass to the API
	 * @param pValue			the value of the parameters to pass to the API
	 */
	private void pushFailedApi(String typeValue, String restRequest, String[] pName, String[] pValue) {
		List<Object> toBatch = new ArrayList<Object>();
		toBatch.add(typeValue);
		toBatch.add(restRequest);
		toBatch.add(pName);
		toBatch.add(pValue);
		try {
			toBatch.add(setDate());
		} catch (IOException e) {
			// IGNORE
		}
		APIBatchExecution.push(toBatch);
	}
	
	
	
	
	
	
	
	
	
	
	/**
	 * "Singleton" to implement the API buffering mechanism.
	 * 
	 * @author Massimo Siani
	 *
	 */
	private static class APIBatchExecution {
		/**
		 * The "stack" contains the failed calls according to the following logic:
		 * 		(java method, request URI, name of parameters, value of parameters)
		 */
		private List<List<Object>> stack = new ArrayList<List<Object>>();
		/**
		 * Backup of the stack, for performing operations on the stack
		 */
		private List<List<Object>> stack_bkp = new ArrayList<List<Object>>();
		/**
		 * Set a new monAPI instance that will not call the batch queue
		 * 		(otherwise infinite loop)
		 */
		private monAPI mapi = new monAPI(false);
		/**
		 * Instance of the present class. To be instantiate only once, thus
		 * keep it private
		 */
		private static volatile APIBatchExecution INSTANCE = null;
		/**
		 * Constructor, to be kept private (only one stack may exist)
		 */
		private APIBatchExecution() {}
		/**
		 * Constructor wrapper, for double-checking the INSTANCE uniqueness
		 */
		private synchronized static void APIBatchExecutionHolder() { 
			if (INSTANCE == null)
				INSTANCE = new APIBatchExecution();
		}
		/**
		 * Instantiate the unique instance of this class
		 * 
		 * @return the instantiated INSTANCE
		 */
		private static APIBatchExecution getInstance() {
			if (INSTANCE == null)
				APIBatchExecutionHolder();
			return INSTANCE;
		}
		/**
		 * Transfer the stack into its backup, and clear the stack
		 */
		private void backupBatchQueue() {
			getInstance().stack_bkp.addAll(getInstance().stack);
			getInstance().stack.clear();
		}
		/**
		 * Return the whole stack list.
		 * @return
		 */
//		public static List<List<Object>> getExecutionStack() {
//			return getInstance().stack;
//		}
		/**
		 * Push an element, on a LIFO
		 * @param toBuffer
		 * @return true if succeeds or false on errors
		 */
		public synchronized static boolean push(List<Object> toBuffer) {
			try {
				getInstance().stack.add(toBuffer);
			} catch (Exception e) {
				return false;
			}
			return true;
		}
		/**
		 * Pop on a LIFO
		 * @return
		 */
//		public synchronized static List<Object> pop() {
//			try {
//				int lastPos = getInstance().stack.size() -1;
//				List<Object> result = getInstance().stack.get(lastPos);
//				getInstance().stack.remove(lastPos);
//				return result;
//			} catch (Exception e) {
//				return null;
//			}
//		}
		/**
		 * Execute the buffer queue.
		 */
		public synchronized static void sendAll() {
			Method method;
			Class<? extends Object> classRequest;
			Class<? extends Object> classOne, classTwo;
			getInstance().backupBatchQueue();
			Logging.info("START BUFFERED REQUESTS: " + getInstance().stack_bkp.size() + " queued requests.");
			for (List<Object> batchCmd : getInstance().stack_bkp) {
				try {
					classRequest = batchCmd.get(1).getClass();
					classOne = batchCmd.get(2).getClass(); classOne =  String[].class;
					classTwo = batchCmd.get(3).getClass(); classTwo =  String[].class;
					method = getInstance().mapi.getClass().getDeclaredMethod((String) batchCmd.get(0),
							classRequest, classOne, classTwo);
					method.invoke(getInstance().mapi,
							batchCmd.get(1), batchCmd.get(2), batchCmd.get(3));
				} catch (ClassCastException e) {
					Logging.debug("Method " + (String) batchCmd.get(0)
							+ ": the parameters must be strings or extensions, got instead "
							+ batchCmd.get(1).getClass().getSimpleName() + ", " 
							+ batchCmd.get(2).getClass().getSimpleName() + ", "
							+ batchCmd.get(3).getClass().getSimpleName());
				} catch (NoSuchMethodException e) {
					Logging.debug("No method " + (String) batchCmd.get(0)
							+ " with parameters of type "
							+ batchCmd.get(1).getClass().getSimpleName() + ", "
							+ batchCmd.get(2).getClass().getSimpleName() + ", "
							+ batchCmd.get(3).getClass().getSimpleName());
				} catch (Exception e) {
					// ignore
				}
			}
			getInstance().stack_bkp.clear();
			Logging.info("STOP BUFFERED REQUESTS");
		}
	}
}