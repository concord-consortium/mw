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
 * the torsional potential is 0.5*strength*[1+cos(n*omega-gamma)] (as per AMBER force field).
 * 
 * Is omega just the angle between the a1-a2 vector and the a3-a4 vector, or the diheral angle between the a1-a2-a3
 * plane and a2-a3-a4 plane? The former case is faster to compute.
 * 
 * @author Charles Xie
 */

public class TBond {

	// torsional bonds are believed to be quite gentle. So let's use a small number.
	final static float DEFAULT_STRENGTH = 1.0f;

	private Atom atom1; // end atom
	private Atom atom2; // middle atom
	private Atom atom3; // middel atom
	private Atom atom4; // end atom
	private byte periodicity = 1; // n
	private float strength = DEFAULT_STRENGTH;
	private float angle; // gamma

	/** We tend to use this constructor in deserialization. */
	public TBond(Atom atom1, Atom atom2, Atom atom3, Atom atom4) {
		if (atom1 == null || atom2 == null || atom3 == null || atom4 == null)
			throw new IllegalArgumentException("Atoms cannot be null.");
		this.atom1 = atom1;
		this.atom2 = atom2;
		this.atom3 = atom3;
		this.atom4 = atom4;
		atom1.addTBond(this);
		atom2.addTBond(this);
		atom3.addTBond(this);
		atom4.addTBond(this);
	}

	/** We use this constructor when creating a new ABond from the GUI. */
	public TBond(ABond a1, ABond a2) {
		if (a1 == null || a2 == null || a1.equals(a2) || a1.getAtom2() == a2.getAtom2())
			throw new IllegalArgumentException("Cannot make a torsional bond.");
		Atom[] at = ABond.getSharedAtom(a1, a2);
		if (at == null)
			throw new IllegalArgumentException("Cannot make a torsional bond.");
		if (at[0] == null || at[1] == null)
			throw new IllegalArgumentException(
					"cannot make a torsional bond between two angular bonds that are not joined.");
		atom2 = at[0];
		atom3 = at[1];
		Atom a = a1.getThirdAtom(atom2, atom3);
		Atom b = a2.getThirdAtom(atom2, atom3);
		if (a.isBonded(atom2)) {
			atom1 = a;
			atom4 = b;
		}
		else {
			atom1 = b;
			atom4 = a;
		}
		atom1.addTBond(this);
		atom2.addTBond(this);
		atom3.addTBond(this);
		atom4.addTBond(this);
		angle = (float) getAngle(atom1, atom2, atom3, atom4) * periodicity;
	}

	/** return the angle between a2-a1 vector and a3-a4 vector (a2 and a3 are in the middle) */
	public static double getAngle(Atom a1, Atom a2, Atom a3, Atom a4) {
		float x21 = a2.rx - a1.rx;
		float y21 = a2.ry - a1.ry;
		float z21 = a2.rz - a1.rz;
		float x34 = a3.rx - a4.rx;
		float y34 = a3.ry - a4.ry;
		float z34 = a3.rz - a4.rz;
		float xx = y21 * z34 - z21 * y34;
		float yy = z21 * x34 - x21 * z34;
		float zz = x21 * y34 - y21 * x34;
		return Math.abs(Math.atan2(Math.sqrt(xx * xx + yy * yy + zz * zz), x21 * x34 + y21 * y34 + z21 * z34));
	}

	// avoid trigonometric calculations
	float cosnx(float cosx, float sinx) {
		switch (periodicity) {
		case 1:
			return cosx;
		case 2:
			return cosx * cosx - sinx * sinx;
		case 3:
			return cosx * cosnx(cosx, sinx) - sinx * sinnx(cosx, sinx);
		case 4:
			float cos2x = cosnx(cosx, sinx);
			float sin2x = sinnx(cosx, sinx);
			return cos2x * cos2x - sin2x * sin2x;
		}
		throw new RuntimeException("cannot compute for n=" + periodicity);
	}

	// avoid trigonometric calculations
	float sinnx(float cosx, float sinx) {
		switch (periodicity) {
		case 1:
			return sinx;
		case 2:
			return cosx * sinx * 2;
		case 3:
			return cosx * sinnx(cosx, sinx) + sinx * cosnx(cosx, sinx);
		case 4:
			return 2 * cosnx(cosx, sinx) * sinnx(cosx, sinx);
		}
		throw new RuntimeException("cannot compute for n=" + periodicity);
	}

	public void setPeriodicity(byte i) {
		periodicity = i;
	}

	public byte getPeriodicity() {
		return periodicity;
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

	public boolean contains(Atom a1, Atom a2) {
		if (a1 != atom1 && a1 != atom2 && a1 != atom3 && a1 != atom4)
			return false;
		if (a2 != atom1 && a2 != atom2 && a2 != atom3 && a2 != atom4)
			return false;
		return true;
	}

	public boolean contains(Atom a1, Atom a2, Atom a3) {
		if (a1 != atom1 && a1 != atom2 && a1 != atom3 && a1 != atom4)
			return false;
		if (a2 != atom1 && a2 != atom2 && a2 != atom3 && a2 != atom4)
			return false;
		if (a3 != atom1 && a3 != atom2 && a3 != atom3 && a3 != atom4)
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

	public Atom getAtom4() {
		return atom4;
	}

	public int hashCode() {
		return atom1.hashCode() ^ atom2.hashCode() ^ atom3.hashCode() ^ atom4.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof TBond))
			return false;
		TBond a = (TBond) o;
		if (a.atom1 != atom1 && a.atom1 != atom2 && a.atom1 != atom3 && a.atom1 != atom4)
			return false;
		if (a.atom2 != atom1 && a.atom2 != atom2 && a.atom2 != atom3 && a.atom2 != atom4)
			return false;
		if (a.atom3 != atom1 && a.atom3 != atom2 && a.atom3 != atom3 && a.atom3 != atom4)
			return false;
		if (a.atom4 != atom1 && a.atom4 != atom2 && a.atom4 != atom3 && a.atom4 != atom4)
			return false;
		return true;
	}

	public String toString() {
		return "torsion " + atom1.index + " " + atom2.index + " " + atom3.index + " " + atom4.index + " " + strength
				+ " " + XyzWriter.FORMAT.format(angle);
	}

}