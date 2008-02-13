/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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
package org.concord.molbio.ui;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;

class AlphaOp implements BufferedImageOp {

	AlphaOp() {
	}

	public synchronized BufferedImage filter(BufferedImage src, BufferedImage dest) {
		if (dest == null)
			dest = createCompatibleDestImage(src, null);
		int w = src.getWidth();
		int h = src.getHeight();
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				int px = src.getRGB(x, y);
				int dpx = px & 0xFFFFFF;
				int r = (px & 0xFF0000) >> 16;
				int g = (px & 0xFF00) >> 8;
				int b = (px & 0xFF);
				if (r > 0xF0 && g > 0xF0 && b > 0xF0) {
					px = dpx;
				}
				dest.setRGB(x, y, px);
			}
		}
		return dest;
	}

	public Rectangle2D getBounds2D(BufferedImage src) {
		return src.getRaster().getBounds();
	}

	public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
		if (destCM == null) {
			destCM = src.getColorModel();
			if (destCM instanceof IndexColorModel) {
				destCM = ColorModel.getRGBdefault();
			}
		}
		int w = src.getWidth();
		int h = src.getHeight();
		return new BufferedImage(destCM, destCM.createCompatibleWritableRaster(w, h), destCM.isAlphaPremultiplied(),
				null);
	}

	public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
		if (dstPt == null)
			dstPt = new Point2D.Float();
		dstPt.setLocation(srcPt);
		return dstPt;
	}

	public RenderingHints getRenderingHints() {
		return null;
	}

}