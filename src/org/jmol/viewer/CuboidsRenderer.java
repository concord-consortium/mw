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

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

/**
 * @author Charles Xie
 * 
 */
class CuboidsRenderer extends ShapeRenderer {

	private final Point3i[] screens;
	private final Point3f[] data;

	CuboidsRenderer() {
		data = new Point3f[8];
		screens = new Point3i[8];
		for (int i = 8; --i >= 0;) {
			data[i] = new Point3f();
			screens[i] = new Point3i();
		}
	}

	void render() {
		Cuboids cuboids = (Cuboids) shape;
		synchronized (cuboids.getLock()) {
			int n = cuboids.count();
			if (n <= 0)
				return;
			Cuboid c;
			for (int i = 0; i < n; i++) {
				c = cuboids.getCuboid(i);
				if (c == null)
					continue;
				if (c instanceof SimulationBox) {
					if (((ExtendedViewer) viewer).simulationBoxVisible)
						render((SimulationBox) c);
				}
				else {
					render(c);
				}
			}
		}
	}

	private void transform(Cuboid c) {
		for (int k = 0; k < 8; k++) {
			data[k].x = Frame.unitBboxPoints[k].x * c.corner.x + c.center.x;
			data[k].y = Frame.unitBboxPoints[k].y * c.corner.y + c.center.y;
			data[k].z = Frame.unitBboxPoints[k].z * c.corner.z + c.center.z;
		}
		for (int j = 8; --j >= 0;) {
			viewer.transformPoint(data[j], screens[j]);
		}
	}

	private void render(SimulationBox box) {
		transform(box);
		short colix = box.getColix();
		for (int i = 0; i < 24; i += 2) {
			g3d.drawLine(colix, screens[Cuboid.EDGES[i]], screens[Cuboid.EDGES[i + 1]]);
		}
	}

	private void render(Cuboid c) {
		transform(c);
		short colix = c.getColix();
		switch (c.getMode()) {
		case Cuboid.LINE_MODE:
			for (int i = 0; i < 24; i += 2) {
				g3d.drawLine(colix, screens[Cuboid.EDGES[i]], screens[Cuboid.EDGES[i + 1]]);
			}
			break;
		case Cuboid.SOLID_MODE:
			g3d.fillQuadrilateral(colix, screens[0], screens[2], screens[6], screens[4]);
			g3d.fillQuadrilateral(colix, screens[1], screens[3], screens[7], screens[5]);
			g3d.fillQuadrilateral(colix, screens[0], screens[1], screens[5], screens[4]);
			g3d.fillQuadrilateral(colix, screens[2], screens[3], screens[7], screens[6]);
			g3d.fillQuadrilateral(colix, screens[0], screens[1], screens[3], screens[2]);
			g3d.fillQuadrilateral(colix, screens[4], screens[5], screens[7], screens[6]);
			break;
		}
	}

}