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

class EventPoint extends MyPoint {

	EventPoint prev, next;

	EventPoint(MyPoint mypoint) {
		super(mypoint);
	}

	EventPoint(double x, double y) {
		super(x, y);
	}

	public void insert(EventPoint eventpoint) {

		if (eventpoint.x > x || eventpoint.x == x && eventpoint.y > y) {

			if (next != null) {
				next.insert(eventpoint);
				return;
			}
			next = eventpoint;
			eventpoint.prev = this;
			return;

		}

		if (eventpoint.x != x || eventpoint.y != y || (eventpoint instanceof CirclePoint)) {

			eventpoint.prev = prev;
			eventpoint.next = this;

			if (prev != null) {
				prev.next = eventpoint;
			}
			prev = eventpoint;
			return;

		}

		eventpoint.prev = eventpoint;
		System.out.println("Double point ignored: " + eventpoint.toString());
		return;

	}

	public void action(Fortune fortune) {
		fortune.tree.insert(this, fortune.xpos, fortune.queue);
	}

}