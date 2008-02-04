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
package org.jmol.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @ThreadSafe
 * @author Charles Xie
 * 
 */
class Planes extends Shape {

	private List<Plane> list;

	Planes() {
		list = Collections.synchronizedList(new ArrayList<Plane>());
	}

	void addPlane(Plane p) {
		if (p == null)
			return;
		synchronized (list) {
			if (list.contains(p))
				return;
			list.add(p);
		}
	}

	void removePlane(Plane p) {
		if (p == null)
			return;
		list.remove(p);
	}

	Object getLock() {
		return list;
	}

	/**
	 * Clients calling this method and count() to iterate through the elements must use the lock returned by getLock()
	 * to guard their iteration code.
	 */
	Plane getPlane(int i) {
		if (i < 0)
			return null;
		synchronized (list) {
			if (i >= list.size())
				return null;
			return list.get(i);
		}
	}

	boolean isEmpty() {
		return list.isEmpty();
	}

	void clear() {
		list.clear();
	}

	int count() {
		return list.size();
	}

}