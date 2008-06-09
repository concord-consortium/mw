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

import javax.vecmath.Point3f;

/**
 * @author Charles Xie
 * 
 */
class Trajectory {

	Point3f[] points;

	private int index;
	private int length;

	Trajectory(int index) {
		this.index = index;
	}

	int getIndex() {
		return index;
	}

	void setPoints(int length, float[] x, float[] y, float[] z) {
		if (x == null || y == null || z == null)
			return;
		if (points == null || points.length < x.length) {
			points = new Point3f[x.length];
			for (int i = 0; i < points.length; i++) {
				points[i] = new Point3f();
			}
		}
		this.length = length;
		for (int i = 0; i < length; i++) {
			points[i].x = x[i];
			points[i].y = y[i];
			points[i].z = z[i];
		}
	}

	int getLength() {
		return length;
	}

}