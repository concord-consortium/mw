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

import org.myjmol.g3d.Graphics3D;

/**
 * Currently, we use a thin cylinder to model circle.
 * 
 * @author Charles Xie
 * 
 */
class Ellipse {

	private final static float thickness = 0.25f;

	char axis = 'z';
	Point3f center;
	float a = 2, b = 2;
	short colix = Graphics3D.GOLD;

	Ellipse() {
		center = new Point3f();
	}

	void setColix(short colix) {
		this.colix = colix;
	}

	void setTop(Point3f p) {
		switch (axis) {
		case 'x':
			p.set(center.x + thickness, center.y, center.z);
			break;
		case 'y':
			p.set(center.x, center.y + thickness, center.z);
			break;
		case 'z':
			p.set(center.x, center.y, center.z + thickness);
			break;
		}
	}

	void setBottom(Point3f p) {
		switch (axis) {
		case 'x':
			p.set(center.x - thickness, center.y, center.z);
			break;
		case 'y':
			p.set(center.x, center.y - thickness, center.z);
			break;
		case 'z':
			p.set(center.x, center.y, center.z - thickness);
			break;
		}
	}

	void setCenter(float x, float y, float z) {
		center.set(x, y, z);
	}

	void moveCenter(float dx, float dy, float dz) {
		center.x += dx;
		center.y += dy;
		center.z += dz;
	}

}