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

package org.concord.mw2d.models;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Arrays;

/**
 * <p>
 * A random walk on a lattice. This class uses short integers for the coordinates because we cannot go too far. When a
 * walk is long, the probability of not finding a path in a Monte Carlo search becomes low.
 * </p>
 * 
 * <p>
 * A walk is denoted by the directions it takes at each node. For example, <code>flflrfrfrl</code> stands for a
 * ten-step walk. This string, however, does not account for the initial step. The initial step does not matter for the
 * reason of symmetry, i.e. it does not really matter which direction a walk starts, as the whole path is rotationally
 * symmetric about the starting point.
 * </p>
 * 
 * <p>
 * The direction string of a walk is good for path enumeration. It does not, however, directly give the information
 * about the coordinates of the path, which is what we need to form a polymer. Therefore, we need to construct a
 * geometric path for a walk. This involves deciding the origin and the initial step, which is not taken into account in
 * a characteristic string.
 * </p>
 * 
 * <p>
 * Coordinate-step table:
 * 
 * <pre>
 *             f    l    r
 *        N   y++  x--  x++
 *        S   y--  x++  x--
 *        E   x++  y++  y--
 *        W   x--  y--  y++
 * </pre>
 * 
 * @author Charles Xie
 */

class Walk {

	final static byte FORWARD = 0;
	final static byte LEFT = 1;
	final static byte RIGHT = 2;

	private final static byte NORTH = 11;
	private final static byte SOUTH = 12;
	private final static byte EAST = 13;
	private final static byte WEST = 14;

	/*
	 * store the coordinates of the origin of this walk and the first node (which the initial step arrives)
	 */
	private short x0, y0, x1, y1;

	/* store the coordinates of nodes from the second one */
	private short[] x, y;

	/*
	 * store the path of this walk by recording its walking directions in forward(f), left(l), and right(r) notation
	 */
	private byte[] path;

	public Walk(short n) {
		path = new byte[n];
		x = new short[n];
		y = new short[n];
		double ran = Math.random();
		if (ran < 0.25) {
			x1 = 1;
		}
		else if (ran >= 0.25 && ran < 0.5) {
			x1 = -1;
		}
		else if (ran >= 0.5 && ran < 0.75) {
			y1 = 1;
		}
		else {
			y1 = -1;
		}
	}

	protected Walk(byte[] b) {
		if (b == null)
			throw new IllegalArgumentException("null input");
		path = new byte[b.length];
		System.arraycopy(b, 0, path, 0, path.length);
	}

	public void setOrigin(short x0, short y0) {
		this.x0 = x0;
		this.y0 = y0;
	}

	public short getOriginX() {
		return x0;
	}

	public short getOriginY() {
		return y0;
	}

	public void setFirstNode(short x1, short y1) {
		if (!isNearestNeighbor(x1, y1, x0, y0))
			throw new IllegalArgumentException("First node must be a nearest neighbor of origin!");
		this.x1 = x1;
		this.y1 = y1;
	}

	public short getFirstNodeX() {
		return x1;
	}

	public short getFirstNodeY() {
		return y1;
	}

	public short[] getXArray() {
		return x;
	}

	public short[] getYArray() {
		return y;
	}

	public void setStep(short i, byte b) {
		if (i < 0 || i >= path.length)
			throw new IllegalArgumentException("Step number exceeds array size");
		path[i] = b;
		byte direction;
		if (i == 0) {
			direction = getDirection(x0, y0, x1, y1);
			x[0] = x1;
			y[0] = y1;
		}
		else if (i == 1) {
			direction = getDirection(x1, y1, x[0], y[0]);
			x[1] = x[0];
			y[1] = y[0];
		}
		else {
			direction = getDirection(x[i - 2], y[i - 2], x[i - 1], y[i - 1]);
			x[i] = x[i - 1];
			y[i] = y[i - 1];
		}
		switch (direction) {
		case NORTH:
			switch (b) {
			case FORWARD:
				y[i]++;
				break;
			case LEFT:
				x[i]--;
				break;
			case RIGHT:
				x[i]++;
				break;
			}
			break;
		case SOUTH:
			switch (b) {
			case FORWARD:
				y[i]--;
				break;
			case LEFT:
				x[i]++;
				break;
			case RIGHT:
				x[i]--;
				break;
			}
			break;
		case EAST:
			switch (b) {
			case FORWARD:
				x[i]++;
				break;
			case LEFT:
				y[i]++;
				break;
			case RIGHT:
				y[i]--;
				break;
			}
			break;
		case WEST:
			switch (b) {
			case FORWARD:
				x[i]--;
				break;
			case LEFT:
				y[i]--;
				break;
			case RIGHT:
				y[i]++;
				break;
			}
			break;
		}
	}

	/** test if the n-th step results in self-intersection */
	public boolean isSelfIntersected(short n) {
		if (n < 0)
			throw new IllegalArgumentException("n<0");
		if (n >= path.length)
			throw new IllegalArgumentException("overflow");
		if (n == 0)
			return x[0] == x0 && y[0] == y0;
		if (n == 1)
			return x[1] == x1 && y[1] == y1;
		if (x[n] == x0 && y[n] == y0)
			return true;
		if (x[n] == x1 && y[n] == y1)
			return true;
		for (short i = 0; i < n - 1; i++) {
			if (x[n] == x[i] && y[n] == y[i])
				return true;
		}
		return false;
	}

	/**
	 * test if the n-step walks out of the boundary. If a null argument is passed as the bound, return false.
	 */
	public boolean isOutOfBound(short n, Dimension bound) {
		if (n < 0)
			throw new IllegalArgumentException("n<0");
		if (n >= path.length)
			throw new IllegalArgumentException("overflow");
		if (bound == null)
			return false;
		if (x[n] < -bound.width / 2 || x[n] > bound.width / 2 || y[n] < -bound.height / 2 || y[n] > bound.height / 2)
			return true;
		return false;
	}

	private boolean isNearestNeighbor(short x1, short y1, short x2, short y2) {
		return Math.abs(x1 - x2) + Math.abs(y1 - y2) == 1;
	}

	private byte getDirection(short x1, short y1, short x2, short y2) {
		if (!isNearestNeighbor(x1, y1, x2, y2))
			throw new IllegalArgumentException("Not nearest neighbor error: (" + x1 + "," + y1 + ")(" + x2 + "," + y2
					+ ")");
		if (x2 > x1)
			return EAST;
		else if (x2 < x1)
			return WEST;
		else if (y2 > y1)
			return NORTH;
		return SOUTH;
	}

	public byte getStep(short i) {
		if (i < 0 || i >= path.length)
			throw new IllegalArgumentException("Step number exceeds array size");
		return path[i];
	}

	public void setLength(short length) {
		if (path == null) {
			path = new byte[length];
		}
		else {
			byte[] oldPath = new byte[path.length];
			System.arraycopy(path, 0, oldPath, 0, path.length);
			path = new byte[length];
			System.arraycopy(oldPath, 0, path, 0, Math.min(path.length, oldPath.length));
		}
	}

	public short getLength() {
		return path == null ? 0 : (short) path.length;
	}

	/**
	 * paint this random walk on a graphics with the given scale. The origin and initial step are not drawn.
	 */
	public void paint(Graphics g, float scale) {
		g.setColor(Color.black);
		int n = x.length;
		for (short i = 0; i < n - 1; i++) {
			g.drawLine((int) (x[i] * scale), (int) (y[i] * scale), (int) (x[i + 1] * scale), (int) (y[i + 1] * scale));
		}
	}

	/**
	 * two walks are considered equal if their characteristic strings are identical
	 */
	public boolean equals(Object o) {
		if (!(o instanceof Walk))
			return false;
		return Arrays.equals(path, ((Walk) o).path);
	}

	public int hashCode() {
		return path.hashCode();
	}

	/** return the characteristic string of this walk */
	public String toString() {
		if (path == null)
			return null;
		char[] c = new char[path.length];
		for (short i = 0; i < path.length; i++) {
			switch (path[i]) {
			case FORWARD:
				c[i] = 'f';
				break;
			case LEFT:
				c[i] = 'l';
				break;
			case RIGHT:
				c[i] = 'r';
				break;
			}
		}
		return new String(c);
	}

}