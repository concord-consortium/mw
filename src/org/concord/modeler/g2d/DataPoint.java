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

package org.concord.modeler.g2d;

public class DataPoint {

	private double x;
	private double y;

	public DataPoint(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public String toString() {
		return "[" + x + ", " + y + "]";
	}

	public boolean equals(Object o) {
		if (!(o instanceof DataPoint))
			return false;
		return x == ((DataPoint) o).x && y == ((DataPoint) o).y;
	}

	public int hashCode() {
		double w = x + y;
		w = 0.5 * w * (w + 1) + x;
		long bits = Double.doubleToLongBits(w);
		return (int) (bits ^ (bits >>> 32));
	}

}
