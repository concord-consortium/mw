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

class ArcNode extends ParabolaPoint {

	ArcNode next, prev;
	CirclePoint circlePoint;
	MyPoint startOfTrace;

	public ArcNode(MyPoint mypoint) {
		super(mypoint);
	}

	public void checkCircle(EventQueue eventqueue) {
		if (prev != null && next != null) {
			circlePoint = calculateCenter(next, this, prev);
			if (circlePoint != null)
				eventqueue.insert(circlePoint);
		}
	}

	public void removeCircle(EventQueue eventqueue) {
		if (circlePoint != null) {
			eventqueue.remove(circlePoint);
			circlePoint = null;
		}
	}

	@SuppressWarnings("unchecked")
	public void completeTrace(Fortune f, MyPoint mypoint) {
		if (startOfTrace != null) {
			f.voronoi.add(new MyLine(startOfTrace, mypoint));
			f.delaunay.add(new MyLine(this, next));
			startOfTrace = null;
		}
	}

	public void checkBounds(Fortune f, double d) {
		if (next != null) {
			next.init(d);
			if (d > next.x && d > x && startOfTrace != null) {
				try {
					double ad[] = solveQuadratic(a - next.a, b - next.b, c - next.c);
					double d1 = ad[0];
					double d2 = d - F(d1);
					if (d2 < startOfTrace.x && d2 < 0.0D || d1 < 0.0D || d2 >= f.width || d1 >= f.height)
						completeTrace(f, new MyPoint(d2, d1));
				}
				catch (Throwable _ex) {
					System.out.println("*** exception");
				}
			}
			next.checkBounds(f, d);
		}
	}

	public void insert(ParabolaPoint parabolapoint, double sline, EventQueue eventqueue) throws Throwable {

		boolean split = true;
		if (next != null) {
			next.init(sline);
			if (sline > next.x && sline > x) {
				double xs[] = solveQuadratic(a - next.a, b - next.b, c - next.c);
				if (xs[0] <= parabolapoint.realX() && xs[0] != xs[1]) {
					split = false;
				}
			}
			else {
				split = false;
			}
		}

		if (split) {

			removeCircle(eventqueue);

			ArcNode arcnode = new ArcNode(parabolapoint);
			arcnode.next = new ArcNode(this);
			arcnode.prev = this;
			arcnode.next.next = next;
			arcnode.next.prev = arcnode;

			if (next != null)
				next.prev = arcnode.next;

			next = arcnode;

			checkCircle(eventqueue);
			next.next.checkCircle(eventqueue);

			next.next.startOfTrace = startOfTrace;
			startOfTrace = new MyPoint(sline - F(parabolapoint.y), parabolapoint.y);
			next.startOfTrace = new MyPoint(sline - F(parabolapoint.y), parabolapoint.y);

		}
		else {
			next.insert(parabolapoint, sline, eventqueue);
		}
	}

}