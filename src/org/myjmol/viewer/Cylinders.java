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
package org.myjmol.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Charles Xie
 * 
 */
class Cylinders extends Shape {

	private List<Cylinder> list;

	Cylinders() {
		list = Collections.synchronizedList(new ArrayList<Cylinder>());
	}

	Object getLock() {
		return list;
	}

	void addCylinder(Cylinder c) {
		if (c == null)
			return;
		synchronized (list) {
			if (list.contains(c))
				return;
			list.add(c);
		}
	}

	void removeCylinder(Cylinder c) {
		if (c == null)
			return;
		list.remove(c);
	}

	/**
	 * Clients calling this method and count() to iterate through the elements must use the lock returned by getLock()
	 * to guard their iteration code.
	 */
	Cylinder getCylinder(int i) {
		if (i < 0)
			return null;
		synchronized (list) {
			if (i >= list.size())
				return null;
			return list.get(i);
		}
	}

	void clear() {
		list.clear();
	}

	boolean isEmpty() {
		return list.isEmpty();
	}

	int count() {
		return list.size();
	}

}