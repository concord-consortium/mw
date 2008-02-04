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

import java.awt.Point;

/**
 * @author Charles Xie
 * 
 */
class AtomPin {

	private Atom atom;

	int width, diameter, r;

	AtomPin(Atom atom) {
		this.atom = atom;
	}

	void setSize() {
		width = atom.screenDiameter >> 2;
		if (width < 4)
			width = 4;
		else if (width > 10)
			width = 10;
		diameter = atom.screenDiameter + 2 * width;
		r = diameter / 3;
	}

	int handleYmin() {
		return atom.screenY - 4 * r;
	}

	int handleYmax() {
		return atom.screenY - (diameter >> 1);
	}

	Point handleCenter() {
		return new Point(atom.screenX, (handleYmin() + handleYmax()) >> 1);
	}

	boolean withinHandle(int x, int y) {
		return x < atom.screenX + (atom.screenDiameter >> 1) && x > atom.screenX - (atom.screenDiameter >> 1)
				&& y < handleYmax() && y > handleYmin();
	}

}
