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

import java.util.Iterator;
import java.util.List;

/**
 * This class models a DNA single strand.
 * 
 * @author Charles Xie
 */

public class DNAStrand extends Molecule {

	public DNAStrand() {
		super();
	}

	public DNAStrand(Molecule mol) {
		synchronized (mol.getSynchronizedLock()) {
			for (Iterator it = mol.iterator(); it.hasNext();) {
				addAtom((Atom) it.next());
			}
		}
		setModel(mol.getHostModel());
	}

	public Molecule duplicate() {
		return new DNAStrand(super.duplicate());
	}

	public String toString() {
		return getNucleotideCode();
	}

	/**
	 * return the sequence of the nucleotides. The sequence is read from left to right when the strand faces up, i.e.
	 * when the bases are above the backbone. If the strand faces down, then the sequence should be read from right to
	 * left.
	 */
	public String getNucleotideCode() {
		Atom[] nc = getBaseSequence();
		if (nc == null)
			return null;
		String s = "";
		for (int i = 0; i < nc.length; i++)
			s += nc[i].getName();
		return s;
	}

	/** set the nucleotide sequence. */
	public void setNucleotideCode(String code) {
		if (code == null)
			return;
		if (code.length() * 2 != size())
			throw new IllegalArgumentException("code length incompatible with the length of this DNA");
		Atom[] nc = getBaseSequence();
		if (nc == null)
			return;
		for (int i = 0; i < nc.length; i++) {
			nc[i].setElement(model.getElement("" + code.charAt(i)));
		}
	}

	/**
	 * return the sequence of the atoms that represent the bases. The sequence is read from left to right when the
	 * strand faces up, i.e. when the bases are above the backbone. If the strand faces down, then the sequence should
	 * be read from right to left.
	 */
	public Atom[] getBaseSequence() {

		int n = size();
		if (n <= 1)
			return new Atom[0];

		Atom[] sp = new Atom[n / 2];
		Atom[] at;
		List<RadialBond> bond = getBonds();

		Atom[] nc = new Atom[n / 2];

		if (n > 2) {

			/* find the Sp atoms at the ends */
			Atom a = null;
			for (int i = 0; i < n; i++) {
				a = getAtom(i);
				at = getBondedPartners(bond, a);
				if (at.length == 2) {
					double dx1, dx2, dy1, dy2;
					if (at[0].getID() != Element.ID_SP) {
						dx1 = at[0].rx - a.rx;
						dx2 = at[1].rx - a.rx;
						dy1 = at[0].ry - a.ry;
						dy2 = at[1].ry - a.ry;
					}
					else {
						dx1 = at[1].rx - a.rx;
						dx2 = at[0].rx - a.rx;
						dy1 = at[1].ry - a.ry;
						dy2 = at[0].ry - a.ry;
					}
					if (dx1 * dy2 - dx2 * dy1 > 0) {
						sp[0] = a;
					}
					else {
						sp[sp.length - 1] = a;
					}
				}
			}

			/* find the Sp chains */
			a = sp[0];
			int k = 1;
			while (a != sp[sp.length - 1]) {
				at = getBondedPartners(bond, a);
				for (int i = 0; i < at.length; i++) {
					if (at[i].getID() == Element.ID_SP) {
						boolean b = false;
						for (int j = 0; j < k; j++) {
							if (at[i] == sp[j]) {
								b = true;
								break;
							}
						}
						if (!b) {
							sp[k] = a = at[i];
							k++;
							break;
						}
					}
				}
			}

			/* find the bases attached to the Sp atoms */
			for (int i = 0; i < sp.length; i++) {
				at = getBondedPartners(bond, sp[i]);
				for (int j = 0; j < at.length; j++) {
					if (at[j].isNucleotide() && at[j].getID() != Element.ID_SP) {
						nc[i] = at[j];
						break;
					}
				}
			}

		}
		else {

			Atom a = null;
			for (int i = 0; i < n; i++) {
				a = getAtom(i);
				if (a.isNucleotide() && a.getID() != Element.ID_SP) {
					nc[0] = a;
					break;
				}
			}

		}

		return nc;

	}

	/**
	 * attach a nucleotide (randomly be A, C, G, T) to this DNA strand. The specified atom in the argument is the
	 * sugar-phosphate part of the end to which a new nucleotide will be joined to.
	 */
	public boolean attachRandomNucleotide(Atom endToAttach) {
		int id1 = Element.ID_SP;
		int id2 = Math.round(Element.ID_A + (float) Math.random() * 3);
		double d1 = 1.2 * model.getElement(id1).getSigma();
		double d2 = 0.6 * (model.getElement(id1).getSigma() + model.getElement(id2).getSigma());
		int n = size();
		if (n <= 2 && endToAttach == null) {
			double x1 = getAtom(n - 2).getRx();
			double y1 = getAtom(n - 2).getRy();
			double x2 = getAtom(n - 1).getRx();
			double y2 = getAtom(n - 1).getRy();
			double rd = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
			double costheta = (x2 - x1) / rd;
			double sintheta = (y2 - y1) / rd;
			double x = x1 - d1 * sintheta;
			double y = y1 + d1 * costheta;
			if (model.view.insertAnAtom(x, y, id1, false)) {
				if (model.view.insertAnAtom(x + d2 * costheta, y + d2 * sintheta, id2, false)) {
					int k = model.getNumberOfAtoms();
					int m = getAtom(n - 2).getIndex();
					model.getBonds().add(new RadialBond(model.getAtom(k - 2), model.getAtom(k - 1), d2));
					model.getBonds().add(new RadialBond(model.getAtom(k - 2), model.getAtom(m), d1));
					model.getBends().add(
							new AngularBond(model.getAtom(k - 2), model.getAtom(m + 1), model.getAtom(m), Math.PI / 2));
					model.getBends().add(
							new AngularBond(model.getAtom(k - 1), model.getAtom(m), model.getAtom(k - 2), Math.PI / 2));
					MoleculeCollection.sort(model);
					return true;
				}
				model.getAtom(model.getNumberOfAtoms() - 1).setSelected(true);
				model.view.removeSelectedComponent();
			}
		}
		else if (endToAttach != null) {
			Atom sp = null;
			Atom nc = null;
			Atom[] at = model.bonds.getBondedPartners(endToAttach, true);
			if (at != null) {
				for (int i = 0; i < at.length; i++) {
					if (at[i].getID() == Element.ID_SP) {
						sp = at[i];
					}
					else if (at[i].isNucleotide()) {
						nc = at[i];
					}
				}
			}
			if (sp == null)
				return false;
			double x1 = sp.getRx();
			double y1 = sp.getRy();
			double x2 = endToAttach.getRx();
			double y2 = endToAttach.getRy();
			double r2 = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
			double costheta = (x2 - x1) / r2;
			double sintheta = (y2 - y1) / r2;
			x1 = x2 + d1 * costheta;
			y1 = y2 + d1 * sintheta;
			if (model.view.insertAnAtom(x1, y1, id1, false)) {
				if (nc == null)
					return false;
				x2 = nc.getRx();
				y2 = nc.getRy();
				x1 = x2 + d1 * costheta;
				y1 = y2 + d1 * sintheta;
				if (model.view.insertAnAtom(x1, y1, id2, false)) {
					int k = model.getNumberOfAtoms();
					model.getBonds().add(new RadialBond(model.getAtom(k - 2), model.getAtom(k - 1), d2));
					model.getBonds().add(new RadialBond(model.getAtom(k - 2), endToAttach, d1));
					model.getBends().add(new AngularBond(model.getAtom(k - 2), nc, endToAttach, Math.PI / 2));
					model.getBends().add(
							new AngularBond(model.getAtom(k - 1), endToAttach, model.getAtom(k - 2), Math.PI / 2));
					MoleculeCollection.sort(model);
					return true;
				}
				model.getAtom(model.getNumberOfAtoms() - 1).setSelected(true);
				model.view.removeSelectedComponent();
			}
		}
		return false;
	}

}