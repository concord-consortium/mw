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

class ArcTree {

	ArcNode arcNode;

	public void insert(MyPoint mypoint, double d, EventQueue eventqueue) {

		if (arcNode == null) {
			arcNode = new ArcNode(mypoint);
			return;
		}

		try {

			ParabolaPoint parabolapoint = new ParabolaPoint(mypoint);
			parabolapoint.init(d);
			arcNode.init(d);
			arcNode.insert(parabolapoint, d, eventqueue);
			return;

		}
		catch (Throwable _ex) {

			System.out.println("error: No parabola intersection during ArcTree.insert()");

		}

	}

	public void checkBounds(Fortune f, double d) {
		if (arcNode != null) {
			arcNode.init(d);
			arcNode.checkBounds(f, d);
		}
	}

}