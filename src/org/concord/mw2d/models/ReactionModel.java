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

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;

import org.concord.modeler.math.Vector2D;
import org.concord.modeler.process.AbstractLoadable;
import org.concord.modeler.process.Loadable;
import org.concord.mw2d.AtomisticView;
import org.concord.mw2d.event.ParameterChangeEvent;

import static org.concord.mw2d.models.Element.ID_NT;
import static org.concord.mw2d.models.Element.ID_PL;
import static org.concord.mw2d.models.Element.ID_WS;
import static org.concord.mw2d.models.Element.ID_CK;

public class ReactionModel extends MolecularModel {

	public final static String REACT = "Reaction Dynamics";

	private final static float H2O_ANGLE = 109.47f / 180.0f * (float) Math.PI;
	private final static float ANGLE_TOLL = (float) (0.05 * Math.PI);
	private final static float BOND_LENGTH_RATIO = 0.6f;
	private boolean allowUselessCollision;
	private boolean conserveEnergy = true;
	Reaction type;

	/* variables shared among different methods (to reduce garbage) */
	private RadialBond rBond;// the current bond in a bond iteration
	private boolean bondChanged; // flag indicating bond structure has changed
	private double xij, yij, rij, rijsr; // store coordinate of current pair
	private double vxij, vyij, vxy; // store velocity of current pair
	private double bondEnergy;
	private double bondLength;
	private double bondStrength;
	private boolean bonded;
	private double dpot;
	private double ecol;
	private Vector2D vvi = new Vector2D();
	private Vector2D vvj = new Vector2D();
	private Vector2D vrij = new Vector2D();

	/* the subtask of making the reaction */
	private Loadable react = new AbstractLoadable(20) {
		public void execute() {
			if (type != null)
				react();
		}

		public String getName() {
			return REACT;
		}

		public int getLifetime() {
			return ETERNAL;
		}

		public String getDescription() {
			return "This task realizes the reaction dynamics by searching periodically in the phase\n"
					+ "space the critical conditions for the elementary. The critical conditions are\n"
					+ "created by reactive collisions or UV photolysis.\n\n"
					+ "Note: You may decrease the interval in order not to miss any reactive collision,\n"
					+ "or increase the interval if speed is more important than accuracy.";
		}
	};

	public ReactionModel() {
		super();
		setType(new Reaction.A2_B2__2AB());
		init();
	}

	public ReactionModel(int xbox, int ybox, int tapeLength) {
		super(xbox, ybox, tapeLength);
		setType(new Reaction.A2_B2__2AB());
		init();
	}

	private void init() {
		Action a = new ReactionDirectionAction(this);
		choiceMap.put(a.toString(), a);
		addTimeSeries();
	}

	/** any non-bonded atom in this container is supposed to be a free radical */
	public void reassertFreeRadicals() {
		for (int i = 0; i < numberOfAtoms; i++) {
			if (!atom[i].isBonded())
				atom[i].setRadical(true);
		}
	}

	public void run() {
		super.run();
		List<RadialBond.Delegate> list = new ArrayList<RadialBond.Delegate>();
		synchronized (bonds.getSynchronizationLock()) {
			for (Iterator it = bonds.iterator(); it.hasNext();)
				list.add(new RadialBond.Delegate((RadialBond) it.next()));
		}
		stateHolder.setBonds(list);
	}

	public boolean revert() {
		if (type instanceof Reaction.A2_B2__2AB) {
			Reaction.A2_B2__2AB r = (Reaction.A2_B2__2AB) type;
			r.moleFractionA2().setPointer(0);
			r.moleFractionB2().setPointer(0);
			r.moleFractionAB().setPointer(0);
			r.numberOfA2().setPointer(0);
			r.numberOfB2().setPointer(0);
			r.numberOfAB().setPointer(0);
		}
		else if (type instanceof Reaction.A2_B2_C__2AB_C) {
			Reaction.A2_B2_C__2AB_C r = (Reaction.A2_B2_C__2AB_C) type;
			r.moleFractionA2().setPointer(0);
			r.moleFractionB2().setPointer(0);
			r.moleFractionAB().setPointer(0);
			r.numberOfA2().setPointer(0);
			r.numberOfB2().setPointer(0);
			r.numberOfAB().setPointer(0);
			r.numberOfC().setPointer(0);
		}
		else if (type instanceof Reaction.O2_2H2__2H2O) {
			Reaction.O2_2H2__2H2O r = (Reaction.O2_2H2__2H2O) type;
			r.moleFractionH2().setPointer(0);
			r.moleFractionO2().setPointer(0);
			r.moleFractionH2O().setPointer(0);
			r.numberOfH2().setPointer(0);
			r.numberOfO2().setPointer(0);
			r.numberOfH2O().setPointer(0);
		}
		bonds.getBondQueue().setPointer(0);
		for (int i = 0; i < numberOfAtoms; i++)
			atom[i].moveRadicalPointer(0);
		List list = stateHolder.getBonds();
		if (list == null)
			return false;
		synchronized (bonds.getSynchronizationLock()) {
			for (Iterator it = bonds.iterator(); it.hasNext();)
				((RadialBond) it.next()).destroy();
		}
		bonds.clear();
		RadialBond.Delegate rbd = null;
		RadialBond rBond = null;
		for (Iterator it = list.iterator(); it.hasNext();) {
			rbd = (RadialBond.Delegate) it.next();
			rBond = new RadialBond(atom[rbd.getAtom1()], atom[rbd.getAtom2()], rbd.getBondLength(), rbd
					.getBondStrength(), rbd.getChemicalEnergy());
			rBond.setModel(this);
			bonds.add(rBond);
		}
		MoleculeCollection.sort(this);
		return super.revert();
	}

	void record() {
		super.record();
		updateAllReactionQ();
		count();
	}

	private void updateAllReactionQ() {
		int c = movie.getCapacity();
		try {
			bonds.updateBondQ();
		}
		catch (Exception e) {
			bonds.initializeBondQ(c);
			bonds.updateBondQ();
		}
		try {
			bends.updateBondQ();
		}
		catch (Exception e) {
			bends.initializeBondQ(c);
			bends.updateBondQ();
		}
		for (int i = 0; i < numberOfAtoms; i++) {
			try {
				atom[i].updateRadicalQ();
			}
			catch (Exception e) {
				atom[i].initializeRadicalQ(c);
				atom[i].updateRadicalQ();
			}
		}
	}

	public void activateHeatBath(boolean b) {
		super.activateHeatBath(b);
		conserveEnergy = !b;
	}

	void initializeJob() {
		super.initializeJob();
		if (!job.contains(react))
			job.add(react);
	}

	public void parameterChanged(ParameterChangeEvent e) {
		super.parameterChanged(e);
		String name = e.getParameterName();
		if ("Energy Parameters".equals(name))
			setType((Reaction) e.getNewValue());
	}

	private void setQueueLength(int n) {
		if (type instanceof Reaction.A2_B2__2AB) {
			Reaction.A2_B2__2AB r = (Reaction.A2_B2__2AB) type;
			if (r.moleFractionA2() != null) {
				r.moleFractionA2().setLength(n);
				r.moleFractionB2().setLength(n);
				r.moleFractionAB().setLength(n);
				r.numberOfA2().setLength(n);
				r.numberOfB2().setLength(n);
				r.numberOfAB().setLength(n);
			}
		}
		else if (type instanceof Reaction.A2_B2_C__2AB_C) {
			Reaction.A2_B2_C__2AB_C r = (Reaction.A2_B2_C__2AB_C) type;
			if (r.moleFractionA2() != null) {
				r.moleFractionA2().setLength(n);
				r.moleFractionB2().setLength(n);
				r.moleFractionAB().setLength(n);
				r.numberOfA2().setLength(n);
				r.numberOfB2().setLength(n);
				r.numberOfAB().setLength(n);
				r.numberOfC().setLength(n);
			}
		}
		else if (type instanceof Reaction.O2_2H2__2H2O) {
			Reaction.O2_2H2__2H2O r = (Reaction.O2_2H2__2H2O) type;
			if (r.moleFractionH2() != null) {
				r.moleFractionH2().setLength(n);
				r.moleFractionO2().setLength(n);
				r.moleFractionH2O().setLength(n);
				r.numberOfH2().setLength(n);
				r.numberOfO2().setLength(n);
				r.numberOfH2O().setLength(n);
			}
		}
		bonds.initializeBondQ(n);
	}

	public void activateEmbeddedMovie(boolean b) {
		super.activateEmbeddedMovie(b);
		if (b) {
			addTimeSeries();
			int n = movie.getCapacity();
			for (int i = 0; i < numberOfAtoms; i++)
				atom[i].initializeRadicalQ(n);
			setQueueLength(n);
		}
		else {
			for (int i = 0; i < numberOfAtoms; i++)
				atom[i].initializeRadicalQ(-1);
			setQueueLength(-1);
		}
	}

	private void addTimeSeries() {
		int m = movieUpdater.getInterval();
		if (type instanceof Reaction.A2_B2__2AB) {
			Reaction.A2_B2__2AB r = (Reaction.A2_B2__2AB) type;
			r.init(movie.getCapacity(), modelTimeQueue);
			r.moleFractionA2().setInterval(m);
			r.moleFractionB2().setInterval(m);
			r.moleFractionAB().setInterval(m);
			r.numberOfA2().setInterval(m);
			r.numberOfB2().setInterval(m);
			r.numberOfAB().setInterval(m);
			movieQueueGroup.add(r.moleFractionA2());
			movieQueueGroup.add(r.moleFractionB2());
			movieQueueGroup.add(r.moleFractionAB());
			movieQueueGroup.add(r.numberOfA2());
			movieQueueGroup.add(r.numberOfB2());
			movieQueueGroup.add(r.numberOfAB());
		}
		else if (type instanceof Reaction.A2_B2_C__2AB_C) {
			Reaction.A2_B2_C__2AB_C r = (Reaction.A2_B2_C__2AB_C) type;
			r.init(movie.getCapacity(), modelTimeQueue);
			r.moleFractionA2().setInterval(m);
			r.moleFractionB2().setInterval(m);
			r.moleFractionAB().setInterval(m);
			r.numberOfA2().setInterval(m);
			r.numberOfB2().setInterval(m);
			r.numberOfAB().setInterval(m);
			r.numberOfC().setInterval(m);
			movieQueueGroup.add(r.moleFractionA2());
			movieQueueGroup.add(r.moleFractionB2());
			movieQueueGroup.add(r.moleFractionAB());
			movieQueueGroup.add(r.numberOfA2());
			movieQueueGroup.add(r.numberOfB2());
			movieQueueGroup.add(r.numberOfAB());
			movieQueueGroup.add(r.numberOfC());
		}
		else if (type instanceof Reaction.O2_2H2__2H2O) {
			Reaction.O2_2H2__2H2O r = (Reaction.O2_2H2__2H2O) type;
			r.init(movie.getCapacity(), modelTimeQueue);
			r.moleFractionH2().setInterval(m);
			r.moleFractionO2().setInterval(m);
			r.moleFractionH2O().setInterval(m);
			r.numberOfH2().setInterval(m);
			r.numberOfO2().setInterval(m);
			r.numberOfH2O().setInterval(m);
			movieQueueGroup.add(r.moleFractionH2());
			movieQueueGroup.add(r.moleFractionO2());
			movieQueueGroup.add(r.moleFractionH2O());
			movieQueueGroup.add(r.numberOfH2());
			movieQueueGroup.add(r.numberOfO2());
			movieQueueGroup.add(r.numberOfH2O());
		}
	}

	public void destroy() {
		super.destroy();
		if (bonds.getBondQueue() != null)
			bonds.getBondQueue().setLength(-1);
	}

	public String toString() {
		return "<Reaction Model> " + getProperty("filename");
	}

	public void setAllowUselessCollision(boolean b) {
		allowUselessCollision = b;
	}

	public boolean getAllowUselessCollision() {
		return allowUselessCollision;
	}

	/**
	 * When the heat bath is turned on, set "conserveEnergy" to be false. Doing energy conservation when the heat bath
	 * is activated is a waste of time.
	 */
	public void setConserveEnergy(boolean b) {
		conserveEnergy = b;
	}

	public boolean getConserveEnergy() {
		return conserveEnergy;
	}

	/**
	 * Several reaction types are supported. If null is passed, this class effectively falls back to the
	 * <code>MolecularModel</code> class.
	 */
	public void setType(Reaction type) {
		this.type = type;
		if (type instanceof Reaction.A2_B2__2AB) {
			double vAA = type.getParameter(Reaction.A2_B2__2AB.VAA).doubleValue();
			double vBB = type.getParameter(Reaction.A2_B2__2AB.VBB).doubleValue();
			double vAB = type.getParameter(Reaction.A2_B2__2AB.VAB).doubleValue();
			react.setInterval(type.getFrequency());
			int idA = ((Reaction.A2_B2__2AB) type).getIDA();
			int idB = ((Reaction.A2_B2__2AB) type).getIDB();
			synchronized (bonds.getSynchronizationLock()) {
				for (Iterator it = bonds.iterator(); it.hasNext();) {
					rBond = (RadialBond) it.next();
					if (rBond.atom1.getID() == idA && rBond.atom2.getID() == idA) {
						rBond.setChemicalEnergy(vAA);
					}
					else if (rBond.atom1.getID() == idB && rBond.atom2.getID() == idB) {
						rBond.setChemicalEnergy(vBB);
					}
					else if ((rBond.atom1.getID() == idA && rBond.atom2.getID() == idB)
							|| (rBond.atom1.getID() == idB && rBond.atom2.getID() == idA)) {
						rBond.setChemicalEnergy(vAB);
					}
				}
			}
		}
		else if (type instanceof Reaction.O2_2H2__2H2O) {
			double vHH = type.getParameter(Reaction.O2_2H2__2H2O.VHH).doubleValue();
			double vOO = type.getParameter(Reaction.O2_2H2__2H2O.VOO).doubleValue();
			double vHO = type.getParameter(Reaction.O2_2H2__2H2O.VHO).doubleValue();
			react.setInterval(type.getFrequency());
			int idH = ((Reaction.O2_2H2__2H2O) type).getIDH();
			int idO = ((Reaction.O2_2H2__2H2O) type).getIDO();
			synchronized (bonds.getSynchronizationLock()) {
				for (Iterator it = bonds.iterator(); it.hasNext();) {
					rBond = (RadialBond) it.next();
					if (rBond.atom1.getID() == idH && rBond.atom2.getID() == idH) {
						rBond.setChemicalEnergy(vHH);
					}
					else if (rBond.atom1.getID() == idO && rBond.atom2.getID() == idO) {
						rBond.setChemicalEnergy(vOO);
					}
					else if ((rBond.atom1.getID() == idH && rBond.atom2.getID() == idO)
							|| (rBond.atom1.getID() == idO && rBond.atom2.getID() == idH)) {
						rBond.setChemicalEnergy(vHO);
					}
				}
			}
		}
		else if (type instanceof Reaction.A2_B2_C__2AB_C) {
			double vAA = type.getParameter(Reaction.A2_B2_C__2AB_C.VAA).doubleValue();
			double vBB = type.getParameter(Reaction.A2_B2_C__2AB_C.VBB).doubleValue();
			double vAB = type.getParameter(Reaction.A2_B2_C__2AB_C.VAB).doubleValue();
			double vAC = type.getParameter(Reaction.A2_B2_C__2AB_C.VAC).doubleValue();
			double vBC = type.getParameter(Reaction.A2_B2_C__2AB_C.VBC).doubleValue();
			react.setInterval(type.getFrequency());
			int idA = ((Reaction.A2_B2_C__2AB_C) type).getIDA();
			int idB = ((Reaction.A2_B2_C__2AB_C) type).getIDB();
			int idC = ((Reaction.A2_B2_C__2AB_C) type).getIDC();
			synchronized (bonds.getSynchronizationLock()) {
				for (Iterator it = bonds.iterator(); it.hasNext();) {
					rBond = (RadialBond) it.next();
					if (rBond.atom1.getID() == idA && rBond.atom2.getID() == idA) {
						rBond.setChemicalEnergy(vAA);
					}
					else if (rBond.atom1.getID() == idB && rBond.atom2.getID() == idB) {
						rBond.setChemicalEnergy(vBB);
					}
					else if ((rBond.atom1.getID() == idA && rBond.atom2.getID() == idB)
							|| (rBond.atom1.getID() == idB && rBond.atom2.getID() == idA)) {
						rBond.setChemicalEnergy(vAB);
					}
					else if ((rBond.atom1.getID() == idA && rBond.atom2.getID() == idC)
							|| (rBond.atom1.getID() == idC && rBond.atom2.getID() == idA)) {
						rBond.setChemicalEnergy(vAC);
					}
					else if ((rBond.atom1.getID() == idB && rBond.atom2.getID() == idC)
							|| (rBond.atom1.getID() == idC && rBond.atom2.getID() == idB)) {
						rBond.setChemicalEnergy(vBC);
					}
				}
			}
		}
		else if (type instanceof Reaction.nA__An) {
			double vAA = type.getParameter(Reaction.nA__An.VAA).doubleValue();
			double vBB = type.getParameter(Reaction.nA__An.VBB).doubleValue();
			double vCC = type.getParameter(Reaction.nA__An.VCC).doubleValue();
			double vDD = type.getParameter(Reaction.nA__An.VDD).doubleValue();
			double vAB = type.getParameter(Reaction.nA__An.VAB).doubleValue();
			double vAC = type.getParameter(Reaction.nA__An.VAC).doubleValue();
			double vAD = type.getParameter(Reaction.nA__An.VAD).doubleValue();
			double vBC = type.getParameter(Reaction.nA__An.VBC).doubleValue();
			double vBD = type.getParameter(Reaction.nA__An.VBD).doubleValue();
			double vCD = type.getParameter(Reaction.nA__An.VCD).doubleValue();
			synchronized (bonds.getSynchronizationLock()) {
				for (Iterator it = bonds.iterator(); it.hasNext();) {
					rBond = (RadialBond) it.next();
					if (rBond.atom1.getID() == ID_NT && rBond.atom2.getID() == ID_NT) {
						rBond.setChemicalEnergy(vAA);
					}
					else if (rBond.atom1.getID() == ID_PL && rBond.atom2.getID() == ID_PL) {
						rBond.setChemicalEnergy(vBB);
					}
					else if (rBond.atom1.getID() == ID_WS && rBond.atom2.getID() == ID_WS) {
						rBond.setChemicalEnergy(vCC);
					}
					else if (rBond.atom1.getID() == ID_CK && rBond.atom2.getID() == ID_CK) {
						rBond.setChemicalEnergy(vDD);
					}
					else if ((rBond.atom1.getID() == ID_NT && rBond.atom2.getID() == ID_PL)
							|| (rBond.atom1.getID() == ID_PL && rBond.atom2.getID() == ID_NT)) {
						rBond.setChemicalEnergy(vAB);
					}
					else if ((rBond.atom1.getID() == ID_NT && rBond.atom2.getID() == ID_WS)
							|| (rBond.atom1.getID() == ID_WS && rBond.atom2.getID() == ID_NT)) {
						rBond.setChemicalEnergy(vAC);
					}
					else if ((rBond.atom1.getID() == ID_NT && rBond.atom2.getID() == ID_CK)
							|| (rBond.atom1.getID() == ID_CK && rBond.atom2.getID() == ID_NT)) {
						rBond.setChemicalEnergy(vAD);
					}
					else if ((rBond.atom1.getID() == ID_PL && rBond.atom2.getID() == ID_WS)
							|| (rBond.atom1.getID() == ID_WS && rBond.atom2.getID() == ID_PL)) {
						rBond.setChemicalEnergy(vBC);
					}
					else if ((rBond.atom1.getID() == ID_PL && rBond.atom2.getID() == ID_CK)
							|| (rBond.atom1.getID() == ID_CK && rBond.atom2.getID() == ID_PL)) {
						rBond.setChemicalEnergy(vBD);
					}
					else if ((rBond.atom1.getID() == ID_WS && rBond.atom2.getID() == ID_CK)
							|| (rBond.atom1.getID() == ID_CK && rBond.atom2.getID() == ID_WS)) {
						rBond.setChemicalEnergy(vCD);
					}
				}
			}
		}
	}

	public Reaction getType() {
		return type;
	}

	public void react() {
		dissociate();
		scan();
		if (bondChanged) {
			MoleculeCollection.sort(this);
			bondChanged = false;
		}
	}

	/*
	 * <p> Scan the system to find the pairs of atoms that have reached the corresponding critical collision diameters
	 * (given by the Lorentz-Berthelot rule based on the van der Waals diameters). If such a pair of atoms is found,
	 * regardless of whether they are already bonded, sumbit their indices to the <code>collide(int, int)</code>
	 * method. </p> <p> The reason that this method does not check whether a pair of atoms belong to the same bond
	 * before entering the <code>collide</code> method is because searching through the <code>ArrayList</code>
	 * <code>bonds</code> to see if the atoms are bound in the same bond for every pair of atoms (N*(N-1)/2 pairs) can
	 * be much more expensive than calculating the distances of every pair and then searching the bonding status of the
	 * pairs that are within the collision diameters, the number of which is much smaller than N*(N-1)/2. </p> <p> The
	 * Verlet neighbor list method (same as that used in calculating the Lennard-Jones potentials) is used. </p>
	 */
	private void scan() {

		if (updateList) {

			for (int i = 0; i < numberOfAtoms - 1; i++) {
				for (int j = i + 1; j < numberOfAtoms; j++) {
					xij = atom[i].rx - atom[j].rx;
					yij = atom[i].ry - atom[j].ry;
					rij = xij * xij + yij * yij;
					bondLength = getBondLength(atom[i], atom[j]);
					if (rij <= bondLength * bondLength)
						collide(i, j);
				}
			}

		}
		else {

			int i, j, jbeg, jend, jnab;
			for (i = 0; i < numberOfAtoms - 1; i++) {
				jbeg = pointer[i];
				jend = pointer[i + 1];
				if (jbeg < jend) {
					for (jnab = jbeg; jnab < jend; jnab++) {
						j = neighborList[jnab];
						xij = atom[i].rx - atom[j].rx;
						yij = atom[i].ry - atom[j].ry;
						rij = xij * xij + yij * yij;
						bondLength = getBondLength(atom[i], atom[j]);
						if (rij <= bondLength * bondLength)
							collide(i, j);
					}
				}
			}

		}

	}

	/*
	 * given a pair of atoms (i, j) with an interatomic distance shorter than the collision diameter, figure out whether
	 * or not the atoms are moving towards each other, or are moving apart from each other. The reaction happens only in
	 * the first case, which is the beginning of the collision. IMPORTANT: This is where we apply rules. We consider
	 * ONLY two-body and three-body reaction steps.
	 */
	private void collide(int i, int j) {

		if (type instanceof Reaction.A2_B2__2AB) {

			int idA = ((Reaction.A2_B2__2AB) type).getIDA();
			int idB = ((Reaction.A2_B2__2AB) type).getIDB();

			if ((atom[i].getID() == idA || atom[i].getID() == idB)
					&& (atom[j].getID() == idA || atom[j].getID() == idB)) {

				if (atom[i].isRadical() && atom[j].isRadical()) {

					/* if both i and j are free radicals */

					vvi.setX(atom[i].vx);
					vvi.setY(atom[i].vy);
					vvj.setX(atom[j].vx);
					vvj.setY(atom[j].vy);
					vrij.setX(xij);
					vrij.setY(yij);
					if (willCollide(vvi, vvj, vrij))
						makeBond(i, j);

				}
				else {

					/* if at least one atom is not radical */

					if (!checkBond(i, j)) {

						vvi.setX(atom[i].vx);
						vvi.setY(atom[i].vy);
						vvj.setX(atom[j].vx);
						vvj.setY(atom[j].vy);
						vrij.setX(xij);
						vrij.setY(yij);
						if (willCollide(vvi, vvj, vrij))
							breakAndMakeBonds(i, j);

					}

				}

			}

		}

		else if (type instanceof Reaction.A2_B2_C__2AB_C) {

			int idA = ((Reaction.A2_B2_C__2AB_C) type).getIDA();
			int idB = ((Reaction.A2_B2_C__2AB_C) type).getIDB();
			int idC = ((Reaction.A2_B2_C__2AB_C) type).getIDC();

			if ((atom[i].getID() == idA && atom[j].getID() == idA)
					|| (atom[i].getID() == idB && atom[j].getID() == idB)
					|| (atom[i].getID() == idA && atom[j].getID() == idB)
					|| (atom[i].getID() == idB && atom[j].getID() == idA)
					|| (atom[i].getID() == idA && atom[j].getID() == idC)
					|| (atom[i].getID() == idC && atom[j].getID() == idA)
					|| (atom[i].getID() == idB && atom[j].getID() == idC)
					|| (atom[i].getID() == idC && atom[j].getID() == idB)) {

				if (atom[i].isRadical() && atom[j].isRadical()) {

					/* if both i and j are free radicals */

					vvi.setX(atom[i].vx);
					vvi.setY(atom[i].vy);
					vvj.setX(atom[j].vx);
					vvj.setY(atom[j].vy);
					vrij.setX(xij);
					vrij.setY(yij);
					if (willCollide(vvi, vvj, vrij))
						makeBond(i, j);

				}
				else {

					/* if at least one atom is not radical */

					if (!checkBond(i, j)) {

						vvi.setX(atom[i].vx);
						vvi.setY(atom[i].vy);
						vvj.setX(atom[j].vx);
						vvj.setY(atom[j].vy);
						vrij.setX(xij);
						vrij.setY(yij);
						if (willCollide(vvi, vvj, vrij))
							breakAndMakeBonds(i, j);

					}

				}

			}

		}

		else if (type instanceof Reaction.nA__An) {

			// in a polymerization reaction, monomers stick to only the ends of the chain
			if (!molecules.sameMolecule(atom[i], atom[j]) && bonds.getBondedPartnerCount(atom[i]) <= 1
					&& bonds.getBondedPartnerCount(atom[j]) <= 1) {

				vvi.setX(atom[i].vx);
				vvi.setY(atom[i].vy);
				vvj.setX(atom[j].vx);
				vvj.setY(atom[j].vy);
				vrij.setX(xij);
				vrij.setY(yij);
				if (willCollide(vvi, vvj, vrij))
					makeBond(i, j);

			}

		}

		else if (type instanceof Reaction.O2_2H2__2H2O) {

			int idH = ((Reaction.O2_2H2__2H2O) type).getIDH();
			int idO = ((Reaction.O2_2H2__2H2O) type).getIDO();

			if ((atom[i].getID() == idH || atom[i].getID() == idO)
					&& (atom[j].getID() == idH || atom[j].getID() == idO)) {

				if (atom[i].isRadical() && atom[j].isRadical()) {

					/* if both i and j are free radicals, chain termination. */

					vvi.setX(atom[i].vx);
					vvi.setY(atom[i].vy);
					vvj.setX(atom[j].vx);
					vvj.setY(atom[j].vy);
					vrij.setX(xij);
					vrij.setY(yij);
					if (willCollide(vvi, vvj, vrij))
						makeBond(i, j);

				}
				else {

					/* if at least one atom is not radical */

					if (((atom[i].getID() == idH && atom[j].getID() == idO) || (atom[j].getID() == idH && atom[i]
							.getID() == idO))
							&& !checkBond(i, j)) {

						vvi.setX(atom[i].vx);
						vvi.setY(atom[i].vy);
						vvj.setX(atom[j].vx);
						vvj.setY(atom[j].vy);
						vrij.setX(xij);
						vrij.setY(yij);
						if (willCollide(vvi, vvj, vrij)) {
							Atom[] at1 = bonds.getBondedPartners(atom[i], false);
							Atom[] at2 = bonds.getBondedPartners(atom[j], false);
							if (at1.length == 1 && at2.length == 1) {
								if (at1[0].getID() == idH && at2[0].getID() == idH) {
									if (atom[i].getID() == idO) {
										OH_H2(atom[i], at1[0], atom[j], at2[0]);
									}
									else {
										OH_H2(atom[j], at2[0], atom[i], at1[0]);
									}
								}
							}
							else if (at1.length == 1 && at2.length == 0 && atom[i].getID() == idO
									&& at1[0].getID() == idH) {
								H_OH(atom[i], atom[j], at1[0]);
							}
							else if (at1.length == 0 && at2.length == 1 && atom[j].getID() == idO
									&& at2[0].getID() == idH) {
								H_OH(atom[j], atom[i], at2[0]);
							}
							else {
								if (at1.length < 2 && at2.length < 2)
									breakAndMakeBonds(i, j);
							}
						}

					}

				}

			}

		}

	}

	private void H_OH(Atom atomO, Atom atomH, Atom atomH2) {
		double angle = Math.abs(AngularBond.getAngle(atomH, atomO, atomH2));
		if (Math.abs(angle - H2O_ANGLE) > ANGLE_TOLL)
			return;
		double bondLength = getBondLength(atomO, atomH);
		double bondStrength = getBondStrength(atomO, atomH);
		double bondEnergy = type.getParameter(Reaction.O2_2H2__2H2O.VHO).doubleValue();
		double xOH = atomO.rx - atomH.rx;
		double yOH = atomO.ry - atomH.ry;
		double rOH = Math.hypot(xOH, yOH);
		RadialBond rBond = new RadialBond(atomO, atomH, bondLength, bondStrength, bondEnergy);
		AngularBond aBond = new AngularBond(atomH, atomH2, atomO, H2O_ANGLE);
		double pot = bondEnergy - 0.5 * bondStrength * (rOH - bondLength) * (rOH - bondLength) - 0.5
				* aBond.getBondStrength() * (angle - H2O_ANGLE) * (angle - H2O_ANGLE);
		atomO.storeCurrentVelocity();
		atomH.storeCurrentVelocity();
		atomH2.storeCurrentVelocity();
		if (conserve(atomO.getIndex(), atomH.getIndex(), atomH2.getIndex(), pot)) {
			bonds.add(rBond);
			bends.add(aBond);
			atomO.setRadical(false);
			atomH.setRadical(false);
			atomH2.setRadical(false);
			bondChanged = true;
		}
		else {
			atomO.restoreVelocity();
			atomH.restoreVelocity();
			atomH2.restoreVelocity();
		}
	}

	private void OH_H2(Atom atomO, Atom atomH, Atom atomH1, Atom atomH2) {
		double angle = Math.abs(AngularBond.getAngle(atomH, atomO, atomH1));
		if (Math.abs(angle - H2O_ANGLE) > ANGLE_TOLL)
			return;
		double bondLength = getBondLength(atomO, atomH1);
		double bondStrength = getBondStrength(atomO, atomH1);
		double bondEnergy = type.getParameter(Reaction.O2_2H2__2H2O.VHO).doubleValue();
		RadialBond rBond = bonds.getBond(atomH1, atomH2);
		RadialBond newRBond = new RadialBond(atomO, atomH1, bondLength, bondStrength, bondEnergy);
		AngularBond aBond = new AngularBond(atomH, atomH1, atomO, H2O_ANGLE);
		double x12 = atomH1.rx - atomH2.rx;
		double y12 = atomH1.ry - atomH2.ry;
		double r12 = Math.hypot(x12, y12);
		double pot = bondEnergy - rBond.getChemicalEnergy() + 0.5 * rBond.getBondStrength()
				* (r12 - rBond.getBondLength()) * (r12 - rBond.getBondLength());
		x12 = atomH1.rx - atomO.rx;
		y12 = atomH1.ry - atomO.ry;
		r12 = Math.hypot(x12, y12);
		pot += -0.5 * bondStrength * (r12 - bondLength) * (r12 - bondLength) - 0.5 * aBond.getBondStrength()
				* (angle - H2O_ANGLE) * (angle - H2O_ANGLE);
		atomO.storeCurrentVelocity();
		atomH.storeCurrentVelocity();
		atomH1.storeCurrentVelocity();
		atomH2.storeCurrentVelocity();
		if (conserve(atomO.getIndex(), atomH.getIndex(), atomH1.getIndex(), atomH2.getIndex(), pot)) {
			bonds.remove(rBond);
			rBond.destroy();
			bonds.add(newRBond);
			bends.add(aBond);
			atomO.setRadical(false);
			atomH.setRadical(false);
			atomH1.setRadical(false);
			atomH2.setRadical(true);
			bondChanged = true;
		}
		else {
			atomO.restoreVelocity();
			atomH.restoreVelocity();
			atomH1.restoreVelocity();
			atomH2.restoreVelocity();
		}
	}

	/*
	 * detect whether or not a pair of atoms have the tendency of collision shortly. If two atoms tends to move away
	 * from each other, or move in the tangential direction, return false.
	 * 
	 * @param vi the velocity vector of atom i @param vj the velocity vector of atom j @param d the distance vector
	 * between atoms i and j, pointing from j to i.
	 */
	private boolean willCollide(Vector2D vi, Vector2D vj, Vector2D d) {
		return !(vi.dot(d) >= 0.0 && vj.dot(d) <= 0.0);
	}

	/* TWO-BODY ACTION: make a radial bond between atom i and j. */
	private void makeBond(int i, int j) {

		// if the chemical energy stored in the supposed bond is zero, do not make it
		bondEnergy = getBondEnergy(i, j);
		if (bondEnergy <= 0.0)
			return;

		/*
		 * "dpot" is the amount of energy needed to be exchanged with the kinetic energy reservior. If it is negative,
		 * it will be substracted from the reservior. This means that, for example, this amount of heat needs to be
		 * borrowed from the reservior to form a chemical bond. If it is positive, it will be added to the reservior.
		 * This means that, for example, this amount of heat will be returned to the reservior to increase the
		 * temperature.
		 */
		bondLength = getBondLength(atom[i], atom[j]);
		bondStrength = getBondStrength(atom[i], atom[j]);
		dpot = Math.sqrt(rij) - bondLength;
		dpot *= dpot;
		dpot *= -0.5 * bondStrength;
		dpot += bondEnergy;

		atom[i].storeCurrentVelocity();
		atom[j].storeCurrentVelocity();
		if (conserve(i, j, dpot)) {

			bonds.add(new RadialBond(atom[i], atom[j], bondLength, bondStrength, bondEnergy));

			if ((type instanceof Reaction.A2_B2__2AB) || (type instanceof Reaction.A2_B2_C__2AB_C)) {
				// atoms of A2, B2 and AB should not be treated as free radicals any more
				atom[i].setRadical(false);
				atom[j].setRadical(false);
			}
			else if (type instanceof Reaction.O2_2H2__2H2O) {
				// atoms of H2 and O2 molecules should not be treated as free radials any more, but atoms of HO
				// fragments are still reactive.
				if (atom[i].getID() == atom[j].getID()) {
					atom[i].setRadical(false);
					atom[j].setRadical(false);
				}
			}

			bondChanged = true;

		}
		else {
			atom[i].restoreVelocity();
			atom[j].restoreVelocity();
		}

	}

	/*
	 * THREE-BODY ACTION: break a bond first, then make a new bond between the colliding pair. Reject any pathway other
	 * than the two chain propagation reactions.
	 */
	private void breakAndMakeBonds(int i, int j) {

		rBond = null;
		RadialBond rb = null;
		synchronized (bonds.getSynchronizationLock()) {
			for (Iterator it = bonds.iterator(); it.hasNext();) {
				rb = (RadialBond) it.next();
				if ((rb.contains(atom[i]) && !rb.contains(atom[j])) || (rb.contains(atom[j]) && !rb.contains(atom[i]))) {
					if (rBond != null)
						return; // both i and j are bonded, their reaction is not permitted.
					rBond = rb;
				}
			}
		}
		if (rBond == null)
			return; // no required bond found

		dpot = 0.0;
		int i1 = rBond.atom1.getIndex();
		int i2 = rBond.atom2.getIndex();
		int k = -1; // the bonded atom not colliding with the radical
		int m = -1; // the atom bonded to k
		int n = -1; // the free radical: m-n the colliding pair
		if (i1 != i && i1 != j) {
			k = i1;
			m = i2;
		}
		else if (i2 != i && i2 != j) {
			k = i2;
			m = i1;
		}
		if (m == i) {
			n = j;
		}
		else if (m == j) {
			n = i;
		}

		if (k != -1 && m != -1 && n != -1) {

			if (atom[k].getID() == atom[n].getID()) {// useless collision

				// if(allowUselessCollision){}

			}

			else {

				// figure out the activation energies for different scenarios of collision.
				ecol = Double.MAX_VALUE;

				if (type instanceof Reaction.A2_B2__2AB) {

					double vAA = type.getParameter(Reaction.A2_B2__2AB.VAA).doubleValue();
					double vBB = type.getParameter(Reaction.A2_B2__2AB.VBB).doubleValue();
					double vAB = type.getParameter(Reaction.A2_B2__2AB.VAB).doubleValue();
					double vA2B = type.getParameter(Reaction.A2_B2__2AB.VA2B).doubleValue();
					double vAB2 = type.getParameter(Reaction.A2_B2__2AB.VAB2).doubleValue();
					int idA = ((Reaction.A2_B2__2AB) type).getIDA();
					int idB = ((Reaction.A2_B2__2AB) type).getIDB();

					if (atom[k].getID() == idA) { // i.e. atom n must be an B
						if (atom[m].getID() == idA) { // A2+B*-->A*+AB
							if (vAB > vAA) {
								ecol = vA2B;
							}
							else {
								ecol = vA2B + vAA - vAB;
							}
							/*
							 * (imagine vA2B=0) If the final state is more stable than the initial one (vAB>vAA), the
							 * transition should be easy. Conversely, if the final state is less stable than the initial
							 * one (vAB<vAA), the transition should be harder. Same applied to the following
							 * conditional branches.
							 */
						}
						else if (atom[m].getID() == idB) { // AB+B*-->A*+B2
							if (vAB > vBB) {
								ecol = vAB2 + vAB - vBB;
							}
							else {
								ecol = vAB2;
							}
						}
					}
					else if (atom[k].getID() == idB) { // i.e. atom n must be an A
						if (atom[m].getID() == idB) { // A*+B2-->AB+B*
							if (vAB > vBB) {
								ecol = vAB2;
							}
							else {
								ecol = vAB2 + vBB - vAB;
							}
						}
						else if (atom[m].getID() == idA) { // A*+AB-->A2+B*
							if (vAB > vAA) {
								ecol = vA2B + vAB - vAA;
							}
							else {
								ecol = vA2B;
							}
						}
					}

				}

				else if (type instanceof Reaction.A2_B2_C__2AB_C) {

					double vAA = type.getParameter(Reaction.A2_B2_C__2AB_C.VAA).doubleValue();
					double vBB = type.getParameter(Reaction.A2_B2_C__2AB_C.VBB).doubleValue();
					double vAB = type.getParameter(Reaction.A2_B2_C__2AB_C.VAB).doubleValue();
					double vAC = type.getParameter(Reaction.A2_B2_C__2AB_C.VAC).doubleValue();
					double vBC = type.getParameter(Reaction.A2_B2_C__2AB_C.VBC).doubleValue();
					double vBA2 = type.getParameter(Reaction.A2_B2_C__2AB_C.VBA2).doubleValue();
					double vAB2 = type.getParameter(Reaction.A2_B2_C__2AB_C.VAB2).doubleValue();
					double vCA2 = type.getParameter(Reaction.A2_B2_C__2AB_C.VCA2).doubleValue();
					double vCB2 = type.getParameter(Reaction.A2_B2_C__2AB_C.VCB2).doubleValue();
					double vABC = type.getParameter(Reaction.A2_B2_C__2AB_C.VABC).doubleValue();
					double vBAC = type.getParameter(Reaction.A2_B2_C__2AB_C.VBAC).doubleValue();
					int idA = ((Reaction.A2_B2_C__2AB_C) type).getIDA();
					int idB = ((Reaction.A2_B2_C__2AB_C) type).getIDB();
					int idC = ((Reaction.A2_B2_C__2AB_C) type).getIDC();

					if (atom[k].getID() == idA) { // i.e. atom n must be an B or C
						if (atom[n].getID() == idB) {
							if (atom[m].getID() == idA) { // B*+A2-->AB+A*
								if (vAB > vAA) {
									ecol = vBA2;
								}
								else {
									ecol = vBA2 + vAA - vAB;
								}
							}
							else if (atom[m].getID() == idB) { // B*+AB-->B2+A*
								if (vBB > vAB) {
									ecol = vAB2;
								}
								else {
									ecol = vAB2 + vAB - vBB;
								}
							}
							else if (atom[m].getID() == idC) { // AC+B*-->A*+BC
								if (vBC > vAC) {
									ecol = (vABC + vBAC) * 0.5;
								}
								else {
									ecol = 0.5 * (vABC + vBAC) + vAC - vBC;
								}
							}
						}
						else if (atom[n].getID() == idC) {
							if (atom[m].getID() == idA) { // A2+C*-->A*+AC
								if (vAC > vAA) {
									ecol = vCA2;
								}
								else {
									ecol = vCA2 + vAA - vAC;
								}
							}
							else if (atom[m].getID() == idB) { // AB+C*-->A*+BC
								if (vBC > vAB) {
									ecol = vABC;
								}
								else {
									ecol = vABC + vAB - vBC;
								}
							}
						}
					}
					else if (atom[k].getID() == idB) { // i.e. atom n = A or C
						if (atom[n].getID() == idA) {
							if (atom[m].getID() == idA) { // A*+AB-->A2+B*
								if (vAA > vAB) {
									ecol = vBA2;
								}
								else {
									ecol = vBA2 + vAB - vAA;
								}
							}
							else if (atom[m].getID() == idB) { // A*+B2-->AB+B*
								if (vAB > vBB) {
									ecol = vAB2;
								}
								else {
									ecol = vAB2 + vBB - vAB;
								}
							}
							else if (atom[m].getID() == idC) { // A*+BC-->AC+B*
								if (vAC > vBC) {
									ecol = 0.5 * (vABC + vBAC);
								}
								else {
									ecol = 0.5 * (vABC + vBAC) + vBC - vAC;
								}
							}
						}
						else if (atom[n].getID() == idC) {
							if (atom[m].getID() == idA) { // C*+AB-->AC+B*
								if (vAC > vAB) {
									ecol = vBAC;
								}
								else {
									ecol = vBAC + vAC - vAB;
								}
							}
							else if (atom[m].getID() == idB) { // C*+B2-->BC+B*
								if (vBC > vBB) {
									ecol = vCB2;
								}
								else {
									ecol = vCB2 + vBB - vBC;
								}
							}
						}
					}
					else if (atom[k].getID() == idC) { // i.e. atom n = A or B
						if (atom[n].getID() == idA) {
							if (atom[m].getID() == idA) { // A*+AC-->A2+C*
								if (vAA > vAC) {
									ecol = vCA2;
								}
								else {
									ecol = vCA2 + vAC - vAA;
								}
							}
							else if (atom[m].getID() == idB) { // A*+BC-->AB+C*
								if (vAB > vBC) {
									ecol = vABC;
								}
								else {
									ecol = vABC + vBC - vAB;
								}
							}
						}
						else if (atom[n].getID() == idB) {
							if (atom[m].getID() == idA) { // B*+AC-->AB+C*
								if (vAB > vAC) {
									ecol = vBAC;
								}
								else {
									ecol = vBAC + vAC - vAB;
								}
							}
							else if (atom[m].getID() == idB) { // B*+BC-->B2+C*
								if (vBB > vBC) {
									ecol = vCB2;
								}
								else {
									ecol = vCB2 + vBC - vBB;
								}
							}
						}
					}

				}

				else if (type instanceof Reaction.O2_2H2__2H2O) {

					double vHH = type.getParameter(Reaction.O2_2H2__2H2O.VHH).doubleValue();
					double vOO = type.getParameter(Reaction.O2_2H2__2H2O.VOO).doubleValue();
					double vHO = type.getParameter(Reaction.O2_2H2__2H2O.VHO).doubleValue();
					double vHO2 = type.getParameter(Reaction.O2_2H2__2H2O.VHO2).doubleValue();
					double vOH2 = type.getParameter(Reaction.O2_2H2__2H2O.VOH2).doubleValue();
					int idH = ((Reaction.O2_2H2__2H2O) type).getIDH();
					int idO = ((Reaction.O2_2H2__2H2O) type).getIDO();

					if (atom[k].getID() == idH) { // i.e. atom n must be an O
						if (atom[m].getID() == idH) { // H2+O*-->H*+HO
							if (vHO > vHH) {
								ecol = vOH2;
							}
							else {
								ecol = vOH2 + vHH - vHO;
							}
							/*
							 * (imagine vOH2=0) If the final state is more stable than the initial one (vHO>vHH), the
							 * transition should be easy. Conversely, if the final state is less stable than the initial
							 * one (vHO<vHH), the transition should be harder. Same applied to the following
							 * conditional branches.
							 */
						}
						else if (atom[m].getID() == idO) { // HO+O*-->H*+O2
							if (vHO > vOO) {
								ecol = vHO2 + vHO - vOO;
							}
							else {
								ecol = vHO2;
							}
						}
					}
					else if (atom[k].getID() == idO) { // i.e. atom n must be
						// an H
						if (atom[m].getID() == idO) { // H*+O2-->HO+O*
							if (vHO > vOO) {
								ecol = vHO2;
							}
							else {
								ecol = vHO2 + vOO - vHO;
							}
						}
						else if (atom[m].getID() == idH) { // H*+HO-->H2+O*
							if (vHO > vHH) {
								ecol = vOH2 + vHO - vHH;
							}
							else {
								ecol = vOH2;
							}
						}
					}

				}

				if (ecol >= Double.MAX_VALUE - 1)
					return;

				/* calculate the line-of-centers energy */
				rijsr = 1.0 / Math.sqrt(rij);
				vxij = atom[i].vx - atom[j].vx;
				vyij = atom[i].vy - atom[j].vy;
				vxy = vxij * xij * rijsr + vyij * yij * rijsr;

				/* use reduced mass to compute head-on kinetic energy */
				if (atom[i].mass * atom[j].mass / (atom[i].mass + atom[j].mass) * vxy * vxy * EV_CONVERTER > ecol) {

					double bx2 = atom[i1].rx - atom[i2].rx;
					bx2 *= bx2;
					double by2 = atom[i1].ry - atom[i2].ry;
					by2 *= by2;
					double br2 = bx2 + by2;
					br2 = Math.sqrt(br2) - rBond.getBondLength();
					dpot += 0.5 * rBond.getBondStrength() * br2 * br2;
					dpot -= rBond.getChemicalEnergy();

					bondLength = getBondLength(atom[i], atom[j]);
					bondStrength = getBondStrength(atom[i], atom[j]);
					bondEnergy = getBondEnergy(i, j);
					br2 = Math.sqrt(rij) - bondLength;
					dpot -= 0.5 * bondStrength * br2 * br2;
					dpot += bondEnergy;

					atom[i].storeCurrentVelocity();
					atom[j].storeCurrentVelocity();
					atom[k].storeCurrentVelocity();
					if (conserve(i, j, k, dpot)) {

						bonds.remove(rBond);
						rBond.destroy();
						removeAssociatedAngularBonds();
						bonds.add(new RadialBond(atom[i], atom[j], bondLength, bondStrength, bondEnergy));
						/*
						 * temporarily indicate that the atoms should not be treated as a free radical. CHECK!!! In H2O
						 * reaction, this is not true any more, but in order for the OH fragments not to react with
						 * other fragments, this had better be set.
						 */
						atom[i].setRadical(false);
						atom[j].setRadical(false);
						bondChanged = true;

					}
					else {

						atom[i].restoreVelocity();
						atom[j].restoreVelocity();
						atom[k].restoreVelocity();

					}

				}

			}

		}
		else {
			System.err.println("Error in <bonds>...");
		}

	}

	/*
	 * This searches the critical conditions for breaking bonds between a pair of atoms. The rule is to break a bond if
	 * its vibrational potential energy exceeds the value of depth of the potential well of dissociation while the bond
	 * is being <i>stretched</i>. This means: (a)the deeper the potential well is, the harder it is to break the bond.
	 * (b)at higher temperature, the probability of bond-breaking is higher because the chance that the vibrational
	 * energy exceeds the well depth is better. The dissociated atoms will be marked as free radicals. APPLY-TO-ALL:
	 * This method should be applied to all reactions.
	 */
	private void dissociate() {

		synchronized (bonds.getSynchronizationLock()) {

			for (Iterator it = bonds.iterator(); it.hasNext();) {

				rBond = (RadialBond) it.next();
				xij = rBond.atom1.rx - rBond.atom2.rx;
				yij = rBond.atom1.ry - rBond.atom2.ry;
				rij = xij * xij + yij * yij;

				dpot = Math.sqrt(rij) - rBond.bondLength;
				if (dpot > 0.0) {
					dpot = 0.5 * rBond.bondStrength * dpot * dpot;
					bondEnergy = rBond.getChemicalEnergy();
					if (dpot > bondEnergy) {
						dpot -= bondEnergy;
						rBond.atom1.storeCurrentVelocity();
						rBond.atom2.storeCurrentVelocity();
						if (conserve(rBond.atom1.getIndex(), rBond.atom2.getIndex(), dpot)) {
							it.remove();
							removeAssociatedAngularBonds();
							rBond.destroy();
							bondChanged = true;
						}
						else {
							rBond.atom1.restoreVelocity();
							rBond.atom2.restoreVelocity();
						}
					}
				}

			}

		}

	}

	private void removeAssociatedAngularBonds() {
		if (bends.isEmpty())
			return;
		Atom a1 = rBond.atom1;
		Atom a2 = rBond.atom2;
		AngularBond b = null;
		synchronized (bends.getSynchronizationLock()) {
			for (Iterator i = bends.iterator(); i.hasNext();) {
				b = (AngularBond) i.next();
				if (b.contains(a1) && b.contains(a2) && (b.indexOf(a1) == 2 || b.indexOf(a2) == 2)) {
					i.remove();
					b.destroy();
				}
			}
		}
	}

	void setTapePointer(int n) {
		super.setTapePointer(n);
		bonds.getBondQueue().setPointer(n);
		if (type instanceof Reaction.A2_B2__2AB) {
			Reaction.A2_B2__2AB r = (Reaction.A2_B2__2AB) type;
			r.moleFractionA2().setPointer(n);
			r.moleFractionB2().setPointer(n);
			r.moleFractionAB().setPointer(n);
			r.numberOfA2().setPointer(n);
			r.numberOfB2().setPointer(n);
			r.numberOfAB().setPointer(n);
		}
		else if (type instanceof Reaction.A2_B2_C__2AB_C) {
			Reaction.A2_B2_C__2AB_C r = (Reaction.A2_B2_C__2AB_C) type;
			r.moleFractionA2().setPointer(n);
			r.moleFractionB2().setPointer(n);
			r.moleFractionAB().setPointer(n);
			r.numberOfA2().setPointer(n);
			r.numberOfB2().setPointer(n);
			r.numberOfAB().setPointer(n);
			r.numberOfC().setPointer(n);
		}
		else if (type instanceof Reaction.O2_2H2__2H2O) {
			Reaction.O2_2H2__2H2O r = (Reaction.O2_2H2__2H2O) type;
			r.moleFractionH2().setPointer(n);
			r.moleFractionO2().setPointer(n);
			r.moleFractionH2O().setPointer(n);
			r.numberOfH2().setPointer(n);
			r.numberOfO2().setPointer(n);
			r.numberOfH2O().setPointer(n);
		}
	}

	/* check if atom i, j are bonded by a single bond */
	private boolean checkBond(int i, int j) {
		synchronized (bonds.getSynchronizationLock()) {
			for (Iterator it = bonds.iterator(); it.hasNext();) {
				rBond = (RadialBond) it.next();
				bonded = (rBond.atom1 == atom[i] && rBond.atom2 == atom[j])
						|| (rBond.atom2 == atom[i] && rBond.atom1 == atom[j]);
				if (bonded)
					return true;
			}
		}
		return false;
	}

	private static double getBondLength(Atom a, Atom b) {
		if (a == null || b == null)
			throw new IllegalArgumentException("Cannot get bond length for null atom");
		return BOND_LENGTH_RATIO * (a.sigma + b.sigma);
	}

	private static double getBondStrength(Atom a, Atom b) {
		if (a == null || b == null)
			throw new IllegalArgumentException("Cannot get bond strength for null atom");
		return 2 * Math.sqrt(a.epsilon * b.epsilon);
	}

	private double getBondEnergy(int i, int j) {

		if (type instanceof Reaction.A2_B2__2AB) {

			int idA = ((Reaction.A2_B2__2AB) type).getIDA();
			int idB = ((Reaction.A2_B2__2AB) type).getIDB();

			if (atom[i].getID() == idA && atom[j].getID() == idA)
				return type.getParameter(Reaction.A2_B2__2AB.VAA).doubleValue();

			if (atom[i].getID() == idB && atom[j].getID() == idB)
				return type.getParameter(Reaction.A2_B2__2AB.VBB).doubleValue();

			if ((atom[i].getID() == idA && atom[j].getID() == idB)
					|| (atom[i].getID() == idB && atom[j].getID() == idA))
				return type.getParameter(Reaction.A2_B2__2AB.VAB).doubleValue();

		}
		else if (type instanceof Reaction.O2_2H2__2H2O) {

			int idH = ((Reaction.O2_2H2__2H2O) type).getIDH();
			int idO = ((Reaction.O2_2H2__2H2O) type).getIDO();

			if (atom[i].getID() == idH && atom[j].getID() == idH)
				return type.getParameter(Reaction.O2_2H2__2H2O.VHH).doubleValue();

			if (atom[i].getID() == idO && atom[j].getID() == idO)
				return type.getParameter(Reaction.O2_2H2__2H2O.VOO).doubleValue();

			if ((atom[i].getID() == idH && atom[j].getID() == idO)
					|| (atom[i].getID() == idO && atom[j].getID() == idH))
				return type.getParameter(Reaction.O2_2H2__2H2O.VHO).doubleValue();

		}
		else if (type instanceof Reaction.A2_B2_C__2AB_C) {

			int idA = ((Reaction.A2_B2_C__2AB_C) type).getIDA();
			int idB = ((Reaction.A2_B2_C__2AB_C) type).getIDB();
			int idC = ((Reaction.A2_B2_C__2AB_C) type).getIDC();

			if (atom[i].getID() == idA && atom[j].getID() == idA)
				return type.getParameter(Reaction.A2_B2_C__2AB_C.VAA).doubleValue();

			if (atom[i].getID() == idB && atom[j].getID() == idB)
				return type.getParameter(Reaction.A2_B2_C__2AB_C.VBB).doubleValue();

			if ((atom[i].getID() == idA && atom[j].getID() == idB)
					|| (atom[i].getID() == idB && atom[j].getID() == idA))
				return type.getParameter(Reaction.A2_B2_C__2AB_C.VAB).doubleValue();

			if ((atom[i].getID() == idA && atom[j].getID() == idC)
					|| (atom[i].getID() == idC && atom[j].getID() == idA))
				return type.getParameter(Reaction.A2_B2_C__2AB_C.VAC).doubleValue();

			if ((atom[i].getID() == idB && atom[j].getID() == idC)
					|| (atom[i].getID() == idC && atom[j].getID() == idB))
				return type.getParameter(Reaction.A2_B2_C__2AB_C.VBC).doubleValue();

		}
		else if (type instanceof Reaction.nA__An) {

			if (atom[i].getID() == ID_NT && atom[j].getID() == ID_NT)
				return type.getParameter(Reaction.nA__An.VAA).doubleValue();

			if (atom[i].getID() == ID_PL && atom[j].getID() == ID_PL)
				return type.getParameter(Reaction.nA__An.VBB).doubleValue();

			if (atom[i].getID() == ID_WS && atom[j].getID() == ID_WS)
				return type.getParameter(Reaction.nA__An.VCC).doubleValue();

			if (atom[i].getID() == ID_CK && atom[j].getID() == ID_CK)
				return type.getParameter(Reaction.nA__An.VDD).doubleValue();

			if ((atom[i].getID() == ID_NT && atom[j].getID() == ID_PL)
					|| (atom[i].getID() == ID_PL && atom[j].getID() == ID_NT))
				return type.getParameter(Reaction.nA__An.VAB).doubleValue();

			if ((atom[i].getID() == ID_NT && atom[j].getID() == ID_WS)
					|| (atom[i].getID() == ID_WS && atom[j].getID() == ID_NT))
				return type.getParameter(Reaction.nA__An.VAC).doubleValue();

			if ((atom[i].getID() == ID_NT && atom[j].getID() == ID_CK)
					|| (atom[i].getID() == ID_CK && atom[j].getID() == ID_NT))
				return type.getParameter(Reaction.nA__An.VAD).doubleValue();

			if ((atom[i].getID() == ID_PL && atom[j].getID() == ID_WS)
					|| (atom[i].getID() == ID_WS && atom[j].getID() == ID_PL))
				return type.getParameter(Reaction.nA__An.VBC).doubleValue();

			if ((atom[i].getID() == ID_PL && atom[j].getID() == ID_CK)
					|| (atom[i].getID() == ID_CK && atom[j].getID() == ID_PL))
				return type.getParameter(Reaction.nA__An.VBD).doubleValue();

			if ((atom[i].getID() == ID_WS && atom[j].getID() == ID_CK)
					|| (atom[i].getID() == ID_CK && atom[j].getID() == ID_WS))
				return type.getParameter(Reaction.nA__An.VCD).doubleValue();

		}

		return 0.0;

	}

	/* @return the kinetic energy of the given pair of atoms. */
	private double getKE(int i, int j) {
		double ke = (atom[i].vx * atom[i].vx + atom[i].vy * atom[i].vy) * atom[i].mass
				+ (atom[j].vx * atom[j].vx + atom[j].vy * atom[j].vy) * atom[j].mass;
		return ke * EV_CONVERTER;
	}

	/* @return the kinetic energy of the given three atoms. */
	private double getKE(int i, int j, int k) {
		double ke = (atom[i].vx * atom[i].vx + atom[i].vy * atom[i].vy) * atom[i].mass
				+ (atom[j].vx * atom[j].vx + atom[j].vy * atom[j].vy) * atom[j].mass
				+ (atom[k].vx * atom[k].vx + atom[k].vy * atom[k].vy) * atom[k].mass;
		return ke * EV_CONVERTER;
	}

	/* @return the kinetic energy of the given four atoms. */
	private double getKE(int i, int j, int k, int l) {
		double ke = (atom[i].vx * atom[i].vx + atom[i].vy * atom[i].vy) * atom[i].mass
				+ (atom[j].vx * atom[j].vx + atom[j].vy * atom[j].vy) * atom[j].mass
				+ (atom[k].vx * atom[k].vx + atom[k].vy * atom[k].vy) * atom[k].mass
				+ (atom[l].vx * atom[k].vx + atom[l].vy * atom[l].vy) * atom[l].mass;
		return ke * EV_CONVERTER;
	}

	private boolean conserve(int i, int j, double change) {
		if (!conserveEnergy)
			return true;
		double oldKE = getKE(i, j);
		double newKE = oldKE + change;
		if (newKE < 0.0)
			return false;
		if (oldKE <= 0) {
			double half = newKE / (2 * EV_CONVERTER);
			atom[i].setRandomVelocity(Math.sqrt(half / atom[i].mass));
			atom[j].setRandomVelocity(Math.sqrt(half / atom[j].mass));
		}
		else {
			double ratio = Math.sqrt(newKE / oldKE);
			atom[i].vx *= ratio;
			atom[i].vy *= ratio;
			atom[j].vx *= ratio;
			atom[j].vy *= ratio;
		}
		return true;
	}

	private boolean conserve(int i, int j, int k, double change) {
		if (!conserveEnergy)
			return true;
		double oldKE = getKE(i, j, k);
		double newKE = oldKE + change;
		if (newKE < 0.0)
			return false;
		if (oldKE <= ZERO) {
			double onethird = newKE / (3 * EV_CONVERTER);
			atom[i].setRandomVelocity(Math.sqrt(onethird / atom[i].mass));
			atom[j].setRandomVelocity(Math.sqrt(onethird / atom[j].mass));
			atom[k].setRandomVelocity(Math.sqrt(onethird / atom[k].mass));
		}
		else {
			double ratio = Math.sqrt(newKE / oldKE);
			atom[i].vx *= ratio;
			atom[i].vy *= ratio;
			atom[j].vx *= ratio;
			atom[j].vy *= ratio;
			atom[k].vx *= ratio;
			atom[k].vy *= ratio;
		}
		return true;
	}

	private boolean conserve(int i, int j, int k, int l, double change) {
		if (!conserveEnergy)
			return true;
		double oldKE = getKE(i, j, k, l);
		double newKE = oldKE + change;
		if (newKE < 0.0)
			return false;
		if (oldKE <= ZERO) {
			double x = 0.25 * newKE / EV_CONVERTER;
			atom[i].setRandomVelocity(Math.sqrt(x / atom[i].mass));
			atom[j].setRandomVelocity(Math.sqrt(x / atom[j].mass));
			atom[k].setRandomVelocity(Math.sqrt(x / atom[k].mass));
			atom[l].setRandomVelocity(Math.sqrt(x / atom[l].mass));
		}
		else {
			double ratio = Math.sqrt(newKE / oldKE);
			atom[i].vx *= ratio;
			atom[i].vy *= ratio;
			atom[j].vx *= ratio;
			atom[j].vy *= ratio;
			atom[k].vx *= ratio;
			atom[k].vy *= ratio;
			atom[l].vx *= ratio;
			atom[l].vy *= ratio;
		}
		return true;
	}

	public void changeChemicalEnergies() {
		if (type instanceof Reaction.A2_B2__2AB) {
			int idA = ((Reaction.A2_B2__2AB) type).getIDA();
			int idB = ((Reaction.A2_B2__2AB) type).getIDB();
			synchronized (bonds.getSynchronizationLock()) {
				for (Iterator it = bonds.iterator(); it.hasNext();) {
					rBond = (RadialBond) it.next();
					if (rBond.atom1.getID() == idA && rBond.atom2.getID() == idA) {
						rBond.setChemicalEnergy(type.getParameter(Reaction.A2_B2__2AB.VAA).doubleValue());
					}
					else if (rBond.atom1.getID() == idB && rBond.atom2.getID() == idB) {
						rBond.setChemicalEnergy(type.getParameter(Reaction.A2_B2__2AB.VBB).doubleValue());
					}
					else if ((rBond.atom1.getID() == idA && rBond.atom2.getID() == idB)
							|| (rBond.atom1.getID() == idB && rBond.atom2.getID() == idA)) {
						rBond.setChemicalEnergy(type.getParameter(Reaction.A2_B2__2AB.VAB).doubleValue());
					}
				}
			}
		}
	}

	private void count() {
		if (type instanceof Reaction.A2_B2__2AB) {
			Reaction.A2_B2__2AB r = (Reaction.A2_B2__2AB) type;
			int idA = r.getIDA();
			int idB = r.getIDB();
			int nAA = 0;
			int nBB = 0;
			int nAB = 0;
			synchronized (bonds.getSynchronizationLock()) {
				for (Iterator it = bonds.iterator(); it.hasNext();) {
					rBond = (RadialBond) it.next();
					if (rBond.atom1.getID() == idA && rBond.atom2.getID() == idA)
						nAA++;
					else if (rBond.atom1.getID() == idB && rBond.atom2.getID() == idB)
						nBB++;
					else if ((rBond.atom1.getID() == idA && rBond.atom2.getID() == idB)
							|| (rBond.atom1.getID() == idB && rBond.atom2.getID() == idA))
						nAB++;
				}
			}
			float inv = 200.0f / numberOfAtoms;
			try {
				r.moleFractionA2().update(nAA * inv);
				r.moleFractionB2().update(nBB * inv);
				r.moleFractionAB().update(nAB * inv);
				r.numberOfA2().update(nAA);
				r.numberOfB2().update(nBB);
				r.numberOfAB().update(nAB);
			}
			catch (Exception e) {
				int n = movie.getCapacity();
				r.init(n, modelTimeQueue);
				r.moleFractionA2().update(nAA * inv);
				r.moleFractionB2().update(nBB * inv);
				r.moleFractionAB().update(nAB * inv);
				r.numberOfA2().update(nAA);
				r.numberOfB2().update(nBB);
				r.numberOfAB().update(nAB);
			}
		}
		else if (type instanceof Reaction.A2_B2_C__2AB_C) {
			Reaction.A2_B2_C__2AB_C r = (Reaction.A2_B2_C__2AB_C) type;
			int idA = r.getIDA();
			int idB = r.getIDB();
			int idC = r.getIDC();
			int nAA = 0;
			int nBB = 0;
			int nAB = 0;
			synchronized (bonds.getSynchronizationLock()) {
				for (Iterator it = bonds.iterator(); it.hasNext();) {
					rBond = (RadialBond) it.next();
					if (rBond.atom1.getID() == idA && rBond.atom2.getID() == idA)
						nAA++;
					else if (rBond.atom1.getID() == idB && rBond.atom2.getID() == idB)
						nBB++;
					else if ((rBond.atom1.getID() == idA && rBond.atom2.getID() == idB)
							|| (rBond.atom1.getID() == idB && rBond.atom2.getID() == idA))
						nAB++;
				}
			}
			int nC = 0;
			for (int i = 0; i < numberOfAtoms; i++) {
				if (atom[i].getID() == idC && atom[i].isRadical())
					nC++;
			}
			float inv = 200.0f / numberOfAtoms;
			try {
				r.moleFractionA2().update(nAA * inv);
				r.moleFractionB2().update(nBB * inv);
				r.moleFractionAB().update(nAB * inv);
				r.numberOfA2().update(nAA);
				r.numberOfB2().update(nBB);
				r.numberOfAB().update(nAB);
				r.numberOfC().update(nC);
			}
			catch (Exception e) {
				int n = movie.getCapacity();
				r.init(n, modelTimeQueue);
				r.moleFractionA2().update(nAA * inv);
				r.moleFractionB2().update(nBB * inv);
				r.moleFractionAB().update(nAB * inv);
				r.numberOfA2().update(nAA);
				r.numberOfB2().update(nBB);
				r.numberOfAB().update(nAB);
				r.numberOfC().update(nC);
			}
		}
		else if (type instanceof Reaction.O2_2H2__2H2O) {
			Reaction.O2_2H2__2H2O r = (Reaction.O2_2H2__2H2O) type;
			int idH = r.getIDH();
			int idO = r.getIDO();
			int nH2 = 0;
			int nO2 = 0;
			int nH2O = 0;
			synchronized (bonds.getSynchronizationLock()) {
				for (Iterator it = bonds.iterator(); it.hasNext();) {
					rBond = (RadialBond) it.next();
					if (rBond.atom1.getID() == idH && rBond.atom2.getID() == idH)
						nH2++;
					else if (rBond.atom1.getID() == idO && rBond.atom2.getID() == idO)
						nO2++;
				}
			}
			synchronized (molecules.getSynchronizationLock()) {
				for (Iterator it = molecules.iterator(); it.hasNext();) {
					if (((Molecule) it.next()).size() == 3)
						nH2O++;
				}
			}
			float inv = 100.0f / numberOfAtoms;
			try {
				r.moleFractionH2().update(2 * nH2 * inv);
				r.moleFractionO2().update(2 * nO2 * inv);
				r.moleFractionH2O().update(3 * nH2O * inv);
				r.numberOfH2().update(nH2);
				r.numberOfO2().update(nO2);
				r.numberOfH2O().update(nH2O);
			}
			catch (Exception e) {
				int n = movie.getCapacity();
				r.init(n, modelTimeQueue);
				r.moleFractionH2().update(2 * nH2 * inv);
				r.moleFractionO2().update(2 * nO2 * inv);
				r.moleFractionH2O().update(3 * nH2O * inv);
				r.numberOfH2().update(nH2);
				r.numberOfO2().update(nO2);
				r.numberOfH2O().update(nH2O);
			}
		}
	}

	void encodeBonds(XMLEncoder out) {
		super.encodeBonds(out);
		if (type == null)
			return;
		type.setFrequency(react.getInterval());
		out.writeObject(type);
	}

	void decodeBonds(XMLDecoder in, AtomisticView.State viewState) throws Exception {

		super.decodeBonds(in, viewState);

		Reaction reaction = null;
		try {
			reaction = (Reaction) in.readObject();
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			return;
		}
		if (reaction == null)
			return;
		setType(reaction);
		setConserveEnergy(heatBath == null);

		// any non-bonded atom is supposed to be a radical. We perform this check here in case a free atom's
		// radical state persists to the point when the model is saved.
		reassertFreeRadicals();

	}

	/* show the <i>i</i>-th frame of the movie */
	void showMovieFrame(int frame) {

		if (frame < 0 || movie.length() <= 0)
			return;

		super.showMovieFrame(frame);

		if (type == null)
			return;

		for (int i = 0; i < numberOfAtoms; i++) {
			atom[i].setRadical(atom[i].getRadicalQ().getData(frame));
		}

		List list = (ArrayList) (bonds.getBondQueue().getData(frame));
		if (list != null) {
			synchronized (bonds.getSynchronizationLock()) {
				for (Iterator it = bonds.iterator(); it.hasNext();)
					((RadialBond) it.next()).destroy();
			}
			bonds.clear();
			molecules.clear();
			if (!list.isEmpty()) {
				RadialBond.Delegate rbd = null;
				RadialBond rBond = null;
				for (Iterator it = list.iterator(); it.hasNext();) {
					rbd = (RadialBond.Delegate) it.next();
					rBond = new RadialBond(atom[rbd.getAtom1()], atom[rbd.getAtom2()], rbd.getBondLength(), rbd
							.getBondStrength(), rbd.getChemicalEnergy());
					rBond.setModel(this);
					bonds.add(rBond);
				}
				MoleculeCollection.sort(this);
			}
		}

		list = (ArrayList) (bends.getBondQueue().getData(frame));
		if (list != null) {
			if (!bends.isEmpty()) {
				synchronized (bends.getSynchronizationLock()) {
					for (Iterator it = bends.iterator(); it.hasNext();)
						((AngularBond) it.next()).destroy();
				}
				bends.clear();
			}
			if (!list.isEmpty()) {
				AngularBond.Delegate abd = null;
				AngularBond aBond = null;
				for (Iterator it = list.iterator(); it.hasNext();) {
					abd = (AngularBond.Delegate) it.next();
					aBond = new AngularBond(atom[abd.getAtom1()], atom[abd.getAtom2()], atom[abd.getAtom3()], abd
							.getBondAngle(), abd.getBondStrength());
					aBond.setModel(this);
					bends.add(aBond);
				}
			}
		}

	}

}