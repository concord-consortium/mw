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

import javax.vecmath.Point3f;

import org.myjmol.g3d.Graphics3D;

/**
 * @author Charles Xie
 * 
 */
class EllipsesRenderer extends ShapeRenderer {

	private final Point3f screenTop, screenBot;
	private final Point3f top, bot;
	private int a, b;

	EllipsesRenderer() {
		screenTop = new Point3f();
		screenBot = new Point3f();
		top = new Point3f();
		bot = new Point3f();
	}

	void render() {
		Ellipses ellipses = (Ellipses) shape;
		synchronized (ellipses.getLock()) {
			int n = ellipses.count();
			if (n <= 0)
				return;
			Ellipse ellipse;
			int screenZCenter;
			for (int i = 0; i < n; i++) {
				ellipse = ellipses.getEllipse(i);
				if (ellipse == null)
					continue;
				ellipse.setTop(top);
				ellipse.setBottom(bot);
				// Watch this: TransformManager.transformPoint(Point3f pointAngstroms, Point3f screen) is said to be
				// soly used by RocketsRenderer. It seems to work fine here.
				viewer.transformPoint(top, screenTop);
				viewer.transformPoint(bot, screenBot);
				screenZCenter = (int) ((screenTop.z + screenBot.z) * 0.5f);
				a = viewer.scaleToScreen(screenZCenter, (int) (ellipse.a * 1000));
				b = viewer.scaleToScreen(screenZCenter, (int) (ellipse.b * 1000));
				g3d.fillEllipticalCylinder(ellipse.colix, Graphics3D.ENDCAPS_FLAT, a, b, screenTop, screenBot);
			}
		}
	}

}