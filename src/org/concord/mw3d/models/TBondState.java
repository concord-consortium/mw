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

public class TBondState implements Serializable {

	private int iat1;
	private int iat2;
	private int iat3;
	private int iat4;
	private byte periodicity = 1;
	private float strength = TBond.DEFAULT_STRENGTH;
	private float angle;

	public TBondState() {
	}

	public TBondState(TBond tbond) {
		iat1 = tbond.getAtom1().getIndex();
		iat2 = tbond.getAtom2().getIndex();
		iat3 = tbond.getAtom3().getIndex();
		iat4 = tbond.getAtom4().getIndex();
		strength = tbond.getStrength();
		angle = tbond.getAngle();
		periodicity = tbond.getPeriodicity();
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

	public void setAtom3(int i) {
		iat3 = i;
	}

	public int getAtom3() {
		return iat3;
	}

	public void setAtom4(int i) {
		iat4 = i;
	}

	public int getAtom4() {
		return iat4;
	}

	public int hashCode() {
		return iat1 ^ iat2 ^ iat3 ^ iat4;
	}

	public boolean equals(Object o) {
		if (!(o instanceof TBondState))
			return false;
		TBondState s = (TBondState) o;
		if (s.iat1 != iat1 && s.iat1 != iat2 && s.iat1 != iat3 && s.iat1 != iat4)
			return false;
		if (s.iat2 != iat1 && s.iat2 != iat2 && s.iat2 != iat3 && s.iat2 != iat4)
			return false;
		if (s.iat3 != iat1 && s.iat3 != iat2 && s.iat3 != iat3 && s.iat3 != iat4)
			return false;
		if (s.iat4 != iat1 && s.iat4 != iat2 && s.iat4 != iat3 && s.iat4 != iat4)
			return false;
		return true;
	}

	public String toString() {
		return "<" + iat1 + "," + iat2 + "," + iat3 + "," + iat4 + ">";
	}

}