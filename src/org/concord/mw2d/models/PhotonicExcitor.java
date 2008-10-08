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
class PhotonicExcitor {

	private final static float ENERGY_GAP_TOLL = 0.05f;
	private Photon photon;
	private Atom atom;
	private AtomicModel model;

	PhotonicExcitor(AtomicModel model) {
		this.model = model;
	}

	Photon interact(Photon photon, Atom atom) {
		this.photon = photon;
		this.atom = atom;
		float prob;
		try {
			prob = model.quantumRule.getProbability(QuantumRule.STIMULATED_EMISSION);
		}
		catch (Exception e) {
			prob = 0.5f;
		}
		if (Math.random() < prob) {
			float preciseEnergy = stimulatedEmission();
			if (preciseEnergy > 0) {
				Photon p2 = new Photon(photon);
				p2.setY(p2.getY() + 4);
				p2.setEnergy(preciseEnergy);
				return p2;
			}
		}
		else {
			float preciseEnergy = excite();
			if (preciseEnergy > 0) {
				photon.setEnergy(preciseEnergy);
				return photon;
			}
		}
		return null;
	}

	/*
	 * Excite an electron, if the photonic energy matches the energy gap between the current state and a higher state.
	 * Photonic excitation happens as soon as the photon impacts the atom, regardless of the lifetime of the current
	 * energy level.
	 */
	private float excite() {

		if (atom.electrons.isEmpty())
			return 0;

		Electron e = atom.electrons.get(0);
		EnergyLevel level = e.getEnergyLevel();
		ElectronicStructure es = model.getElement(atom.id).getElectronicStructure();
		int n = es.getNumberOfEnergyLevels();
		int m = es.indexOf(level);
		if (m == -1 || m >= n - 1)
			return 0;

		double excess = photon.getEnergy() + level.getEnergy();
		if (excess > 0) {
			loseElectron(e, excess);
		}
		else {
			EnergyLevel excite;
			for (int i = m + 1; i < n; i++) {
				excite = es.getEnergyLevel(i);
				if (Math.abs(excite.getEnergy() - level.getEnergy() - photon.getEnergy()) < ENERGY_GAP_TOLL) {
					e.setEnergyLevel(excite);
					return excite.getEnergy() - level.getEnergy(); // return the precise energy
				}
			}
		}

		return 0;

	}

	/*
	 * Stimulated emission happens as soon as the photon hits the atom, regardless of the lifetime set for the current
	 * electronic state.
	 */
	private float stimulatedEmission() {
		if (atom.electrons.isEmpty())
			return 0;
		Electron e = atom.electrons.get(0);
		EnergyLevel level = e.getEnergyLevel();
		ElectronicStructure es = model.getElement(atom.id).getElectronicStructure();
		int m = es.indexOf(level);
		if (m <= 0)
			return 0;
		EnergyLevel state;
		for (int i = 0; i < m; i++) {
			state = es.getEnergyLevel(i);
			if (Math.abs(level.getEnergy() - state.getEnergy() - photon.getEnergy()) < ENERGY_GAP_TOLL) {
				e.setEnergyLevel(state);
				return level.getEnergy() - state.getEnergy(); // return the precise energy
			}
		}
		return 0;
	}

	private void loseElectron(Electron e, double excess) {
		// place the electron just barely inside the edge of the atom to prevent it from overlapping
		// with another atom immediately upon releasing
		double cos = Math.cos(photon.getAngle());
		double sin = Math.sin(photon.getAngle());
		e.rx = atom.rx + 0.55 * atom.sigma * cos;
		e.ry = atom.ry + 0.55 * atom.sigma * sin;

		double v = Math.sqrt(excess / (MDModel.EV_CONVERTER * Electron.mass));
		e.vx = atom.vx + v * cos;
		e.vy = atom.vy + v * sin;
		// detach the electron from the atom and make it a free electron
		e.setAtom(null);
		atom.electrons.remove(e);

		// positively charge the ion that is left behind
		atom.setCharge(1);
		model.addFreeElectron(e);
	}

}
