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
 * De-excite an excited electron. There are two mechanisms for de-excitation. The first is through emitting a photon
 * that has energy equal to the gap between the current excited state and a lower-lying state. The second is the
 * radiationless transition to a lower-lying state, meaning that no photon is emitted.
 * 
 * @author Charles Xie
 * 
 */
class ThermalDeexcitor {

	private AtomicModel model;
	private Atom a1, a2;
	private double u1, u2; // speed of atom 1 and 2 in the contact direction before collision
	private double v1, v2; // speed of atom 1 and 2 in the contact direction after collision
	private double w1, w2; // speed of atom 1 and 2 in the tangential direction (no change)
	private double dx, dy; // unit vector pointing from atom 1's center to atom 2's center
	private float rtProbability = 0.5f; // the probability of radiationless transition
	private Electron electron;

	ThermalDeexcitor(AtomicModel model) {
		this.model = model;
		try {
			rtProbability = model.quantumRule.getProbability(QuantumRule.RADIATIONLESS_TRANSITION);
		}
		catch (Exception e) {
			rtProbability = 0.5f;
		}
	}

	Electron getElectron() {
		return electron;
	}

	/*
	 * returns a photon with the energy equal to the excess energy. Returns null if the de-excitation happens through a
	 * radiationless transition, or the electron cannot be de-excited for various reasons (for example, the electron is
	 * already at the ground state).
	 */
	Photon deexcite(Atom atom1, Atom atom2) {

		a1 = atom1;
		a2 = atom2;

		if (!a1.isExcitable() && !a2.isExcitable())
			return null;

		if (a1.electrons.isEmpty() && a2.electrons.isEmpty()) // neither a1 nor a2 has electrons
			return null;

		Electron e1 = a1.electrons.isEmpty() ? null : a1.electrons.get(0);
		Electron e2 = a2.electrons.isEmpty() ? null : a2.electrons.get(0);
		if (e1 == null && (e2 != null && (a2.isExcitable() || isInGroundState(e2))))
			return null;
		if (e2 == null && (e1 != null && (a1.isExcitable() || isInGroundState(e1))))
			return null;

		if (e1 != null && e2 != null) {
			Photon p = null;
			if (Math.random() < 0.5) {
				p = deexcite(e1);
				if (p == null)
					p = deexcite(e2);
			}
			else {
				p = deexcite(e2);
				if (p == null)
					p = deexcite(e1);
			}
			return p;
		}
		else if (e1 == null) {
			return deexcite(e2);
		}
		else if (e2 == null) {
			return deexcite(e1);
		}

		return null;

	}

	private Photon deexcite(Electron e) {

		int m = getIndexOfEnergyLevel(e);
		if (m == 0)
			return null; // electron already in the ground state

		if (!e.readyToDeexcite(model.getModelTime())) // the electron is just excited
			return null;

		electron = e;

		// assume that the probability for the electron to transition to any lower state is equal
		float prob = 1.0f / m;
		double r1 = Math.random(); // the random number used to select a lower state
		double r2 = Math.random(); // the random number used to select a transition mechanism
		EnergyLevel level;
		float excess;
		Atom a = e.getAtom();
		ElectronicStructure es = model.getElement(a.id).getElectronicStructure();

		for (int i = 0; i < m; i++) {

			if (r1 >= i * prob && r1 < (i + 1) * prob) {

				level = es.getEnergyLevel(i);
				excess = level.getEnergy() - e.getEnergyLevel().getEnergy();
				e.setEnergyLevel(level);

				/* emit a photon */
				if (r2 > rtProbability)
					return new Photon((float) a.rx, (float) a.ry, -excess / MDModel.PLANCK_CONSTANT);

				/* the excess energy is converted into the kinetic energy of the atoms */
				transformVelocities();
				solve(excess);
				transformVelocitiesBack();

				break;

			}
		}

		return null;

	}

	private int getIndexOfEnergyLevel(Electron e) {
		Atom a = e.getAtom();
		ElectronicStructure es = model.getElement(a.id).getElectronicStructure();
		return es.indexOf(e.getEnergyLevel());
	}

	private boolean isInGroundState(Electron e) {
		return getIndexOfEnergyLevel(e) <= 0;
	}

	private void transformVelocities() {
		dx = a2.rx - a1.rx;
		dy = a2.ry - a1.ry;
		double tmp = 1.0 / Math.hypot(dx, dy);
		dx *= tmp;
		dy *= tmp;
		u1 = a1.vx * dx + a1.vy * dy;
		u2 = a2.vx * dx + a2.vy * dy;
		w1 = a1.vy * dx - a1.vx * dy;
		w2 = a2.vy * dx - a2.vx * dy;
	}

	private void transformVelocitiesBack() {
		a1.vx = v1 * dx - w1 * dy;
		a1.vy = v1 * dy + w1 * dx;
		a2.vx = v2 * dx - w2 * dy;
		a2.vy = v2 * dy + w2 * dx;
	}

	// solve v1 and v2 according to the conservation of momentum and energy
	private void solve(double delta) {
		double m1 = a1.mass;
		double m2 = a2.mass;
		// convert the energy unit into the default unit for delta in the following
		double J = m1 * u1 * u1 + m2 * u2 * u2 - delta / MDModel.EV_CONVERTER;
		double g = m1 * u1 + m2 * u2;
		v1 = (g - Math.sqrt(m2 / m1 * (J * (m1 + m2) - g * g))) / (m1 + m2);
		v2 = (g + Math.sqrt(m1 / m2 * (J * (m1 + m2) - g * g))) / (m1 + m2);
	}

}
