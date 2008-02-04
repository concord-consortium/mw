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
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.concord.modeler.util.ObjectQueue;

public class AngularBondCollection {

	private List<AngularBond> bonds;
	private MolecularModel model;
	private ObjectQueue bondQ;

	public AngularBondCollection() {
		bonds = Collections.synchronizedList(new ArrayList<AngularBond>());
	}

	public AngularBondCollection(List<AngularBond> c) {
		bonds = Collections.synchronizedList(new ArrayList<AngularBond>(c));
	}

	public void setModel(MolecularModel m) {
		model = m;
	}

	public MolecularModel getModel() {
		return model;
	}

	public void add(int i, AngularBond o) {
		o.setModel(model);
		bonds.add(i, o);
	}

	public boolean add(AngularBond o) {
		o.setModel(model);
		return bonds.add(o);
	}

	public boolean addAll(List<AngularBond> c) {
		if (c == null)
			return false;
		for (AngularBond b : c)
			b.setModel(model);
		return bonds.addAll(c);
	}

	public boolean addAll(int i, List<AngularBond> c) {
		if (c == null)
			return false;
		for (AngularBond b : c)
			b.setModel(model);
		return bonds.addAll(i, c);
	}

	public boolean contains(Object o) {
		return bonds.contains(o);
	}

	public boolean containsAll(Collection c) {
		return bonds.containsAll(c);
	}

	public int indexOf(Object o) {
		return bonds.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return bonds.lastIndexOf(o);
	}

	public AngularBond set(int i, AngularBond o) {
		o.setModel(model);
		return bonds.set(i, o);
	}

	public boolean remove(AngularBond o) {
		return bonds.remove(o);
	}

	public boolean removeAll(List<AngularBond> c) {
		return bonds.removeAll(c);
	}

	public boolean isEmpty() {
		return bonds.isEmpty();
	}

	public int size() {
		return bonds.size();
	}

	public Iterator iterator() {
		return bonds.iterator();
	}

	public Object getSynchronizationLock() {
		return bonds;
	}

	public void clear() {
		bonds.clear();
	}

	public AngularBond get(int index) {
		if (index < 0 || index >= bonds.size())
			return null;
		return bonds.get(index);
	}

	/** exclusively select the specified bond */
	public void select(AngularBond aBond) {
		synchronized (bonds) {
			for (AngularBond b : bonds)
				b.setSelected(b == aBond);
		}
	}

	public void clearSelection() {
		synchronized (bonds) {
			for (AngularBond b : bonds)
				b.setSelected(false);
		}
	}

	/**
	 * returns the angular bond that binds atom a, b and c. Return null if no such bond is found. *
	 * 
	 * @param b
	 *            is the atom at the center of the angle
	 */
	public AngularBond getBond(Atom a, Atom b, Atom c) {
		if (isEmpty())
			return null;
		synchronized (bonds) {
			for (AngularBond x : bonds) {
				if (x.atom3 == b) {
					if ((x.atom1 == a && x.atom2 == c) || (x.atom1 == c && x.atom2 == a))
						return x;
				}
			}
		}
		return null;
	}

	/** select a set of bonds according to the instruction BitSet. */
	public void setSelectionSet(BitSet set) {
		if (set == null) {
			synchronized (bonds) {
				for (AngularBond b : bonds)
					b.setSelected(false);
			}
			model.getView().repaint();
			return;
		}
		model.setExclusiveSelection(false);
		synchronized (bonds) {
			for (int i = 0; i < bonds.size(); i++)
				bonds.get(i).setSelected(set.get(i));
		}
		model.getView().repaint();
	}

	/** return the selected set of bonds in BitSet. */
	public BitSet getSelectionSet() {
		BitSet bs = new BitSet(size());
		synchronized (bonds) {
			for (int i = 0; i < bonds.size(); i++) {
				if (bonds.get(i).isSelected())
					bs.set(i);
			}
		}
		return bs;
	}

	/** initialize the bond queue. If the passed integer is less than 1, nullify the array. */
	public void initializeBondQ(int n) {
		if (bondQ == null) {
			if (n < 1)
				return;
			bondQ = new ObjectQueue("Angular bonds", n);
			bondQ.setInterval(model.movieUpdater.getInterval());
			bondQ.setPointer(0);
		}
		else {
			bondQ.setLength(n);
			if (n < 1) {
				bondQ = null;
			}
			else {
				bondQ.setPointer(0);
			}
		}
	}

	public ObjectQueue getBondQueue() {
		return bondQ;
	}

	public void setBondQueue(ObjectQueue oq) {
		bondQ = oq;
	}

	/** push the current bonds into the bond queue */
	public void updateBondQ() {
		ArrayList<AngularBond.Delegate> x = new ArrayList<AngularBond.Delegate>();
		synchronized (bonds) {
			for (AngularBond b : bonds)
				x.add(new AngularBond.Delegate(b));
		}
		bondQ.update(x);
	}

}