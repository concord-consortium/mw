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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;

class MagnifyGlassOp implements BufferedImageOp {

	private BasicStroke stroke = new BasicStroke(4);

	public final static byte GLASS_AS_CIRCLE = 0;
	public final static byte GLASS_AS_RECTANGLE = 1;

	private int drawMode = GLASS_AS_CIRCLE;

	float mx;
	float my;
	private float rw = 0.5f;
	float rh = 0.5f;

	private float red = 0;
	private float green = 1;
	private float blue = 0;
	private float red2 = 0;
	private float green2 = 0.7f;
	private float blue2 = 0;

	private Shape needClip;
	private BufferedImage internalImage;
	private boolean drawImage;

	MagnifyGlassOp(float power, float mx, float my, float rw, float rh, int drawMode) {
		this.mx = mx;
		this.my = my;
		this.rw = rw;
		this.rh = rh;
		this.drawMode = (drawMode < GLASS_AS_CIRCLE || GLASS_AS_CIRCLE > GLASS_AS_RECTANGLE) ? GLASS_AS_RECTANGLE
				: drawMode;
		setColorComponents(red, green, blue);

	}

	public synchronized BufferedImage filter(BufferedImage src, BufferedImage dest) {
		if (dest == null)
			dest = createCompatibleDestImage(src, null);
		float xc = mx;
		float yc = my;
		float r0 = rw - 2;
		float rrh = rh;
		Graphics2D g2d = dest.createGraphics();
		g2d.drawImage(src, null, 0, 0);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setStroke(stroke);
		g2d.setPaint(new Color(red, green, blue, 0.2f));
		Shape oldClip = g2d.getClip();
		g2d.setClip(needClip);
		if (drawImage && internalImage != null && (drawMode == GLASS_AS_RECTANGLE)) {
			Composite oldComp = g2d.getComposite();
			Color oldColor = g2d.getColor();
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
			g2d.drawImage(internalImage, null, Math.round(xc - r0), Math.round(yc - rrh - 1));
			g2d.setComposite(oldComp);
			g2d.setColor(oldColor);
		}
		else {
			if (drawMode == GLASS_AS_RECTANGLE) {
				g2d.fillRoundRect(Math.round(xc - r0), Math.round(yc - rrh - 1), Math.round(2 * r0), Math
						.round(2 * rrh), 2, 2);
			}
			else {
				g2d.fillOval(Math.round(xc - r0), Math.round(yc - r0), 2 * Math.round(r0), 2 * Math.round(r0));
			}
			g2d.setPaint(new Color(red2, green2, blue2, 0.5f));
			if (drawMode == GLASS_AS_RECTANGLE) {
				g2d.drawRoundRect(Math.round(xc - r0), Math.round(yc - rrh - 1), Math.round(2 * r0), Math
						.round(2 * rrh), 2, 2);
			}
			else {
				g2d.drawOval(Math.round(xc - r0), Math.round(yc - r0), 2 * Math.round(r0), 2 * Math.round(r0));
			}
		}
		g2d.setClip(oldClip);
		g2d.drawImage(dest, 0, 0, null);
		g2d.dispose();
		return dest;
	}

	public void setColorComponents() {
		green = blue = 0;
		red = 0.5f;
		green2 = blue2 = 0;
		red2 = 0.35f;
	}

	public void setColorComponents(float red, float green, float blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		red2 = red * 0.7f;
		green2 = green * 0.7f;
		blue2 = blue * 0.7f;
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

	static float d2(float x1, float y1, float x2, float y2) {
		return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
	}

	public void setX(float x) {
		mx = x;
	}

	public void setY(float y) {
		my = y;
	}

	public void setR(float r) {
		rw = r;
	}

	public float getX() {
		return mx;
	}

	public float getR() {
		return rw;
	}

	public float getH() {
		return rh;
	}

	public void setImage(BufferedImage img) {
		if (img == null) {
			internalImage = null;
			return;
		}
		BufferedImageOp bop = new AlphaOp();
		internalImage = DNAScroller.createImageFromImage(img, bop);
	}

	public void setDrawImage(boolean drawImage) {
		this.drawImage = drawImage;
	}

	public void setNeedClip(Shape needClip) {
		this.needClip = needClip;
	}

}