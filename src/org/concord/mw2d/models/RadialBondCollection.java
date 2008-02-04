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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.concord.modeler.util.ObjectQueue;

public class RadialBondCollection {

	private List<RadialBond> list;
	private MolecularModel model;
	private ObjectQueue bondQ;

	public RadialBondCollection() {
		list = Collections.synchronizedList(new ArrayList<RadialBond>());
	}

	public RadialBondCollection(List<RadialBond> c) {
		list = Collections.synchronizedList(new ArrayList<RadialBond>(c));
	}

	public void setModel(MolecularModel m) {
		model = m;
	}

	public MolecularModel getModel() {
		return model;
	}

	public void add(int i, RadialBond b) {
		b.setModel(model);
		list.add(i, b);
	}

	public boolean add(RadialBond b) {
		b.setModel(model);
		return list.add(b);
	}

	public boolean addAll(List<RadialBond> c) {
		if (c == null)
			return false;
		for (RadialBond b : c)
			b.setModel(model);
		return list.addAll(c);
	}

	public synchronized boolean addAll(int i, List<RadialBond> c) {
		if (c == null)
			return false;
		for (RadialBond b : c)
			b.setModel(model);
		return list.addAll(i, c);
	}

	public boolean contains(Object o) {
		return list.contains(o);
	}

	public boolean containsAll(List<RadialBond> c) {
		return list.containsAll(c);
	}

	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	public RadialBond set(int i, RadialBond b) {
		if (b != null)
			b.setModel(model);
		return list.set(i, b);
	}

	public boolean remove(RadialBond o) {
		boolean b = list.remove(o);
		if (b) {
			o.getAtom1().setRadical(true);
			o.getAtom2().setRadical(true);
		}
		return b;
	}

	public boolean removeAll(List<RadialBond> c) {
		boolean b = false;
		for (RadialBond rb : c) {
			if (remove(rb)) {
				rb.getAtom1().setRadical(true);
				rb.getAtom2().setRadical(true);
				b = true;
			}
		}
		return b;
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public int size() {
		return list.size();
	}

	public void clear() {
		list.clear();
	}

	public Iterator iterator() {
		return list.iterator();
	}

	public Object getSynchronizationLock() {
		return list;
	}

	public RadialBond get(int index) {
		if (index < 0 || index >= list.size())
			return null;
		return list.get(index);
	}

	/** return the bonds that involve the specified atom in a List. */
	public List<RadialBond> getBonds(Atom a) {
		List<RadialBond> x = new ArrayList<RadialBond>();
		synchronized (list) {
			for (RadialBond rb : list) {
				if (rb.atom1 == a || rb.atom2 == a)
					x.add(rb);
			}
		}
		return x;
	}

	/** exclusively select the specified bond */
	public void select(RadialBond rBond) {
		synchronized (list) {
			for (RadialBond rb : list)
				rb.setSelected(rb == rBond);
		}
	}

	/** returns the radial bond that binds atom a and b. Return null if no such bond is found. */
	public RadialBond getBond(Atom a, Atom b) {
		if (isEmpty())
			return null;
		synchronized (list) {
			for (RadialBond rb : list) {
				if ((rb.atom1 == a && rb.atom2 == b) || (rb.atom2 == a && rb.atom1 == b))
					return rb;
			}
		}
		return null;
	}

	/** initialize the bond queue. If the passed integer is less than 1, nullify the array. */
	public void initializeBondQ(int n) {
		if (bondQ == null) {
			if (n < 1)
				return;
			bondQ = new ObjectQueue("Radial bonds", n);
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
		ArrayList<RadialBond.Delegate> q = new ArrayList<RadialBond.Delegate>();
		synchronized (list) {
			for (RadialBond rb : list) {
				q.add(new RadialBond.Delegate(rb));
			}
		}
		bondQ.update(q);
	}

	public void clearSelection() {
		synchronized (list) {
			for (RadialBond rb : list)
				rb.setSelected(false);
		}
	}

	/** count the bonded partners for a given atom. */
	public int getBondedPartnerCount(Atom atom) {
		int count = 0;
		synchronized (list) {
			for (RadialBond rb : list) {
				if (atom.equals(rb.getAtom1()) || atom.equals(rb.getAtom2()))
					count++;
			}
		}
		return count;
	}

	/**
	 * return the atoms that are bonded to the specified atom.
	 * 
	 * @param sort
	 *            whether or not the atoms should be sorted in the array to be returned.
	 */
	@SuppressWarnings("unchecked")
	public Atom[] getBondedPartners(Atom atom, boolean sort) {
		List<Atom> x = new ArrayList<Atom>();
		for (RadialBond rb : list) {
			if (atom.equals(rb.getAtom1()))
				x.add(rb.getAtom2());
			else if (atom.equals(rb.getAtom2()))
				x.add(rb.getAtom1());
		}
		if (x.isEmpty())
			return new Atom[0];
		if (sort)
			Collections.sort(x);
		Atom[] a = new Atom[x.size()];
		for (int i = 0; i < a.length; i++)
			a[i] = x.get(i);
		return a;
	}

	/** select a set of bonds according to the instruction BitSet. */
	public void setSelectionSet(BitSet set) {
		if (set == null) {
			synchronized (list) {
				for (RadialBond rb : list)
					rb.setSelected(false);
			}
			model.getView().repaint();
			return;
		}
		model.setExclusiveSelection(false);
		synchronized (list) {
			for (int i = 0; i < list.size(); i++)
				list.get(i).setSelected(set.get(i));
		}
		model.getView().repaint();
	}

	/** return the selected set of bonds in BitSet. */
	public BitSet getSelectionSet() {
		BitSet bs = new BitSet(size());
		synchronized (list) {
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i).isSelected())
					bs.set(i);
			}
		}
		return bs;
	}

}