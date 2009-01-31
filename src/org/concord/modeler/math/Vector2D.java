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

package org.concord.modeler.math;

import java.io.Serializable;

/**
 * This is an implementation of 2D vector.
 * 
 * @author Charles Xie
 */

public class Vector2D implements Serializable {

	private double x, y;

	/**
	 * By default, construct a unit vector pointing in the abscissa direction.
	 */
	public Vector2D() {
		x = 1.0;
	}

	public Vector2D(double x, double y) {
		setX(x);
		setY(y);
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getX() {
		return x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getY() {
		return y;
	}

	public double length() {
		return Math.hypot(x, y);
	}

	public boolean equals(Object o) {
		if (!(o instanceof Vector2D))
			return false;
		Vector2D v = (Vector2D) o;
		return Double.doubleToLongBits(x) == Double.doubleToLongBits(v.x)
				&& Double.doubleToLongBits(y) == Double.doubleToLongBits(v.y);
	}

	public int hashCode() {
		return new Double(x).hashCode() ^ new Double(y).hashCode();
	}

	public Vector2D unit() {
		double invlen = 1.0 / length();
		return new Vector2D(x * invlen, y * invlen);
	}

	public double dot(Vector2D v) {
		return x * v.x + y * v.y;
	}

	public double angle() {
		return Math.atan2(y, x);
	}

	/**
	 * substract the passed vector <code>v</code> from the current vector, and return the result.
	 */
	public Vector2D substract(Vector2D v) {
		return new Vector2D(x - v.x, y - v.y);
	}

}
