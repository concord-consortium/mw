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

class CirclePoint extends EventPoint {

	double radius;
	ArcNode arc;

	CirclePoint(double d, double d1, ArcNode arcnode) {
		super(d, d1);
		arc = arcnode;
		radius = distance(arcnode);
		x += radius;
	}

	public void paint(Graphics g) {
		super.paint(g);
		double d = radius;
		g.drawOval((int) (x - 2D * d), (int) (y - d), (int) (2D * d), (int) (2D * d));
	}

	public void action(Fortune f) {
		ArcNode arcnode = arc.prev;
		ArcNode arcnode1 = arc.next;
		MyPoint mypoint = new MyPoint(x - radius, y);
		arc.completeTrace(f, mypoint);
		arcnode.completeTrace(f, mypoint);
		arcnode.startOfTrace = mypoint;
		arcnode.next = arcnode1;
		arcnode1.prev = arcnode;
		if (arcnode.circlePoint != null) {
			f.queue.remove(arcnode.circlePoint);
			arcnode.circlePoint = null;
		}
		if (arcnode1.circlePoint != null) {
			f.queue.remove(arcnode1.circlePoint);
			arcnode1.circlePoint = null;
		}
		arcnode.checkCircle(f.queue);
		arcnode1.checkCircle(f.queue);
	}

}