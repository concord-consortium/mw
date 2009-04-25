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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * This implements component pooling, which facilitates the reuse of components. Note: ComponentPool is the legacy code
 * from old MW implementation. Ideally, it should be merged with this class.
 * 
 * @author Charles Xie
 */

public final class InstancePool {

	private final static int MAX = 100;
	private static InstancePool instancePool = new InstancePool();
	private final Semaphore available = new Semaphore(MAX, true);
	private Map<Object, Boolean> pool;

	private InstancePool() {
		pool = Collections.synchronizedMap(new LinkedHashMap<Object, Boolean>());
	}

	public static InstancePool sharedInstance() {
		return instancePool;
	}

	public int getSize() {
		return pool.size();
	}

	public String[][] getSnapshotInfo() {
		String[][] s = new String[pool.size()][2];
		int i = 0;
		synchronized (pool) {
			for (Object o : pool.keySet()) {
				if (o instanceof PageMolecularViewer) {
					s[i][0] = "Jmol viewer";
				}
				else if (o instanceof PageMd3d) {
					s[i][0] = "3D simulator";
				}
				else {
					s[i][0] = "Unknown instance";
				}
				s[i][1] = pool.get(o) ? "Used" : "Available";
				i++;
			}
		}
		return s;
	}

	public void reset() {
		synchronized (pool) {
			for (Object o : pool.keySet()) {
				if (pool.get(o) == Boolean.FALSE)
					continue;
				if (o instanceof Engine) {
					Engine e = (Engine) o;
					e.stopImmediately();
					// FIXME: This method is ineffective when the instance is in the previous and current page,
					// because the reused component is always "showing". It works only when the instance is not
					// in the previous page.
					if (e.isShowing())
						continue;
					e.reset();
				}
				if (o instanceof BasicModel) {
					((BasicModel) o).haltScriptExecution();
				}
			}
		}
	}

	// what should be the policy here? should we stop a model that is running but hiden by the front window?
	public void stopAllRunningModels() {
		synchronized (pool) {
			for (Object o : pool.keySet()) {
				if (pool.get(o) == Boolean.FALSE) {
					if (o instanceof Engine) {
						((Engine) o).stopImmediately();
					}
					if (o instanceof BasicModel) {
						((BasicModel) o).haltScriptExecution();
					}
				}
			}
		}
	}

	public void setStatus(Object object, boolean used) {
		if (!used)
			available.release();
		if (pool.containsKey(object))
			pool.put(object, used);
	}

	public Object getUnusedInstance(Class c) {

		try {
			available.acquire();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}

		// search for an existing instance of the specified class that is not currently used
		synchronized (pool) {
			for (Object o : pool.keySet()) {
				if (o.getClass().equals(c)) {
					if (pool.get(o) == Boolean.FALSE) {
						pool.put(o, true);
						return o;
					}
				}
			}
		}

		// if no unused existing instance is found, create a new one and put it into the pool

		Object o = createInstance(c);
		if (o != null)
			return o;
		return createInstance(c); // try once and only once again (to avoid possible infinite loops)

	}

	private Object createInstance(Class c) {
		try {
			Object o = c.newInstance();
			pool.put(o, true);
			return o;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}