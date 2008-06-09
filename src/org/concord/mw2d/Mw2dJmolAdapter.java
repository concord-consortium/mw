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

package org.concord.mw2d;

import org.myjmol.api.JmolAdapter;

import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.RadialBond;
import org.concord.mw2d.models.RadialBondCollection;

class Mw2dJmolAdapter extends JmolAdapter {

	public Mw2dJmolAdapter(Logger logger) {
		super("Mw2dJmolAdapter", logger);
	}

	// The frame related methods

	public String getClientAtomStringProperty(Object clientAtom, String propertyName) {
		return null;
	}

	public JmolAdapter.AtomIterator getAtomIterator(Object clientFile) {
		return new AtomIterator((MolecularModel) clientFile);
	}

	public JmolAdapter.BondIterator getBondIterator(Object clientFile) {
		return new BondIterator((MolecularModel) clientFile);
	}

	public int getEstimatedAtomCount(Object clientFile) {
		return ((MolecularModel) clientFile).getNumberOfAtoms();
	}

	// the frame iterators

	class AtomIterator extends JmolAdapter.AtomIterator {

		MolecularModel model;
		int atomCount, iatom;
		Atom atom;

		AtomIterator(MolecularModel model) {
			this.model = model;
			atomCount = model.getNumberOfAtoms();
			iatom = 0;
		}

		public boolean hasNext() {
			if (iatom == atomCount)
				return false;
			atom = model.getAtom(iatom++);
			return true;
		}

		public Object getUniqueID() {
			if (atom == null)
				return new Object();
			return atom;
		}

		public int getElementNumber() {
			return atom.getID() + 1;
		}

		public String getElementSymbol() {
			return "Xx";
		}

		public float getX() {
			return 0.1f * (float) (atom.getRx() - 0.5 * model.getView().getWidth());
		}

		public float getY() {
			return 0.1f * (float) (0.5 * model.getView().getHeight() - atom.getRy());
		}

		public float getZ() {
			return 0;
		}

		public String getPdbAtomRecord() {
			return null;
		}

		public Object getClientAtomReference() {
			return atom;
		}

	}

	class BondIterator extends JmolAdapter.BondIterator {

		MolecularModel model;
		RadialBondCollection rbc;
		int ibond;
		RadialBond bond;

		BondIterator(MolecularModel model) {
			this.model = model;
			this.rbc = model.getBonds();
		}

		public boolean hasNext() {
			if (ibond == rbc.size())
				return false;
			bond = rbc.get(ibond++);
			return true;
		}

		public Object getAtomUniqueID1() {
			return bond.getAtom1();
		}

		public Object getAtomUniqueID2() {
			return bond.getAtom2();
		}

		// If the bond is a single bond, return 1. If a double bond, return 2. If 0 is returned, no bond is drawn.
		public int getEncodedOrder() {
			if (!bond.isVisible())
				return 0;
			byte style = bond.getBondStyle();
			if (style != RadialBond.STANDARD_STICK_STYLE && style != RadialBond.UNICOLOR_STICK_STYLE)
				return 0;
			return 1;
		}

	}

}