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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.BitSet;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.IconPool;

class DefaultPopupMenu extends JPopupMenu {

	private MolecularContainer container;
	private JMenu minimizationMenu;
	private JMenuItem miPaste, miSpin, miRove, miMenuBar, miToolBar, miBottomBar, miBorder;
	private JMenuItem miMinimizeSelected;
	private JMenuItem miMinimizeUnselected;

	public void show(Component c, int x, int y) {
		boolean b = container.view.getPastingObject() != null;
		miPaste.setEnabled(b);
		minimizationMenu.setEnabled(!container.model.isRunning());
		miMinimizeSelected.setEnabled(container.view.getSelectionSet().cardinality() > 0);
		miMinimizeUnselected
				.setEnabled(container.view.getSelectionSet().cardinality() < container.model.getAtomCount());
		super.show(c, x, y);
	}

	DefaultPopupMenu(MolecularContainer c) {

		super("Default");
		container = c;

		miPaste = new JMenuItem(container.view.getActionMap().get("paste"));
		miPaste.setEnabled(false);
		String s = MolecularContainer.getInternationalText("Paste");
		if (s != null)
			miPaste.setText(s);
		add(miPaste);
		addSeparator();

		JMenuItem mi = new JMenuItem(container.view.getActionMap().get("open model"));
		s = MolecularContainer.getInternationalText("OpenModel");
		if (s != null)
			mi.setText(s);
		add(mi);

		mi = new JMenuItem(container.view.getActionMap().get("save model"));
		s = MolecularContainer.getInternationalText("SaveModel");
		if (s != null)
			mi.setText(s);
		add(mi);
		addSeparator();

		mi = new JMenuItem(container.view.getActionMap().get("invert selection"));
		s = MolecularContainer.getInternationalText("InvertSelection");
		if (s != null)
			mi.setText(s);
		add(mi);

		s = MolecularContainer.getInternationalText("NavigationMode");
		miRove = new JCheckBoxMenuItem(s != null ? s : "Navigation Mode");
		miRove.setIcon(new ImageIcon(MolecularContainer.class.getResource("resources/Immersive.gif")));
		miRove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				container.view.getViewer().setNavigationMode(((JCheckBoxMenuItem) e.getSource()).isSelected());
				container.notifyChange();
			}
		});
		add(miRove);

		s = MolecularContainer.getInternationalText("Spin");
		miSpin = new JCheckBoxMenuItem(s != null ? s : "Spin");
		miSpin.setIcon(IconPool.getIcon("spin"));
		miSpin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				container.view.setSpinOn(((JCheckBoxMenuItem) e.getSource()).isSelected());
			}
		});
		add(miSpin);
		addSeparator();

		s = MolecularContainer.getInternationalText("ShowForceFields");
		JMenu subMenu = new JMenu(s != null ? s : "Show Force Fields");
		subMenu.setIcon(new ImageIcon(MolecularContainer.class.getResource("resources/forcefield.gif")));
		add(subMenu);

		s = MolecularContainer.getInternationalText("AngularBonds");
		mi = new JCheckBoxMenuItem(s != null ? s : "Angular Bonds");
		mi.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				container.view.getViewer().setABondRendered(e.getStateChange() == ItemEvent.SELECTED);
				container.view.repaint();
			}
		});
		subMenu.add(mi);

		s = MolecularContainer.getInternationalText("TorsionalBonds");
		mi = new JCheckBoxMenuItem(s != null ? s : "Torsional Bonds");
		mi.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				container.view.getViewer().setTBondRendered(e.getStateChange() == ItemEvent.SELECTED);
				container.view.repaint();
			}
		});
		subMenu.add(mi);

		s = MolecularContainer.getInternationalText("RunEnergyMinimization");
		minimizationMenu = new JMenu(s != null ? s : "Run Energy Minimization");
		minimizationMenu.setIcon(IconPool.getIcon("steepest descent"));
		add(minimizationMenu);
		addSeparator();

		s = MolecularContainer.getInternationalText("ForSelectedAtoms");
		miMinimizeSelected = new JMenuItem(s != null ? s : "For Selected Atoms");
		miMinimizeSelected.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (container.minimizerDialog == null)
					container.minimizerDialog = new MinimizerDialog(container.model);
				container.minimizerDialog.setLocationRelativeTo(container);
				container.minimizerDialog.setVisible(true);
				container.minimizerDialog.runMinimizer(container.view.getSelectionSet());
			}
		});
		minimizationMenu.add(miMinimizeSelected);

		s = MolecularContainer.getInternationalText("ForUnselectedAtoms");
		miMinimizeUnselected = new JMenuItem(s != null ? s : "For Unselected Atoms");
		miMinimizeUnselected.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (container.minimizerDialog == null)
					container.minimizerDialog = new MinimizerDialog(container.model);
				container.minimizerDialog.setLocationRelativeTo(container);
				container.minimizerDialog.setVisible(true);
				int n = container.model.getAtomCount();
				BitSet bs = new BitSet(n);
				bs.set(0, n);
				bs.andNot(container.view.getSelectionSet());
				container.minimizerDialog.runMinimizer(bs);
			}
		});
		minimizationMenu.add(miMinimizeUnselected);

		s = MolecularContainer.getInternationalText("ForAllAtoms");
		mi = new JMenuItem(s != null ? s : "For All Atoms");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (container.minimizerDialog == null)
					container.minimizerDialog = new MinimizerDialog(container.model);
				container.minimizerDialog.setLocationRelativeTo(container);
				container.minimizerDialog.setVisible(true);
				container.minimizerDialog.runMinimizer();
			}
		});
		minimizationMenu.add(mi);

		mi = new JMenuItem(container.view.getActionMap().get("properties"));
		s = MolecularContainer.getInternationalText("Properties");
		if (s != null)
			mi.setText(s);
		add(mi);

		mi = new JMenuItem(container.view.getActionMap().get("snapshot"));
		s = MolecularContainer.getInternationalText("Snapshot");
		if (s != null)
			mi.setText(s);
		add(mi);

		mi = new JMenuItem(container.view.getActionMap().get("view options"));
		s = MolecularContainer.getInternationalText("ViewOption");
		if (s != null)
			mi.setText(s);
		add(mi);

		mi = new JMenuItem(container.view.getActionMap().get("task manager"));
		s = MolecularContainer.getInternationalText("TaskManager");
		if (s != null)
			mi.setText(s);
		add(mi);
		addSeparator();

		s = MolecularContainer.getInternationalText("ShowMenuBar");
		miMenuBar = new JCheckBoxMenuItem(s != null ? s : "Show Menu Bar");
		miMenuBar.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				container.enableMenuBar(e.getStateChange() == ItemEvent.SELECTED);
				container.notifyChange();
			}
		});
		add(miMenuBar);

		s = MolecularContainer.getInternationalText("ShowToolBar");
		miToolBar = new JCheckBoxMenuItem(s != null ? s : "Show Tool Bar");
		miToolBar.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				container.enableToolBar(e.getStateChange() == ItemEvent.SELECTED);
				container.notifyChange();
			}
		});
		add(miToolBar);

		s = MolecularContainer.getInternationalText("ShowBottomBar");
		miBottomBar = new JCheckBoxMenuItem(s != null ? s : "Show Bottom Bar");
		miBottomBar.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				container.enableBottomBar(e.getStateChange() == ItemEvent.SELECTED);
				container.notifyChange();
			}
		});
		add(miBottomBar);

		s = MolecularContainer.getInternationalText("ShowBorder");
		miBorder = new JCheckBoxMenuItem(s != null ? s : "Show Border");
		miBorder.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					container.setBorder(BorderFactory.createRaisedBevelBorder());
					container.view.setBorder(BorderFactory.createLoweredBevelBorder());
				}
				else {
					container.setBorder(BorderFactory.createEmptyBorder());
					container.view.setBorder(BorderFactory.createEmptyBorder());
				}
				container.notifyChange();
			}
		});
		add(miBorder);

		addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				ModelerUtilities.setWithoutNotifyingListeners(miRove, container.view.getViewer().getNavigationMode());
				ModelerUtilities.setWithoutNotifyingListeners(miSpin, container.view.isSpinOn());
				ModelerUtilities.setWithoutNotifyingListeners(miMenuBar, container.isMenuBarEnabled());
				ModelerUtilities.setWithoutNotifyingListeners(miToolBar, container.isToolBarEnabled());
				ModelerUtilities.setWithoutNotifyingListeners(miBottomBar, container.isBottomBarEnabled());
				ModelerUtilities
						.setWithoutNotifyingListeners(miBorder, !(container.getBorder() instanceof EmptyBorder));
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}
		});

		pack();

	}
}