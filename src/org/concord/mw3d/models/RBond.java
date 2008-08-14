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

package org.concord.mw3d.models;

/**
 * The bond-stretching potential is 0.5*strength*(r-length)^2
 * 
 * See AMBER force field: http://en.wikipedia.org/wiki/AMBER
 * 
 * @author Charles Xie
 */

public class RBond {

	final static float DEFAULT_STRENGTH = 4.5f;

	Atom atom1;
	Atom atom2;
	private byte order = 1;
	private float strength = DEFAULT_STRENGTH;
	private float length = 2.0f;
	private boolean selected;
	private boolean visible = true;

	public RBond(Atom atom1, Atom atom2) {
		if (atom1 == null || atom2 == null)
			throw new IllegalArgumentException("Atoms cannot be null.");
		this.atom1 = atom1;
		this.atom2 = atom2;
		atom1.addRBond(this);
		atom2.addRBond(this);
	}

	public static Atom getSharedAtom(RBond r1, RBond r2) {
		if (r1 == null || r2 == null || r1.equals(r2))
			throw new IllegalArgumentException("RBond error");
		if (r1.atom1 == r2.atom1 || r1.atom1 == r2.atom2)
			return r1.atom1;
		if (r1.atom2 == r2.atom1 || r1.atom2 == r2.atom2)
			return r1.atom2;
		return null;
	}

	/** return true if the atom of the specified type is involved in this radial bond. */
	public boolean containsElement(String symbol) {
		return atom1.getSymbol().equalsIgnoreCase(symbol) || atom2.getSymbol().equalsIgnoreCase(symbol);
	}

	/** return true if the atom of the specified index is involved in this radial bond. */
	public boolean contains(int index) {
		return atom1.index == index || atom2.index == index;
	}

	public void setSelected(boolean b) {
		selected = b;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setVisible(boolean b) {
		visible = b;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setLength(float length) {
		this.length = length;
	}

	public float getLength() {
		return length;
	}

	public float getLength(int frame) {
		if (frame < 0)
			return (float) Math.sqrt(atom1.distanceSquare(atom2));
		float dx = atom1.rQ.getQueue1().getData(frame) - atom2.rQ.getQueue1().getData(frame);
		float dy = atom1.rQ.getQueue2().getData(frame) - atom2.rQ.getQueue2().getData(frame);
		float dz = atom1.rQ.getQueue3().getData(frame) - atom2.rQ.getQueue3().getData(frame);
		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public void setStrength(float strength) {
		this.strength = strength;
	}

	public float getStrength() {
		return strength;
	}

	public Atom getAtom1() {
		return atom1;
	}

	public Atom getAtom2() {
		return atom2;
	}

	public byte getOrder() {
		return order;
	}

	public int hashCode() {
		return atom1.hashCode() ^ atom2.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof RBond))
			return false;
		RBond r = (RBond) o;
		return (r.atom1 == atom1 && r.atom2 == atom2) || (r.atom1 == atom2 && r.atom2 == atom1);
	}

	public String toString() {
		return "bond " + atom1.index + " " + atom2.index + " " + strength + " " + XyzWriter.FORMAT.format(length);
	}

}