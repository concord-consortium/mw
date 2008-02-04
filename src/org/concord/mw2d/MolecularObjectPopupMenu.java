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

import java.awt.EventQueue;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import org.concord.mw2d.models.CurvedSurface;
import org.concord.mw2d.models.MolecularObject;

class MolecularObjectPopupMenu extends MoleculePopupMenu {

	private JMenuItem miSS, miCharge, miNone;
	private JMenu menu;

	void setCoor(int x, int y) {
		super.setCoor(x, y);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (view.selectedComponent instanceof MolecularObject) {
					miSS.setSelected(view.getShowSites());
					if (view.selectedComponent instanceof CurvedSurface) {
						menu.setEnabled(true);
						int i = ((CurvedSurface) view.selectedComponent).getColorMode();
						miNone.setSelected(i == CurvedSurface.NONE);
						miCharge.setSelected(i == CurvedSurface.CHARGE);
					}
					else {
						menu.setEnabled(false);
					}
				}
			}
		});
	}

	MolecularObjectPopupMenu(AtomisticView v) {

		super(v);

		addSeparator();
		miSS = new JCheckBoxMenuItem("Show Sites");
		miSS.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof MolecularObject) {
					view.setShowSites(miSS.isSelected());
					view.repaint();
				}
			}
		});
		add(miSS);

		JMenuItem mi = new JMenuItem("Increase Stretching Flexibility");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof MolecularObject)
					((MolecularObject) view.selectedComponent).increaseStretchingFlexibility(true);
			}
		});
		add(mi);

		mi = new JMenuItem("Decrease Stretching Flexibility");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof MolecularObject)
					((MolecularObject) view.selectedComponent).increaseStretchingFlexibility(false);
			}
		});
		add(mi);

		mi = new JMenuItem("Increase Bending Flexibility");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof MolecularObject)
					((MolecularObject) view.selectedComponent).increaseBendingFlexibility(true);
			}
		});
		add(mi);

		mi = new JMenuItem("Decrease Bending Flexibility");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof MolecularObject)
					((MolecularObject) view.selectedComponent).increaseBendingFlexibility(false);
			}
		});
		add(mi);
		addSeparator();

		mi = new JMenuItem("Rigidify");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof MolecularObject)
					((MolecularObject) view.selectedComponent).rigidify();
			}
		});
		add(mi);

		mi = new JMenuItem("Soften");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.selectedComponent instanceof MolecularObject)
					((MolecularObject) view.selectedComponent).soften();
			}
		});
		add(mi);
		addSeparator();

		menu = new JMenu("Colorization");
		add(menu);

		ButtonGroup bg = new ButtonGroup();
		miNone = new JRadioButtonMenuItem("None");
		miNone.setSelected(true);
		miNone.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (view.selectedComponent instanceof CurvedSurface) {
						((CurvedSurface) view.selectedComponent).setColorMode(CurvedSurface.NONE);
						view.repaint();
					}
				}
			}
		});
		menu.add(miNone);
		bg.add(miNone);

		miCharge = new JRadioButtonMenuItem("Charge");
		miCharge.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					if (view.selectedComponent instanceof CurvedSurface) {
						((CurvedSurface) view.selectedComponent).setColorMode(CurvedSurface.CHARGE);
						view.repaint();
					}
				}
			}
		});
		menu.add(miCharge);
		bg.add(miCharge);

		pack();

	}

}