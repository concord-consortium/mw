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

import javax.vecmath.Point3f;

/**
 * @author Charles Xie
 * 
 */
class Cuboids extends Shape {

	private List<Cuboid> list;
	private Point3f min, max;

	Cuboids() {
		list = Collections.synchronizedList(new ArrayList<Cuboid>());
	}

	Object getLock() {
		return list;
	}

	float getRotationRadius(Point3f center) {
		float max = 0;
		float rad;
		synchronized (list) {
			for (Cuboid c : list) {
				rad = c.getRotationRadius(center);
				if (max < rad)
					max = rad;
			}
		}
		return max;
	}

	Point3f getMin() {
		if (min == null)
			min = new Point3f();
		if (list.isEmpty()) {
			min.set(0, 0, 0);
			return min;
		}
		Cuboid c = getCuboid(0);
		min.x = c.getMinX();
		min.y = c.getMinY();
		min.z = c.getMinZ();
		synchronized (list) {
			int n = count();
			if (n > 1) {
				for (int i = 1; i < n; i++) {
					c = getCuboid(i);
					if (min.x > c.getMinX()) {
						min.x = c.getMinX();
					}
					if (min.y > c.getMinY()) {
						min.y = c.getMinY();
					}
					if (min.z > c.getMinZ()) {
						min.z = c.getMinZ();
					}
				}
			}
		}
		return min;
	}

	Point3f getMax() {
		if (max == null)
			max = new Point3f();
		if (list.isEmpty()) {
			max.set(0, 0, 0);
			return max;
		}
		Cuboid c = getCuboid(0);
		max.x = c.getMaxX();
		max.y = c.getMaxY();
		max.z = c.getMaxZ();
		synchronized (list) {
			int n = count();
			if (n > 1) {
				for (int i = 1; i < n; i++) {
					c = getCuboid(i);
					if (max.x < c.getMaxX()) {
						max.x = c.getMaxX();
					}
					if (max.y < c.getMaxY()) {
						max.y = c.getMaxY();
					}
					if (max.z < c.getMaxZ()) {
						max.z = c.getMaxZ();
					}
				}
			}
		}
		return max;
	}

	void addCuboid(Cuboid c) {
		if (c == null)
			return;
		synchronized (list) {
			if (list.contains(c))
				return;
			list.add(c);
		}
	}

	void removeCuboid(Cuboid c) {
		if (c == null)
			return;
		list.remove(c);
	}

	/**
	 * Clients calling this method and count() to iterate through the elements must use the lock returned by getLock()
	 * to guard their iteration code.
	 */
	Cuboid getCuboid(int i) {
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