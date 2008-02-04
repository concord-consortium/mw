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

import java.awt.geom.RoundRectangle2D;

class RoundRectangle extends RoundRectangle2D.Float {

	private float x0, y0;

	RoundRectangle() {
		super();
	}

	RoundRectangle(float x, float y, float w, float h, float arcw, float arch) {
		super(x, y, w, h, arcw, arch);
		setOrigin(x, y);
	}

	void setOrigin(float x, float y) {
		x0 = x;
		y0 = y;
	}

	float getX0() {
		return x0;
	}

	float getY0() {
		return y0;
	}

}