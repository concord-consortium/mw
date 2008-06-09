/**
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.myjmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

// XIE: rewritten class

class Axes extends FontLineShape {

	final static byte CENTERED = 0x00;
	final static byte OFFSET = 0x01;

	private final static Point3f[] unitAxisPoints = new Point3f[6];
	static {
		unitAxisPoints[0] = new Point3f(1, 0, 0);
		unitAxisPoints[1] = new Point3f(0, 1, 0);
		unitAxisPoints[2] = new Point3f(0, 0, 1);
		unitAxisPoints[3] = new Point3f(-1, 0, 0);
		unitAxisPoints[4] = new Point3f(0, -1, 0);
		unitAxisPoints[5] = new Point3f(0, 0, -1);
	}

	final Point3f originPoint = new Point3f();
	final Point3f[] axisPoints = new Point3f[6];
	final Point3f[] conePoints = new Point3f[3];

	byte style = CENTERED;

	Axes() {
		for (int i = 6; --i >= 0;)
			axisPoints[i] = new Point3f();
		for (int i = 3; --i >= 0;)
			conePoints[i] = new Point3f();
	}

	void initShape() {
		font3d = g3d.getFont3D(JmolConstants.AXES_DEFAULT_FONTSIZE);
		style = viewer.getAxisStyle();
		switch (style) {
		case OFFSET:
			for (int i = 0; i < 3; i++) {
				axisPoints[i].scale(12, unitAxisPoints[i]);
				conePoints[i].scale(10, unitAxisPoints[i]);
			}
			break;
		case CENTERED:
			initShape2();
			break;
		}
	}

	private void initShape2() {
		originPoint.set(viewer.getBoundBoxCenter());
		Vector3f corner = viewer.getBoundBoxCornerVector();
		for (int i = 6; --i >= 0;) {
			Point3f axisPoint = axisPoints[i];
			axisPoint.set(unitAxisPoints[i]);
			// we have just set the axisPoint to be a unit on a single axis therefore only one of these values
			// (x, y, or z) will be nonzero it will have value 1 or -1
			axisPoint.x *= corner.x;
			axisPoint.y *= corner.y;
			axisPoint.z *= corner.z;
			if (i < 3) {
				Point3f conePoint = conePoints[i];
				conePoint.x = axisPoint.x;
				conePoint.y = axisPoint.y;
				conePoint.z = axisPoint.z;
				if (conePoint.x != 0)
					conePoint.x += 2;
				if (conePoint.y != 0)
					conePoint.y += 2;
				if (conePoint.z != 0)
					conePoint.z += 2;
				conePoint.add(originPoint);
			}
			axisPoint.add(originPoint);
		}
	}

}