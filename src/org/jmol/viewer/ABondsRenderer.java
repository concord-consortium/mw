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

import java.util.Iterator;

import javax.vecmath.Point3i;

/**
 * @author Charles Xie
 * 
 */
class ABondsRenderer extends ShapeRenderer {

	private final Point3i screenP1, screenP2, screenP3;

	ABondsRenderer() {
		screenP1 = new Point3i();
		screenP2 = new Point3i();
		screenP3 = new Point3i();
	}

	void render() {
		if (!((ExtendedViewer) viewer).aBondRendered)
			return;
		ABonds abonds = (ABonds) shape;
		if (abonds.isEmpty())
			return;
		synchronized (abonds.getLock()) {
			ABond abond;
			int atomCount = viewer.getAtomCount();
			for (Iterator it = abonds.iterator(); it.hasNext();) {
				abond = (ABond) it.next();
				if (abond.atom1 >= atomCount || abond.atom2 >= atomCount || abond.atom3 >= atomCount)
					continue;
				viewer.transformPoint(viewer.getAtomPoint3f(abond.atom1), screenP1);
				viewer.transformPoint(viewer.getAtomPoint3f(abond.atom2), screenP2);
				viewer.transformPoint(viewer.getAtomPoint3f(abond.atom3), screenP3);
				g3d.fillTriangle(abond.colix, screenP1, screenP2, screenP3);
			}
		}
	}
}