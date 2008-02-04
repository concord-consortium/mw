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

/**
 * @author Charles Xie
 * 
 */
class CylindersRenderer extends ShapeRenderer {

	private final Point3f screenTop, screenBot;
	private final Point3f top, bot;
	private int a, b;

	CylindersRenderer() {
		screenTop = new Point3f();
		screenBot = new Point3f();
		top = new Point3f();
		bot = new Point3f();
	}

	void render() {
		Cylinders cylinders = (Cylinders) shape;
		synchronized (cylinders.getLock()) {
			int n = cylinders.count();
			if (n <= 0)
				return;
			Cylinder cylinder;
			int screenZCenter;
			short mad;
			for (int i = 0; i < n; i++) {
				cylinder = cylinders.getCylinder(i);
				if (cylinder == null)
					continue;
				if (cylinder.axis == '0') {
					top.set(cylinder.getEnd1());
					bot.set(cylinder.getEnd2());
				}
				else {
					cylinder.setTop(top);
					cylinder.setBottom(bot);
				}
				// Watch this: TransformManager.transformPoint(Point3f pointAngstroms, Point3f screen) is said to be
				// soly used by RocketsRenderer. It seems to work fine here.
				viewer.transformPoint(top, screenTop);
				viewer.transformPoint(bot, screenBot);
				screenZCenter = (int) ((screenTop.z + screenBot.z) * 0.5f);
				mad = (short) (cylinder.a * 1000);
				if (mad < 100)
					mad = 100;
				a = viewer.scaleToScreen(screenZCenter, mad);
				mad = (short) (cylinder.b * 1000);
				if (mad < 100)
					mad = 100;
				b = viewer.scaleToScreen(screenZCenter, mad);
				g3d.fillEllipticalCylinder(cylinder.colix, cylinder.endcaps, a, b, screenTop, screenBot);
			}
		}
	}

}