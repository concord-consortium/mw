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

class EventQueue {

	EventPoint event;

	public void insert(EventPoint p) {

		if (event != null)
			event.insert(p);

		if (p.prev == null)
			event = p;

	}

	public void remove(EventPoint eventpoint) {

		if (eventpoint.next != null)
			eventpoint.next.prev = eventpoint.prev;

		if (eventpoint.prev != null) {
			eventpoint.prev.next = eventpoint.next;
		}
		else {
			event = eventpoint.next;
		}

	}

	public EventPoint pop() {

		EventPoint eventpoint = event;
		if (eventpoint != null) {
			event = event.next;
			if (event != null) {
				event.prev = null;
			}
		}

		return eventpoint;

	}

	public void paint(Graphics g, boolean flag) {

		for (EventPoint eventpoint = event; eventpoint != null; eventpoint = eventpoint.next) {

			if (flag || !(eventpoint instanceof CirclePoint)) {
				eventpoint.paint(g);
			}

		}

	}

}