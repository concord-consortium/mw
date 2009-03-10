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

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;

import org.concord.molbio.engine.Nucleotide;
import org.concord.molbio.ui.DNAScroller;
import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.MolecularModel;

class NucleotidePopupMenu extends ViewPopupMenu {

	private JRadioButtonMenuItem miA;
	private JRadioButtonMenuItem miC;
	private JRadioButtonMenuItem miG;
	private JRadioButtonMenuItem miT;
	private JRadioButtonMenuItem miU;
	private Atom atom;

	public NucleotidePopupMenu(AtomisticView v) {

		super("Nucleotide", v);

		ItemListener itemListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (atom == null)
					return;
				if (!atom.isNucleotide())
					return;
				JRadioButtonMenuItem rmbi = (JRadioButtonMenuItem) e.getSource();
				if (e.getStateChange() == ItemEvent.SELECTED) {
					Element elem = ((MolecularModel) atom.getHostModel()).getElement(rmbi.getText());
					atom.setElement(elem);
					((AtomisticView) atom.getHostModel().getView()).refreshJmol();
					atom.getHostModel().getView().paintImmediately(atom.getBounds(10));
				}
			}
		};

		ButtonGroup bg = new ButtonGroup();

		miA = new JRadioButtonMenuItem("A");
		miA.setToolTipText(Nucleotide.getAdenine().getFullName());
		miA.addItemListener(itemListener);
		add(miA);
		bg.add(miA);

		miC = new JRadioButtonMenuItem("C");
		miC.setToolTipText(Nucleotide.getCytosine().getFullName());
		miC.addItemListener(itemListener);
		add(miC);
		bg.add(miC);

		miG = new JRadioButtonMenuItem("G");
		miG.setToolTipText(Nucleotide.getGuanine().getFullName());
		miG.addItemListener(itemListener);
		add(miG);
		bg.add(miG);

		miT = new JRadioButtonMenuItem("T");
		miT.setToolTipText(Nucleotide.getThymine().getFullName());
		miT.addItemListener(itemListener);
		add(miT);
		bg.add(miT);

		miU = new JRadioButtonMenuItem("U");
		miU.setToolTipText(Nucleotide.getUracil().getFullName());
		miU.addItemListener(itemListener);
		add(miU);
		bg.add(miU);

	}

	public void show(Component invoker, int x, int y) {
		if (atom == null)
			return;
		if (!atom.isNucleotide())
			return;
		if (!((MolecularModel) atom.getHostModel()).changeApprovedByRecorder())
			return;
		String s = atom.getName();
		if (s.equals("A"))
			miA.setSelected(true);
		else if (s.equals("C"))
			miC.setSelected(true);
		else if (s.equals("G"))
			miG.setSelected(true);
		else if (s.equals("T"))
			miT.setSelected(true);
		else if (s.equals("U"))
			miU.setSelected(true);
		s = ((AtomisticView) atom.getHostModel().getView()).getColorCoding();
		miA.setBackground(DNAScroller.getCodonColor(Nucleotide.ADENINE_NAME, s));
		miC.setBackground(DNAScroller.getCodonColor(Nucleotide.CYTOSINE_NAME, s));
		miG.setBackground(DNAScroller.getCodonColor(Nucleotide.GUANINE_NAME, s));
		miT.setBackground(DNAScroller.getCodonColor(Nucleotide.THYMINE_NAME, s));
		miU.setBackground(DNAScroller.getCodonColor(Nucleotide.URACIL_NAME, s));
		super.show(invoker, x, y);
	}

	public void setAtom(Atom a) {
		atom = a;
	}

}