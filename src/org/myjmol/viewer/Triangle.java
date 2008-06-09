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

/*
 * @author Charles Xie
 * 
 */
class Triangle {

	Point3f p1;
	Point3f p2;
	Point3f p3;
	short colix = Graphics3D.GOLD;

	Triangle() {
		p1 = new Point3f();
		p2 = new Point3f();
		p3 = new Point3f();
	}

	void setVertices(Point3f v1, Point3f v2, Point3f v3) {
		p1.set(v1);
		p2.set(v2);
		p3.set(v3);
	}

	void setColix(short colix) {
		this.colix = colix;
	}

}