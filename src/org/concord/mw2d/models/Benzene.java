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

public final class Benzene extends Molecule {

	private double outerRadius = 35.0;
	private double innerRadius = 20.0;
	private double bondLengthCC = 20.0;
	private double bondLengthCH = 15.0;
	private double bondStrength = 0.2;
	private double bendAngle = Math.toRadians(120.0);
	private double bendStrength = 2.0;
	private final static double ANGLE_INCR = Math.toRadians(60.0);

	public final int getNumberOfAtoms() {
		return 12;
	}

	public final void init(MolecularModel model) {

		int noa = model.getNumberOfAtoms();

		for (int i = noa; i < noa + getNumberOfAtoms(); i++) {
			model.atom[i].setRestraint(null);
			model.atom[i].setCharge(0);
			model.atom[i].setCustom(0);
			model.atom[i].setUserField(null);
			model.atom[i].setShowRTraj(false);
			model.atom[i].setFriction(0);
			model.atom[i].setRadical(false);
			model.atom[i].setVx(0);
			model.atom[i].setVy(0);
			model.atom[i].setColor(null);
		}

		double angle = 0.0;

		for (int n = 0; n < 6; n++) {

			model.atom[noa].setElement(model.nt);
			model.atom[noa].setRx(outerRadius * Math.cos(angle));
			model.atom[noa].setRy(outerRadius * Math.sin(angle));
			addAtom(model.atom[noa]);
			noa++;

			model.atom[noa].setElement(model.pl);
			model.atom[noa].setRx(innerRadius * Math.cos(angle));
			model.atom[noa].setRy(innerRadius * Math.sin(angle));
			addAtom(model.atom[noa]);
			noa++;

			angle += ANGLE_INCR;

		}

		setModel(model);
		model.setNumberOfAtoms(noa);

	}

	public final List<RadialBond> buildBonds(MolecularModel model) {

		List<RadialBond> bonds = new ArrayList<RadialBond>();

		int num = model.numberOfAtoms - getNumberOfAtoms();

		bonds.add(new RadialBond.Builder(model.atom[1 + num], model.atom[num]).bondLength(bondLengthCH).bondStrength(bondStrength).build());
		bonds.add(new RadialBond.Builder(model.atom[1 + num], model.atom[3 + num]).bondLength(bondLengthCC).bondStrength(bondStrength * 2).build());
		bonds.add(new RadialBond.Builder(model.atom[3 + num], model.atom[2 + num]).bondLength(bondLengthCH).bondStrength(bondStrength).build());
		bonds.add(new RadialBond.Builder(model.atom[3 + num], model.atom[5 + num]).bondLength(bondLengthCC).bondStrength(bondStrength * 2).build());
		bonds.add(new RadialBond.Builder(model.atom[5 + num], model.atom[4 + num]).bondLength(bondLengthCH).bondStrength(bondStrength).build());
		bonds.add(new RadialBond.Builder(model.atom[5 + num], model.atom[7 + num]).bondLength(bondLengthCC).bondStrength(bondStrength * 2).build());
		bonds.add(new RadialBond.Builder(model.atom[7 + num], model.atom[6 + num]).bondLength(bondLengthCH).bondStrength(bondStrength).build());
		bonds.add(new RadialBond.Builder(model.atom[7 + num], model.atom[9 + num]).bondLength(bondLengthCC).bondStrength(bondStrength * 2).build());
		bonds.add(new RadialBond.Builder(model.atom[9 + num], model.atom[8 + num]).bondLength(bondLengthCH).bondStrength(bondStrength).build());
		bonds.add(new RadialBond.Builder(model.atom[9 + num], model.atom[11 + num]).bondLength(bondLengthCC).bondStrength(bondStrength * 2).build());
		bonds.add(new RadialBond.Builder(model.atom[11 + num], model.atom[10 + num]).bondLength(bondLengthCH).bondStrength(bondStrength).build());
		bonds.add(new RadialBond.Builder(model.atom[11 + num], model.atom[1 + num]).bondLength(bondLengthCC).bondStrength(bondStrength * 2).build());

		return bonds;

	}

	public final List<AngularBond> buildBends(MolecularModel model) {

		List<AngularBond> bends = new ArrayList<AngularBond>();

		int num = model.numberOfAtoms - getNumberOfAtoms();

		bends.add(new AngularBond(model.atom[11 + num], model.atom[3 + num], model.atom[1 + num], bendAngle, bendStrength));
		bends.add(new AngularBond(model.atom[1 + num], model.atom[5 + num], model.atom[3 + num], bendAngle, bendStrength));
		bends.add(new AngularBond(model.atom[3 + num], model.atom[7 + num], model.atom[5 + num], bendAngle, bendStrength));
		bends.add(new AngularBond(model.atom[5 + num], model.atom[9 + num], model.atom[7 + num], bendAngle, bendStrength));
		bends.add(new AngularBond(model.atom[7 + num], model.atom[11 + num], model.atom[9 + num], bendAngle, bendStrength));
		bends.add(new AngularBond(model.atom[9 + num], model.atom[1 + num], model.atom[11 + num], bendAngle, bendStrength));
		bends.add(new AngularBond(model.atom[num], model.atom[3 + num], model.atom[1 + num], bendAngle, bendStrength));
		bends.add(new AngularBond(model.atom[2 + num], model.atom[5 + num], model.atom[3 + num], bendAngle, bendStrength));
		bends.add(new AngularBond(model.atom[4 + num], model.atom[7 + num], model.atom[5 + num], bendAngle, bendStrength));
		bends.add(new AngularBond(model.atom[6 + num], model.atom[9 + num], model.atom[7 + num], bendAngle, bendStrength));
		bends.add(new AngularBond(model.atom[8 + num], model.atom[11 + num], model.atom[9 + num], bendAngle, bendStrength));
		bends.add(new AngularBond(model.atom[10 + num], model.atom[1 + num], model.atom[11 + num], bendAngle, bendStrength));

		return bends;

	}

}