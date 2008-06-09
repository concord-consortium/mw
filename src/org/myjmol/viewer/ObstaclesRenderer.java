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

import java.util.Arrays;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.myjmol.g3d.Graphics3D;

/**
 * @author Charles Xie
 * 
 */
class ObstaclesRenderer extends ShapeRenderer {

	private final Point3i[] screens;
	private final Point3f[] data;
	private final Point3f screenTop, screenBot;
	private final Point3f top, bot;
	private final int[] z1 = new int[6];
	private final int[] z2 = new int[6];
	private final int[] zin = new int[3];
	private int a, b;

	ObstaclesRenderer() {

		data = new Point3f[8];
		screens = new Point3i[8];
		for (int i = 8; --i >= 0;) {
			data[i] = new Point3f();
			screens[i] = new Point3i();
		}

		screenTop = new Point3f();
		screenBot = new Point3f();
		top = new Point3f();
		bot = new Point3f();

	}

	void render() {
		Obstacles obstacles = (Obstacles) shape;
		synchronized (obstacles.getLock()) {
			int n = obstacles.count();
			if (n <= 0)
				return;
			Object3D obs;
			int screenZCenter;
			for (int i = 0; i < n; i++) {
				obs = obstacles.getObstacle(i);
				if (obs == null)
					continue;
				if (obs instanceof Cylinder) {
					Cylinder cylinder = (Cylinder) obs;
					cylinder.setTop(top);
					cylinder.setBottom(bot);
					// Watch this: TransformManager.transformPoint(Point3f pointAngstroms, Point3f screen) is said to be
					// soly used by RocketsRenderer. It seems to work fine here.
					viewer.transformPoint(top, screenTop);
					viewer.transformPoint(bot, screenBot);
					screenZCenter = (int) ((screenTop.z + screenBot.z) * 0.5f);
					a = viewer.scaleToScreen(screenZCenter, (int) (cylinder.a * 1000));
					b = viewer.scaleToScreen(screenZCenter, (int) (cylinder.b * 1000));
					g3d.fillEllipticalCylinder(cylinder.colix, Graphics3D.ENDCAPS_FLAT, a, b, screenTop, screenBot);
				}
				else if (obs instanceof Cuboid) {
					Cuboid cuboid = (Cuboid) obs;
					for (int k = 0; k < 8; k++) {
						data[k].x = Frame.unitBboxPoints[k].x * cuboid.corner.x + cuboid.center.x;
						data[k].y = Frame.unitBboxPoints[k].y * cuboid.corner.y + cuboid.center.y;
						data[k].z = Frame.unitBboxPoints[k].z * cuboid.corner.z + cuboid.center.z;
						viewer.transformPoint(data[k], screens[k]);
					}
					render(cuboid);
				}
			}
		}
	}

	private void render(Cuboid c) {
		// an awkward back-face culling
		z1[0] = z2[0] = sumScreenZ(0, 2, 6, 4);
		z1[1] = z2[1] = sumScreenZ(1, 3, 7, 5);
		z1[2] = z2[2] = sumScreenZ(0, 1, 5, 4);
		z1[3] = z2[3] = sumScreenZ(2, 3, 7, 6);
		z1[4] = z2[4] = sumScreenZ(0, 1, 3, 2);
		z1[5] = z2[5] = sumScreenZ(4, 5, 7, 6);
		Arrays.sort(z2);
		for (int m = 0; m < 3; m++) {
			for (int n = 0; n < 6; n++) {
				if (z1[n] == z2[m]) {
					zin[m] = n;
					break;
				}
			}
		}
		short colix = c.getColix();
		for (int m = 0; m < 3; m++) {
			switch (zin[m]) {
			case 0:
				g3d.fillQuadrilateral(colix, screens[0], screens[2], screens[6], screens[4]);
				break;
			case 1:
				g3d.fillQuadrilateral(colix, screens[1], screens[3], screens[7], screens[5]);
				break;
			case 2:
				g3d.fillQuadrilateral(colix, screens[0], screens[1], screens[5], screens[4]);
				break;
			case 3:
				g3d.fillQuadrilateral(colix, screens[2], screens[3], screens[7], screens[6]);
				break;
			case 4:
				g3d.fillQuadrilateral(colix, screens[0], screens[1], screens[3], screens[2]);
				break;
			case 5:
				g3d.fillQuadrilateral(colix, screens[4], screens[5], screens[7], screens[6]);
				break;
			}
		}
	}

	private int sumScreenZ(int i, int j, int k, int l) {
		return screens[i].z + screens[j].z + screens[k].z + screens[l].z;
	}

}