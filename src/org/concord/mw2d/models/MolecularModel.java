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
import java.awt.geom.Point2D;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;

import org.concord.mw2d.AtomisticView;
import org.concord.mw2d.MDState;
import org.concord.mw2d.event.BondChangeEvent;
import org.concord.mw2d.event.BondChangeListener;

import static org.concord.mw2d.models.Element.*;

public class MolecularModel extends AtomicModel {

	final static short SHOW_NONE = 3101;
	final static short SHOW_CHARGE = 3102;
	final static short SHOW_HYDROPHOBICITY = 3103;

	RadialBondCollection bonds;
	AngularBondCollection bends;
	MoleculeCollection molecules;
	Solvent solvent;

	private double eRB, eAB;
	private List<int[]> vdwLines, chargeLines, ssLines, bpLines;
	private List<BondChangeListener> bondChangeListeners;

	private double bondLength, bondStrength;
	private double distanceInX, distanceInY, distance;
	private double bendAngle, bendStrength;
	private double xij, yij, rij, rijsq;
	private double xkj, ykj, rkj, rkjsq;
	private double theta, costheta, sintheta;
	private double forceInXForI, forceInXForK, forceInYForI, forceInYForK;

	public MolecularModel() {
		super();
		suplement();
	}

	public MolecularModel(int xbox, int ybox) {
		super(xbox, ybox);
		suplement();
	}

	/**
	 * create a molecular model with the given size.
	 * 
	 * @param xbox
	 *            width of the simulation box
	 * @param ybox
	 *            height of the simulation box
	 * @param tapeLength
	 *            the length of the recorder tape
	 */
	public MolecularModel(int xbox, int ybox, int tapeLength) {
		super(xbox, ybox, tapeLength);
		suplement();
	}

	public void addBondChangeListener(BondChangeListener bcl) {
		if (bcl == null)
			return;
		if (bondChangeListeners == null)
			bondChangeListeners = Collections.synchronizedList(new ArrayList<BondChangeListener>());
		bondChangeListeners.add(bcl);
	}

	public void removeBondChangeListener(BondChangeListener bcl) {
		if (bcl == null)
			return;
		if (bondChangeListeners == null || bondChangeListeners.isEmpty())
			return;
		bondChangeListeners.remove(bcl);
	}

	void notifyBondChangeListeners() {
		if (bondChangeListeners == null || bondChangeListeners.isEmpty())
			return;
		synchronized (bondChangeListeners) {
			for (BondChangeListener l : bondChangeListeners)
				l.bondChanged(new BondChangeEvent(this));
		}
	}

	/**
	 * return the van der Waals interaction lines in a List of integer arrays, which store a pair of atoms that are
	 * supposed to have a van der Waals attraction. The neighbor list is used to accelerate the calculation. If the van
	 * der waals attractions are turned off, it will return an empty list. If the whole Lennard-Jones interactions
	 * between a specific pair of atoms are turned off, there will not be lines shown between them.
	 */
	public List getVDWLines() {
		if (vdwLines == null) {
			vdwLines = Collections.synchronizedList(new ArrayList<int[]>());
		}
		else {
			vdwLines.clear();
		}
		if (getCutOff() < 1.001f)
			return vdwLines;
		double ratio = view.getVDWLinesRatio() * view.getVDWLinesRatio();
		if (((hasEmbeddedMovie() && movie.getCurrentFrameIndex() == movie.length() - 1) || !hasEmbeddedMovie())
				&& (job != null && !job.isStopped())) {
			// neighbor list can be used
			double rxi, ryi, rxij, ryij, rijsq, sig, eps;
			int jbeg, jend, jnab, j;
			for (int i = 0; i < numberOfAtoms - 1; i++) {
				if (atom[i].outOfView())
					continue;
				rxi = atom[i].rx;
				ryi = atom[i].ry;
				jbeg = pointer[i];
				jend = pointer[i + 1];
				if (jbeg < jend) {
					for (jnab = jbeg; jnab < jend; jnab++) {
						j = neighborList[jnab];
						if (atom[j].outOfView())
							continue;
						if (atom[i].charge * atom[j].charge <= 0) {
							if (!affinity.isRepulsive(getElement(atom[i].getID()), getElement(atom[j].getID()))) {
								if (bonds.getBond(atom[i], atom[j]) == null) {
									rxij = rxi - atom[j].rx;
									ryij = ryi - atom[j].ry;
									rijsq = rxij * rxij + ryij * ryij;
									sig = 0.5 * (atom[i].sigma + atom[j].sigma);
									sig *= sig;
									eps = atom[i].epsilon * atom[j].epsilon;
									if (rijsq < sig * ratio && eps > ZERO) {
										vdwLines.add(new int[] { i, j });
									}
								}
							}
						}
					}
				}
			}
		}
		else {
			// neighbor list cannot be used
			double rxi, ryi, rxij, ryij, rijsq, sig, eps;
			int i, j;
			for (i = 0; i < numberOfAtoms - 1; i++) {
				if (atom[i].outOfView())
					continue;
				rxi = atom[i].rx;
				ryi = atom[i].ry;
				for (j = i + 1; j < numberOfAtoms; j++) {
					if (atom[j].outOfView())
						continue;
					if (atom[i].charge * atom[j].charge <= 0) {
						if (!affinity.isRepulsive(getElement(atom[i].getID()), getElement(atom[j].getID()))) {
							if (bonds.getBond(atom[i], atom[j]) == null) {
								rxij = rxi - atom[j].rx;
								ryij = ryi - atom[j].ry;
								rijsq = rxij * rxij + ryij * ryij;
								sig = 0.5 * (atom[i].sigma + atom[j].sigma);
								sig *= sig;
								eps = atom[i].epsilon * atom[j].epsilon;
								if (rijsq < sig * ratio && eps > ZERO) {
									vdwLines.add(new int[] { i, j });
								}
							}
						}
					}
				}
			}
		}
		return vdwLines;
	}

	public List getChargeLines() {
		if (chargeLines == null) {
			chargeLines = Collections.synchronizedList(new ArrayList<int[]>());
		}
		else {
			chargeLines.clear();
		}
		if ((hasEmbeddedMovie() && movie.getCurrentFrameIndex() == movie.length() - 1) || !hasEmbeddedMovie()
				&& (job != null && !job.isStopped())) {
			// neighbor list can be used
			double rxi, ryi, rxij, ryij, rijsq, sig;
			int jbeg, jend, jnab, j;
			for (int i = 0; i < numberOfAtoms - 1; i++) {
				if (atom[i].outOfView())
					continue;
				rxi = atom[i].rx;
				ryi = atom[i].ry;
				jbeg = pointer[i];
				jend = pointer[i + 1];
				if (jbeg < jend) {
					for (jnab = jbeg; jnab < jend; jnab++) {
						j = neighborList[jnab];
						if (atom[j].outOfView())
							continue;
						if (atom[i].charge * atom[j].charge < -0.001) {
							if (bonds.getBond(atom[i], atom[j]) == null) {
								rxij = rxi - atom[j].rx;
								ryij = ryi - atom[j].ry;
								rijsq = rxij * rxij + ryij * ryij;
								sig = 0.5 * (atom[i].sigma + atom[j].sigma);
								sig *= sig;
								if (rijsq < sig * 4) {
									chargeLines.add(new int[] { i, j });
								}
							}
						}
					}
				}
			}
		}
		else {
			// neighbor list cannot be used
			double rxi, ryi, rxij, ryij, rijsq, sig;
			int i, j;
			for (i = 0; i < numberOfAtoms - 1; i++) {
				if (atom[i].outOfView())
					continue;
				rxi = atom[i].rx;
				ryi = atom[i].ry;
				for (j = i + 1; j < numberOfAtoms; j++) {
					if (atom[j].outOfView())
						continue;
					if (atom[i].charge * atom[j].charge < -0.001) {
						if (bonds.getBond(atom[i], atom[j]) == null) {
							rxij = rxi - atom[j].rx;
							ryij = ryi - atom[j].ry;
							rijsq = rxij * rxij + ryij * ryij;
							sig = 0.5 * (atom[i].sigma + atom[j].sigma);
							sig *= sig;
							if (rijsq < sig * 4) {
								chargeLines.add(new int[] { i, j });
							}
						}
					}
				}
			}
		}
		return chargeLines;
	}

	public List getSSLines() {
		if (ssLines == null) {
			ssLines = Collections.synchronizedList(new ArrayList<int[]>());
		}
		else {
			ssLines.clear();
		}
		if ((hasEmbeddedMovie() && movie.getCurrentFrameIndex() == movie.length() - 1) || !hasEmbeddedMovie()
				&& (job != null && !job.isStopped())) {
			// neighbor list can be used
			double rxi, ryi, rxij, ryij, rijsq, sig;
			int jbeg, jend, jnab, j;
			for (int i = 0; i < numberOfAtoms - 1; i++) {
				if (atom[i].outOfView())
					continue;
				rxi = atom[i].rx;
				ryi = atom[i].ry;
				jbeg = pointer[i];
				jend = pointer[i + 1];
				if (jbeg < jend) {
					for (jnab = jbeg; jnab < jend; jnab++) {
						j = neighborList[jnab];
						if (atom[j].outOfView())
							continue;
						if ((atom[i].getID() == ID_CYS || atom[i].getID() == ID_MET)
								&& (atom[j].getID() == ID_CYS || atom[j].getID() == ID_MET)) {
							if (bonds.getBond(atom[i], atom[j]) == null) {
								rxij = rxi - atom[j].rx;
								ryij = ryi - atom[j].ry;
								rijsq = rxij * rxij + ryij * ryij;
								sig = 0.5 * (atom[i].sigma + atom[j].sigma);
								sig *= sig;
								if (rijsq < sig * 4) {
									ssLines.add(new int[] { i, j });
								}
							}
						}
					}
				}
			}
		}
		else {
			// neighbor list cannot be used
			double rxi, ryi, rxij, ryij, rijsq, sig;
			int i, j;
			for (i = 0; i < numberOfAtoms - 1; i++) {
				if (atom[i].outOfView())
					continue;
				rxi = atom[i].rx;
				ryi = atom[i].ry;
				for (j = i + 1; j < numberOfAtoms; j++) {
					if (atom[j].outOfView())
						continue;
					if ((atom[i].getID() == ID_CYS || atom[i].getID() == ID_MET)
							&& (atom[j].getID() == ID_CYS || atom[j].getID() == ID_MET)) {
						if (bonds.getBond(atom[i], atom[j]) == null) {
							rxij = rxi - atom[j].rx;
							ryij = ryi - atom[j].ry;
							rijsq = rxij * rxij + ryij * ryij;
							sig = 0.5 * (atom[i].sigma + atom[j].sigma);
							sig *= sig;
							if (rijsq < sig * 4) {
								ssLines.add(new int[] { i, j });
							}
						}
					}
				}
			}
		}
		return ssLines;
	}

	public List getBPLines() {
		if (bpLines == null) {
			bpLines = Collections.synchronizedList(new ArrayList<int[]>());
		}
		else {
			bpLines.clear();
		}
		if ((hasEmbeddedMovie() && movie.getCurrentFrameIndex() == movie.length() - 1) || !hasEmbeddedMovie()
				&& (job != null && !job.isStopped())) {
			// neighbor list can be used
			double rxi, ryi, rxij, ryij, rijsq, sig;
			int jbeg, jend, jnab, j;
			for (int i = 0; i < numberOfAtoms - 1; i++) {
				if (atom[i].outOfView())
					continue;
				rxi = atom[i].rx;
				ryi = atom[i].ry;
				jbeg = pointer[i];
				jend = pointer[i + 1];
				if (jbeg < jend) {
					for (jnab = jbeg; jnab < jend; jnab++) {
						j = neighborList[jnab];
						if (atom[j].outOfView())
							continue;
						if (isPairComplementary(i, j)) {
							rxij = rxi - atom[j].rx;
							ryij = ryi - atom[j].ry;
							rijsq = rxij * rxij + ryij * ryij;
							sig = 0.5 * (atom[i].sigma + atom[j].sigma);
							sig *= sig;
							if (rijsq < sig * 4) {
								bpLines.add(new int[] { i, j });
							}
						}
					}
				}
			}
		}
		else {
			// neighbor list cannot be used
			double rxi, ryi, rxij, ryij, rijsq, sig;
			int i, j;
			for (i = 0; i < numberOfAtoms - 1; i++) {
				if (atom[i].outOfView())
					continue;
				rxi = atom[i].rx;
				ryi = atom[i].ry;
				for (j = i + 1; j < numberOfAtoms; j++) {
					if (atom[j].outOfView())
						continue;
					if (isPairComplementary(i, j)) {
						rxij = rxi - atom[j].rx;
						ryij = ryi - atom[j].ry;
						rijsq = rxij * rxij + ryij * ryij;
						sig = 0.5 * (atom[i].sigma + atom[j].sigma);
						sig *= sig;
						if (rijsq < sig * 4) {
							bpLines.add(new int[] { i, j });
						}
					}
				}
			}
		}
		return bpLines;
	}

	private boolean isPairComplementary(int i, int j) {
		if (molecules.sameMolecule(atom[i], atom[j]))
			return false;
		int iID = atom[i].getID();
		int jID = atom[j].getID();
		return (iID == ID_A && jID == ID_T) || (iID == ID_T && jID == ID_A) || (iID == ID_C && jID == ID_G)
				|| (iID == ID_G && jID == ID_C) || (iID == ID_A && jID == ID_U) || (iID == ID_U && jID == ID_A);
	}

	private void resetBondTable() {
		int n = bondTable.length;
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				bondTable[i][j] = false;
				bondTable[j][i] = false;
			}
			bondTable[i][i] = false;
		}
	}

	private void updateBondTable() {
		resetBondTable();
		if (bonds.isEmpty())
			return;
		RadialBond rb;
		int i1, i2;
		synchronized (bonds.getSynchronizationLock()) {
			for (Iterator it = bonds.iterator(); it.hasNext();) {
				rb = (RadialBond) it.next();
				i1 = rb.getAtom1().getIndex();
				i2 = rb.getAtom2().getIndex();
				bondTable[i1][i2] = true;
				bondTable[i2][i1] = true;
			}
		}
	}

	boolean needMinimization() {
		double xij, yij, rijsq;
		for (int i = 0; i < numberOfAtoms - 1; i++) {
			for (int j = i + 1; j < numberOfAtoms; j++) {
				xij = atom[i].rx - atom[j].rx;
				yij = atom[i].ry - atom[j].ry;
				rijsq = xij * xij + yij * yij;
				xij = 0.36 * (atom[i].sigma + atom[j].sigma);
				if (rijsq < xij * xij) {
					if (bonds.getBond(atom[i], atom[j]) != null)
						continue;
					return true;
				}
			}
		}
		return false;
	}

	private void suplement() {

		bonds = new RadialBondCollection();
		bends = new AngularBondCollection();
		molecules = new MoleculeCollection();
		bonds.setModel(this);
		bends.setModel(this);
		molecules.setModel(this);

		Action a = new SolventTypeAction(this);
		choiceMap.put(a.toString(), a);
		a = new ResetProteinAction(this);
		actionMap.put(a.toString(), a);

	}

	public void run() {
		if (!(this instanceof ReactionModel)) {
			if (!ljBetweenBondPairs)
				updateBondTable();
		}
		super.run();
	}

	public RadialBondCollection getBonds() {
		return bonds;
	}

	public AngularBondCollection getBends() {
		return bends;
	}

	public MoleculeCollection getMolecules() {
		return molecules;
	}

	public void setSolvent(Solvent s) {
		solvent = s;
	}

	public Solvent getSolvent() {
		return solvent;
	}

	public synchronized float getTotalRadialBondEnergy() {
		return (float) eRB;
	}

	public synchronized float getTotalAngularBondEnergy() {
		return (float) eAB;
	}

	public void putInBounds() {
		super.putInBounds();
		switch (boundary.getType()) {
		case RectangularBoundary.PBC_ID:
			boundary.processBondCrossingUnderPBC();
			break;
		case RectangularBoundary.XRYPBC_ID:
			boundary.processBondCrossingUnderXRYPBC();
			break;
		case RectangularBoundary.XPYRBC_ID:
			boundary.processBondCrossingUnderXPYRBC();
			break;
		}
	}

	private void setMoleculeTemperature(Molecule mol, double temperature) {
		if (mol == null)
			return;
		if (temperature < ZERO)
			temperature = 0.0;
		double temp1 = mol.getKineticEnergy() * UNIT_EV_OVER_KB;
		if (temp1 < ZERO) {
			assignTemperature(mol.atoms, 100.0);
			temp1 = mol.getKineticEnergy() * UNIT_EV_OVER_KB;
		}
		double ratio = Math.sqrt(temperature / temp1);
		double sumx = 0.0, sumy = 0.0, inv = 0.0;
		Atom a = null;
		synchronized (mol) {
			for (Iterator it = mol.iterator(); it.hasNext();) {
				a = (Atom) it.next();
				a.vx *= ratio;
				a.vy *= ratio;
				sumx += a.vx * a.mass;
				sumy += a.vy * a.mass;
				inv += a.mass;
			}
			inv = 1.0 / inv;
			for (Iterator it = mol.iterator(); it.hasNext();) {
				a = (Atom) it.next();
				a.vx -= sumx * inv;
				a.vy -= sumy * inv;
			}
		}
	}

	/** if there is a solvent at presence, and the heat bath is turned on, mimick the solvent effect */
	public void setTemperature(double temperature) {
		if (molecules.isEmpty()) {
			super.setTemperature(temperature);
			return;
		}
		if (!heatBathActivated()) {
			super.setTemperature(temperature);
			return;
		}
		if (solvent != null && solvent.getType() != Solvent.VACUUM) {
			Molecule mol = null;
			List<Atom> list = new ArrayList<Atom>();
			synchronized (molecules.getSynchronizationLock()) {
				for (Iterator i = molecules.iterator(); i.hasNext();) {
					mol = (Molecule) i.next();
					if (mol instanceof Polypeptide)
						setMoleculeTemperature(mol, temperature);
					else {
						for (Iterator it = mol.iterator(); it.hasNext();)
							list.add((Atom) it.next());
					}
				}
			}
			for (int i = 0; i < numberOfAtoms; i++) {
				if (!atom[i].isBonded())
					list.add(atom[i]);
			}
			if (!list.isEmpty())
				setTemperature(list, temperature);
		}
		else {
			super.setTemperature(temperature);
		}
	}

	public synchronized double computeForce(int time) {

		if (numberOfAtoms <= 1)
			return super.computeForce(time);

		double vsum = super.computeForce(time) * numberOfAtoms;
		eRB = 0.0;
		eAB = 0.0;
		double etemp;
		Atom atom1, atom2;

		if (!bonds.isEmpty()) {

			RadialBond rBond;
			synchronized (bonds.getSynchronizationLock()) {
				for (int ib = 0, nb = bonds.size(); ib < nb; ib++) {
					rBond = bonds.get(ib);
					atom1 = rBond.atom1;
					atom2 = rBond.atom2;
					bondLength = rBond.bondLength;
					bondStrength = rBond.bondStrength;
					distanceInX = atom2.rx - atom1.rx;
					distanceInY = atom2.ry - atom1.ry;
					distance = Math.hypot(distanceInX, distanceInY);
					etemp = bondStrength * GF_CONVERSION_CONSTANT * (distance - bondLength) / distance;
					atom1.fx += etemp * distanceInX / atom1.mass;
					atom1.fy += etemp * distanceInY / atom1.mass;
					atom2.fx -= etemp * distanceInX / atom2.mass;
					atom2.fy -= etemp * distanceInY / atom2.mass;
					etemp = distance - bondLength;
					etemp = 0.5 * bondStrength * etemp * etemp;
					vsum += etemp - rBond.getChemicalEnergy();
					eRB += etemp;
					rBond.applyTorque();
					rBond.forceVibration(modelTime);
				}
			}

			if (bends != null && !bends.isEmpty()) {

				double commonPrefactor;
				AngularBond aBond;
				Atom atom3;

				synchronized (bends.getSynchronizationLock()) {

					for (int ib = 0, nb = bends.size(); ib < nb; ib++) {

						aBond = bends.get(ib);
						atom1 = aBond.atom1;
						atom2 = aBond.atom2;
						atom3 = aBond.atom3;
						bendAngle = aBond.bondAngle;
						bendStrength = aBond.bondStrength;
						xij = atom1.rx - atom3.rx;
						xkj = atom2.rx - atom3.rx;
						yij = atom1.ry - atom3.ry;
						ykj = atom2.ry - atom3.ry;
						rijsq = xij * xij + yij * yij;
						rkjsq = xkj * xkj + ykj * ykj;
						rij = Math.sqrt(rijsq);
						rkj = Math.sqrt(rkjsq);
						costheta = (xij * xkj + yij * ykj) / (rij * rkj);
						if (costheta > 1.0)
							costheta = 1.0;
						else if (costheta < -1.0)
							costheta = -1.0;
						sintheta = Math.sqrt(1.0 - costheta * costheta);
						theta = Math.acos(costheta);
						if (sintheta < 0.0001)
							sintheta = 0.0001;

						commonPrefactor = bendStrength * (theta - bendAngle) / (sintheta * rij * rkj)
								* GF_CONVERSION_CONSTANT;
						etemp = xij * xkj + yij * ykj;

						forceInXForI = commonPrefactor * (xkj - etemp * xij / rijsq);
						forceInYForI = commonPrefactor * (ykj - etemp * yij / rijsq);
						forceInXForK = commonPrefactor * (xij - etemp * xkj / rkjsq);
						forceInYForK = commonPrefactor * (yij - etemp * ykj / rkjsq);

						atom1.fx += forceInXForI / atom1.mass;
						atom1.fy += forceInYForI / atom1.mass;
						atom2.fx += forceInXForK / atom2.mass;
						atom2.fy += forceInYForK / atom2.mass;
						atom3.fx -= (forceInXForI + forceInXForK) / atom3.mass;
						atom3.fy -= (forceInYForI + forceInYForK) / atom3.mass;

						etemp = 0.5 * bendStrength * (theta - bendAngle) * (theta - bendAngle);
						vsum += etemp - aBond.getChemicalEnergy();
						eAB += etemp;

					}

				}

			}

			synchronized (molecules.getSynchronizationLock()) {
				for (Iterator it = molecules.iterator(); it.hasNext();)
					((Molecule) it.next()).applyTorque();
			}

			/*
			 * Compute hydrophobic/hydrophilic force for each molecule. This is mainly used to model protein folding. No
			 * periodic boundary condition should be applied. Heat bath should be on.
			 */

			/*
			 * test code for local mean field approximation if(solvent!=null && solvent.getType()==Solvent.VACUUM-10){
			 * 
			 * double x12, y12, r12; Atom[] partner; Atom at; Molecule mol;
			 * 
			 * synchronized(molecules){
			 * 
			 * for(int im=0, nm=molecules.size(); im<nm; im++){
			 * 
			 * mol=(Molecule)molecules.get(im); if(mol instanceof Polypeptide) { synchronized(mol){ for(Iterator
			 * j=mol.iterator(); j.hasNext();){ at=(Atom)j.next(); partner=bonds.getBondedPartners(at);
			 * if(partner.length==2){ x12=partner[0].rx-partner[1].rx; y12=partner[0].ry-partner[1].ry;
			 * r12=Math.sqrt(x12*x12+y12*y12); at.fx-=at.hydrophobic*solvent.getType()*
			 * GF_CONVERSION_CONSTANT*y12/(r12*at.mass); at.fy+=at.hydrophobic*solvent.getType()*
			 * GF_CONVERSION_CONSTANT*x12/(r12*at.mass); } } } } } } }
			 */

			if (solvent != null && solvent.getType() != Solvent.VACUUM) {
				Molecule mol = null;
				Atom a = null;
				Point2D com = null;
				double rxij = 0.0, ryij = 0.0, temp = 0.0, rij = 0.0, fxij = 0.0, fyij = 0.0;
				// double sumx=0.0, sumy=0.0;
				double factor = 5 * solvent.getTemperatureFactor(heatBathActivated() ? heatBath
						.getExpectedTemperature() : getTemperature());
				synchronized (molecules.getSynchronizationLock()) {
					for (int im1 = 0, nm1 = molecules.size(); im1 < nm1; im1++) {
						mol = molecules.get(im1);
						com = mol.getCenterOfMass2D();
						synchronized (mol.getSynchronizedLock()) {
							for (int im2 = 0, nm2 = mol.size(); im2 < nm2; im2++) {
								a = mol.getAtom(im2);
								rxij = a.rx - com.getX();
								ryij = a.ry - com.getY();
								rij = Math.hypot(rxij, ryij);
								if (a.hydrophobic != 0) {
									temp = factor * a.hydrophobic * solvent.getType();
									vsum += temp * rij;
									fxij = temp * rxij / (rij * a.mass);
									fyij = temp * ryij / (rij * a.mass);
									a.fx -= fxij;
									a.fy -= fyij;
									// sumx += fxij;
									// sumy += fyij;
								}
							}
						}
						/*
						 * This was previously for ensuring that the net force on the molecule sums to zero. But this
						 * violates the energy conservation law. if(mol.size()>1){ double molsize=1.0/mol.size();
						 * synchronized(mol){ for(j=mol.iterator(); j.hasNext();){ a=(Atom)j.next(); a.fx +=
						 * sumx*molsize; a.fy += sumy*molsize; } } }
						 */
					}
				}
			}

		}

		return vsum / numberOfAtoms;

	}

	public void destroy() {
		super.destroy();
		destroyBonds();
	}

	public void clear() {
		super.clear();
		resetBondTable();
		destroyBonds();
		setSolvent(null);
		if (vdwLines != null && !vdwLines.isEmpty())
			vdwLines.clear();
		if (chargeLines != null && !chargeLines.isEmpty())
			chargeLines.clear();
		if (ssLines != null && !ssLines.isEmpty())
			ssLines.clear();
		if (bpLines != null && !bpLines.isEmpty())
			bpLines.clear();
		if (job != null)
			job.processPendingRequests();
	}

	private void destroyBonds() {
		if (bonds != null && !bonds.isEmpty()) {
			synchronized (bonds.getSynchronizationLock()) {
				for (Iterator it = bonds.iterator(); it.hasNext();) {
					((RadialBond) it.next()).destroy();
				}
			}
		}
		bonds.clear();
		if (bends != null && !bends.isEmpty()) {
			synchronized (bends.getSynchronizationLock()) {
				for (Iterator it = bends.iterator(); it.hasNext();) {
					((AngularBond) it.next()).destroy();
				}
			}
		}
		bends.clear();
		if (molecules != null && !molecules.isEmpty()) {
			synchronized (molecules.getSynchronizationLock()) {
				for (Iterator it = molecules.iterator(); it.hasNext();) {
					((Molecule) it.next()).destroy();
				}
			}
		}
		molecules.clear();
	}

	/* this method is called by the script interpreter after radial bonds are removed. */
	void removeGhostAngularBonds() {
		if (bends.isEmpty())
			return;
		AngularBond ab;
		synchronized (bends.getSynchronizationLock()) {
			for (Iterator i = bends.iterator(); i.hasNext();) {
				ab = (AngularBond) i.next();
				if (bonds.getBond(ab.atom1, ab.atom3) == null || bonds.getBond(ab.atom2, ab.atom3) == null) {
					i.remove();
				}
			}
		}
	}

	public String toString() {
		return "<Molecular Model> " + getProperty("filename");
	}

	void encodeBonds(XMLEncoder out) {

		State state = new State();
		state.setNumberOfBonds(bonds.size());
		state.setNumberOfBends(bends.size());
		state.setMolecularTorque(getMolecularTorques());
		if (solvent != null && solvent.getType() != Solvent.VACUUM)
			state.setSolvent(solvent);
		out.writeObject(state);
		monitor.setMaximum(getNumberOfParticles() + bonds.size() + bends.size() + 8);
		out.flush();

		RadialBond rb;
		synchronized (bonds.getSynchronizationLock()) {
			for (Iterator it = bonds.iterator(); it.hasNext();) {
				rb = (RadialBond) it.next();
				rb.setSelected(false);
				monitor.setProgressMessage("Writing " + rb + "...");
				out.writeObject(new RadialBond.Delegate(rb));
			}
		}
		out.flush();

		AngularBond ab;
		synchronized (bends.getSynchronizationLock()) {
			for (Iterator it = bends.iterator(); it.hasNext();) {
				ab = (AngularBond) it.next();
				ab.setSelected(false);
				monitor.setProgressMessage("Writing " + ab + "...");
				out.writeObject(new AngularBond.Delegate(ab));
			}
		}
		out.flush();

	}

	void decodeBonds(XMLDecoder in, AtomisticView.State viewState) throws Exception {

		State state = (State) in.readObject();

		solvent = state.getSolvent();
		if (solvent != null)
			universe.setDielectricConstant(solvent.getDielectricConstant());

		int n = state.getNumberOfBonds();
		if (n == 0)
			return;

		int i = 0;

		RadialBond.Delegate rbd = null;
		RadialBond rBond = null;
		for (i = 0; i < n; i++) {
			rbd = (RadialBond.Delegate) in.readObject();
			monitor.setProgressMessage("Reading " + rbd + "...");
			rBond = new RadialBond(atom[rbd.getAtom1()], atom[rbd.getAtom2()], rbd.getBondLength(), rbd
					.getBondStrength(), rbd.getChemicalEnergy());
			rBond.setTorque(rbd.getTorque());
			rBond.setTorqueType(rbd.getTorqueType());
			rBond.setAmplitude(rbd.getAmplitude());
			rBond.setPeriod(rbd.getPeriod());
			rBond.setPhase(rbd.getPhase());
			rBond.setVisible(rbd.isVisible());
			rBond.setSmart(rbd.isSmart());
			rBond.setSolid(rbd.isSolid());
			rBond.setClosed(rbd.isClosed());
			rBond.setBondColor(rbd.getColor());
			rBond.setBondStyle(rbd.getStyle());
			rBond.setModel(this);
			bonds.add(rBond);
		}

		AngularBond.Delegate abd = null;
		AngularBond aBond = null;
		n = state.getNumberOfBends();
		for (i = 0; i < n; i++) {
			abd = (AngularBond.Delegate) in.readObject();
			monitor.setProgressMessage("Reading " + abd + "...");
			aBond = new AngularBond(atom[abd.getAtom1()], atom[abd.getAtom2()], atom[abd.getAtom3()], abd
					.getBondAngle(), abd.getBondStrength(), abd.getChemicalEnergy());
			aBond.setModel(this);
			bends.add(aBond);
		}

		MoleculeCollection.sort(this);

		MolecularTorque[] mts = state.getMolecularTorque();
		if (mts != null) {
			i = 0;
			synchronized (molecules.getSynchronizationLock()) {
				for (Iterator it = molecules.iterator(); it.hasNext();)
					((Molecule) it.next()).setTorque(mts[i++]);
			}
		}

		if (viewState != null) {
			Color[] c = viewState.getMolecularObjectColors();
			if (c != null) {
				Molecule mol;
				int nmo = 0;
				synchronized (molecules.getSynchronizationLock()) {
					for (Iterator it = molecules.iterator(); it.hasNext();) {
						mol = (Molecule) it.next();
						if (mol instanceof MolecularObject) {
							((MolecularObject) mol).setBackground(c[nmo]);
							nmo++;
						}
					}
				}
			}
		}

	}

	Color[] getMolecularObjectColors() {
		if (molecules.isEmpty())
			return null;
		Molecule mol = null;
		int nmo = 0;
		synchronized (molecules.getSynchronizationLock()) {
			for (Iterator it = molecules.iterator(); it.hasNext();) {
				mol = (Molecule) it.next();
				if (mol instanceof MolecularObject)
					nmo++;
			}
		}
		if (nmo > 0) {
			Color[] c = new Color[nmo];
			nmo = 0;
			synchronized (molecules.getSynchronizationLock()) {
				for (Iterator it = molecules.iterator(); it.hasNext();) {
					mol = (Molecule) it.next();
					if (mol instanceof MolecularObject) {
						c[nmo++] = ((MolecularObject) mol).getBackground();
					}
				}
			}
			return c;
		}
		return null;
	}

	MolecularTorque[] getMolecularTorques() {
		if (molecules.isEmpty())
			return null;
		MolecularTorque[] t = new MolecularTorque[molecules.size()];
		int i = 0;
		synchronized (molecules.getSynchronizationLock()) {
			for (Iterator it = molecules.iterator(); it.hasNext();)
				t[i++] = ((Molecule) it.next()).getTorque();
		}
		return t;
	}

	/**
	 * Delegate of the state of this model, <b>augmented</b> to the <tt>AtomicModel</tt> (Notice that this class is
	 * not derived from <tt>AtomicModel.State</tt>).
	 */
	public static class State extends MDState {

		private int numberOfBonds, numberOfBends;
		private Solvent solvent;
		private MolecularTorque[] molecularTorque;

		public State() {
		}

		public void setSolvent(Solvent s) {
			solvent = s;
		}

		public Solvent getSolvent() {
			return solvent;
		}

		public void setNumberOfBonds(int i) {
			numberOfBonds = i;
		}

		public int getNumberOfBonds() {
			return numberOfBonds;
		}

		public void setNumberOfBends(int i) {
			numberOfBends = i;
		}

		public int getNumberOfBends() {
			return numberOfBends;
		}

		public void setMolecularTorque(MolecularTorque[] mt) {
			molecularTorque = mt;
		}

		public MolecularTorque[] getMolecularTorque() {
			return molecularTorque;
		}

	}

}