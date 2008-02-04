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

package org.concord.mw2d;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

class ControlPoint extends Point {

	public int x;
	public int y;
	public static final int PT_SIZE = 3;

	private boolean selected;
	private Color color;

	public void paint(Graphics g) {
		Color oldColor = g.getColor();
		int d = PT_SIZE + PT_SIZE;
		g.setColor(Color.white);
		g.fillRect(x - PT_SIZE, y - PT_SIZE, d, d);
		g.setColor(color == null ? Color.black : color);
		g.drawRect(x - PT_SIZE, y - PT_SIZE, d, d);
		g.setColor(oldColor);
	}

	public boolean within(int a, int b) {
		return a >= x - PT_SIZE && b >= y - PT_SIZE && a <= x + PT_SIZE && b <= y + PT_SIZE;
	}

	public void setColor(Color c) {
		color = c;
	}

	public Color getColor() {
		return color;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean b) {
		selected = b;
	}

}