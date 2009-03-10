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

import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;

import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.MolecularModel;

class AtomMutationPopupMenu extends ViewPopupMenu {

	private JRadioButtonMenuItem miNt;
	private JRadioButtonMenuItem miPl;
	private JRadioButtonMenuItem miWs;
	private JRadioButtonMenuItem miCk;
	private Atom atom;

	public AtomMutationPopupMenu(AtomisticView v) {

		super("Atom Mutation", v);

		ItemListener itemListener = new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (atom == null)
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

		miNt = new JRadioButtonMenuItem("Nt");
		miNt.setBackground(Color.white);
		miNt.addItemListener(itemListener);
		add(miNt);
		bg.add(miNt);

		miPl = new JRadioButtonMenuItem("Pl");
		miPl.setBackground(Color.green);
		miPl.addItemListener(itemListener);
		add(miPl);
		bg.add(miPl);

		miWs = new JRadioButtonMenuItem("Ws");
		miWs.setBackground(Color.blue);
		miWs.addItemListener(itemListener);
		add(miWs);
		bg.add(miWs);

		miCk = new JRadioButtonMenuItem("Ck");
		miCk.setBackground(Color.magenta);
		miCk.addItemListener(itemListener);
		add(miCk);
		bg.add(miCk);

	}

	public void show(Component invoker, int x, int y) {
		if (atom == null)
			return;
		if (!((MolecularModel) atom.getHostModel()).changeApprovedByRecorder())
			return;
		switch (atom.getID()) {
		case Element.ID_NT:
			miNt.setSelected(true);
			break;
		case Element.ID_PL:
			miPl.setSelected(true);
			break;
		case Element.ID_WS:
			miWs.setSelected(true);
			break;
		case Element.ID_CK:
			miCk.setSelected(true);
			break;
		}
		super.show(invoker, x, y);
	}

	public void setAtom(Atom a) {
		atom = a;
	}

}