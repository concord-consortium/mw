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
class ThreeBodyRecombination {

	private AtomicModel model;
	private Atom a1, a2;
	private Electron e;
	private double u1, u2; // speed of atom 1 and 2 in the contact direction before collision
	private double v1, v2; // speed of atom 1 and 2 in the contact direction after collision
	private double w1, w2; // speed of atom 1 and 2 in the tangential direction (no change)
	private double ue, we; // speed components of the electron in the two directions
	private double cos, sin; // unit vector pointing from atom 1's center to atom 2's center
	private int lastStep;

	ThreeBodyRecombination(AtomicModel model) {
		this.model = model;
	}

	boolean recombine(Atom atom, Electron electron) {
		if (!atom.isExcitable())
			return false;
		// give the pair a grace period to leave each other
		if (a1 == atom && e == electron) {
			if (model.job.getIndexOfStep() - lastStep <= model.electronicDynamics.getInterval())
				return false;
		}
		lastStep = model.job.getIndexOfStep();

		a1 = atom; // the electron will recombine with a1 with the assistance of a2
		e = electron;
		double rxij, ryij, rijsq, sig;
		for (int i = 0; i < model.numberOfAtoms; i++) {
			a2 = model.atom[i];
			if (a1 == a2)
				continue;
			rxij = a1.rx - a2.rx;
			ryij = a1.ry - a2.ry;
			rijsq = rxij * rxij + ryij * ryij;
			sig = 0.55 * (a1.sigma + a2.sigma);
			sig *= sig;
			if (rijsq < sig) {
				break;
			}
			a2 = null;
		}
		System.out.println(a1 + ">>>" + a2);
		if (a2 == null)
			return false;
		transformVelocities();
		double excess = gainElectron();
		if (!solve(excess))
			return false;
		transformVelocitiesBack();
		return true;
	}

	private double gainElectron() {
		ElectronicStructure es = model.getElement(a1.id).getElectronicStructure();
		// always put the electron at the highest energy level
		EnergyLevel top = es.getEnergyLevel(es.getNumberOfEnergyLevels() - 1);
		e.setEnergyLevel(top);
		// associate the electron with this atom
		e.setAtom(a1);
		a1.electrons.add(e);
		// neutralize the atom
		a1.setCharge(0);
		return top.getEnergy();
	}

	private void transformVelocities() {
		cos = a2.rx - a1.rx;
		sin = a2.ry - a1.ry;
		double tmp = 1.0 / Math.hypot(cos, sin);
		cos *= tmp;
		sin *= tmp;
		u1 = a1.vx * cos + a1.vy * sin;
		u2 = a2.vx * cos + a2.vy * sin;
		ue = e.vx * cos + e.vy * sin;
		w1 = a1.vy * cos - a1.vx * sin;
		w2 = a2.vy * cos - a2.vx * sin;
		we = e.vy * cos - e.vx * sin;
	}

	private void transformVelocitiesBack() {
		a1.vx = v1 * cos - w1 * sin;
		a1.vy = v1 * sin + w1 * cos;
		a2.vx = v2 * cos - w2 * sin;
		a2.vy = v2 * sin + w2 * cos;
	}

	// solve v1 and v2 according to the conservation of momentum and energy
	private boolean solve(double delta) {
		double m1 = a1.mass;
		double m2 = a2.mass;
		// convert the energy unit into the default unit for delta in the following
		double L = Electron.mass * ue * ue + m1 * u1 * u1 + m2 * u2 * u2 - delta / MDModel.EV_CONVERTER;
		double q = Electron.mass * ue + m1 * u1 + m2 * u2;
		double s = L * (m1 + m2 + Electron.mass) - q * q;
		if (s < 0)
			return false;
		v1 = (q - Math.signum(u2 - u1) * Math.sqrt(m2 / (m1 + Electron.mass) * s)) / (m1 + m2 + Electron.mass);
		v2 = (q + Math.signum(u2 - u1) * Math.sqrt((m1 + Electron.mass) / m2 * s)) / (m1 + m2 + Electron.mass);
		return true;
	}

}
