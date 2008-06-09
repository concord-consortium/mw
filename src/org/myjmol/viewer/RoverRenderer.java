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

import javax.vecmath.Point3i;

import org.myjmol.g3d.Graphics3D;

/**
 * @author Charles Xie
 * 
 */
class RoverRenderer extends ShapeRenderer {

	private Point3i screen1, screen2;
	private float angle;
	private int d = 45;
	private Rover rover;

	RoverRenderer(Rover rover) {
		this.rover = rover;
		screen1 = new Point3i();
		screen2 = new Point3i();
	}

	void render() {
		rover.transform(viewer);
		// draw left swing
		screen1.set(rover.screen.x - 45, rover.screen.y + 10, rover.screen.z); // base
		screen2.set(rover.screen.x - 40, rover.screen.y - 10, rover.screen.z + 200); // tip
		g3d.fillCone(rover.getColix(), Graphics3D.ENDCAPS_FLAT, d >> 2, screen1, screen2);
		g3d.fillCircleCentered(viewer.engineOn ? Graphics3D.YELLOW : Graphics3D.BLACK, d >> 3, rover.screen.x - 45,
				rover.screen.y + 10, rover.screen.z - 10);
		// draw right wing
		screen1.set(rover.screen.x + 45, rover.screen.y + 10, rover.screen.z); // base
		screen2.set(rover.screen.x + 40, rover.screen.y - 10, rover.screen.z + 200); // tip
		g3d.fillCone(rover.getColix(), Graphics3D.ENDCAPS_FLAT, d >> 2, screen1, screen2);
		g3d.fillCircleCentered(viewer.engineOn ? Graphics3D.YELLOW : Graphics3D.BLACK, d >> 3, rover.screen.x + 45,
				rover.screen.y + 10, rover.screen.z - 10);
		// draw body disk
		screen1.set(rover.screen.x, rover.screen.y + 5, rover.screen.z);
		screen2.set(rover.screen.x, rover.screen.y - 5, rover.screen.z - 2);
		g3d.fillCylinder(rover.getColix(), Graphics3D.ENDCAPS_FLAT, d, screen1, screen2);
		// draw upper disk
		screen1.set(rover.screen.x, rover.screen.y, rover.screen.z);
		screen2.set(rover.screen.x, rover.screen.y - 8, rover.screen.z - 2);
		g3d.fillCylinder(rover.getColix(), Graphics3D.ENDCAPS_FLAT, d >> 1, screen1, screen2);
		// draw antenna rod
		screen1.set(rover.screen.x, rover.screen.y - 8, rover.screen.z);
		screen2.set(rover.screen.x, rover.screen.y - 18, rover.screen.z - 2);
		g3d.fillCylinder(rover.getColix(), Graphics3D.ENDCAPS_FLAT, d >> 4, screen1, screen2);
		// draw ?
		screen1.set(rover.screen.x - 40, rover.screen.y, rover.screen.z);
		screen2.set(rover.screen.x + 40, rover.screen.y, rover.screen.z);
		g3d.fillCylinder(rover.getColix(), Graphics3D.ENDCAPS_FLAT, d / 6, screen1, screen2);
		// draw antenna
		angle += Math.PI * 0.02f;
		if (angle > 2 * Math.PI)
			angle -= 2 * Math.PI;
		int dx = (int) (8 * Math.cos(angle));
		int dy = (int) (8 * Math.sin(angle));
		screen1.set(rover.screen.x - dx, rover.screen.y - 18, rover.screen.z - dy);
		screen2.set(rover.screen.x + dx, rover.screen.y - 18, rover.screen.z + dy);
		g3d.fillCylinder(rover.getColix(), Graphics3D.ENDCAPS_FLAT, d >> 4, screen1, screen2);
	}

}