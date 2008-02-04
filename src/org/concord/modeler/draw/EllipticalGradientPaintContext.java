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

package org.concord.modeler.draw;

import java.awt.Color;
import java.awt.PaintContext;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

class EllipticalGradientPaintContext implements PaintContext {

	private Point2D point;
	private double a, b;
	private Color c1, c2;

	public EllipticalGradientPaintContext(Point2D p, double a, double b, double angle, Color c1, Color c2) {
		point = p;
		this.a = a;
		this.b = b;
		this.c1 = c1;
		this.c2 = c2;
	}

	public void dispose() {
	}

	public ColorModel getColorModel() {
		return ColorModel.getRGBdefault();
	}

	public Raster getRaster(int x, int y, int w, int h) {

		WritableRaster raster = getColorModel().createCompatibleWritableRaster(w, h);

		int[] data = new int[w * h * 4];
		double ratio = 0;
		int r12 = c2.getRed() - c1.getRed();
		int g12 = c2.getGreen() - c1.getGreen();
		int b12 = c2.getBlue() - c1.getBlue();
		int a12 = c2.getAlpha() - c1.getAlpha();
		int base;
		double dx = 0, dy = 0;
		double a2 = 1.0 / (a * a);
		double b2 = 1.0 / (b * b);
		for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {
				dx = x + i - point.getX();
				dy = y + j - point.getY();
				ratio = Math.sqrt(dx * dx * a2 + dy * dy * b2);
				if (ratio > 1.0)
					ratio = 1.0;
				base = (j * w + i) * 4;
				data[base] = (int) (c1.getRed() + ratio * r12);
				data[base + 1] = (int) (c1.getGreen() + ratio * g12);
				data[base + 2] = (int) (c1.getBlue() + ratio * b12);
				data[base + 3] = (int) (c1.getAlpha() + ratio * a12);
			}
		}
		raster.setPixels(0, 0, w, h, data);

		return raster;

	}

}
