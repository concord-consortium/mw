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

package org.concord.modeler.g2d;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

public class Tracer {

	private Point[] points;
	private Color color = Color.black;
	private int size = 6;

	public Tracer(Point[] p) {
		points = p;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getSize() {
		return size;
	}

	public void paint(Graphics g) {
		if (points == null || points.length == 0)
			return;
		Color oldColor = g.getColor();
		g.setColor(color);
		for (Point p : points) {
			g.fillOval(p.x - size / 2, p.y - size / 2, size, size);
			g.drawLine(p.x - 10, p.y, p.x + 10, p.y);
			g.drawLine(p.x, p.y - 10, p.x, p.y + 10);
		}
		g.setColor(oldColor);
	}

}
