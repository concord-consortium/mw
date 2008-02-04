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

import java.io.Serializable;

/** This class models a quantum energy level. */

public class EnergyLevel implements Serializable, Comparable {

	public final static int SHORT_LIFETIME = 250;
	public final static int MEDIUM_LIFETIME = 2500;
	public final static int LONG_LIFETIME = 25000;

	private int lifetime = SHORT_LIFETIME;
	private int degeneracy = 2;
	private float energy = -1.0f;

	public EnergyLevel() {
	}

	public EnergyLevel(float energy) {
		this();
		setEnergy(energy);
	}

	public synchronized void setLifetime(int i) {
		lifetime = i;
	}

	public synchronized int getLifetime() {
		return lifetime;
	}

	public synchronized void setDegeneracy(int i) {
		degeneracy = i;
	}

	public synchronized int getDegeneracy() {
		return degeneracy;
	}

	public synchronized void setEnergy(float energy) {
		this.energy = energy;
	}

	public synchronized float getEnergy() {
		return energy;
	}

	public String toString() {
		return "Level: [d=" + degeneracy + ", e=" + energy + "]";
	}

	/** compare the energy of this energy level with the input. */
	public int compareTo(Object o) {
		if (!(o instanceof EnergyLevel))
			throw new IllegalArgumentException("cannot compare");
		if (energy < ((EnergyLevel) o).energy)
			return -1;
		if (energy > ((EnergyLevel) o).energy)
			return 1;
		return 0;
	}

}