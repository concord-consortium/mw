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

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class HistoryManager {

	private final static long DAY = 24 * 60 * 60 * 1000;
	private final static HistoryManager sharedInstance = new HistoryManager();

	private Map<String, Long> map;
	private int nday = 7;

	private HistoryManager() {
		map = new HashMap<String, Long>();
	}

	public final static HistoryManager sharedInstance() {
		return sharedInstance;
	}

	public void setDays(int n) {
		nday = n;
	}

	public int getDays() {
		return nday;
	}

	public boolean wasVisited(String address) {
		if (address == null)
			return false;
		return map.containsKey(address);
	}

	public void addAddress(String address) {
		if (address == null)
			return;
		map.put(address, System.currentTimeMillis());
	}

	public void clear() {
		map.clear();
	}

	@SuppressWarnings("unchecked")
	public void readHistory(File file) {
		XMLDecoder in = null;
		try {
			in = new XMLDecoder(new BufferedInputStream(new FileInputStream(file)));
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
		if (in == null)
			return;
		try {
			map = (HashMap) in.readObject();
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
		}
		finally {
			in.close();
		}
		long t1 = System.currentTimeMillis();
		long t0;
		for (Iterator it = map.keySet().iterator(); it.hasNext();) {
			t0 = ((Long) map.get(it.next())).longValue();
			if (t1 - t0 > nday * DAY)
				it.remove();
		}
	}

	public void writeHistory(File file) {
		XMLEncoder out = null;
		try {
			out = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(file)));
		}
		catch (IOException e) {
			e.printStackTrace(System.err);
		}
		if (out == null)
			return;
		out.writeObject(map);
		out.close();
	}

}