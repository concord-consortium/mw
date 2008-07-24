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

package org.concord.mw3d.models;

import javax.vecmath.Vector3f;

import org.myjmol.api.Pair;

class ForceCalculator {

	/*
	 * Coulomb's constant times the square of electron charge (k*e^2), in eV*angstrom. see
	 * http://hyperphysics.phy-astr.gsu.edu/hbase/electric/elefor.html
	 */
	private final static float COULOMB_CONSTANT = 14.4f;

	/*
	 * converts energy gradient unit into force unit: 1.6E-19 [J] / ( E-10 [m] x E-3 / 6E23 [kg] ) / ( E-10 / ( E-15 )
	 * ^2 ) [m/s^2]
	 */
	final static float GF_CONVERSION_CONSTANT = 0.0096f;
	private final static float SIX_TIMES_UNIT_FORCE = 6.0f * GF_CONVERSION_CONSTANT;
	private final static float MIN_SINTHETA = 0.001f;

	float xbox = 50f, ybox = 25f, zbox = 25f; // half size of the simulation box

	private MolecularModel model;
	private Atom[] atom;
	private float[] rx0, ry0, rz0;
	private int nlist;
	private int jbeg, jend;
	private float sigmai, sigmaj, sigmaij;
	private float rxi, ryi, rzi;
	private float fxi, fyi, fzi;
	private float fxk, fyk, fzk;
	private float fxl, fyl, fzl;
	private float rxij, ryij, rzij, rij, rijsq;
	private float rxkj, rykj, rzkj, rkj, rkjsq;
	private float rxlk, rylk, rzlk, rlk, rlksq;
	private float virialLJ, virialEL;
	private float sr2, sr6, sr12, vij, wij, fij;
	private float fxij, fyij, fzij;
	private float rCutOff = 2.5f, rList = rCutOff + 1.0f;
	private float rCutOffSq;
	private float sigab, epsab;
	private float strength, angle, length;
	private float inverseMass1, inverseMass2, inverseMass3, inverseMass4;
	private Atom atom1, atom2, atom3, atom4;
	private float theta, sintheta;
	private Vector3f vector1, vector2;

	private volatile boolean updateList = true;
	private int[] neighborList, pointer;
	private boolean isPBC;
	private Pair[] pairs;

	ForceCalculator(MolecularModel model) {
		this.model = model;
		this.atom = model.atom;
		rx0 = new float[MolecularModel.SIZE];
		ry0 = new float[MolecularModel.SIZE];
		rz0 = new float[MolecularModel.SIZE];
		neighborList = new int[MolecularModel.SIZE * MolecularModel.SIZE >> 1];
		pointer = new int[MolecularModel.SIZE];
		vector1 = new Vector3f();
		vector2 = new Vector3f();
		rCutOffSq = rCutOff * rCutOff;
	}

	void setUpdateList(boolean b) {
		updateList = b;
	}

	void setSimulationBox(float xbox, float ybox, float zbox) {
		this.xbox = xbox;
		this.ybox = ybox;
		this.zbox = zbox;
	}

	synchronized void applyBoundary() {
		int iAtom = model.iAtom;
		float radius = 0;
		Atom a = null;
		for (int i = 0; i < iAtom; i++) {
			a = atom[i];
			if (!a.isMovable())
				continue;
			radius = 0.5f * a.sigma;
			if (a.rx > xbox - radius) {
				a.vx = -Math.abs(a.vx);
			}
			else if (a.rx < radius - xbox) {
				a.vx = Math.abs(a.vx);
			}
			if (a.ry > ybox - radius) {
				a.vy = -Math.abs(a.vy);
			}
			else if (a.ry < radius - ybox) {
				a.vy = Math.abs(a.vy);
			}
			if (a.rz > zbox - radius) {
				a.vz = -Math.abs(a.vz);
			}
			else if (a.rz < radius - zbox) {
				a.vz = Math.abs(a.vz);
			}
		}
	}

	private void expandPairs() {
		Pair[] newPairs = new Pair[pairs.length + 100];
		System.arraycopy(pairs, 0, newPairs, 0, pairs.length);
		for (int i = pairs.length; i < newPairs.length; i++) {
			newPairs[i] = new Pair(-1, -1);
		}
		pairs = newPairs;
	}

	synchronized Pair[] generateVdwPairs() {
		if (pairs == null) {
			pairs = new Pair[100];
			for (int i = 0; i < pairs.length; i++) {
				pairs[i] = new Pair(-1, -1);
			}
		}
		else {
			for (Pair p : pairs) {
				p.setIndices(-1, -1);
			}
		}
		float ratio = model.getView().getVdwLinesRatio();
		ratio *= ratio;
		int iAtom = model.iAtom;
		int iPair = 0;
		boolean iMovable = false;
		if (((model.hasEmbeddedMovie() && model.movie.getCurrentFrameIndex() == model.movie.length() - 1) || !model
				.hasEmbeddedMovie())
				&& (model.job != null && !model.job.isStopped())) {
			// neighbor list can be used
			int jnab, j;
			for (int i = 0; i < iAtom - 1; i++) {
				iMovable = atom[i].isMovable();
				rxi = atom[i].rx;
				ryi = atom[i].ry;
				rzi = atom[i].rz;
				jbeg = pointer[i];
				jend = pointer[i + 1];
				if (jbeg < jend) {
					for (jnab = jbeg; jnab < jend; jnab++) {
						j = neighborList[jnab];
						if (!iMovable && !atom[j].isMovable())
							continue;
						if (atom[i].isBonded(atom[j]) || atom[i].isABonded(atom[j]) || atom[i].isTBonded(atom[j]))
							continue;
						if (atom[i].charge * atom[j].charge <= 0) {
							rxij = rxi - atom[j].rx;
							ryij = ryi - atom[j].ry;
							rzij = rzi - atom[j].rz;
							rijsq = rxij * rxij + ryij * ryij + rzij * rzij;
							// sigab = 0.001f * (JmolConstants.vanderwaalsMars[atom[i].getElementNumber()] +
							// JmolConstants.vanderwaalsMars[atom[j].getElementNumber()]);
							sigab = (atom[i].sigma + atom[j].sigma) * 0.5f;
							sigab *= sigab;
							if (rijsq > sigab && rijsq < sigab * ratio) {
								pairs[iPair].setIndices(i, j);
								iPair++;
								if (iPair >= pairs.length)
									expandPairs();
							}
						}
					}
				}
			}
		}
		else {
			// neighbor list cannot be used
			int i, j;
			for (i = 0; i < iAtom - 1; i++) {
				iMovable = atom[i].isMovable();
				rxi = atom[i].rx;
				ryi = atom[i].ry;
				rzi = atom[i].rz;
				for (j = i + 1; j < iAtom; j++) {
					if (!iMovable && !atom[j].isMovable())
						continue;
					if (atom[i].isBonded(atom[j]) || atom[i].isABonded(atom[j]) || atom[i].isTBonded(atom[j]))
						continue;
					if (atom[i].charge * atom[j].charge <= 0) {
						rxij = rxi - atom[j].rx;
						ryij = ryi - atom[j].ry;
						rzij = rzi - atom[j].rz;
						rijsq = rxij * rxij + ryij * ryij + rzij * rzij;
						// sigab = 0.001f * (JmolConstants.vanderwaalsMars[atom[i].getElementNumber()] +
						// JmolConstants.vanderwaalsMars[atom[j].getElementNumber()]);
						sigab = (atom[i].sigma + atom[j].sigma) * 0.5f;
						sigab *= sigab;
						if (rijsq > sigab && rijsq < sigab * ratio) {
							pairs[iPair].setIndices(i, j);
							iPair++;
							if (iPair >= pairs.length)
								expandPairs();
						}
					}
				}
			}
		}
		return pairs;
	}

	private void minimumImageConvention() {
		if (rxij > xbox * 0.5f) {
			rxij -= xbox;
		}
		if (rxij <= -xbox * 0.5f) {
			rxij += xbox;
		}
		if (ryij > ybox * 0.5f) {
			ryij -= ybox;
		}
		if (ryij <= -ybox * 0.5f) {
			ryij += ybox;
		}
		if (rzij > zbox * 0.5f) {
			rzij -= zbox;
		}
		if (rzij <= -zbox * 0.5f) {
			rzij += zbox;
		}
	}

	synchronized float compute(int time) {

		int iAtom = model.iAtom;

		if (iAtom < 0)
			return 0.0f;

		int movableCount = 0;
		for (int i = 0; i < iAtom; i++) {
			atom[i].fx = 0.0f;
			atom[i].fy = 0.0f;
			atom[i].fz = 0.0f;
			if (atom[i].isMovable())
				movableCount++;
		}
		if (movableCount == 0)
			return 0;

		checkNeighborList(iAtom);

		float vsum = 0.0f;
		float coul;
		boolean iMovable = true;
		virialLJ = 0.0f;

		isPBC = model.boundaryType == MolecularModel.PERIODIC_BOUNDARY;

		if (updateList) {

			for (int i = 0; i < iAtom; i++) {
				rx0[i] = atom[i].rx;
				ry0[i] = atom[i].ry;
				rz0[i] = atom[i].rz;
			}

			nlist = 0;

			for (int i = 0, iAtom1 = iAtom - 1; i < iAtom1; i++) {

				pointer[i] = nlist;

				rxi = atom[i].rx;
				ryi = atom[i].ry;
				rzi = atom[i].rz;
				fxi = atom[i].fx;
				fyi = atom[i].fy;
				fzi = atom[i].fz;

				iMovable = atom[i].isMovable();

				for (int j = i + 1; j < iAtom; j++) {

					if (!iMovable && !atom[j].isMovable())
						continue;
					if (atom[i].isBonded(atom[j]) || atom[i].isABonded(atom[j]))
						continue;
					// do not compute LJ and Coulombic forces for bonded pairs.
					// if there are many bonds, perhaps we should pre-generate a bonding table.

					rxij = rxi - atom[j].rx;
					ryij = ryi - atom[j].ry;
					rzij = rzi - atom[j].rz;

					/* applying the minimum image conventions */
					if (isPBC) {
						minimumImageConvention();
					}

					rijsq = rxij * rxij + ryij * ryij + rzij * rzij;

					sigmai = atom[i].sigma;
					sigmaj = atom[j].sigma;
					sigmaij = sigmai * sigmaj;

					if (rijsq < rList * rList * sigmaij) {
						neighborList[nlist++] = j;
					}

					if (rijsq < rCutOffSq * sigmaij) {

						sigab = 0.5f * (sigmai + sigmaj);
						sigab *= sigab;
						sr2 = sigab / rijsq;
						/* check if this pair gets too close */
						if (sr2 > 2.0f) {
							sr2 = 2.0f;
							rijsq = 0.5f * sigab;
						}
						sr6 = sr2 * sr2 * sr2;
						sr12 = sr6 * sr6;
						// geometric mean is costly
						// epsab = 4 * (float) Math.sqrt(atom[i].epsilon * atom[j].epsilon);
						// use arithmetic mean instead
						epsab = 2.0f * (atom[i].epsilon + atom[j].epsilon);
						vij = (sr12 - sr6) * epsab;
						wij = vij + sr12 * epsab;

						/*
						 * if(cutOffShift){ vij-=poten_LJ[atom[i].getID()][atom[j].getID()];
						 * wij-=slope_LJ[atom[i].getID()][atom[j].getID()]; }
						 */

						vsum += vij;
						fij = wij / rijsq * SIX_TIMES_UNIT_FORCE;
						fxij = fij * rxij;
						fyij = fij * ryij;
						fzij = fij * rzij;
						fxi += fxij;
						fyi += fyij;
						fzi += fzij;
						atom[j].fx -= fxij;
						atom[j].fy -= fyij;
						atom[j].fz -= fzij;

					}

					if (model.coulombicIsOn) {
						if (Math.abs(atom[i].charge) + Math.abs(atom[j].charge) < MolecularModel.ZERO)
							continue;
						coul = COULOMB_CONSTANT * atom[i].charge * atom[j].charge / (float) Math.sqrt(rijsq);
						vsum += coul;
						virialEL += coul;
						fij = coul / rijsq * GF_CONVERSION_CONSTANT;
						fxij = fij * rxij;
						fyij = fij * ryij;
						fzij = fij * rzij;
						fxi += fxij;
						fyi += fyij;
						fzi += fzij;
						atom[j].fx -= fxij;
						atom[j].fy -= fyij;
						atom[j].fz -= fzij;
					}

				}

				atom[i].fx = fxi;
				atom[i].fy = fyi;
				atom[i].fz = fzi;

			}

			if (iAtom > 0)
				pointer[iAtom - 1] = nlist;

		}
		else {

			int j;

			for (int i = 0, iAtom1 = iAtom - 1; i < iAtom1; i++) {

				iMovable = atom[i].isMovable();

				rxi = atom[i].rx;
				ryi = atom[i].ry;
				rzi = atom[i].rz;
				fxi = atom[i].fx;
				fyi = atom[i].fy;
				fzi = atom[i].fz;

				jbeg = pointer[i];
				jend = pointer[i + 1];

				if (jbeg < jend) {

					for (int jnab = jbeg; jnab < jend; jnab++) {

						j = neighborList[jnab];
						if (!iMovable && !atom[j].isMovable())
							continue;
						if (atom[i].isBonded(atom[j]) || atom[i].isABonded(atom[j]))
							continue;
						// do not compute LJ for bonded pairs.
						// there can be many bonds, perhaps we should pre-generate a bonding table.

						rxij = rxi - atom[j].rx;
						ryij = ryi - atom[j].ry;
						rzij = rzi - atom[j].rz;

						if (isPBC) {
							minimumImageConvention();
						}
						rijsq = rxij * rxij + ryij * ryij + rzij * rzij;

						sigmai = atom[i].sigma;
						sigmaj = atom[j].sigma;

						if (rijsq < rCutOffSq * sigmai * sigmaj) {

							sigab = 0.5f * (sigmai + sigmaj);
							sigab *= sigab;
							sr2 = sigab / rijsq;
							/* check if this pair gets too close */
							if (sr2 > 2.0f) {
								sr2 = 2.0f;
								rijsq = 0.5f * sigab;
							}
							sr6 = sr2 * sr2 * sr2;
							sr12 = sr6 * sr6;
							// geometric mean is costly
							// epsab = 4 * (float) Math.sqrt(atom[i].epsilon * atom[j].epsilon);
							// use arithmetic mean instead
							epsab = 2.0f * (atom[i].epsilon + atom[j].epsilon);
							vij = (sr12 - sr6) * epsab;
							wij = vij + sr12 * epsab;

							/*
							 * if(cutOffShift){ vij-=poten_LJ[atom[i].getID()][atom[j].getID()];
							 * wij-=slope_LJ[atom[i].getID()][atom[j].getID()]; }
							 */

							vsum += vij;
							virialLJ += wij;

							fij = wij / rijsq * SIX_TIMES_UNIT_FORCE;
							fxij = fij * rxij;
							fyij = fij * ryij;
							fzij = fij * rzij;
							fxi += fxij;
							fyi += fyij;
							fzi += fzij;
							atom[j].fx -= fxij;
							atom[j].fy -= fyij;
							atom[j].fz -= fzij;

						}
					}
				}

				// do not use neighbor list for computing Coulombic forces
				if (model.coulombicIsOn) {

					for (j = i + 1; j < iAtom; j++) {
						if (Math.abs(atom[i].charge) + Math.abs(atom[j].charge) < MolecularModel.ZERO)
							continue;
						if (!iMovable && !atom[j].isMovable())
							continue;
						if (atom[i].isBonded(atom[j]) || atom[i].isABonded(atom[j]))
							continue;
						rxij = rxi - atom[j].rx;
						ryij = ryi - atom[j].ry;
						rzij = rzi - atom[j].rz;
						if (isPBC) {
							minimumImageConvention();
						}
						rijsq = rxij * rxij + ryij * ryij + rzij * rzij;
						coul = COULOMB_CONSTANT * atom[i].charge * atom[j].charge / (float) Math.sqrt(rijsq);
						vsum += coul;
						virialEL += coul;
						fij = coul / rijsq * GF_CONVERSION_CONSTANT;
						fxij = fij * rxij;
						fyij = fij * ryij;
						fzij = fij * rzij;
						fxi += fxij;
						fyi += fyij;
						fzi += fzij;
						atom[j].fx -= fxij;
						atom[j].fy -= fyij;
						atom[j].fz -= fzij;
					}

				}

				atom[i].fx = fxi;
				atom[i].fy = fyi;
				atom[i].fz = fzi;

			}

		}

		for (int i = 0; i < iAtom; i++) {
			if (atom[i].isMovable()) {
				vsum += computeFields(atom[i]);
				inverseMass1 = 1.0f / atom[i].mass;
				atom[i].fx *= inverseMass1;
				atom[i].fy *= inverseMass1;
				atom[i].fz *= inverseMass1;
			}
		}

		// must be after the above procedure because the mass is divided separately for the following forces
		// each of the following routines consume much less time than the vdw calculations (for <1000 r, a, t-bonds)
		vsum += calculateRBonds();
		vsum += calculateABonds();
		vsum += calculateTBonds();

		virialLJ *= 3.0f;

		return vsum / movableCount;

	}

	private float computeFields(Atom a) {
		float v = 0;
		if (model.bField != null)
			v += model.bField.compute(a);
		if (model.eField != null)
			v += model.eField.compute(a);
		if (model.gField != null)
			v += model.gField.compute(a);
		return v;
	}

	// v(r)=k*(r-r_0)^2/2
	private float calculateRBonds() {
		int n = model.rBonds.size();
		if (n <= 0)
			return 0;
		float energy = 0;
		RBond rBond;
		synchronized (model.rBonds) {
			for (int i = 0; i < n; i++) {
				rBond = model.rBonds.get(i);
				atom1 = rBond.getAtom1();
				atom2 = rBond.getAtom2();
				if (!atom1.isMovable() && !atom2.isMovable())
					continue;
				length = rBond.getLength();
				strength = rBond.getStrength();
				rxij = atom2.rx - atom1.rx;
				ryij = atom2.ry - atom1.ry;
				rzij = atom2.rz - atom1.rz;
				wij = (float) Math.sqrt(rxij * rxij + ryij * ryij + rzij * rzij);
				sr2 = strength * GF_CONVERSION_CONSTANT * (wij - length) / wij; // reuse sr2
				inverseMass1 = 1.0f / atom1.mass;
				inverseMass2 = 1.0f / atom2.mass;
				atom1.fx += sr2 * rxij * inverseMass1;
				atom1.fy += sr2 * ryij * inverseMass1;
				atom1.fz += sr2 * rzij * inverseMass1;
				atom2.fx -= sr2 * rxij * inverseMass2;
				atom2.fy -= sr2 * ryij * inverseMass2;
				atom2.fz -= sr2 * rzij * inverseMass2;
				wij -= length;
				energy += strength * wij * wij;
			}
		}
		return energy * 0.5f;
	}

	private float calculateABonds() {

		int n = model.aBonds.size();
		if (n <= 0)
			return 0;
		ABond aBond;
		float energy = 0;

		synchronized (model.aBonds) {

			for (int i = 0; i < n; i++) {

				aBond = model.aBonds.get(i);
				atom1 = aBond.getAtom1();
				atom2 = aBond.getAtom2();
				atom3 = aBond.getAtom3();
				if (!atom1.isMovable() && !atom2.isMovable() && !atom3.isMovable())
					continue;
				angle = aBond.getAngle();
				strength = aBond.getStrength();
				rxij = atom1.rx - atom2.rx;
				ryij = atom1.ry - atom2.ry;
				rzij = atom1.rz - atom2.rz;
				vector1.set(rxij, ryij, rzij);
				rxkj = atom3.rx - atom2.rx;
				rykj = atom3.ry - atom2.ry;
				rzkj = atom3.rz - atom2.rz;
				vector2.set(rxkj, rykj, rzkj);
				rijsq = rxij * rxij + ryij * ryij + rzij * rzij;
				rkjsq = rxkj * rxkj + rykj * rykj + rzkj * rzkj;
				rij = (float) Math.sqrt(rijsq);
				rkj = (float) Math.sqrt(rkjsq);
				sr2 = rxij * rxkj + ryij * rykj + rzij * rzkj;
				theta = vector1.angle(vector2);
				sintheta = (float) Math.sin(theta);
				if (Math.abs(sintheta) < MIN_SINTHETA) {// zero or 180 degree disaster
					sintheta = sintheta > 0 ? MIN_SINTHETA : -MIN_SINTHETA;
				}
				sr12 = theta - angle;
				sr6 = strength * sr12 / (sintheta * rij * rkj);

				rijsq = 1.0f / rijsq;
				rkjsq = 1.0f / rkjsq;
				fxi = sr6 * (rxkj - sr2 * rxij * rijsq);
				fyi = sr6 * (rykj - sr2 * ryij * rijsq);
				fzi = sr6 * (rzkj - sr2 * rzij * rijsq);
				fxk = sr6 * (rxij - sr2 * rxkj * rkjsq);
				fyk = sr6 * (ryij - sr2 * rykj * rkjsq);
				fzk = sr6 * (rzij - sr2 * rzkj * rkjsq);

				inverseMass1 = GF_CONVERSION_CONSTANT / atom1.mass;
				inverseMass2 = GF_CONVERSION_CONSTANT / atom2.mass;
				inverseMass3 = GF_CONVERSION_CONSTANT / atom3.mass;
				atom1.fx += fxi * inverseMass1;
				atom1.fy += fyi * inverseMass1;
				atom1.fz += fzi * inverseMass1;
				atom3.fx += fxk * inverseMass3;
				atom3.fy += fyk * inverseMass3;
				atom3.fz += fzk * inverseMass3;
				atom2.fx -= (fxi + fxk) * inverseMass2;
				atom2.fy -= (fyi + fyk) * inverseMass2;
				atom2.fz -= (fzi + fzk) * inverseMass2;

				energy += strength * sr12 * sr12;

			}

		}

		return energy * 0.5f;

	}

	/*
	 * Important note: to save some computation, we do not actually compute the diheral angle. Instead, we compute the
	 * direct angle between A-B bond and C-D bond, which is much faster. The result of this simplification is that one
	 * will have to be careful in seting the equilibrium angle to be that between A-B and C-D. If this is taken care
	 * correctly, there should not be any adverse effect caused by this simplification.
	 */
	private float calculateTBonds() {

		int n = model.tBonds.size();
		if (n <= 0)
			return 0;
		TBond tBond;
		float energy = 0;

		synchronized (model.tBonds) {

			for (int i = 0; i < n; i++) {

				tBond = model.tBonds.get(i);
				atom1 = tBond.getAtom1();
				atom2 = tBond.getAtom2();
				atom3 = tBond.getAtom3();
				atom4 = tBond.getAtom4();
				if (!atom1.isMovable() && !atom2.isMovable() && !atom3.isMovable() && !atom4.isMovable())
					continue;
				angle = tBond.getAngle();
				strength = tBond.getStrength();
				rxij = atom1.rx - atom2.rx;
				ryij = atom1.ry - atom2.ry;
				rzij = atom1.rz - atom2.rz;
				vector1.set(rxij, ryij, rzij);
				rxlk = atom4.rx - atom3.rx;
				rylk = atom4.ry - atom3.ry;
				rzlk = atom4.rz - atom3.rz;
				vector2.set(rxlk, rylk, rzlk);
				rijsq = rxij * rxij + ryij * ryij + rzij * rzij;
				rlksq = rxlk * rxlk + rylk * rylk + rzlk * rzlk;
				rij = (float) Math.sqrt(rijsq);
				rlk = (float) Math.sqrt(rlksq);
				sr2 = rxij * rxlk + ryij * rylk + rzij * rzlk;
				theta = vector1.angle(vector2);
				sintheta = (float) Math.sin(theta);
				if (Math.abs(sintheta) < MIN_SINTHETA) {// zero or 180 degree disaster
					sintheta = sintheta > 0 ? MIN_SINTHETA : -MIN_SINTHETA;
				}
				sr12 = (float) Math.sin(tBond.getPeriodicity() * theta - tBond.getAngle());
				sr6 = 0.5f * tBond.getPeriodicity() * strength * sr12 / (rij * rlk * sintheta);

				rijsq = 1.0f / rijsq;
				rlksq = 1.0f / rlksq;
				fxi = sr6 * (rxlk - sr2 * rxij * rijsq);
				fyi = sr6 * (rylk - sr2 * ryij * rijsq);
				fzi = sr6 * (rzlk - sr2 * rzij * rijsq);
				fxl = sr6 * (rxij - sr2 * rxlk * rlksq);
				fyl = sr6 * (ryij - sr2 * rylk * rlksq);
				fzl = sr6 * (rzij - sr2 * rzlk * rlksq);

				inverseMass1 = GF_CONVERSION_CONSTANT / atom1.mass;
				inverseMass2 = GF_CONVERSION_CONSTANT / atom2.mass;
				inverseMass3 = GF_CONVERSION_CONSTANT / atom3.mass;
				inverseMass4 = GF_CONVERSION_CONSTANT / atom4.mass;
				atom1.fx += fxi * inverseMass1;
				atom1.fy += fyi * inverseMass1;
				atom1.fz += fzi * inverseMass1;
				atom4.fx += fxl * inverseMass4;
				atom4.fy += fyl * inverseMass4;
				atom4.fz += fzl * inverseMass4;
				atom2.fx -= fxi * inverseMass2;
				atom2.fy -= fyi * inverseMass2;
				atom2.fz -= fzi * inverseMass2;
				atom3.fx -= fxl * inverseMass3;
				atom3.fy -= fyl * inverseMass3;
				atom3.fz -= fzl * inverseMass3;

				// note that we use 1-cos(...) instead of 1+cos(...) as used on the following page:
				// http://en.wikipedia.org/wiki/AMBER
				// This reduced the equilibrium energy to zero, as in the case of radial and angular bonds
				energy += strength * (1.0f - Math.cos(tBond.getPeriodicity() * theta - tBond.getAngle()));

			}

		}

		return 0.5f * energy;

	}

	private void checkNeighborList(int iAtom) {
		float dispmax = 0.0f;
		float invsig = 1.0f;
		for (int i = 0; i < iAtom; i++) {
			if (atom[i].isMovable()) {
				invsig = 1.0f / atom[i].sigma;
				dispmax = Math.max(dispmax, Math.abs(atom[i].rx - rx0[i]) * invsig);
				dispmax = Math.max(dispmax, Math.abs(atom[i].ry - ry0[i]) * invsig);
				dispmax = Math.max(dispmax, Math.abs(atom[i].rz - rz0[i]) * invsig);
			}
		}
		dispmax = 2.0f * (float) Math.sqrt(3.0 * dispmax * dispmax);
		updateList = dispmax > rList - rCutOff; // rList & rCutOff are relative to sigma
	}

}