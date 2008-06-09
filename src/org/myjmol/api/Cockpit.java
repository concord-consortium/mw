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

package org.myjmol.api;

import java.awt.Color;
import java.awt.Graphics;

public class Cockpit {

	private JmolViewer viewer;
	private int size = 50;
	private Color contrastBackgroundColor;

	public Cockpit(JmolViewer viewer) {
		this.viewer = viewer;
	}

	public void setBackground(Color c) {
		contrastBackgroundColor = new Color(0xffffff ^ c.getRGB());
	}

	public void paint(Graphics g) {
		g.setColor(contrastBackgroundColor);
		int w = viewer.getScreenWidth();
		int h = viewer.getScreenHeight();
		// upper left
		int x = w / 2 - size;
		int y = h / 2 - size;
		g.drawLine(x, y, x + 10, y);
		g.drawLine(x, y, x, y + 10);
		// upper right
		x = w / 2 + size;
		g.drawLine(x, y, x - 10, y);
		g.drawLine(x, y, x, y + 10);
		// lower right
		y = h / 2 + size;
		g.drawLine(x, y, x - 10, y);
		g.drawLine(x, y, x, y - 10);
		// lower left
		x = w / 2 - size;
		g.drawLine(x, y, x + 10, y);
		g.drawLine(x, y, x, y - 10);
		// central cross
		x = w / 2;
		y = h / 2;
		g.drawLine(x - 5, y, x + 5, y);
		g.drawLine(x, y - 5, x, y + 5);
	}

}