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
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

/**
 * This implements an elliptical gradient paint. Source from Java 2D Graphics By Jonathan Knudsen, which is similar to
 * the source of <code>java.awt.GradientPaint</code>. Slightly modified to make it more efficient.
 * 
 * @author Charles Xie
 */

public class EllipticalGradientPaint implements Paint {

	private Point2D center;
	private double a, b, angle;
	private Color centerColor, edgeColor;

	public EllipticalGradientPaint(double x, double y, double a, double b, double angle, Color centerColor,
			Color edgeColor) {
		if (a <= 0 || b <= 0)
			throw new IllegalArgumentException("a and b must be greater than 0.");
		this.center = new Point2D.Double(x, y);
		this.a = a;
		this.b = b;
		this.angle = angle;
		this.centerColor = centerColor;
		this.edgeColor = edgeColor;
	}

	public void setCenterColor(Color c) {
		centerColor = c;
	}

	public void setEdgeColor(Color c) {
		edgeColor = c;
	}

	public void setCenter(double x, double y) {
		center.setLocation(x, y);
	}

	public void setA(double a) {
		this.a = a;
	}

	public void setB(double b) {
		this.b = b;
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}

	public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds,
			AffineTransform xform, RenderingHints hints) {
		return new EllipticalGradientPaintContext(xform.transform(center, null), a, b, angle, centerColor, edgeColor);
	}

	public int getTransparency() {
		return (((centerColor.getAlpha() & edgeColor.getAlpha()) == 0xff) ? OPAQUE : TRANSLUCENT);
	}

}