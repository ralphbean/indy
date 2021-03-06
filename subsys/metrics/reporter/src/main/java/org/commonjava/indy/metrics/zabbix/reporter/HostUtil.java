/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.metrics.zabbix.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * http://stackoverflow.com/questions/7348711/recommended-way-to-get-hostname-in
 * -java
 * 
 */
public class HostUtil
{
	private static final Logger logger = LoggerFactory.getLogger( HostUtil.class);

	public static void main(String[] args) {
		System.err.println(getHostName());
		System.err.println(getHostAddress());
		System.err.println(getNotLoopbackAddress());
	}

	public static String getHostName() {
		try {
			String hostName = InetAddress.getLocalHost().getHostName();
			if (hostName != null && !hostName.isEmpty()) {
				return hostName;
			}
		} catch (UnknownHostException e) {
			logger.error("get hostName error!", e);
		}

		String host = System.getenv("COMPUTERNAME");
		if (host != null)
			return host;
		host = System.getenv("HOSTNAME");
		if (host != null)
			return host;

		return null;
	}

	public static String getNotLoopbackAddress() {
		String hostName = null;
		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface nic = interfaces.nextElement();
				Enumeration<InetAddress> addresses = nic.getInetAddresses();
				while (hostName == null && addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if (!address.isLoopbackAddress()) {
						hostName = address.getHostName();
					}
				}
			}
		} catch (SocketException e) {
			logger.error("getNotLoopbackAddress error!", e);
		}
		return hostName;
	}

	public static String getHostAddress() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			logger.error("get hostAddress error!", e);
		}

		return null;
	}

}
