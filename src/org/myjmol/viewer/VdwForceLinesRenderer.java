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

import org.myjmol.api.Pair;

/**
 * @author Charles Xie
 * 
 */
class VdwForceLinesRenderer extends ShapeRenderer {

	VdwForceLinesRenderer() {
	}

	void render() {
		VdwForceLines lines = (VdwForceLines) shape;
		Pair[] pairs = lines.pairs;
		if (pairs == null)
			return;
		int n = pairs.length;
		if (n <= 0)
			return;
		Atom[] atoms = frame.atoms;
		Atom a1 = null;
		Atom a2 = null;
		short colix1, colix2;
		int x, y, z;
		int i1, i2;
		int atomCount = viewer.getAtomCount();
		for (int i = 0; i < n; i++) {
			i1 = pairs[i].getIndex1();
			if (i1 == -1 || i1 >= atomCount)
				break;
			i2 = pairs[i].getIndex2();
			if (i2 == -1 || i2 >= atomCount)
				break;
			a1 = atoms[i1];
			a2 = atoms[i2];
			if (a1.screenZ > 1 && a2.screenZ > 1) {
				colix1 = viewer.modelManager.getAtomColix(a1.atomIndex);
				colix2 = viewer.modelManager.getAtomColix(a2.atomIndex);
				x = (a1.screenX + a2.screenX) >> 1;
				y = (a1.screenY + a2.screenY) >> 1;
				z = (a1.screenZ + a2.screenZ) >> 1;
				g3d.drawDottedLine(colix1, a1.screenX, a1.screenY, a1.screenZ, x, y, z);
				g3d.drawDottedLine(colix2, a2.screenX, a2.screenY, a2.screenZ, x, y, z);
			}
		}
	}

}