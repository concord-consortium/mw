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
 * @ThreadSafe
 * @author Charles Xie
 * 
 */
class Obstacles extends Shape {

	private List<Object3D> list;
	private Point3f min, max;

	Obstacles() {
		list = Collections.synchronizedList(new ArrayList<Object3D>());
	}

	Object getLock() {
		return list;
	}

	void addObstacle(Object3D o) {
		if (o == null)
			return;
		synchronized (list) {
			if (list.contains(o))
				return;
			list.add(o);
		}
	}

	void removeObstacle(Object3D o) {
		if (o == null)
			return;
		list.remove(o);
	}

	/**
	 * Clients calling this method and count() to iterate through the elements must use the lock returned by getLock()
	 * to guard their iteration code.
	 */
	Object3D getObstacle(int i) {
		if (i < 0)
			return null;
		synchronized (list) {
			if (i >= list.size())
				return null;
			return list.get(i);
		}
	}

	Point3f getMin() {
		if (min == null)
			min = new Point3f();
		if (list.isEmpty()) {
			min.set(0, 0, 0);
			return min;
		}
		Object3D c = list.get(0);
		if (c != null) {
			min.x = c.getMinX();
			min.y = c.getMinY();
			min.z = c.getMinZ();
		}
		synchronized (list) {
			int n = count();
			if (n > 1) {
				for (int i = 1; i < n; i++) {
					c = list.get(i);
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
		Object3D c = list.get(0);
		max.x = c.getMaxX();
		max.y = c.getMaxY();
		max.z = c.getMaxZ();
		synchronized (list) {
			int n = count();
			if (n > 1) {
				for (int i = 1; i < n; i++) {
					c = list.get(i);
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

	void clear() {
		list.clear();
	}

	boolean isEmpty() {
		return list.isEmpty();
	}

	int count() {
		return list.size();
	}

	float getRotationRadius(Point3f p) {
		float max = 0;
		float rad;
		synchronized (list) {
			for (Object3D o : list) {
				rad = o.getRotationRadius(p);
				if (max < rad)
					max = rad;
			}
		}
		return max;
	}

}