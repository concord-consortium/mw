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

import org.concord.modeler.Model;

public class Electron {

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

}