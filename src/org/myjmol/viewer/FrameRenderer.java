/* $RCSfile: FrameRenderer.java,v $
 * $Author: qxie $
 * $Date: 2007-10-31 14:44:17 $
 * $Revision: 1.16 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.myjmol.viewer;


import java.awt.FontMetrics;
import java.awt.Rectangle;

import javax.vecmath.Point3i;

import org.myjmol.g3d.*;
import org.myjmol.util.Logger;

class FrameRenderer {

	boolean logTime;
	long timeBegin;

	Viewer viewer;

	ShapeRenderer[] renderers = new ShapeRenderer[JmolConstants.SHAPE_MAX];

	FrameRenderer(Viewer viewer) {
		this.viewer = viewer;
	}

	void render(Graphics3D g3d, Rectangle rectClip, Frame frame, int displayModelIndex) {

		if (frame == null || !viewer.mustRenderFlag())
			return;

		logTime = viewer.getTestFlag1();

		viewer.finalizeTransformParameters();

		if (logTime)
			timeBegin = System.currentTimeMillis();

		for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
			Shape shape = frame.shapes[i];

			if (shape == null)
				continue;
			getRenderer(i, g3d).render(g3d, rectClip, frame, displayModelIndex, shape);
		}
		if (logTime)
			Logger.info("render time: " + (System.currentTimeMillis() - timeBegin) + " ms");
	}

	ShapeRenderer getRenderer(int refShape, Graphics3D g3d) {
		if (renderers[refShape] == null)
			renderers[refShape] = allocateRenderer(refShape, g3d);
		return renderers[refShape];
	}

	void clear() {
		for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i)
			renderers[i] = null;
	}

	ShapeRenderer allocateRenderer(int refShape, Graphics3D g3d) {
		String classBase = JmolConstants.shapeClassBases[refShape] + "Renderer";
		String className = "org.jmol.viewer." + classBase;
		try {
			Class shapeClass = Class.forName(className);
			ShapeRenderer renderer = (ShapeRenderer) shapeClass.newInstance();
			renderer.setViewerFrameRenderer(viewer, this, g3d, refShape);
			return renderer;
		}
		catch (Exception e) {
			Logger.error("Could not instantiate renderer:" + classBase, e);
		}
		return null;
	}

	// XIE
	void renderStringOutside(String str, short colix, Font3D font3d, Point3i screen, Graphics3D g3d) {
		renderStringOutside(str, colix, font3d, screen.x, screen.y, screen.z, g3d);
	}

	// XIE
	void renderStringOutside(String str, short colix, Font3D font3d, int x, int y, int z, Graphics3D g3d) {
		FontMetrics fontMetrics = font3d.fontMetrics;
		int strAscent = fontMetrics.getAscent();
		int strWidth = fontMetrics.stringWidth(str);
		int xStrCenter, yStrCenter;
		int xCenter = viewer.getBoundBoxCenterX();
		int yCenter = viewer.getBoundBoxCenterY();
		int dx = x - xCenter;
		int dy = y - yCenter;
		if (dx == 0 && dy == 0) {
			xStrCenter = x;
			yStrCenter = y;
		}
		else {
			int dist = (int) Math.sqrt(dx * dx + dy * dy);
			if (dist == 0)
				dist = 1;
			xStrCenter = xCenter + ((dist + 2 + (strWidth + 1) / 2) * dx / dist);
			yStrCenter = yCenter + ((dist + 3 + (strAscent + 1) / 2) * dy / dist);
		}
		int xStrBaseline = xStrCenter - strWidth / 2;
		int yStrBaseline = yStrCenter + strAscent / 2;
		g3d.drawString(str, font3d, colix, xStrBaseline, yStrBaseline, z);
	}

}
