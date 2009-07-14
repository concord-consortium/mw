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

import org.concord.modeler.event.ModelEvent;
import org.concord.molbio.engine.Aminoacid;
import org.concord.mw2d.ui.GrowthModeDialog;

public class Polypeptide extends Molecule {

	public Polypeptide() {
		super();
	}

	public Polypeptide(Molecule mol) {
		synchronized (mol) {
			for (Iterator it = mol.iterator(); it.hasNext();) {
				addAtom((Atom) it.next());
			}
		}
		setModel(mol.getHostModel());
	}

	public Molecule duplicate() {
		return new Polypeptide(super.duplicate());
	}

	public String toString() {
		return getAminoAcidCode(true);
	}

	/** if this molecule represents a protein, return its possible DNA code. */
	public String getDNACode() {
		int n = size();
		char[] seq = new char[3 * n];
		char[] code = null;
		Atom a = null;
		for (int i = 0; i < n; i++) {
			a = getAtom(i);
			if (a.isAminoAcid()) {
				code = a.getCodon().toCharArray();
			}
			else {
				code = null;
			}
			if (code != null) {
				seq[3 * i] = code[0];
				seq[3 * i + 1] = code[1];
				seq[3 * i + 2] = code[2];
			}
			else {
				return null;
			}
		}
		return new String(seq);
	}

	/** return the sequence of the amino acids if this molecule is a protein, in one-letter code. */
	public String getAminoAcidCode(boolean oneLetter) {
		int n = size();
		String seq = "";
		Atom a = null;
		char[] code = null;
		for (int i = 0; i < n; i++) {
			a = getAtom(i);
			if (a.isAminoAcid()) {
				code = a.getCodon().toCharArray();
			}
			else {
				code = null;
			}
			if (code != null) {
				seq += oneLetter ? Aminoacid.express(code).getLetter() : "-"
						+ Aminoacid.express(code).getAbbreviation();
			}
			else {
				return null;
			}
		}
		if (!oneLetter && seq.length() > 0) {
			seq = seq.substring(1);
		}
		return seq;
	}

	void sortSequence() {
		if (size() < 3)
			return;
		Atom[] end = getTermini();
		if (end.length != 2)
			return;
		clear();
		addAtom(end[0]);
		Atom[] at = model.bonds.getBondedPartners(end[0], true);
		boolean b;
		while (at.length == 1 || at.length == 2) {
			b = false;
			if (!contains(at[0])) {
				addAtom(at[0]);
				at = model.bonds.getBondedPartners(at[0], true);
				b = true;
			}
			if (at.length == 2) {
				if (!contains(at[1])) {
					addAtom(at[1]);
					at = model.bonds.getBondedPartners(at[1], true);
					b = true;
				}
			}
			if (!b)
				break;
		}
		model.notifyModelListeners(new ModelEvent(this, ModelEvent.MODEL_CHANGED, null, new Integer(model.molecules
				.indexOf(this))));
	}

	public boolean attachRandomAminoAcidToTerminus(Atom terminus) {
		if (terminus == null)
			throw new IllegalArgumentException("null arg not allowed");
		if (!contains(terminus))
			throw new IllegalArgumentException("terminus must be part of molecule");
		Atom[] a = model.bonds.getBondedPartners(terminus, true);
		if (a.length != 1)
			return false;
		double x = terminus.rx - a[0].rx;
		double y = terminus.ry - a[0].ry;
		double d = Math.hypot(x, y);
		double costheta = x / d;
		double sintheta = y / d;
		int id = Math.round(Element.ID_ALA + (float) Math.random() * 19);
		Element e = model.getElement(id);
		d = RadialBond.PEPTIDE_BOND_LENGTH_PARAMETER * (terminus.getSigma() + e.getSigma());
		if (GrowthModeDialog.getMode() == GrowthModeDialog.ZIGZAG) {
			if (model.view.insertAnAtom(terminus.rx + d * (costheta * Particle.COS120 - sintheta * Particle.SIN120),
					terminus.ry + d * (sintheta * Particle.COS120 + costheta * Particle.SIN120), id, true)
					|| model.view.insertAnAtom(terminus.rx + d
							* (costheta * Particle.COS240 - sintheta * Particle.SIN240), terminus.ry + d
							* (sintheta * Particle.COS240 + costheta * Particle.SIN240), id, true)) {
				model.bonds.add(new RadialBond.Builder(terminus, model.atom[model.getNumberOfAtoms() - 1])
						.bondLength(d).build());
				addAtom(model.atom[model.getNumberOfAtoms() - 1]);
				MoleculeCollection.sort(model);
				return true;
			}
		}
		else if (GrowthModeDialog.getMode() == GrowthModeDialog.SPIRAL) {
			if (model.view.insertAnAtom(terminus.rx + d * (costheta * Particle.COS120 - sintheta * Particle.SIN120),
					terminus.ry + d * (sintheta * Particle.COS120 + costheta * Particle.SIN120), id, true)
					|| model.view.insertAnAtom(terminus.rx + d
							* (costheta * Particle.COS60 - sintheta * Particle.SIN60), terminus.ry + d
							* (sintheta * Particle.COS60 + costheta * Particle.SIN60), id, true)
					|| model.view.insertAnAtom(terminus.rx + d
							* (costheta * Particle.COS240 - sintheta * Particle.SIN240), terminus.ry + d
							* (sintheta * Particle.COS240 + costheta * Particle.SIN240), id, true)
					|| model.view.insertAnAtom(terminus.rx + d
							* (costheta * Particle.COS300 - sintheta * Particle.SIN300), terminus.ry + d
							* (sintheta * Particle.COS300 + costheta * Particle.SIN300), id, true)) {
				model.bonds.add(new RadialBond.Builder(terminus, model.atom[model.getNumberOfAtoms() - 1])
						.bondLength(d).build());
				addAtom(model.atom[model.getNumberOfAtoms() - 1]);
				MoleculeCollection.sort(model);
				return true;
			}
		}
		return false;
	}

	/** use ONLY when this molecule represents an abstract protein */
	public void adjustPeptideBondLengths() {
		List list = getBonds();
		RadialBond rb = null;
		Atom a1 = null, a2 = null;
		for (Iterator it = list.iterator(); it.hasNext();) {
			rb = (RadialBond) it.next();
			a1 = rb.getAtom1();
			a2 = rb.getAtom2();
			rb.setBondLength(RadialBond.PEPTIDE_BOND_LENGTH_PARAMETER * (a1.getSigma() + a2.getSigma()));
		}
	}

	/** set the DNA code for this molecule, if it represents a protein. Used for insertion and deletion. */
	public int setDNACode(String gene) {

		if (gene == null)
			throw new IllegalArgumentException("gene is null");
		if (gene.length() != 3 * size())
			throw new IllegalArgumentException("gene mismatch with this molecule : " + gene.length() + ":" + 3 * size());

		// first check if there is any STOP codon
		char[] code = new char[3];
		for (int i = 0, j = size(); i < j; i++) {
			code[0] = gene.charAt(3 * i);
			code[1] = gene.charAt(3 * i + 1);
			code[2] = gene.charAt(3 * i + 2);
			if (Codon.isStopCodon(code))
				return i;
		}

		// if there is no STOP codon, set amino acids first
		Aminoacid a = null;
		Atom atom = null;
		for (int i = 0, j = size(); i < j; i++) {
			atom = getAtom(i);
			code = atom.getCodon().toCharArray();
			code[0] = gene.charAt(3 * i);
			code[1] = gene.charAt(3 * i + 1);
			code[2] = gene.charAt(3 * i + 2);
			a = Codon.expressFromDNA(code);
			Object o = a.getProperty("element");
			if (o instanceof Byte) {
				atom.setElement(model.getElement(((Byte) o).byteValue()));
			}
		}
		adjustPeptideBondLengths();
		model.getView().repaint();

		return -1;

	}

}