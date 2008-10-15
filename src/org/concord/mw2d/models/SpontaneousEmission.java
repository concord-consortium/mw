/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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

/**
 * @author Charles Xie
 * 
 */
class SpontaneousEmission {

	private AtomicModel model;

	SpontaneousEmission(AtomicModel model) {
		this.model = model;
	}

	Photon emit(Atom atom) {

		Electron e = atom.getElectron(0);
		ElectronicStructure es = model.getElement(atom.id).getElectronicStructure();
		int m = es.indexOf(e.getEnergyLevel());
		if (m == 0)
			return null; // electron already in the ground state

		if (!e.readyToGo(model.getModelTime())) // the electron is just excited
			return null;

		// assume that the probability for the electron to transition to any lower state is equal
		float prob = 1.0f / m;
		double r1 = Math.random(); // the random number used to select a lower state
		double r2 = Math.random(); // the random number used to select a transition mechanism
		EnergyLevel level;
		float excess;

		for (int i = 0; i < m; i++) {

			if (r1 >= i * prob && r1 < (i + 1) * prob) {

				level = es.getEnergyLevel(i);
				excess = e.getEnergyLevel().getEnergy() - level.getEnergy();

				/* emit a photon */
				if (r2 > model.quantumRule.getProbability(QuantumRule.RADIATIONLESS_TRANSITION)) {
					e.setEnergyLevel(level);
					return new Photon((float) atom.rx, (float) atom.ry, excess / MDModel.PLANCK_CONSTANT);
				}

				break;

			}
		}

		return null;

	}

}
