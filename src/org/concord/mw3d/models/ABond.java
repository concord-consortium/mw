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
 * the angle-bending potential is 0.5*strength*(theta-angle)^2
 * 
 * See AMBER force field: http://en.wikipedia.org/wiki/AMBER
 * 
 * @author Charles Xie
 */

public class ABond {

	final static float DEFAULT_STRENGTH = 50.0f;

	private Atom atom1; // end atom
	private Atom atom2; // middle atom (shared by two flanking radial bonds)
	private Atom atom3; // end atom
	private float strength = DEFAULT_STRENGTH;
	private float angle = (float) Math.PI;
	private boolean selected;

	/** We tend to use this constructor in deserialization. */
	public ABond(Atom atom1, Atom atom2, Atom atom3) {
		if (atom1 == null || atom2 == null || atom3 == null)
			throw new IllegalArgumentException("Atoms cannot be null.");
		this.atom1 = atom1;
		this.atom2 = atom2;
		this.atom3 = atom3;
		atom1.addABond(this);
		atom2.addABond(this);
		atom3.addABond(this);
	}

	/** We use this constructor when creating a new ABond from the GUI. */
	public ABond(RBond r1, RBond r2) {
		if (r1 == null || r2 == null || r1.equals(r2))
			throw new IllegalArgumentException("Cannot make an angular bond.");
		atom2 = RBond.getSharedAtom(r1, r2);
		if (atom2 == null)
			throw new IllegalArgumentException(
					"cannot make an angular bond between two radial bonds that are not joined.");
		if (r1.getAtom1() == atom2) {
			atom1 = r1.getAtom2();
		}
		else {
			atom1 = r1.getAtom1();
		}
		if (r2.getAtom1() == atom2) {
			atom3 = r2.getAtom2();
		}
		else {
			atom3 = r2.getAtom1();
		}
		atom1.addABond(this);
		atom2.addABond(this);
		atom3.addABond(this);
		angle = (float) getAngle(atom1, atom2, atom3);
	}

	Atom getThirdAtom(Atom a, Atom b) {
		if (atom1 != a && atom1 != b)
			return atom1;
		if (atom2 != a && atom2 != b)
			return atom2;
		return atom3;
	}

	public static Atom[] getSharedAtom(ABond a1, ABond a2) {
		if (a1 == null || a2 == null || a1.equals(a2))
			throw new IllegalArgumentException("ABond error");
		Atom[] at = new Atom[2];
		int i = 0;
		if (a1.atom1 == a2.atom1 || a1.atom1 == a2.atom2 || a1.atom1 == a2.atom3)
			at[i++] = a1.atom1;
		if (a1.atom2 == a2.atom1 || a1.atom2 == a2.atom2 || a1.atom2 == a2.atom3)
			at[i++] = a1.atom2;
		if (a1.atom3 == a2.atom1 || a1.atom3 == a2.atom2 || a1.atom3 == a2.atom3)
			at[i++] = a1.atom3;
		return at;
	}

	/** return the angle a1-a2-a3 (a2 is in the middle) */
	public static double getAngle(Atom a1, Atom a2, Atom a3) {
		float x21 = a2.rx - a1.rx;
		float y21 = a2.ry - a1.ry;
		float z21 = a2.rz - a1.rz;
		float x23 = a2.rx - a3.rx;
		float y23 = a2.ry - a3.ry;
		float z23 = a2.rz - a3.rz;
		float xx = y21 * z23 - z21 * y23;
		float yy = z21 * x23 - x21 * z23;
		float zz = x21 * y23 - y21 * x23;
		return Math.abs(Math.atan2(Math.sqrt(xx * xx + yy * yy + zz * zz), x21 * x23 + y21 * y23 + z21 * z23));
	}

	public void setSelected(boolean b) {
		selected = b;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

	public float getAngle() {
		return angle;
	}

	public void setStrength(float strength) {
		this.strength = strength;
	}

	public float getStrength() {
		return strength;
	}

	/** return true if the atom of the specified index is involved in this angular bond. */
	public boolean contains(int index) {
		return atom1.index == index || atom2.index == index || atom3.index == index;
	}

	public boolean contains(Atom a1, Atom a2) {
		if (a1 != atom1 && a1 != atom2 && a1 != atom3)
			return false;
		if (a2 != atom1 && a2 != atom2 && a2 != atom3)
			return false;
		return true;
	}

	public Atom getAtom1() {
		return atom1;
	}

	public Atom getAtom2() {
		return atom2;
	}

	public Atom getAtom3() {
		return atom3;
	}

	public int hashCode() {
		return atom1.hashCode() ^ atom2.hashCode() ^ atom3.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof ABond))
			return false;
		ABond a = (ABond) o;
		if (a.atom1 != atom1 && a.atom1 != atom2 && a.atom1 != atom3)
			return false;
		if (a.atom2 != atom1 && a.atom2 != atom2 && a.atom2 != atom3)
			return false;
		if (a.atom3 != atom1 && a.atom3 != atom2 && a.atom3 != atom3)
			return false;
		return true;
	}

	public String toString() {
		return "angle " + atom1.index + " " + atom2.index + " " + atom3.index + " " + strength + " "
				+ XyzWriter.FORMAT.format(angle);
	}

}