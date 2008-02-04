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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;

public final class PatternFactory {

	private final static BasicStroke xxxx = new BasicStroke(.5f);
	private final static BasicStroke thin = new BasicStroke(1.f);

	public final static byte SMALL = 101;
	public final static byte MEDIUM = 102;
	public final static byte LARGE = 103;

	public final static byte POLKA = 1;
	public final static byte MOSIAC = 2;
	public final static byte POSITIVE = 3;
	public final static byte NEGATIVE = 4;
	public final static byte STARRY = 5;
	public final static byte CIRCULAR = 6;
	public final static byte HORIZONTAL_STRIPE = 7;
	public final static byte VERTICAL_STRIPE = 8;
	public final static byte DIAGONAL_UP_STRIPE = 9;
	public final static byte DIAGONAL_DOWN_STRIPE = 10;
	public final static byte GRID = 11;
	public final static byte HORIZONTAL_BRICK = 12;
	public final static byte DENSITY50 = 13;
	public final static byte DENSITY25 = 14;
	public final static byte DENSITY5 = 15;
	public final static byte DENSITY95 = 16;
	public final static byte CIRCLE_CONTACT = 17;
	public final static byte CIRCLE_SEPARATE = 18;
	public final static byte HORIZONTAL_LATTICE = 19;
	public final static byte TRIANGLE_HALF = 20;
	public final static byte DICE = 21;

	public final static byte[] STYLE_ARRAY = new byte[] { POLKA, // row 1
			POLKA, MOSIAC, MOSIAC, POSITIVE, NEGATIVE, STARRY, // row 2
			CIRCULAR, HORIZONTAL_STRIPE, HORIZONTAL_STRIPE, VERTICAL_STRIPE, VERTICAL_STRIPE, DIAGONAL_UP_STRIPE, // row
																													// 3
			DIAGONAL_UP_STRIPE, DIAGONAL_DOWN_STRIPE, DIAGONAL_DOWN_STRIPE, GRID, GRID, HORIZONTAL_BRICK, // row 4
			HORIZONTAL_BRICK, DENSITY50, DENSITY25, DENSITY5, DENSITY95, DENSITY5, // row 5
			DENSITY95, CIRCLE_CONTACT, CIRCLE_CONTACT, CIRCLE_SEPARATE, CIRCLE_SEPARATE, HORIZONTAL_LATTICE, // row 6
			HORIZONTAL_LATTICE, HORIZONTAL_LATTICE, DICE, DICE, TRIANGLE_HALF };

	public final static byte[] SIZE_ARRAY = new byte[] { SMALL, // row 1
			MEDIUM, SMALL, MEDIUM, MEDIUM, MEDIUM, LARGE, // row 2
			LARGE, SMALL, MEDIUM, SMALL, MEDIUM, MEDIUM, // row 3
			LARGE, SMALL, MEDIUM, SMALL, MEDIUM, SMALL, // row4
			MEDIUM, SMALL, SMALL, SMALL, SMALL, LARGE, // row 5
			LARGE, MEDIUM, LARGE, MEDIUM, LARGE, SMALL, // row 6
			MEDIUM, LARGE, MEDIUM, LARGE, SMALL };

	private static Rectangle r = new Rectangle();

	public static TexturePaint createPattern(int type, int w, int h, Color c1, Color c2) {

		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bi.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setStroke(thin);
		switch (type) {
		case POLKA:
			int x = w / 4;
			int y = h / 4;
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			g.fillOval(x, y, x + x, y + y);
			r.setBounds(x, y, w, h);
			return new TexturePaint(bi, r);
		case MOSIAC:
			x = w / 2;
			y = h / 2;
			g.setColor(c1);
			g.fillRect(0, 0, x, y);
			g.fillRect(x, y, x, y);
			g.setColor(c2);
			g.fillRect(x, 0, x, y);
			g.fillRect(0, y, x, y);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case POSITIVE:
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			x = w / 2 + 2;
			y = h / 2 + 2;
			g.drawLine(1, y, 3, y);
			g.drawLine(2, y - 1, 2, y + 1);
			g.drawLine(x, 1, x, 3);
			g.drawLine(x - 1, 2, x + 1, 2);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case NEGATIVE:
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			x = w / 2 + 2;
			y = h / 2 + 2;
			g.drawLine(1, y, 3, y);
			g.drawLine(x - 1, 2, x + 1, 2);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case CIRCULAR:
			for (int i = 0; i < w / 2; i++) {
				for (int j = 0; j < h / 2; j++) {
					g.setColor(c1);
					g.fillRect(2 * i, 2 * j, 1, 1);
					g.fillRect(2 * i + 1, 2 * j + 1, 1, 1);
					g.setColor(c2);
					g.fillRect(2 * i + 1, 2 * j, 1, 1);
					g.fillRect(2 * i, 2 * j + 1, 1, 1);
				}
			}
			g.setColor(c2);
			g.drawLine(1, h / 2, 3, h / 2);
			g.drawLine(2, h / 2 - 1, 2, h / 2 + 1);
			g.drawLine(w / 2, 1, w / 2, 3);
			g.drawLine(w / 2 - 1, 2, w / 2 + 1, 2);
			r.setBounds(0, 0, w - 2, h - 2);
			return new TexturePaint(bi, r);
		case DENSITY50:
			g.setColor(c1);
			g.fillRect(0, 0, w, h);
			for (int i = 0; i < w / 2; i++) {
				for (int j = 0; j < h / 2; j++) {
					g.setColor(c2);
					g.fillRect(2 * i + 1, 2 * j, 1, 1);
					g.fillRect(2 * i, 2 * j + 1, 1, 1);
				}
			}
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case DENSITY25:
			g.setColor(c1);
			g.fillRect(0, 0, w, h);
			for (int i = 0; i < w / 2; i++) {
				for (int j = 0; j < h / 2; j++) {
					g.setColor(c2);
					g.fillRect(2 * i, 2 * j, 1, 1);
				}
			}
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case DENSITY5:
			x = w / 2 + 1;
			y = h / 2 + 1;
			g.setColor(c1);
			g.fillRect(0, 0, w, h);
			g.setColor(c2);
			g.fillRect(1, y, 1, 1);
			g.fillRect(x, 1, 1, 1);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case DENSITY95:
			x = w / 2 + 1;
			y = h / 2 + 1;
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			g.fillRect(1, y, 1, 1);
			g.fillRect(x, 1, 1, 1);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case STARRY:
			g.setColor(c1);
			g.fillRect(0, 0, w, h);
			for (int i = 0; i < w / 2; i++) {
				for (int j = 0; j < h / 2; j++) {
					g.setColor(c2);
					g.fillRect(2 * i + 1, 2 * j, 1, 1);
					g.fillRect(2 * i, 2 * j + 1, 1, 1);
				}
			}
			g.setColor(c2);
			x = w / 2 + 2;
			y = h / 2 + 2;
			g.drawLine(1, y, 3, y);
			g.drawLine(2, y - 1, 2, y + 1);
			g.drawLine(x, 1, x, 3);
			g.drawLine(x - 1, 2, x + 1, 2);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case HORIZONTAL_STRIPE:
			y = h / 2;
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			g.drawLine(0, y, w, y);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case VERTICAL_STRIPE:
			x = w / 2;
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			g.drawLine(x, 0, x, h);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case DIAGONAL_UP_STRIPE:
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			g.setStroke(xxxx);
			g.drawLine(0, h, w, 0);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case DIAGONAL_DOWN_STRIPE:
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			g.setStroke(xxxx);
			g.drawLine(0, 0, w, h);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case GRID:
			x = w / 2;
			y = h / 2;
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			g.drawLine(0, h / 2, w, h / 2);
			g.drawLine(w / 2, 0, w / 2, h);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case HORIZONTAL_BRICK:
			x = w / 2;
			y = h / 2;
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			g.drawLine(0, 0, w, 0);
			g.drawLine(0, y, w, y);
			g.drawLine(0, 0, 0, y);
			g.drawLine(x, y, x, h);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case CIRCLE_CONTACT:
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			g.drawOval(0, 0, w, h);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case CIRCLE_SEPARATE:
			x = w / 4;
			y = h / 4;
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			g.drawOval(0, 0, w, h);
			g.drawOval(x, y, x + x < w / 2 ? x + x + 2 : w / 2, y + y < h / 2 ? y + y + 2 : h / 2);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case HORIZONTAL_LATTICE:
			x = w / 2;
			y = h / 2;
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			g.drawOval(0, 0, x, y);
			g.drawLine(x, y / 2, w, y / 2);
			g.drawLine(x / 2, y, x / 2, h);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case DICE:
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			g.drawRect(0, 0, w, h);
			g.fillOval(w / 2 - 2, h / 2 - 2, 5, 5);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		case TRIANGLE_HALF:
			g.setColor(c2);
			g.fillRect(0, 0, w, h);
			g.setColor(c1);
			Polygon triangle = new Polygon();
			triangle.addPoint(0, 0);
			triangle.addPoint(w, 0);
			triangle.addPoint(0, h);
			g.fillPolygon(triangle);
			r.setBounds(0, 0, w, h);
			return new TexturePaint(bi, r);
		}
		return null;
	}

}
