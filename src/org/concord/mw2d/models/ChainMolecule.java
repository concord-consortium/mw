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

import org.concord.mw2d.ui.ChainConfigure;

public final class ChainMolecule extends Molecule {

	private double bondLength = 20.0;
	private double bondStrength = 0.2;
	private double bendAngle = Math.toRadians(60.0);
	private double bendStrength = 2.0;
	private int numberOfLinks = 25;
	private int typeOfAtom = 1;
	private int growthMode = ChainConfigure.SAWTOOTH;
	private List<RadialBond> bonds;
	private List<AngularBond> bends;

	public ChainMolecule(int typeOfAtom, int numberOfLinks, int growthMode, double length, double angle) {
		this.typeOfAtom = typeOfAtom;
		this.growthMode = growthMode;
		this.numberOfLinks = numberOfLinks;
		bondLength = length;
		bendAngle = Math.toRadians(angle == 180 ? 179.9 : angle);
	}

	public ChainMolecule(int typeOfAtom, int numberOfLinks, int growthMode, double length, double bondStrength,
			double angle, double bendStrength) {
		this.typeOfAtom = typeOfAtom;
		this.growthMode = growthMode;
		this.numberOfLinks = numberOfLinks;
		bondLength = length;
		bendAngle = Math.toRadians(angle == 180 ? 179.9 : angle);
		this.bondStrength = bondStrength;
		this.bendStrength = bendStrength;
	}

	public final int getNumberOfAtoms() {
		return numberOfLinks;
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
			model.atom[i].setElement(model.getElement(typeOfAtom));
			model.atom[i].setColor(null);
		}

		double angle = 0.0;
		bonds = new ArrayList<RadialBond>();
		bends = new ArrayList<AngularBond>();

		for (int k = 0; k < numberOfLinks; k++) {

			if (growthMode == ChainConfigure.SAWTOOTH) {
				angle = k % 2 == 0 ? bendAngle * 0.5 : -bendAngle * 0.5;
			}
			else if (growthMode == ChainConfigure.CURLUP) {
				angle += bendAngle * 0.5;
			}
			else if (growthMode == ChainConfigure.RANDOM) {
				angle += ((int) (Math.random() * 100.0) % 2 == 0 ? bendAngle * 0.5 : -bendAngle * 0.5);
			}

			model.atom[noa].setRx(k == 0 ? bondLength * Math.cos(angle) : model.atom[noa - 1].getRx() + bondLength
					* Math.cos(angle));
			model.atom[noa].setRy(k == 0 ? bondLength * Math.sin(angle) : model.atom[noa - 1].getRy() + bondLength
					* Math.sin(angle));
			addAtom(model.atom[noa]);
			noa++;

			if (k > 0) {
				bonds.add(new RadialBond(model.atom[noa - 1], model.atom[noa - 2], bondLength, bondStrength));
			}

			if (k > 1) {
				bends.add(new AngularBond(model.atom[noa - 1], model.atom[noa - 3], model.atom[noa - 2], Math.PI
						- bendAngle, bendStrength));
			}

		}

		setModel(model);
		model.setNumberOfAtoms(noa);

	}

	public List<RadialBond> buildBonds(MolecularModel model) {
		return bonds;
	}

	public List<AngularBond> buildBends(MolecularModel model) {
		return bends;
	}

}