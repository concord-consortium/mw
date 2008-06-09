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

import java.awt.Point;

import javax.vecmath.Point3i;

/**
 * @author Charles Xie
 * 
 */
class BondPin {

	private Bond bond;
	private Point handleScreenCenter;
	private int size;

	Point3i center;
	int width;
	float cost, sint;

	BondPin(Bond bond) {
		this.bond = bond;
		center = new Point3i();
		handleScreenCenter = new Point();
	}

	void update(int size) {
		this.size = size;
		center.x = (bond.atom1.screenX + bond.atom2.screenX) >> 1;
		center.y = (bond.atom1.screenY + bond.atom2.screenY) >> 1;
		center.z = (bond.atom1.screenZ + bond.atom2.screenZ) >> 1;
		width = (bond.atom1.screenDiameter + bond.atom2.screenDiameter) >> 3;
		if (width < 4)
			width = 4;
		else if (width > 10)
			width = 10;
		int dx = bond.atom2.screenX - bond.atom1.screenX;
		int dy = bond.atom2.screenY - bond.atom1.screenY;
		float invdis = 1.0f / (float) Math.sqrt(dx * dx + dy * dy);
		cost = dx * invdis;
		sint = dy * invdis;
		handleScreenCenter.x = (int) (center.x - sint * size * 2.5f);
		handleScreenCenter.y = (int) (center.y + cost * size * 2.5f);
	}

	Point handleCenter() {
		return handleScreenCenter;
	}

	/*
	 * first translate the (x, y) coordinate to the coordinate system with the origin at "center", then rotate by an
	 * angle of "-theta". Now we can easily check if it falls within a rectangle (without rotation).
	 */
	boolean withinHandle(int x, int y) {
		int dx = x - center.x;
		int dy = y - center.y;
		double x2 = dx * cost + dy * sint;
		double y2 = dy * cost - dx * sint;
		return x2 > -size && x2 < size && y2 < 4 * size && y2 > size;
	}

}