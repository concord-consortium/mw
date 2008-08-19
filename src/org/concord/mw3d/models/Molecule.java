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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point3f;

/**
 * @author Charles Xie
 * 
 */
public class Molecule {

	private List<Atom> list;
	private boolean selected;

	// used for duplicate() method, supposed to be accessed by a single thread
	private static Map<Atom, Integer> indexAtomMap;

	Molecule() {
		list = Collections.synchronizedList(new ArrayList<Atom>());
	}

	Molecule(List<Atom> a) {
		this();
		list.addAll(a);
	}

	/**
	 * this method just plain-copies all the atoms of this molecule. It doesn't create a new one. To create a new one,
	 * use the duplicate() method.
	 */
	public Molecule(Molecule m) {
		this(m.list);
	}

	public Object getSynchronizationLock() {
		return list;
	}

	public void setSelected(boolean b) {
		selected = b;
		if (b) {
			MolecularModel model = list.get(0).model;
			synchronized (list) {
				for (Atom a : list) {
					model.view.setAtomSelected(a.index);
				}
			}
		}
	}

	public boolean isSelected() {
		return selected;
	}

	public void addAtom(Atom a) {
		if (!list.contains(a))
			list.add(a);
	}

	public void removeAtom(Atom a) {
		list.remove(a);
	}

	public boolean contains(Atom a) {
		return list.contains(a);
	}

	public void clear() {
		list.clear();
	}

	public Atom getAtom(int index) {
		if (index < 0 || index >= list.size())
			return null;
		return list.get(index);
	}

	public int getAtomCount() {
		return list.size();
	}

	public String toString() {
		return list.toString();
	}

	public Point3f getCenterOfMass() {
		Point3f p = new Point3f();
		synchronized (list) {
			for (Atom a : list) {
				p.x += a.rx;
				p.y += a.ry;
				p.z += a.rz;
			}
		}
		p.scale(1.0f / list.size());
		return p;
	}

	@SuppressWarnings("unchecked")
	public Molecule duplicate(MolecularModel model) {
		if (indexAtomMap == null) {
			indexAtomMap = new HashMap<Atom, Integer>();
		}
		else {
			indexAtomMap.clear();
		}
		Molecule newMol = new Molecule();
		// duplicate atoms
		synchronized (list) {
			for (Atom a1 : list) {
				int i = model.getAtomCount();
				indexAtomMap.put(a1, i);
				model.addAtom(a1.getSymbol(), a1.rx, a1.ry, a1.rz, a1.vx, a1.vy, a1.vz, a1.charge);
				newMol.addAtom(model.getAtom(i));
			}
		}
		model.addMolecule(newMol);
		// duplicate r-bonds
		List newbies = new ArrayList(); // we want to reuse this list, so do not set type
		synchronized (model.rBonds) {
			for (RBond rbond : model.rBonds) {
				Atom a1 = rbond.getAtom1();
				Atom a2 = rbond.getAtom2();
				if (contains(a1) && contains(a2)) {
					int i1 = indexAtomMap.get(a1);
					int i2 = indexAtomMap.get(a2);
					RBond newRBond = new RBond(model.getAtom(i1), model.getAtom(i2));
					newRBond.setStrength(rbond.getStrength());
					newRBond.setLength(rbond.getLength());
					newbies.add(newRBond);
				}
			}
		}
		model.rBonds.addAll(newbies);
		// duplicate a-bonds
		newbies.clear();
		synchronized (model.aBonds) {
			for (ABond abond : model.aBonds) {
				Atom a1 = abond.getAtom1();
				Atom a2 = abond.getAtom2();
				Atom a3 = abond.getAtom3();
				if (contains(a1) && contains(a2) && contains(a3)) {
					int i1 = indexAtomMap.get(a1);
					int i2 = indexAtomMap.get(a2);
					int i3 = indexAtomMap.get(a3);
					ABond newABond = new ABond(model.getAtom(i1), model.getAtom(i2), model.getAtom(i3));
					newABond.setStrength(abond.getStrength());
					newABond.setAngle(abond.getAngle());
					newbies.add(newABond);
				}
			}
		}
		model.aBonds.addAll(newbies);
		// duplicate t-bonds
		newbies.clear();
		synchronized (model.tBonds) {
			for (TBond tbond : model.tBonds) {
				Atom a1 = tbond.getAtom1();
				Atom a2 = tbond.getAtom2();
				Atom a3 = tbond.getAtom3();
				Atom a4 = tbond.getAtom4();
				if (contains(a1) && contains(a2) && contains(a3) && contains(a4)) {
					int i1 = indexAtomMap.get(a1);
					int i2 = indexAtomMap.get(a2);
					int i3 = indexAtomMap.get(a3);
					int i4 = indexAtomMap.get(a4);
					TBond newTBond = new TBond(model.getAtom(i1), model.getAtom(i2), model.getAtom(i3), model
							.getAtom(i4));
					newTBond.setStrength(tbond.getStrength());
					newTBond.setAngle(tbond.getAngle());
					newbies.add(newTBond);
				}
			}
		}
		model.tBonds.addAll(newbies);
		return newMol;
	}

}