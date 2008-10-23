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

package org.concord.modeler;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.concord.modeler.text.Page;
import org.concord.modeler.ui.IconPool;

class InsertComponentPopupMenu extends JPopupMenu {

	InsertComponentPopupMenu(Page page) {

		super();

		String s = Modeler.getInternationalText("InsertModel");
		JMenu menu = new JMenu(s != null ? s : "Model Container");

		JMenuItem menuItem = new JMenuItem(page.getAction(Page.INSERT_ATOM_CONTAINER));
		s = Modeler.getInternationalText("InsertBasic2DContainer");
		if (s != null)
			menuItem.setText(s);
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_CHEM_CONTAINER));
		s = Modeler.getInternationalText("InsertReaction2DContainer");
		if (s != null)
			menuItem.setText(s);
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_PROSYN_CONTAINER));
		s = Modeler.getInternationalText("InsertProteinSynthesisContainer");
		if (s != null)
			menuItem.setText(s);
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_GB_CONTAINER));
		s = Modeler.getInternationalText("InsertMesoscaleContainer");
		if (s != null)
			menuItem.setText(s);
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName(Page.INSERT_JMOL);
		s = Modeler.getInternationalText("InsertJmolContainer");
		menuItem.setText((s != null ? s : Page.INSERT_JMOL) + "...");
		menuItem.setIcon(new ImageIcon(Page.class.getResource("images/MV.gif")));
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName(Page.INSERT_MW3D);
		s = Modeler.getInternationalText("InsertBasic3DContainer");
		menuItem.setText((s != null ? s : Page.INSERT_MW3D) + "...");
		menuItem.setIcon(new ImageIcon(Page.class.getResource("images/MD3D.gif")));
		menu.add(menuItem);

		add(menu);

		s = Modeler.getInternationalText("InsertModelOutput");
		menu = new JMenu(s != null ? s : "Model Output");
		add(menu);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Numeric Box");
		s = Modeler.getInternationalText("InsertNumericBox");
		menuItem.setText((s != null ? s : "Numeric Box") + "...");
		menuItem.setIcon(IconPool.getIcon("numeric"));
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Bar Graph");
		s = Modeler.getInternationalText("InsertBarGraph");
		menuItem.setText((s != null ? s : "Bar Graph") + "...");
		menuItem.setIcon(IconPool.getIcon("bargraph"));
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("X-Y Graph");
		s = Modeler.getInternationalText("InsertXYGraph");
		menuItem.setText((s != null ? s : "X-Y Graph") + "...");
		menuItem.setIcon(IconPool.getIcon("linegraph"));
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Gauge");
		s = Modeler.getInternationalText("InsertGauge");
		menuItem.setText((s != null ? s : "Gauge") + "...");
		menuItem.setIcon(IconPool.getIcon("gauge"));
		menu.add(menuItem);

		s = Modeler.getInternationalText("InsertPieChart");
		menuItem = new JMenuItem((s != null ? s : "Pie Chart") + "...", IconPool.getIcon("piechart"));
		menuItem.setEnabled(false);
		menu.add(menuItem);

		s = Modeler.getInternationalText("InsertInstrument");
		menu = new JMenu(s != null ? s : "Instrument");
		add(menu);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Diffraction Device");
		s = Modeler.getInternationalText("InsertDiffractionDevice");
		menuItem.setText((s != null ? s : "Diffraction Device") + "...");
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Emission and Absorption Spectrometer");
		s = Modeler.getInternationalText("InsertSpectrometer");
		menuItem.setText((s != null ? s : "Emission and Absorption Spectrometer") + "...");
		menu.add(menuItem);

		s = Modeler.getInternationalText("InsertStandardController");
		menu = new JMenu(s != null ? s : "Standard Controller");
		add(menu);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Button");
		s = Modeler.getInternationalText("InsertButton");
		menuItem.setText((s != null ? s : "Button") + "...");
		menuItem.setIcon(IconPool.getIcon("button"));
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Check Box");
		s = Modeler.getInternationalText("InsertCheckBox");
		menuItem.setText((s != null ? s : "Check Box") + "...");
		menuItem.setIcon(IconPool.getIcon("checkbox"));
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Spinner");
		s = Modeler.getInternationalText("InsertSpinner");
		menuItem.setText((s != null ? s : "Spinner") + "...");
		menuItem.setIcon(IconPool.getIcon("spinner"));
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Slider");
		s = Modeler.getInternationalText("InsertSlider");
		menuItem.setText((s != null ? s : "Slider") + "...");
		menuItem.setIcon(IconPool.getIcon("slider"));
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Combo Box");
		s = Modeler.getInternationalText("InsertComboBox");
		menuItem.setText((s != null ? s : "Combo Box") + "...");
		menuItem.setIcon(IconPool.getIcon("combobox"));
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("A Group of Radio Buttons");
		s = Modeler.getInternationalText("InsertRadioButton");
		menuItem.setText((s != null ? s : "A Group of Radio Buttons") + "...");
		menuItem.setIcon(IconPool.getIcon("radiobutton"));
		menu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Script Console");
		s = Modeler.getInternationalText("InsertScriptConsole");
		menuItem.setText((s != null ? s : "Script Console") + "...");
		menuItem.setIcon(IconPool.getIcon("console"));
		menu.add(menuItem);

		s = Modeler.getInternationalText("InsertSpecialController");
		menu = new JMenu(s != null ? s : "Special Controller");
		add(menu);

		s = Modeler.getInternationalText("InsertChemicalReactionKinetics");
		JMenu subMenu2 = new JMenu(s != null ? s : "Chemical Reaction Kinetics");
		menu.add(subMenu2);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Bond-Breaking Barrier");
		s = Modeler.getInternationalText("InsertBondBreakingBarrier");
		menuItem.setText((s != null ? s : "Bond-Breaking Barrier") + "...");
		subMenu2.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Activation Barrier");
		s = Modeler.getInternationalText("InsertActivationBarrier");
		menuItem.setText((s != null ? s : "Activation Barrier") + "...");
		subMenu2.add(menuItem);

		s = Modeler.getInternationalText("InsertProteinAndDNA");
		subMenu2 = new JMenu(s != null ? s : "Proteins and DNA");
		menu.add(subMenu2);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("DNA Scroller");
		s = Modeler.getInternationalText("InsertDNAScroller");
		menuItem.setText((s != null ? s : "DNA Scroller") + "...");
		subMenu2.add(menuItem);

		s = Modeler.getInternationalText("InsertLightMatterInteraction");
		subMenu2 = new JMenu(s != null ? s : "Light-Matter Interactions");
		menu.add(subMenu2);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Electronic Structure");
		s = Modeler.getInternationalText("InsertElectronicStructure");
		menuItem.setText((s != null ? s : "Electronic Structure") + "...");
		subMenu2.add(menuItem);

		pack();

	}
}