/* 
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
package org.jmol.viewer;

import org.jmol.g3d.Graphics3D;

import javax.vecmath.Point3i;

// XIE: rewritten class

class AxesRenderer extends ShapeRenderer {

	private final static String[] axisLabels = { "x", "y", "z" };
	private final static short OFFSET_DISTANCE = 40;

	private final Point3i[] axisScreens = new Point3i[6];
	private final Point3i[] coneScreens = new Point3i[3];
	private final Point3i originScreen = new Point3i();
	private final short[] colors = new short[3];

	private int dx, dy;
	private float x12, y12, z12, r12;

	AxesRenderer() {
		for (int i = 6; --i >= 0;)
			axisScreens[i] = new Point3i();
		for (int i = 3; --i >= 0;)
			coneScreens[i] = new Point3i();
		colors[0] = Graphics3D.RED;
		colors[1] = Graphics3D.GREEN;
		colors[2] = Graphics3D.BLUE;
	}

	void render() {
		if (viewer.getShowAxes()) {
			Axes axes = (Axes) shape;
			if (viewer.getAxisStyle() != axes.style)
				axes.initShape();
			switch (viewer.getAxisStyle()) {
			case Axes.CENTERED:
				renderCenteredAxes();
				break;
			case Axes.OFFSET:
				renderOffsetAxes();
				break;
			}
		}
	}

	private void renderOffsetAxes() {
		Axes axes = (Axes) shape;
		boolean b = viewer.transformManager.getPerspectiveDepth();
		viewer.transformManager.setPerspectiveDepth2(false);
		viewer.transformPoint(axes.originPoint, originScreen);
		dx = originScreen.x - (viewer.dimScreen.width - OFFSET_DISTANCE);
		dy = originScreen.y - (viewer.dimScreen.height - OFFSET_DISTANCE);
		originScreen.x -= dx;
		originScreen.y -= dy;
		int originScreenZ;
		for (int i = 3; --i >= 0;) {
			viewer.transformPoint(axes.conePoints[i], coneScreens[i]);
			coneScreens[i].x -= dx;
			coneScreens[i].y -= dy;
			x12 = coneScreens[i].x - originScreen.x;
			y12 = coneScreens[i].y - originScreen.y;
			z12 = coneScreens[i].z - originScreen.z;
			r12 = (float) Math.sqrt(x12 * x12 + y12 * y12 + z12 * z12);
			if (r12 < 0.0001f)
				continue;
			r12 = 0.75f * OFFSET_DISTANCE / r12;
			originScreenZ = originScreen.z;
			coneScreens[i].x = (int) (originScreen.x + x12 * r12);
			coneScreens[i].y = (int) (originScreen.y + y12 * r12);
			coneScreens[i].z = originScreen.z = 1;
			g3d.fillCylinder(colors[i], Graphics3D.ENDCAPS_SPHERICAL, 4, originScreen, coneScreens[i]);
			originScreen.z = originScreenZ;
			viewer.transformPoint(axes.axisPoints[i], axisScreens[i]);
			axisScreens[i].x -= dx;
			axisScreens[i].y -= dy;
			x12 = axisScreens[i].x - originScreen.x;
			y12 = axisScreens[i].y - originScreen.y;
			z12 = axisScreens[i].z - originScreen.z;
			r12 = (float) Math.sqrt(x12 * x12 + y12 * y12 + z12 * z12);
			r12 = OFFSET_DISTANCE / r12;
			axisScreens[i].x = (int) (originScreen.x + x12 * r12);
			axisScreens[i].y = (int) (originScreen.y + y12 * r12);
			axisScreens[i].z = 1;
			g3d.fillCone(colors[i], Graphics3D.ENDCAPS_SPHERICAL, 10, coneScreens[i], axisScreens[i]);
			frameRenderer.renderStringOutside(axisLabels[i], colors[i], axes.font3d, axisScreens[i], g3d);
		}
		viewer.transformManager.setPerspectiveDepth2(b);
	}

	private void renderCenteredAxes() {
		Axes axes = (Axes) shape;
		short mad = axes.mad;
		if (viewer instanceof ExtendedViewer) {
			ExtendedViewer viewer2 = (ExtendedViewer) viewer;
			if (viewer2.getAxisDiameter() > 0)
				mad = (short) viewer2.getAxisDiameter();
		}
		if (mad == 0)
			return;
		viewer.transformPoint(axes.originPoint, originScreen);
		for (int i = 6; --i >= 0;)
			viewer.transformPoint(axes.axisPoints[i], axisScreens[i]);
		for (int i = 3; --i >= 0;)
			viewer.transformPoint(axes.conePoints[i], coneScreens[i]);
		int widthPixels = mad;
		if (mad >= 20)
			widthPixels = viewer.scaleToScreen(originScreen.z, mad);
		for (int i = 6; --i >= 0;) {
			if (axisScreens[i].z <= 1 || originScreen.z <= 1) {
				g3d.drawDottedLine(colors[i % 3], originScreen, axisScreens[i]);
			}
			else {
				if (mad < 0) {
					g3d.drawDottedLine(colors[i % 3], originScreen, axisScreens[i]);
				}
				else {
					g3d.fillCylinder(colors[i % 3], Graphics3D.ENDCAPS_FLAT, widthPixels, originScreen, axisScreens[i]);
				}
				if (i < 3) {
					g3d.fillCone(colors[i], Graphics3D.ENDCAPS_SPHERICAL, 5 + widthPixels, axisScreens[i],
							coneScreens[i]);
					frameRenderer.renderStringOutside(axisLabels[i], colors[i], axes.font3d, coneScreens[i], g3d);
				}
			}
		}
	}

}