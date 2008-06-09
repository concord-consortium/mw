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

package org.concord.mw3d;

import org.myjmol.api.JmolAdapter;

import org.concord.mw3d.models.Atom;
import org.concord.mw3d.models.MolecularModel;
import org.concord.mw3d.models.RBond;

class Mw3dJmolAdapter extends JmolAdapter {

	Mw3dJmolAdapter(Logger logger) {
		super("Mw3dJmolAdapter", logger);
	}

	// The frame related methods

	public String getClientAtomStringProperty(Object clientAtom, String propertyName) {
		Object value = ((Atom) clientAtom).getProperty(propertyName);
		return value == null ? null : "" + value;
	}

	public JmolAdapter.AtomIterator getAtomIterator(Object clientFile) {
		return new AtomIterator((MolecularModel) clientFile);
	}

	public JmolAdapter.BondIterator getBondIterator(Object clientFile) {
		return new BondIterator((MolecularModel) clientFile);
	}

	public int getEstimatedAtomCount(Object clientFile) {
		return ((MolecularModel) clientFile).getAtomCount();
	}

	// the frame iterators

	private class AtomIterator extends JmolAdapter.AtomIterator {

		MolecularModel model;
		int atomCount, iatom;
		Atom atom;

		AtomIterator(MolecularModel model) {
			this.model = model;
			atomCount = model.getAtomCount();
			iatom = 0;
		}

		public boolean hasNext() {
			if (iatom == atomCount)
				return false;
			atom = model.getAtom(iatom++);
			return true;
		}

		public Object getUniqueID() {
			return atom;
		}

		public int getElementNumber() {
			return atom.getElementNumber();
		}

		public String getElementSymbol() {
			return atom.getSymbol();
		}

		public float getX() {
			return atom.getRx();
		}

		public float getY() {
			return atom.getRy();
		}

		public float getZ() {
			return atom.getRz();
		}

		public Object getClientAtomReference() {
			return atom;
		}

	}

	private class BondIterator extends JmolAdapter.BondIterator {

		private MolecularModel model;
		private int ibond;
		private RBond bond;

		BondIterator(MolecularModel model) {
			this.model = model;
		}

		public boolean hasNext() {
			if (ibond == model.getRBondCount())
				return false;
			bond = model.getRBond(ibond++);
			return true;
		}

		public Object getAtomUniqueID1() {
			if (bond == null)
				return null;
			return bond.getAtom1();
		}

		public Object getAtomUniqueID2() {
			if (bond == null)
				return null;
			return bond.getAtom2();
		}

		// If the bond is a single bond, return 1. If a double bond, return 2. If 0 is returned, no bond is drawn.
		public int getEncodedOrder() {
			return bond.getOrder();
		}

	}

}