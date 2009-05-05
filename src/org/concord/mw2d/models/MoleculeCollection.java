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

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class MoleculeCollection {

	private MolecularModel model;
	private List<Molecule> list;

	public MoleculeCollection() {
		list = Collections.synchronizedList(new ArrayList<Molecule>());
	}

	public Object getSynchronizationLock() {
		return list;
	}

	public void setModel(MolecularModel m) {
		model = m;
	}

	public void add(int i, Molecule m) {
		if (model == null)
			throw new RuntimeException("null model");
		m.setModel(model);
		list.add(i, m);
	}

	public boolean add(Molecule m) {
		if (model == null)
			throw new RuntimeException("null model");
		m.setModel(model);
		return list.add(m);
	}

	public boolean addAll(List<Molecule> c) {
		return list.addAll(c);
	}

	public void clear() {
		list.clear();
	}

	public String toString() {
		if (list == null)
			return null;
		return list.toString();
	}

	public boolean contains(Object o) {
		return list.contains(o);
	}

	public boolean containsAll(List<Atom> c) {
		return list.containsAll(c);
	}

	public boolean equals(Object o) {
		if (!(o instanceof MoleculeCollection))
			return false;
		return list.equals(((MoleculeCollection) o).list);
	}

	public int hashCode() {
		return list.hashCode();
	}

	public Molecule get(int index) {
		if (index < 0 || index >= list.size())
			return null;
		return list.get(index);
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public Iterator iterator() {
		return list.iterator();
	}

	public ListIterator listIterator() {
		return list.listIterator();
	}

	public ListIterator listIterator(int index) {
		return list.listIterator(index);
	}

	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	public Molecule remove(int index) {
		return list.remove(index);
	}

	public boolean remove(Molecule o) {
		return list.remove(o);
	}

	public boolean removeAll(List<Atom> c) {
		return list.removeAll(c);
	}

	public Molecule set(int i, Molecule o) {
		o.setModel(model);
		return list.set(i, o);
	}

	public int size() {
		return list.size();
	}

	public Molecule[] toArray() {
		return (Molecule[]) list.toArray();
	}

	public void render(Graphics2D g) {
		synchronized (list) {
			for (Molecule m : list)
				m.render(g);
		}
	}

	public boolean sameMolecule(Atom a, Atom b) {
		if (isEmpty())
			return false;
		synchronized (list) {
			for (Molecule m : list) {
				if (m.contains(a) && m.contains(b))
					return true;
			}
		}
		return false;
	}

	public Molecule getMolecule(Atom a) {
		if (a == null)
			return null;
		if (isEmpty())
			return null;
		synchronized (list) {
			for (Molecule m : list) {
				if (m.contains(a))
					return m;
			}
		}
		return null;
	}

	public Molecule getMolecule(RadialBond rb) {
		if (rb == null)
			return null;
		return getMolecule(rb.atom1);
	}

	public int getMoleculeIndex(Atom a) {
		if (a == null)
			return -1;
		if (isEmpty())
			return -1;
		synchronized (list) {
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i).contains(a))
					return i;
			}
		}
		return -1;
	}

	/**
	 * sort atoms into molecules according to the bond network. Used, for example, after fragmentation of molecules
	 * caused by random deletion.
	 */
	public static void sort(MolecularModel model) {

		RadialBondCollection bonds = model.getBonds();
		int numberOfAtoms = model.getNumberOfAtoms();
		Atom[] atom = model.getAtoms();
		MoleculeCollection molecules = model.getMolecules();
		synchronized (molecules.getSynchronizationLock()) {
			for (Molecule m : molecules.list)
				m.destroy();
		}
		molecules.clear();
		if (bonds == null || bonds.isEmpty())
			return;

		RadialBond rBond;
		synchronized (bonds.getSynchronizationLock()) {
			for (Iterator it = bonds.iterator(); it.hasNext();) {
				rBond = (RadialBond) it.next();
				rBond.setModel(model);
			}
		}

		// find atoms associated with molecules that are not molecular objects
		List<Atom> all = new ArrayList<Atom>();
		for (int n = 0; n < numberOfAtoms; n++) {
			synchronized (bonds.getSynchronizationLock()) {
				for (Iterator it = bonds.iterator(); it.hasNext();) {
					rBond = (RadialBond) it.next();
					if (rBond.contains(atom[n]) && !rBond.isSmart()) {
						if (!all.contains(atom[n])) {
							all.add(atom[n]);
							break;
						}
					}
				}
			}
		}

		// find atoms associated with molecular splines
		List<Atom> al2 = new ArrayList<Atom>();
		for (int n = 0; n < numberOfAtoms; n++) {
			synchronized (bonds.getSynchronizationLock()) {
				for (Iterator it = bonds.iterator(); it.hasNext();) {
					rBond = (RadialBond) it.next();
					if (rBond.contains(atom[n]) && rBond.isSmart() && !rBond.isSolid()) {
						if (!al2.contains(atom[n])) {
							al2.add(atom[n]);
							break;
						}
					}
				}
			}
		}

		// find atoms associated with molecular surfaces
		List<Atom> al3 = new ArrayList<Atom>();
		for (int n = 0; n < numberOfAtoms; n++) {
			synchronized (bonds.getSynchronizationLock()) {
				for (Iterator it = bonds.iterator(); it.hasNext();) {
					rBond = (RadialBond) it.next();
					if (rBond.contains(atom[n]) && rBond.isSmart() && rBond.isSolid()) {
						if (!al3.contains(atom[n])) {
							al3.add(atom[n]);
							break;
						}
					}
				}
			}
		}

		molecules.clear();

		if (!all.isEmpty())
			formNewMolecules(false, false, all, molecules, bonds, model);
		if (!al2.isEmpty())
			formNewMolecules(true, false, al2, molecules, bonds, model);
		if (!al3.isEmpty())
			formNewMolecules(true, true, al3, molecules, bonds, model);

		synchronized (model.molecules.getSynchronizationLock()) {
			for (Molecule mol : model.molecules.list) {
				if (mol instanceof Polypeptide)
					((Polypeptide) mol).sortSequence();
			}
		}

		model.notifyBondChangeListeners();

	}

	private static void formNewMolecules(boolean smart, boolean solid, List<Atom> all, MoleculeCollection molecules,
			RadialBondCollection bonds, MolecularModel model) {

		RadialBond rBond;
		Molecule mol = new Molecule();
		mol.setModel(model);
		Atom pickOne, origin, destin;
		ArrayList<Atom> newFound, tempList;
		int nocc, sizeOfMolecule1;
		boolean b;

		do {

			/* always select the first remaining atom */

			pickOne = all.get(0);
			mol.addAtom(pickOne);
			newFound = new ArrayList<Atom>();
			newFound.add(pickOne);
			int incrementOfSize;

			/*
			 * find all the atoms that are connected to the first one, directly or indirectly, thru the radial bonds
			 * network.
			 */

			do {
				tempList = new ArrayList<Atom>();
				synchronized (bonds.getSynchronizationLock()) {
					for (Iterator it = bonds.iterator(); it.hasNext();) {
						rBond = (RadialBond) it.next();
						b = smart ? rBond.isSmart() : !rBond.isSmart();
						if (b) {
							for (Atom a : newFound) {
								if (a == rBond.getAtom1()) {
									destin = rBond.getAtom2();
									if (!mol.contains(destin) && !tempList.contains(destin)) {
										tempList.add(destin);
									}
								}
								else if (a == rBond.getAtom2()) {
									origin = rBond.getAtom1();
									if (!mol.contains(origin) && !tempList.contains(origin)) {
										tempList.add(origin);
									}
								}
							}
						}
					}
				}
				if (!tempList.isEmpty()) {
					newFound.clear();
					newFound.addAll(tempList);
					mol.addAll(tempList);
				}
				incrementOfSize = tempList.size();
			} while (incrementOfSize != 0);

			sizeOfMolecule1 = mol.size();
			nocc = all.size();

			/*
			 * if the number of atoms that are directly or indirectly connected to the first one is less than the number
			 * of atoms in the current non-bonded atom list, declare them an independent molecule and split them from
			 * the original list, and iterate on.
			 */

			if (sizeOfMolecule1 > 0 && sizeOfMolecule1 <= nocc) {
				for (Iterator it = mol.iterator(); it.hasNext();) {
					Atom a = (Atom) it.next();
					a.setRadical(false);
					all.remove(a);
				}
				if (!smart) {
					if (mol.isDNAStrand()) {
						DNAStrand dna = new DNAStrand(mol);
						dna.setModel(model);
						molecules.add(dna);
					}
					else if (mol.isPolypeptide()) {
						Polypeptide pep = new Polypeptide(mol);
						pep.setModel(model);
						molecules.add(pep);
					}
					else {
						Molecule m2 = mol.getCopy();
						m2.setModel(model);
						molecules.add(m2);
					}
				}
				else {
					mol.sortAtoms();
					MolecularObject ss = solid ? new CurvedSurface(mol) : new CurvedRibbon(mol);
					ss.setModel(model);
					molecules.add(ss);
				}
				mol.clear();
			}

		} while (nocc > sizeOfMolecule1);

	}

	public void setSelectionSet(BitSet bs) {
		if (isEmpty())
			return;
		model.setExclusiveSelection(false);
		int n = size();
		for (int i = 0; i < n; i++) {
			get(i).setSelected(bs == null ? false : bs.get(i));
		}
	}

	public void clearSelection() {
		synchronized (list) {
			for (Molecule m : list)
				m.setSelected(false);
		}
	}

}