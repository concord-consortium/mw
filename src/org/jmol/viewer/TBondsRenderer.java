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

import javax.vecmath.Point3f;

import org.jmol.g3d.Graphics3D;

/**
 * @author Charles Xie
 * 
 */
class TBondsRenderer extends ShapeRenderer {

	private final Point3f screenP1, screenP2, screenP3, screenP4;

	TBondsRenderer() {
		screenP1 = new Point3f();
		screenP2 = new Point3f();
		screenP3 = new Point3f();
		screenP4 = new Point3f();
	}

	void render() {
		boolean toRender = ((ExtendedViewer) viewer).tBondRendered;
		TBonds tbonds = (TBonds) shape;
		int n = 0;
		synchronized (tbonds.getLock()) {
			n = tbonds.count();
		}
		if (n <= 0)
			return;
		TBond tbond;
		float r = 0;
		int mad = (viewer.getMadBond() * 3) >> 2;
		if (mad < 100)
			mad = 100;
		int screenZCenter;
		int atomCount = viewer.getAtomCount();
		synchronized (tbonds.getLock()) {
			for (Iterator it = tbonds.iterator(); it.hasNext();) {
				tbond = (TBond) it.next();
				if (tbond.highlight) {
					mad = viewer.getMadBond();
					if (mad < 100)
						mad = 100;
				}
				else {
					if (!toRender)
						continue;
				}
				if (tbond.atom1 >= atomCount || tbond.atom2 >= atomCount || tbond.atom3 >= atomCount
						|| tbond.atom4 >= atomCount)
					continue;
				viewer.transformPoint(viewer.getAtomPoint3f(tbond.atom1), screenP1);
				viewer.transformPoint(viewer.getAtomPoint3f(tbond.atom2), screenP2);
				viewer.transformPoint(viewer.getAtomPoint3f(tbond.atom3), screenP3);
				viewer.transformPoint(viewer.getAtomPoint3f(tbond.atom4), screenP4);
				screenZCenter = (int) ((screenP1.z + screenP2.z) * 0.5f);
				r = viewer.scaleToScreen(screenZCenter, mad);
				g3d.fillEllipticalCylinder(tbond.colix, Graphics3D.ENDCAPS_FLAT, (int) r, (int) r, screenP1, screenP2);
				screenZCenter = (int) ((screenP2.z + screenP3.z) * 0.5f);
				r = viewer.scaleToScreen(screenZCenter, mad);
				g3d.fillEllipticalCylinder(tbond.colix, Graphics3D.ENDCAPS_FLAT, (int) r, (int) r, screenP2, screenP3);
				screenZCenter = (int) ((screenP3.z + screenP4.z) * 0.5f);
				r = viewer.scaleToScreen(screenZCenter, mad);
				g3d.fillEllipticalCylinder(tbond.colix, Graphics3D.ENDCAPS_FLAT, (int) r, (int) r, screenP3, screenP4);
			}
		}
	}

}