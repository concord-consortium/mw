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
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.Transparency;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class GradientFactory {

	public final static short HORIZONTAL = 1031;
	public final static short VERTICAL = 1032;
	public final static short DIAGONAL_UP = 1033;
	public final static short DIAGONAL_DOWN = 1034;
	public final static short FROM_CORNER = 1035;
	public final static short FROM_CENTER = 1036;

	public final static short VARIANT1 = 1041;
	public final static short VARIANT2 = 1042;
	public final static short VARIANT3 = 1043;
	public final static short VARIANT4 = 1044;

	private static Rectangle2D rectangle;
	private static GeneralPath triangle;

	public static void paintOval(Paint gradientPaint, Graphics2D g, Color edgeColor, Color centerColor, float x,
			float y, float w, float h, float angle) {
		float a = w * 0.5f;
		float b = h * 0.5f;
		// fix bugs on Mac OS X about the Raster out-of-memory error in EllipticalGradientPaintContext
		if (System.getProperty("os.name").startsWith("Mac")) {
			if (gradientPaint == null) {
				gradientPaint = new EllipticalGradientPaint(a, b, a, b, angle, edgeColor, centerColor);
			}
			else {
				EllipticalGradientPaint egp = (EllipticalGradientPaint) gradientPaint;
				egp.setCenterColor(centerColor);
				egp.setEdgeColor(edgeColor);
				egp.setCenter(a, b);
				egp.setA(a);
				egp.setB(b);
				egp.setAngle(angle);
			}
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice gd = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gd.getDefaultConfiguration();
			BufferedImage biOval = gc.createCompatibleImage((int) w, (int) h, Transparency.TRANSLUCENT);
			Graphics2D g2 = biOval.createGraphics();
			g2.setPaint(gradientPaint);
			g2.fillOval(0, 0, (int) w, (int) h);
			g2.dispose();
			g.drawImage(biOval, (int) x, (int) y, null);
		}
		else {
			if (gradientPaint == null) {
				gradientPaint = new EllipticalGradientPaint(x + a, y + b, a, b, angle, edgeColor, centerColor);
			}
			else {
				EllipticalGradientPaint egp = (EllipticalGradientPaint) gradientPaint;
				egp.setCenterColor(centerColor);
				egp.setEdgeColor(edgeColor);
				egp.setCenter(x + a, y + b);
				egp.setA(a);
				egp.setB(b);
				egp.setAngle(angle);
			}
			g.setPaint(gradientPaint);
			g.fillOval((int) x, (int) y, (int) w, (int) h);
		}
	}

	public static void paintRect(Graphics2D g, int style, int variant, Color color1, Color color2, float x, float y,
			float w, float h) {

		if (rectangle == null)
			rectangle = new Rectangle2D.Float();
		if (triangle == null)
			triangle = new GeneralPath();

		GradientPaint gp;
		switch (style) {
		case HORIZONTAL:
			switch (variant) {
			case VARIANT1:
				gp = new GradientPaint(x, y, color1, x, y + h, color2);
				g.setPaint(gp);
				rectangle.setRect(x, y, w, h);
				g.fill(rectangle);
				break;
			case VARIANT2:
				gp = new GradientPaint(x, y, color2, x, y + h, color1);
				g.setPaint(gp);
				rectangle.setRect(x, y, w, h);
				g.fill(rectangle);
				break;
			case VARIANT3:
				gp = new GradientPaint(x, y, color1, x, y + 0.5f * h, color2);
				g.setPaint(gp);
				rectangle.setRect(x, y, w, 0.5f * h);
				g.fill(rectangle);
				gp = new GradientPaint(x, y + 0.5f * h, color2, x, y + h, color1);
				g.setPaint(gp);
				rectangle.setRect(x, y + 0.5f * h, w, 0.5f * h);
				g.fill(rectangle);
				break;
			case VARIANT4:
				gp = new GradientPaint(x, y, color2, x, y + 0.5f * h, color1);
				g.setPaint(gp);
				rectangle.setRect(x, y, w, 0.5f * h);
				g.fill(rectangle);
				gp = new GradientPaint(x, y + 0.5f * h, color1, x, y + h, color2);
				g.setPaint(gp);
				rectangle.setRect(x, y + 0.5f * h, w, 0.5f * h);
				g.fill(rectangle);
				break;
			}
			break;
		case VERTICAL:
			switch (variant) {
			case VARIANT1:
				gp = new GradientPaint(x, y, color1, x + w, y, color2);
				g.setPaint(gp);
				rectangle.setRect(x, y, w, h);
				g.fill(rectangle);
				break;
			case VARIANT2:
				gp = new GradientPaint(x, y, color2, x + w, y, color1);
				g.setPaint(gp);
				rectangle.setRect(x, y, w, h);
				g.fill(rectangle);
				break;
			case VARIANT3:
				gp = new GradientPaint(x, y, color1, x + 0.5f * w, y, color2);
				g.setPaint(gp);
				rectangle.setRect(x, y, 0.5f * w, h);
				g.fill(rectangle);
				gp = new GradientPaint(x + 0.5f * w, y, color2, x + w, y, color1);
				g.setPaint(gp);
				rectangle.setRect(x + 0.5f * w, y, 0.5f * w, h);
				g.fill(rectangle);
				break;
			case VARIANT4:
				gp = new GradientPaint(x, y, color2, x + 0.5f * w, y, color1);
				g.setPaint(gp);
				rectangle.setRect(x, y, 0.5f * w, h);
				g.fill(rectangle);
				gp = new GradientPaint(x + 0.5f * w, y, color1, x + w, y, color2);
				g.setPaint(gp);
				rectangle.setRect(x + 0.5f * w, y, 0.5f * w, h);
				g.fill(rectangle);
				break;
			}
			break;
		case DIAGONAL_UP:
			switch (variant) {
			case VARIANT1:
				gp = new GradientPaint(x, y, color1, x + w, y + h, color2);
				g.setPaint(gp);
				rectangle.setRect(x, y, w, h);
				g.fill(rectangle);
				break;
			case VARIANT2:
				gp = new GradientPaint(x, y, color2, x + w, y + h, color1);
				g.setPaint(gp);
				rectangle.setRect(x, y, w, h);
				g.fill(rectangle);
				break;
			case VARIANT3:
				float dist = 1.0f / (float) Math.hypot(w, h);
				float cost = w * dist;
				float sint = h * dist;
				triangle.reset();
				triangle.moveTo(x, y);
				triangle.lineTo(x, y + h);
				triangle.lineTo(x + w, y);
				triangle.closePath();
				gp = new GradientPaint(x, y, color1, x + h * cost * sint, y + h * cost * cost, color2);
				g.setPaint(gp);
				g.fill(triangle);
				triangle.reset();
				triangle.moveTo(x, y + h);
				triangle.lineTo(x + w, y);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + w - h * cost * sint, y + h - h * cost * cost, color2, x + w, y + h, color1);
				g.setPaint(gp);
				g.fill(triangle);
				break;
			case VARIANT4:
				dist = 1.0f / (float) Math.hypot(w, h);
				cost = w * dist;
				sint = h * dist;
				triangle.reset();
				triangle.moveTo(x, y);
				triangle.lineTo(x, y + h);
				triangle.lineTo(x + w, y);
				triangle.closePath();
				gp = new GradientPaint(x, y, color2, x + h * cost * sint, y + h * cost * cost, color1);
				g.setPaint(gp);
				g.fill(triangle);
				triangle.reset();
				triangle.moveTo(x, y + h);
				triangle.lineTo(x + w, y);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + w - h * cost * sint, y + h - h * cost * cost, color1, x + w, y + h, color2);
				g.setPaint(gp);
				g.fill(triangle);
				break;
			}
			break;
		case DIAGONAL_DOWN:
			switch (variant) {
			case VARIANT1:
				gp = new GradientPaint(x, y + h, color1, x + w, y, color2);
				g.setPaint(gp);
				rectangle.setRect(x, y, w, h);
				g.fill(rectangle);
				break;
			case VARIANT2:
				gp = new GradientPaint(x, y + h, color2, x + w, y, color1);
				g.setPaint(gp);
				rectangle.setRect(x, y, w, h);
				g.fill(rectangle);
				break;
			case VARIANT3:
				float dist = 1.0f / (float) Math.hypot(w, h);
				float cost = w * dist;
				float sint = h * dist;
				float dx = h * cost * sint;
				float dy = h * cost * cost;
				triangle.reset();
				triangle.moveTo(x, y);
				triangle.lineTo(x + w, y);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + w, y, color1, x + w - dx, y + dy, color2);
				g.setPaint(gp);
				g.fill(triangle);
				triangle.reset();
				triangle.moveTo(x, y);
				triangle.lineTo(x, y + h);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + dx, y + h - dy, color2, x, y + h, color1);
				g.setPaint(gp);
				g.fill(triangle);
				break;
			case VARIANT4:
				dist = 1.0f / (float) Math.hypot(w, h);
				cost = w * dist;
				sint = h * dist;
				dx = h * cost * sint;
				dy = h * cost * cost;
				triangle.reset();
				triangle.moveTo(x, y);
				triangle.lineTo(x + w, y);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + w, y, color2, x + w - dx, y + dy, color1);
				g.setPaint(gp);
				g.fill(triangle);
				triangle.reset();
				triangle.moveTo(x, y);
				triangle.lineTo(x, y + h);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + dx, y + h - dy, color1, x, y + h, color2);
				g.setPaint(gp);
				g.fill(triangle);
				break;
			}
			break;
		case FROM_CORNER:
			switch (variant) {
			case VARIANT1:
				triangle.reset();
				triangle.moveTo(x, y);
				triangle.lineTo(x + w, y);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x, y, color1, x + w, y, color2);
				g.setPaint(gp);
				g.fill(triangle);
				triangle.reset();
				triangle.moveTo(x, y);
				triangle.lineTo(x, y + h);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x, y, color1, x, y + h, color2);
				g.setPaint(gp);
				g.fill(triangle);
				break;
			case VARIANT2:
				triangle.reset();
				triangle.moveTo(x, y);
				triangle.lineTo(x + w, y);
				triangle.lineTo(x, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + w, y, color1, x, y, color2);
				g.setPaint(gp);
				g.fill(triangle);
				triangle.reset();
				triangle.moveTo(x + w, y);
				triangle.lineTo(x, y + h);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x, y, color1, x, y + h, color2);
				g.setPaint(gp);
				g.fill(triangle);
				break;
			case VARIANT3:
				triangle.reset();
				triangle.moveTo(x, y);
				triangle.lineTo(x + w, y);
				triangle.lineTo(x, y + h);
				triangle.closePath();
				gp = new GradientPaint(x, y + h, color1, x, y, color2);
				g.setPaint(gp);
				g.fill(triangle);
				triangle.reset();
				triangle.moveTo(x + w, y);
				triangle.lineTo(x, y + h);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x, y, color1, x + w, y, color2);
				g.setPaint(gp);
				g.fill(triangle);
				break;
			case VARIANT4:
				triangle.reset();
				triangle.moveTo(x, y);
				triangle.lineTo(x + w, y);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + w, y + h, color1, x + w, y, color2);
				g.setPaint(gp);
				g.fill(triangle);
				triangle.reset();
				triangle.moveTo(x, y);
				triangle.lineTo(x, y + h);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + w, y + h, color1, x, y + h, color2);
				g.setPaint(gp);
				g.fill(triangle);
				break;
			}
			break;
		case FROM_CENTER:
			switch (variant) {
			case VARIANT1:
				triangle.reset();
				triangle.moveTo(x + 0.5f * w, y + 0.5f * h);
				triangle.lineTo(x, y);
				triangle.lineTo(x + w, y);
				triangle.closePath();
				gp = new GradientPaint(x + 0.5f * w, y + 0.5f * h, color1, x + 0.5f * w, y, color2);
				g.setPaint(gp);
				g.fill(triangle);
				triangle.reset();
				triangle.moveTo(x + 0.5f * w, y + 0.5f * h);
				triangle.lineTo(x, y + h);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + 0.5f * w, y + 0.5f * h, color1, x + 0.5f * w, y + h, color2);
				g.setPaint(gp);
				g.fill(triangle);
				triangle.reset();
				triangle.moveTo(x + 0.5f * w, y + 0.5f * h);
				triangle.lineTo(x, y);
				triangle.lineTo(x, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + 0.5f * w, y + 0.5f * h, color1, x, y + 0.5f * h, color2);
				g.setPaint(gp);
				g.fill(triangle);
				triangle.reset();
				triangle.moveTo(x + 0.5f * w, y + 0.5f * h);
				triangle.lineTo(x + w, y);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + 0.5f * w, y + 0.5f * h, color1, x + w, y + 0.5f * h, color2);
				g.setPaint(gp);
				g.fill(triangle);
				break;
			case VARIANT2:
				triangle.reset();
				triangle.moveTo(x + 0.5f * w, y + 0.5f * h);
				triangle.lineTo(x, y);
				triangle.lineTo(x + w, y);
				triangle.closePath();
				gp = new GradientPaint(x + 0.5f * w, y + 0.5f * h, color2, x + 0.5f * w, y, color1);
				g.setPaint(gp);
				g.fill(triangle);
				triangle.reset();
				triangle.moveTo(x + 0.5f * w, y + 0.5f * h);
				triangle.lineTo(x, y + h);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + 0.5f * w, y + 0.5f * h, color2, x + 0.5f * w, y + h, color1);
				g.setPaint(gp);
				g.fill(triangle);
				triangle.reset();
				triangle.moveTo(x + 0.5f * w, y + 0.5f * h);
				triangle.lineTo(x, y);
				triangle.lineTo(x, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + 0.5f * w, y + 0.5f * h, color2, x, y + 0.5f * h, color1);
				g.setPaint(gp);
				g.fill(triangle);
				triangle.reset();
				triangle.moveTo(x + 0.5f * w, y + 0.5f * h);
				triangle.lineTo(x + w, y);
				triangle.lineTo(x + w, y + h);
				triangle.closePath();
				gp = new GradientPaint(x + 0.5f * w, y + 0.5f * h, color2, x + w, y + 0.5f * h, color1);
				g.setPaint(gp);
				g.fill(triangle);
				break;
			case VARIANT3:
				break;
			case VARIANT4:
				break;
			}
			break;
		}

	}

}
