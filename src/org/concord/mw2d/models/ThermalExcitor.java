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
class ThermalExcitor {

	private AtomicModel model;
	private Atom a1, a2;
	private double u1, u2; // speed of atom 1 and 2 in the contact direction before collision
	private double v1, v2; // speed of atom 1 and 2 in the contact direction after collision
	private double w1, w2; // speed of atom 1 and 2 in the tangential direction (no change)
	private double cos, sin; // unit vector pointing from atom 1's center to atom 2's center
	private int lastStep;
	private final double zeroPoint;

	ThermalExcitor(AtomicModel model) {
		this.model = model;
		zeroPoint = Math.pow(AtomicModel.alpha, 1.0 / 11.0);
	}

	void excite(Atom atom1, Atom atom2) {

		// give the pair a grace period to leave each other
		if (a1 == atom1 && a2 == atom2) {
			if (model.job.getIndexOfStep() - lastStep <= model.electronicDynamics.getInterval())
				return;
		}

		a1 = atom1;
		a2 = atom2;
		lastStep = model.job.getIndexOfStep();

		if (!a1.isExcitable() && !a2.isExcitable())
			return;

		if (a1.electrons.isEmpty() && a2.electrons.isEmpty()) // neither a1 nor a2 has electrons
			return;

		Electron e1 = a1.electrons.isEmpty() ? null : a1.electrons.get(0);
		Electron e2 = a2.electrons.isEmpty() ? null : a2.electrons.get(0);
		if (e1 == null && e2 != null && a2.isExcitable())
			return;
		if (e2 == null && e1 != null && a1.isExcitable())
			return;

		if (e1 != null && e2 != null) {
			if (Math.random() < 0.5) {
				if (!excite(e2))
					excite(e1);
			}
			else {
				if (!excite(e1))
					excite(e2);
			}
		}
		else if (e1 == null) {
			excite(e2);
		}
		else if (e2 == null) {
			excite(e1);
		}

	}

	private boolean excite(Electron e) {

		if (!e.readyToGo(model.getModelTime())) // the electron is just de-excited
			return false;

		Atom a = e.getAtom();
		EnergyLevel level = e.getEnergyLevel();
		ElectronicStructure es = model.getElement(a.id).getElectronicStructure();
		int n = es.getNumberOfEnergyLevels();
		int m = es.indexOf(level);
		if (m == -1 || m >= n - 1)
			return false;

		transformVelocities();

		double relativeKE = getRelativeKE();
		double excess = relativeKE + level.getEnergy();

		if (excess > 0 && !model.quantumRule.isIonizationDisallowed()) { // the energy affords ionization
			if (!loseElectron(e))
				return false;
		}
		else {
			// get the lowest energy level above the current that the relative KE can reach
			EnergyLevel excite = null;
			double energy = 0.0;
			int nmin = 0;
			for (int i = m + 1; i < n; i++) {
				excite = es.getEnergyLevel(i);
				energy = excite.getEnergy() - level.getEnergy();
				if (relativeKE < energy)
					break;
				nmin = i;
			}
			if (nmin == 0) // there is no energy level above that the relative KE can reach
				return false;
			// assuming that all the energy levels above have the same chance of getting the
			// excited electron, we randomly pick one.
			float prob = 1.0f / (nmin - m);
			double r = Math.random();
			for (int i = 0; i < nmin - m; i++) {
				if (r >= i * prob && r < (i + 1) * prob) {
					excite = es.getEnergyLevel(i + m + 1);
					e.setEnergyLevel(excite);
					energy = excite.getEnergy() - level.getEnergy();
					solve(energy);
					break;
				}
			}
		}

		transformVelocitiesBack();

		return true;

	}

	private boolean loseElectron(Electron e) {

		Atom atom = e.getAtom();

		// pop out the electron from the opposite side of the contact of impact
		int sign = atom == a1 ? -1 : 1;
		e.rx = atom.rx + zeroPoint * atom.sigma * cos * sign;
		e.ry = atom.ry + zeroPoint * atom.sigma * sin * sign;

		double energy = e.getEnergyLevel().getEnergy();

		// give the electron the minimum energy needed to leave the Coulombic binding of its original atom
		double coul = model.universe.getCoulombConstant()
				/ (model.universe.getDielectricConstant() * zeroPoint * atom.sigma);
		double ve = Math.sqrt(coul / (Electron.mass * MDModel.EV_CONVERTER));

		double m1 = a1.mass;
		double m2 = a2.mass;
		double K = (Electron.mass + m1) * u1 * u1 + m2 * u2 * u2 - Electron.mass * ve * ve + energy
				/ MDModel.EV_CONVERTER;
		double p = (Electron.mass + m1) * u1 + m2 * u2 - Electron.mass * ve;
		double s = K * (m1 + m2) - p * p;
		if (s < 0)
			return false;
		e.vx = ve * cos * sign;
		e.vy = ve * sin * sign;
		v1 = (p - Math.signum(u1 - u2) * Math.sqrt(m2 / m1 * s)) / (m1 + m2);
		v2 = (p + Math.signum(u1 - u2) * Math.sqrt(m1 / m2 * s)) / (m1 + m2);

		// detach the electron from the atom and make it a free electron
		atom.electrons.remove(e);
		e.setAtom(null);
		// positively charge the ion that is left behind
		atom.setCharge(1);
		model.addFreeElectron(e);

		return true;

	}

	private void transformVelocities() {
		cos = a2.rx - a1.rx;
		sin = a2.ry - a1.ry;
		double tmp = 1.0 / Math.hypot(cos, sin);
		cos *= tmp;
		sin *= tmp;
		u1 = a1.vx * cos + a1.vy * sin;
		u2 = a2.vx * cos + a2.vy * sin;
		w1 = a1.vy * cos - a1.vx * sin;
		w2 = a2.vy * cos - a2.vx * sin;
	}

	private void transformVelocitiesBack() {
		a1.vx = v1 * cos - w1 * sin;
		a1.vy = v1 * sin + w1 * cos;
		a2.vx = v2 * cos - w2 * sin;
		a2.vy = v2 * sin + w2 * cos;
	}

	private double getRelativeKE() {
		double du = u2 - u1;
		// the prefactor 0.5 doesn't show up here because of mass unit conversion.
		return du * du * a1.mass * a2.mass / (a1.mass + a2.mass) * MDModel.EV_CONVERTER;
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
