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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.concord.modeler.event.ModelEvent;
import org.concord.molbio.engine.Aminoacid;
import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.Codon;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.Molecule;
import org.concord.mw2d.models.RadialBond;

class AminoAcidPopupMenu extends JPopupMenu {

	private final static Aminoacid[] AMINO_ACID = Aminoacid.getAllAminoacids();
	private JRadioButtonMenuItem[] mi;
	private Atom atom;

	public AminoAcidPopupMenu() {

		super("Amino Acid");

		mi = new JRadioButtonMenuItem[AMINO_ACID.length];

		ButtonGroup bg = new ButtonGroup();
		for (int i = 0; i < AMINO_ACID.length; i++) {
			mi[i] = new JRadioButtonMenuItem(AMINO_ACID[i].getFullName() + " (" + AMINO_ACID[i].getLetter() + ")");
			if (AMINO_ACID[i].getCharge() > 0) {
				mi[i].setBackground(new Color(ColorManager.POSITIVE_CHARGE_COLOR));
			}
			else if (AMINO_ACID[i].getCharge() < 0) {
				mi[i].setBackground(new Color(ColorManager.NEGATIVE_CHARGE_COLOR));
			}
			else {
				mi[i].setBackground(new Color(ColorManager.NEUTRAL_CHARGE_COLOR));
				if (AMINO_ACID[i].getHydrophobicity() > 0) {
					mi[i].setBackground(new Color(ColorManager.HYDROPHOBIC_COLOR));
				}
				else {
					mi[i].setBackground(new Color(ColorManager.HYDROPHILIC_COLOR));
				}
			}
			final int ii = i;
			mi[i].addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (atom == null)
						return;
					if (!atom.isAminoAcid())
						return;
					if (e.getStateChange() == ItemEvent.SELECTED) {
						Element elem = ((MolecularModel) atom.getHostModel()).getElement(AMINO_ACID[ii]
								.getAbbreviation());
						atom.setElement(elem);
						adjustBondLength(atom);
						((AtomisticView) atom.getHostModel().getView()).refreshJmol();
						atom.getHostModel().getView().paintImmediately(atom.getBounds(10));
						Molecule mol = ((MolecularModel) atom.getHostModel()).getMolecules().getMolecule(atom);
						atom.getHostModel().notifyModelListeners(
								new ModelEvent(atom, "Selected index", null, mol == null ? 0 : new Integer(mol
										.indexOfAtom(atom))));
					}
				}
			});
			add(mi[i]);
			bg.add(mi[i]);
		}

	}

	private void adjustBondLength(Atom a) {
		MolecularModel model = (MolecularModel) atom.getHostModel();
		List list = model.getBonds().getBonds(a);
		if (list != null && !list.isEmpty()) {
			RadialBond rb = null;
			Atom a1 = null, a2 = null;
			synchronized (model.getBonds().getSynchronizationLock()) {
				for (Iterator it = list.iterator(); it.hasNext();) {
					rb = (RadialBond) it.next();
					a1 = rb.getAtom1();
					a2 = rb.getAtom2();
					rb.setBondLength(RadialBond.PEPTIDE_BOND_LENGTH_PARAMETER * (a1.getSigma() + a2.getSigma()));
				}
			}
		}
	}

	public void show(Component invoker, int x, int y) {
		if (atom == null)
			return;
		if (!((MolecularModel) atom.getHostModel()).changeApprovedByRecorder())
			return;
		char[] code = null;
		if (atom.isAminoAcid()) {
			if (atom.getCodon() != null)
				code = atom.getCodon().toCharArray();
		}
		if (code == null)
			return;
		Aminoacid a = Codon.expressFromDNA(code);
		if (a == null)
			return;
		for (int i = 0; i < AMINO_ACID.length; i++) {
			if (a == AMINO_ACID[i])
				mi[i].setSelected(true);
		}
		super.show(invoker, x, y);
	}

	public void setAtom(Atom a) {
		atom = a;
	}

}