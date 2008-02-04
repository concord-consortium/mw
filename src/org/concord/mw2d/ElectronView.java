/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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
package org.concord.mw2d;

import java.awt.Color;
import java.awt.Graphics;

import org.concord.mw2d.models.Electron;

/**
 * @author Charles Xie
 * 
 */
class ElectronView {

	private Electron electron;
	private int x, y;
	private static int radius = 3;

	ElectronView(Electron electron) {
		this.electron = electron;
	}

	Electron getModel() {
		return electron;
	}

	void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
	}

	static int getRadius() {
		return radius;
	}

	int getX() {
		return x;
	}

	int getY() {
		return y;
	}

	boolean contains(int rx, int ry) {
		if (Math.abs(rx - x - radius) > radius)
			return false;
		if (Math.abs(ry - y - radius) > radius)
			return false;
		return true;
	}

	void draw(Graphics g) {
		g.setColor(Color.white);
		g.fillOval(x, y, radius * 2, radius * 2);
		g.setColor(Color.black);
		g.drawOval(x, y, radius * 2, radius * 2);
	}

}
