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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;

import javax.swing.JComponent;

public class ArrowRectangle extends JComponent {

	public final static byte NO_ARROW = 0;
	public final static byte STYLE1 = 1;
	public final static byte STYLE2 = 2;
	public final static byte STYLE3 = 3;
	public final static byte STYLE4 = 4;
	public final static byte STYLE5 = 5;

	private final static BasicStroke stroke = new BasicStroke(3.0f);
	private static Polygon polygon;

	private byte type = NO_ARROW;
	private int margin = 10;
	private int xend, yend;

	public ArrowRectangle() {
		setPreferredSize(new Dimension(60, 20));
	}

	public void setArrowType(byte i) {
		type = i;
	}

	public byte getArrowType() {
		return type;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {

		int w = getWidth();
		int h = getHeight();
		g.setColor(getBackground());
		g.fillRect(0, 0, w, h);

		g.setColor(getForeground());
		xend = w - margin * 2;
		yend = h / 2;
		g.drawLine(margin, yend - 1, xend, yend - 1);
		g.drawLine(margin, yend, xend, yend);
		g.drawLine(margin, yend + 1, xend, yend + 1);

		switch (type) {

		case STYLE1:
			if (polygon == null)
				polygon = new Polygon();
			else polygon.reset();
			polygon.addPoint(xend + 2, yend);
			polygon.addPoint(xend - 16, yend - 8);
			polygon.addPoint(xend - 16, yend + 8);
			g.fillPolygon(polygon);
			break;

		case STYLE2:
			((Graphics2D) g).setStroke(stroke);
			g.drawLine(xend, yend, xend - 16, yend - 6);
			g.drawLine(xend, yend, xend - 16, yend + 6);
			break;

		case STYLE3:
			if (polygon == null)
				polygon = new Polygon();
			else polygon.reset();
			polygon.addPoint(xend + 2, yend);
			polygon.addPoint(xend - 16, yend - 8);
			polygon.addPoint(xend - 10, yend);
			polygon.addPoint(xend - 16, yend + 8);
			g.fillPolygon(polygon);
			break;

		case STYLE4:
			if (polygon == null)
				polygon = new Polygon();
			else polygon.reset();
			polygon.addPoint(xend + 2, yend);
			polygon.addPoint(xend - 4, yend - 6);
			polygon.addPoint(xend - 10, yend);
			polygon.addPoint(xend - 4, yend + 6);
			g.fillPolygon(polygon);
			break;

		case STYLE5:
			g.fillOval(xend - 10, yend - 6, 12, 12);
			break;

		}

	}

}
