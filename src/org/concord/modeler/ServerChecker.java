/*
 *   Copyright (C) 2006  The Concord Consortium, Inc.,
 *   25 Love Lane, Concord, MA 01742
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * END LICENSE */

package org.concord.modeler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.concord.modeler.util.FileUtilities;

class ServerChecker {

	private static ServerChecker checker = new ServerChecker();
	private static int CHECK_INTERVAL = 5000; // check every 5 seconds?

	private Map<String, Long> lastCheckTimeMap;
	private Map<String, Boolean> availabilityMap;

	private ServerChecker() {
		lastCheckTimeMap = new HashMap<String, Long>();
		availabilityMap = new HashMap<String, Boolean>();
	}

	public static ServerChecker sharedInstance() {
		return checker;
	}

	/** check the MW server */
	public boolean check() {
		return check(Modeler.getContextRoot());
	}

	/** check the server at the specified address */
	private boolean check(String address) {
		if (address == null)
			return false;
		if (!FileUtilities.isRemote(address))
			return false;
		URL url = null;
		try {
			url = new URL(address);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
		return check(url);
	}

	private boolean check(URL url) {
		if (url == null)
			return false;
		long lastCheckTime = 0;
		boolean available = true;
		String host = url.getHost();
		Long o = lastCheckTimeMap.get(host);
		if (o != null)
			lastCheckTime = o.longValue();
		Boolean b = availabilityMap.get(host);
		if (b != null)
			available = b.booleanValue();
		if (lastCheckTime > 0 && System.currentTimeMillis() - lastCheckTime < CHECK_INTERVAL)
			return available;
		InetAddress address = null;
		try {
			address = InetAddress.getByName(url.getHost());
			available = address.isReachable(10000);
		}
		catch (Throwable e) {
			available = false;
		}
		setLastCheckTime(host, System.currentTimeMillis());
		setAvailable(host, available);
		return available;
	}

	public void setLastCheckTime(String host, long t) {
		lastCheckTimeMap.put(host, t);
	}

	public boolean shouldCheck(String host) {
		if (host == null)
			return false;
		Long o = lastCheckTimeMap.get(host);
		if (o != null)
			return System.currentTimeMillis() - o.longValue() > CHECK_INTERVAL;
		return true;
	}

	public void setAvailable(String host, boolean b) {
		availabilityMap.put(host, b ? Boolean.TRUE : Boolean.FALSE);
	}

	public boolean isAvailable(String host) {
		if (host == null)
			return false;
		Boolean o = availabilityMap.get(host);
		if (o != null)
			return o.booleanValue();
		return true;
	}

	public static boolean ping(String host, int timeout) {
		InetAddress address = null;
		try {
			address = InetAddress.getByName(host);
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
		boolean b = false;
		try {
			b = address.isReachable(timeout);
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return b;
	}

}