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
import java.awt.Rectangle;

class MyLine implements Paintable {

	private MyPoint p1, p2;

	MyLine(MyPoint mypoint, MyPoint mypoint1) {
		p1 = mypoint;
		p2 = mypoint1;
	}

	public void paint(Graphics g) {
		Rectangle r = g.getClipBounds();
		if (!r.contains(p1.toPoint()) && !r.contains(p2.toPoint()))
			return;
		g.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
	}

}