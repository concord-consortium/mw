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

package org.concord.modeler.g2d;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class CurvePool {

	private Map<Curve, Boolean> pool;

	public CurvePool(int initCurves) {
		pool = Collections.synchronizedMap(new HashMap<Curve, Boolean>(initCurves));
		for (int i = 0; i < initCurves; i++)
			pool.put(new Curve(), Boolean.FALSE);
	}

	public int getCapacity() {
		return pool.size();
	}

	public Curve getCurve() {

		synchronized (pool) {
			for (Curve c : pool.keySet()) {
				Boolean b = pool.get(c);
				if (b == Boolean.FALSE) {
					pool.put(c, Boolean.TRUE);
					return c;
				}
			}
		}

		// if we get here, there were no free curves. Create one more.

		pool.put(new Curve(), Boolean.FALSE);

		return getCurve();

	}

	public void reset() {
		synchronized (pool) {
			for (Curve c : pool.keySet()) {
				pool.put(c, Boolean.FALSE);
			}
		}
	}

}
