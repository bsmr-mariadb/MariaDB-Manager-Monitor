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

import java.util.Iterator;
import java.util.List;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;


/**
 * PublicIP address monitor
 * 
 * This runs as a separate thread to the rest of the monitor and looks for Public IP address changes in
 * the cluster. This is useful for when the elastic IP address migrates between nodes of the cluster or
 * a stopped node returns to the cluster with a new Public IP address.
 * 
 * @author Mark Riddoch
 */
public class PublicIPMonitor extends Thread {

	/**
	 * The monitoring database
	 */
	private mondata			m_db;
	/**
	 * The connection to the Amazon API
	 */
	private AmazonEC2		m_ec2Client;
	/**
	 * The log verbosity level
	 */
	private boolean			m_verbose;
	
	/**
	 * Constructor for the IP monitor, the main function of the constructor is to 
	 * create the connection to the Amazon API
	 * 
	 * @param db		Monitoring database
	 * @param verbose	Logging verbosity
	 */
	public PublicIPMonitor(mondata db, boolean verbose)
	{
		m_db = db;
		m_verbose = verbose;
		String timeout = System.getenv("EC2_CONNECTION_TIMEOUT");
		String retry = System.getenv("EC2_RETRY_COUNT");
		ClientConfiguration config = new ClientConfiguration();
		if (timeout != null)
		{
			  Integer i = new Integer(timeout);
			  int to = i.intValue() * 1000;
			  config.setConnectionTimeout(to);
		}
		else
		{
			  config.setConnectionTimeout(1000 * 60 * 2);	// A 2 minute connection timeout
		}
		if (retry != null)
		{
			  Integer i = new Integer(retry);
			  config.setMaxErrorRetry(i.intValue());
		}
		else
		{
			  config.setMaxErrorRetry(10);			// Max number of retries for a retryable error
		}
		m_ec2Client = new AmazonEC2Client(config);
	}
	
	/**
	 * The thread entry point. Loop around fetching data from the Amazon EC2 API as to the
	 * current IP addresses of the nods within the cluster and update the monitoring database
	 */
	public void run()
	{
		System.out.println("PublicIP address monitor for Amazon EC2 instances started.");
		while (true)
		{
			try {
				Thread.sleep(60000);	// Sleep for 60 seconds
			} catch (Exception ex) {
				// ignore
			}
			List<String> instanceList = m_db.getInstances();
			
			DescribeInstancesResult res;
			try {
				DescribeInstancesRequest req = new DescribeInstancesRequest();
				req.withInstanceIds(instanceList);
				if (m_verbose)
				{
					System.out.println("Obtain instance information from AWS");
				}
				res = m_ec2Client.describeInstances(req);
			} catch (Exception ex) {
				System.err.println(ex.getMessage() + " getting instance information from EC2");
				continue;
			}
			List<Reservation> reservations = res.getReservations();
			Iterator<Reservation> ritr = reservations.iterator();
		    while (ritr.hasNext())
		    {
		    	  Reservation reserve = ritr.next();
		    	  List<Instance> instList = reserve.getInstances();
		    	  Iterator<Instance> instIt = instList.iterator();
		    	  while (instIt.hasNext())
		    	  {
		    		  Instance inst = instIt.next();
		    		  if (m_db.setPublicIP(inst.getInstanceId(), inst.getPublicIpAddress()) && m_verbose)
		    		  {
		    			  System.out.println("Updated public IP address for instance " + inst.getImageId());
		    		  }
		    		  if (m_db.setPrivateIP(inst.getInstanceId(), inst.getPrivateIpAddress()) && m_verbose)
		    		  {
		    			  System.out.println("Updated private IP address for instance " + inst.getImageId());
		    		  }
		    	  }
		    }
		}
	}
}
