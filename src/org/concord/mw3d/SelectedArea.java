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

package org.concord.mw3d;

import java.awt.Rectangle;

class SelectedArea extends Rectangle {

	private int x0, y0;

	public SelectedArea() {
		super();
	}

	public SelectedArea(int x, int y, int w, int h) {
		super(x, y, w, h);
		setOrigin(x, y);
	}

	public void setRect(int x, int y, int w, int h) {
		super.setRect(x, y, w, h);
		setOrigin(x, y);
	}

	public void setLocation(int x, int y) {
		super.setLocation(x, y);
		setOrigin(x, y);
	}

	public void setOrigin(int x, int y) {
		x0 = x;
		y0 = y;
	}

	public int getX0() {
		return x0;
	}

	public int getY0() {
		return y0;
	}

}
