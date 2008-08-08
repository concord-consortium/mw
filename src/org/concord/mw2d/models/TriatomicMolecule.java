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

public final class TriatomicMolecule extends Molecule {

	private double bendAngle = Math.toRadians(120.0);
	private double bendStrength = 2.0;
	private double d12 = 20.0, d23 = 20.0;
	private double s12 = 0.2, s23 = 0.2;
	private int typeOfA = 1, typeOfB = 2, typeOfC = 1;
	private boolean charged = false;
	private double charge = 2;

	public TriatomicMolecule(int typeOfA, int typeOfB, int typeOfC, double d12, double s12, double d23, double s23,
			double angle, double bendStrength) {
		bendAngle = Math.toRadians(angle == 180 ? 179.9 : angle);
		this.typeOfA = typeOfA;
		this.typeOfB = typeOfB;
		this.typeOfC = typeOfC;
		this.s12 = s12;
		this.s23 = s23;
		this.d12 = d12;
		this.d23 = d23;
		this.bendStrength = bendStrength;
	}

	public final int getNumberOfAtoms() {
		return 3;
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

		double angle = Math.PI / 6.0;

		model.atom[noa].setElement(model.getElement(typeOfA));
		model.atom[noa].setRx(d12 * Math.cos(angle));
		model.atom[noa].setRy(d12 * Math.sin(angle));
		if (charged)
			model.atom[noa].setCharge(charge);
		addAtom(model.atom[noa]);
		noa++;

		model.atom[noa].setElement(model.getElement(typeOfB));
		model.atom[noa].setRx(0.0);
		model.atom[noa].setRy(0.0);
		if (charged)
			model.atom[noa].setCharge(-2.0 * charge);
		addAtom(model.atom[noa]);
		noa++;

		model.atom[noa].setElement(model.getElement(typeOfC));
		model.atom[noa].setRx(d23 * Math.cos(angle + bendAngle));
		model.atom[noa].setRy(d23 * Math.sin(angle + bendAngle));
		if (charged)
			model.atom[noa].setCharge(charge);
		addAtom(model.atom[noa]);
		noa++;

		setModel(model);
		model.setNumberOfAtoms(noa);

	}

	public List<RadialBond> buildBonds(MolecularModel model) {
		List<RadialBond> x = new ArrayList<RadialBond>();
		x.add(new RadialBond.Builder(model.atom[model.numberOfAtoms - 3], model.atom[model.numberOfAtoms - 2])
				.bondLength(d12).bondStrength(s12).build());
		x.add(new RadialBond.Builder(model.atom[model.numberOfAtoms - 1], model.atom[model.numberOfAtoms - 2])
				.bondLength(d23).bondStrength(s23).build());
		return x;
	}

	public List<AngularBond> buildBends(MolecularModel model) {
		List<AngularBond> x = new ArrayList<AngularBond>();
		x.add(new AngularBond(model.atom[model.numberOfAtoms - 3], model.atom[model.numberOfAtoms - 1],
				model.atom[model.numberOfAtoms - 2], bendAngle, bendStrength));
		return x;
	}

}