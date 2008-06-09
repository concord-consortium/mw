/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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
import javax.vecmath.Point3i;

import org.myjmol.g3d.Graphics3D;

/**
 * @author Charles Xie
 * 
 */
class Rover extends Shape {

	private Point3f position;
	private short colix = Graphics3D.getColix(0xffcccccc);
	Point3i screen;
	float scale = 1;

	Rover() {
		position = new Point3f();
	}

	void setPosition(float x, float y, float z) {
		position.set(x, y, z);
	}

	void transform(Viewer viewer) {
		screen = viewer.transformPoint(position);
	}

	void setColix(int argb) {
		colix = Graphics3D.getColix(argb);
	}

	short getColix() {
		return colix;
	}

}
