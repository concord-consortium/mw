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

import java.io.Serializable;

public class RBondState implements Serializable {

	private int iat1;
	private int iat2;
	private float strength = RBond.DEFAULT_STRENGTH;
	private float length = 2.0f;

	public RBondState() {
	}

	public RBondState(RBond rbond) {
		iat1 = rbond.getAtom1().getIndex();
		iat2 = rbond.getAtom2().getIndex();
		strength = rbond.getStrength();
		length = rbond.getLength();
	}

	public void setLength(float length) {
		this.length = length;
	}

	public float getLength() {
		return length;
	}

	public void setStrength(float strength) {
		this.strength = strength;
	}

	public float getStrength() {
		return strength;
	}

	public void setAtom1(int i) {
		iat1 = i;
	}

	public int getAtom1() {
		return iat1;
	}

	public void setAtom2(int i) {
		iat2 = i;
	}

	public int getAtom2() {
		return iat2;
	}

	public int hashCode() {
		return iat1 ^ iat2;
	}

	public boolean equals(Object o) {
		if (!(o instanceof RBondState))
			return false;
		RBondState s = (RBondState) o;
		return (s.iat1 == iat1 && s.iat2 == iat2) || (s.iat1 == iat2 && s.iat2 == iat1);
	}

}