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

import java.awt.Color;
import java.awt.Graphics2D;

import org.concord.modeler.Model;
import org.concord.mw2d.ViewAttribute;

public class Electron {

	volatile double rx;
	volatile double ry;
	volatile double vx;
	volatile double vy;
	volatile double ax;
	volatile double ay;
	double fx, fy;

	static double mass = 0.1;

	private MolecularModel model;
	private float enterStateTime;
	private EnergyLevel energyLevel;
	private Atom atom;

	/** create a free electron that is not bound to any atom */
	public Electron() {
	}

	/** create an electron that is bound to the specified atom */
	public Electron(Atom atom) {
		this();
		setAtom(atom);
	}

	/**
	 * set the atom that owns this electron. This method should be called when an electron is captured by an atom. In
	 * the case of ionization, setAtom(null) must be called to indicate that the electron is no longer bound to the
	 * atom.
	 */
	public void setAtom(Atom atom) {
		this.atom = atom;
	}

	/** get the atom that owns this electron. */
	public Atom getAtom() {
		return atom;
	}

	public void setModel(Model model) {
		if (model == null) {
			this.model = null;
			return;
		}
		if (!(model instanceof MolecularModel))
			throw new IllegalArgumentException("Model type error");
		this.model = (MolecularModel) model;
	}

	public Model getHostModel() {
		return model;
	}

	public boolean readyToGo(float t) {
		if (energyLevel == null)
			return true;
		return t - enterStateTime >= energyLevel.getLifetime() * (0.8 + 0.4 * Math.random());
	}

	/**
	 * Set the energy level for this electron. Null means a free electron. Also record the time when the electron enters
	 * its current state.
	 */
	public void setEnergyLevel(EnergyLevel level) {
		energyLevel = level;
		if (model != null)
			enterStateTime = model.getModelTime();
	}

	public EnergyLevel getEnergyLevel() {
		return energyLevel;
	}

	public int getEnergyLevelIndex() {
		ElectronicStructure es = model.getElement(atom.id).getElectronicStructure();
		return es.indexOf(energyLevel);
	}

	/* predict this electron's new state using 2nd order Taylor expansion */
	void predict(double dt, double dt2) {
		rx += vx * dt + ax * dt2;
		ry += vy * dt + ay * dt2;
		vx += ax * dt;
		vy += ay * dt;
	}

	/*
	 * @param half half of the time increment
	 */
	void correct(double half) {
		vx += half * (fx - ax);
		vy += half * (fy - ay);
		ax = fx;
		ay = fy;
		fx *= mass;
		fy *= mass;
	}

	/** @return the classic kinetic energy of this electron in electronic volt (eV) */
	public double getKineticEnergy() {
		// the prefactor 0.5 doesn't show up here because of mass unit conversion.
		return (vx * vx + vy * vy) * mass * MDModel.EV_CONVERTER;
	}

	public void render(Graphics2D g) {
		if (atom != null) {
			renderBoundState(g);
		}
		else {
			renderFreeState(g);
		}
	}

	// what to display if the electron is bound to an atom?
	private void renderBoundState(Graphics2D g) {

	}

	// render the free electron as a tiny dot
	private void renderFreeState(Graphics2D g) {
		g.setStroke(ViewAttribute.THIN);
		g.setColor(Color.white);
		g.fillOval((int) (rx - 1), (int) (ry - 1), 2, 2);
		g.setColor(Color.black);
		g.drawOval((int) (rx - 2), (int) (ry - 2), 4, 4);
	}

}