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

import javax.vecmath.Point3i;

/**
 * @author Charles Xie
 * 
 */
class TrianglesRenderer extends ShapeRenderer {

	private final Point3i screenP1, screenP2, screenP3;

	TrianglesRenderer() {
		screenP1 = new Point3i();
		screenP2 = new Point3i();
		screenP3 = new Point3i();
	}

	void render() {
		Triangles triangles = (Triangles) shape;
		synchronized (triangles.getLock()) {
			int n = triangles.count();
			if (n <= 0)
				return;
			Triangle triangle;
			for (int i = 0; i < n; i++) {
				triangle = triangles.getTriangle(i);
				if (triangle == null)
					continue;
				viewer.transformPoint(triangle.p1, screenP1);
				viewer.transformPoint(triangle.p2, screenP2);
				viewer.transformPoint(triangle.p3, screenP3);
				g3d.fillTriangle(triangle.colix, screenP1, screenP2, screenP3);
			}
		}
	}

}