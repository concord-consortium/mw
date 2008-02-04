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

package org.concord.mw2d.geometry;

import java.awt.Graphics;
import java.awt.Point;

class MyPoint implements Paintable {

	volatile double x, y;

	public MyPoint(double d, double d1) {
		x = d;
		y = d1;
	}

	public MyPoint(MyPoint mypoint) {
		x = mypoint.x;
		y = mypoint.y;
	}

	public void paint(Graphics g) {
		g.drawOval((int) (x - 1.0), (int) (y - 1.0), 2, 2);
	}

	public double distance(MyPoint mypoint) {
		double d = mypoint.x - x;
		double d1 = mypoint.y - y;
		return Math.sqrt(d * d + d1 * d1);
	}

	public String toString() {
		return "[" + x + "," + y + "]";
	}

	public Point toPoint() {
		return new Point((int) x, (int) y);
	}

}