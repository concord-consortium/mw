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

import javax.vecmath.Point3i;

/**
 * @author Charles Xie
 * 
 */
class TrajectoriesRenderer extends ShapeRenderer {

	private Point3i[] screens;

	TrajectoriesRenderer() {
	}

	private void initScreenPoints(int n) {
		if (screens == null || screens.length != n) {
			screens = new Point3i[n];
			for (int i = n; --i >= 0;) {
				screens[i] = new Point3i();
			}
		}
	}

	void render() {
		Trajectories trajectories = (Trajectories) shape;
		synchronized (trajectories.getLock()) {
			int n = trajectories.count();
			if (n <= 0)
				return;
			Trajectory t;
			for (int i = 0; i < n; i++) {
				t = trajectories.getTrajectory(i);
				if (t == null)
					continue;
				render(t);
			}
		}
	}

	void render(Trajectory t) {

		if (t.points == null)
			return;

		if (t.getIndex() >= viewer.getAtomCount())
			return; // in case the atom is removed but the rendering call will continue

		int n = t.points.length;
		initScreenPoints(n);

		for (int i = 0; i < t.getLength(); i++) {
			viewer.transformPoint(t.points[i], screens[i]);
		}

		short colix = viewer.modelManager.getAtomColix(t.getIndex());
		for (int i = 0; i < t.getLength() - 1; i++) {
			if (screens[i].z > 1 && screens[i + 1].z > 1)
				g3d.drawDottedLine(colix, screens[i], screens[i + 1]);
		}

	}

}