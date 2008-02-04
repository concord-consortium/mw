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

import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;

/**
 * @author Charles Xie
 * 
 */
class PlanesRenderer extends ShapeRenderer {

	final Point3i[] screens;

	private static Point3i tempPoint1, tempPoint2;
	private static Point3f temp1, temp2, temp3, temp4;
	private static Font3D font3d;

	PlanesRenderer() {
		screens = new Point3i[4];
		for (int i = 4; --i >= 0;) {
			screens[i] = new Point3i();
		}
	}

	void render() {
		Planes planes = (Planes) shape;
		synchronized (planes.getLock()) {
			int n = planes.count();
			if (n <= 0)
				return;
			Plane p;
			for (int i = 0; i < n; i++) {
				p = planes.getPlane(i);
				if (p != null)
					render(p);
			}
		}
	}

	void render(Plane p) {

		for (int i = 4; --i >= 0;) {
			viewer.transformPoint(p.getVertex(i), screens[i]);
		}

		switch (p.getMode()) {
		case Plane.LINE_MODE:
			g3d.drawLine(p.getColix(), screens[0], screens[1]);
			g3d.drawLine(p.getColix(), screens[1], screens[2]);
			g3d.drawLine(p.getColix(), screens[2], screens[3]);
			g3d.drawLine(p.getColix(), screens[3], screens[0]);
			break;
		case Plane.FILL_MODE:
			g3d.fillQuadrilateral(p.getColix(), screens[0], screens[1], screens[2], screens[3]);
			break;
		case Plane.FILL_AND_FACE_LINE_MODE:
			fillAndDrawFaceLines(p);
			break;
		case Plane.FILL_WITH_DOTS_MODE:
			g3d.fillQuadrilateral(p.getColix(), screens[0], screens[1], screens[2], screens[3]);
			initTempPoints();
			drawDotArray((short) 5, screens[1], screens[0], screens[2], screens[3]);
			drawDotArray((short) 5, screens[1], screens[2], screens[0], screens[3]);
			if (font3d == null)
				font3d = g3d.getFont3D(JmolConstants.LABEL_DEFAULT_FONTSIZE);
			screens[0].add(screens[1]);
			screens[0].add(screens[2]);
			screens[0].add(screens[3]);
			screens[0].x = screens[0].x >> 2;
			screens[0].y = screens[0].y >> 2;
			screens[0].z = screens[0].z >> 2;
			frameRenderer.renderStringOutside("" + p.getCenter(), Graphics3D.YELLOW, font3d, screens[0], g3d);
			break;
		case Plane.GRID_MODE:
			fillAndDrawFaceLines(p);
			drawLineArray(p.m, screens[1], screens[0], screens[2], screens[3]);
			drawLineArray(p.n, screens[1], screens[2], screens[0], screens[3]);
			break;
		}
	}

	private void fillAndDrawFaceLines(Plane p) {
		g3d.fillQuadrilateral(p.getColix(), screens[0], screens[1], screens[2], screens[3]);
		initTempPoints();
		tempPoint1.x = (screens[0].x + screens[1].x) >> 1;
		tempPoint1.y = (screens[0].y + screens[1].y) >> 1;
		tempPoint1.z = (screens[0].z + screens[1].z) >> 1;
		tempPoint2.x = (screens[2].x + screens[3].x) >> 1;
		tempPoint2.y = (screens[2].y + screens[3].y) >> 1;
		tempPoint2.z = (screens[2].z + screens[3].z) >> 1;
		// g3d.drawLine(Graphics3D.RED, tempPoint1, tempPoint2);
		short r = viewer.scaleToScreen((tempPoint1.z + tempPoint2.z) >> 1, 400);
		short colix = Graphics3D.GOLD;
		switch (p.axis) {
		case 'x':
			colix = Graphics3D.BLUE;
			break;
		case 'y':
			colix = Graphics3D.BLUE;
			break;
		case 'z':
			colix = Graphics3D.GREEN;
			break;
		}
		g3d.fillCylinder(colix, Graphics3D.ENDCAPS_NONE, r, tempPoint1, tempPoint2);
		tempPoint1.x = (screens[1].x + screens[2].x) >> 1;
		tempPoint1.y = (screens[1].y + screens[2].y) >> 1;
		tempPoint1.z = (screens[1].z + screens[2].z) >> 1;
		tempPoint2.x = (screens[0].x + screens[3].x) >> 1;
		tempPoint2.y = (screens[0].y + screens[3].y) >> 1;
		tempPoint2.z = (screens[0].z + screens[3].z) >> 1;
		// g3d.drawLine(Graphics3D.RED, tempPoint1, tempPoint2);
		switch (p.axis) {
		case 'x':
			colix = Graphics3D.GREEN;
			break;
		case 'y':
			colix = Graphics3D.RED;
			break;
		case 'z':
			colix = Graphics3D.RED;
			break;
		}
		g3d.fillCylinder(colix, Graphics3D.ENDCAPS_NONE, r, tempPoint1, tempPoint2);
	}

	private void drawLineArray(short m, Point3i p1, Point3i p2, Point3i p3, Point3i p4) {
		initTemps();
		temp1.x = p1.x - p2.x;
		temp1.y = p1.y - p2.y;
		temp1.z = p1.z - p2.z;
		temp1.x /= m;
		temp1.y /= m;
		temp1.z /= m;
		temp2.x = p3.x - p4.x;
		temp2.y = p3.y - p4.y;
		temp2.z = p3.z - p4.z;
		temp2.x /= m;
		temp2.y /= m;
		temp2.z /= m;
		temp3.set(p2.x, p2.y, p2.z);
		temp4.set(p4.x, p4.y, p4.z);
		for (int i = 0; i < m - 1; i++) {
			temp3.add(temp1);
			temp4.add(temp2);
			g3d.drawLine(viewer.colorManager.colixBackgroundContrast, (int) temp3.x, (int) temp3.y, (int) temp3.z,
					(int) temp4.x, (int) temp4.y, (int) temp4.z);
		}
	}

	private void drawDotArray(short m, Point3i p1, Point3i p2, Point3i p3, Point3i p4) {
		initTemps();
		temp1.x = p1.x - p2.x;
		temp1.y = p1.y - p2.y;
		temp1.z = p1.z - p2.z;
		temp1.x /= m;
		temp1.y /= m;
		temp1.z /= m;
		temp2.x = p3.x - p4.x;
		temp2.y = p3.y - p4.y;
		temp2.z = p3.z - p4.z;
		temp2.x /= m;
		temp2.y /= m;
		temp2.z /= m;
		temp3.set(p2.x, p2.y, p2.z);
		temp4.set(p4.x, p4.y, p4.z);
		for (int i = 0; i < m - 1; i++) {
			temp3.add(temp1);
			temp4.add(temp2);
			g3d.drawDashedLine(Graphics3D.WHITE, 10, 1, (int) temp3.x, (int) temp3.y, (int) temp3.z, (int) temp4.x,
					(int) temp4.y, (int) temp4.z);
		}
	}

	private static void initTempPoints() {
		if (tempPoint1 == null) {
			tempPoint1 = new Point3i();
			tempPoint2 = new Point3i();
		}
	}

	private static void initTemps() {
		if (temp1 == null) {
			temp1 = new Point3f();
			temp2 = new Point3f();
			temp3 = new Point3f();
			temp4 = new Point3f();
		}
	}

}