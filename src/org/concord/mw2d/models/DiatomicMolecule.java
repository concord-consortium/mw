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

import java.util.ArrayList;
import java.util.List;

public final class DiatomicMolecule extends Molecule {

	private int idOfA = 3, idOfB = 1;
	private double bondLength = 20.0;
	private double bondStrength = 0.2;

	public DiatomicMolecule(int typeA, int typeB, double length, double strength) {
		idOfA = typeA;
		idOfB = typeB;
		bondLength = length;
		bondStrength = strength;
	}

	public final int getNumberOfAtoms() {
		return 2;
	}

	public void init(MolecularModel model) {

		int noa = model.getNumberOfAtoms();

		for (int i = noa; i < noa + getNumberOfAtoms(); i++) {
			model.atom[i].setRestraint(null);
			model.atom[i].setCharge(0.0);
			model.atom[i].setUserField(null);
			model.atom[i].setShowRTraj(false);
			model.atom[i].setFriction(0);
			model.atom[i].setRadical(false);
			model.atom[i].setVx(0);
			model.atom[i].setVy(0);
			model.atom[i].setColor(null);
		}

		model.atom[noa].setElement(model.getElement(idOfA));
		model.atom[noa].setRx(0.5 * bondLength);
		model.atom[noa].setRy(0.0);
		addAtom(model.atom[noa]);
		noa++;

		model.atom[noa].setElement(model.getElement(idOfB));
		model.atom[noa].setRx(-0.5 * bondLength);
		model.atom[noa].setRy(0.0);
		addAtom(model.atom[noa]);
		noa++;

		setModel(model);
		model.setNumberOfAtoms(noa);

	}

	public List<RadialBond> buildBonds(MolecularModel model) {
		List<RadialBond> bonds = new ArrayList<RadialBond>();
		bonds.add(new RadialBond.Builder(model.atom[model.numberOfAtoms - 2], model.atom[model.numberOfAtoms - 1])
				.bondLength(bondLength).bondStrength(bondStrength).build());
		return bonds;
	}

}