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

package org.concord.modeler.text;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.concord.modeler.Initializer;
import org.concord.modeler.Modeler;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.ui.TextComponentPopupMenu;

class PagePopupMenu extends TextComponentPopupMenu {

	private Page page;

	private JMenu insertModelMenu;
	private JMenu insertInstrumentMenu;
	private JMenu insertOutputMenu;
	private JMenu insertControllerMenu;
	private JMenu insertSpecialControllerMenu;
	private JMenu insertComponentMenu;

	PagePopupMenu(Page p) {

		super(p);
		page = p;
		setLabel("Default");

		addSeparator();

		JMenuItem menuItem = new JMenuItem(page.getAction("Font"));
		String s = Modeler.getInternationalText("Font");
		menuItem.setText((s != null ? s : menuItem.getText()) + "...");
		add(menuItem);

		menuItem = new JMenuItem(page.getAction("Paragraph"));
		s = Modeler.getInternationalText("Paragraph");
		menuItem.setText((s != null ? s : menuItem.getText()) + "...");
		add(menuItem);

		menuItem = new JMenuItem(page.getAction("Bullet"));
		s = Modeler.getInternationalText("Bullet");
		menuItem.setText((s != null ? s : menuItem.getText()) + "...");
		add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.SET_PROPERTIES));
		s = Modeler.getInternationalText("Properties");
		menuItem.setText((s != null ? s : menuItem.getText()) + "...");
		add(menuItem);
		addSeparator();

		if (Modeler.isLaunchedByJws()) {
			s = Modeler.getInternationalText("SystemSettings");
			JMenu menu = new JMenu(s != null ? s : "System Settings");
			menu.setIcon(new ImageIcon(getClass().getResource("images/Desktop.gif")));
			add(menu);
			addSeparator();
			s = Modeler.getInternationalText("ResetDesktopLauncher");
			menuItem = new JMenuItem(s != null ? s : "Reset Desktop Launcher");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					page.getNavigator().visitLocation(Initializer.sharedInstance().getResetJnlpAddress());
				}
			});
			menu.add(menuItem);
		}

		menuItem = new JMenuItem(page.getAction("Hyperlink"));
		s = Modeler.getInternationalText("InsertHyperlink");
		menuItem.setText((s != null ? s : menuItem.getText()) + "...");
		add(menuItem);

		s = Modeler.getInternationalText("InsertModel");
		insertModelMenu = new JMenu(s != null ? s : "Insert Model Container");
		insertModelMenu.setIcon(new ImageIcon(getClass().getResource("images/Bean.gif")));
		add(insertModelMenu);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_ATOM_CONTAINER));
		s = Modeler.getInternationalText("InsertBasic2DContainer");
		if (s != null)
			menuItem.setText(s);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertModelMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_CHEM_CONTAINER));
		s = Modeler.getInternationalText("InsertReaction2DContainer");
		if (s != null)
			menuItem.setText(s);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertModelMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_PROSYN_CONTAINER));
		s = Modeler.getInternationalText("InsertProteinSynthesisContainer");
		if (s != null)
			menuItem.setText(s);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertModelMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_GB_CONTAINER));
		s = Modeler.getInternationalText("InsertMesoscaleContainer");
		if (s != null)
			menuItem.setText(s);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertModelMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName(Page.INSERT_JMOL);
		s = Modeler.getInternationalText("InsertJmolContainer");
		menuItem.setText((s != null ? s : Page.INSERT_JMOL) + "...");
		menuItem.setIcon(new ImageIcon(Page.class.getResource("images/MV.gif")));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertModelMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName(Page.INSERT_MW3D);
		s = Modeler.getInternationalText("InsertBasic3DContainer");
		menuItem.setText((s != null ? s : Page.INSERT_MW3D) + "...");
		menuItem.setIcon(new ImageIcon(Page.class.getResource("images/MD3D.gif")));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertModelMenu.add(menuItem);

		s = Modeler.getInternationalText("InsertModelOutput");
		insertOutputMenu = new JMenu(s != null ? s : "Insert Model Output");
		insertOutputMenu.setIcon(new ImageIcon(getClass().getResource("images/BeanGraph.gif")));
		add(insertOutputMenu);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Numeric Box");
		s = Modeler.getInternationalText("InsertNumericBox");
		menuItem.setText((s != null ? s : "Numeric Box") + "...");
		menuItem.setIcon(IconPool.getIcon("numeric"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertOutputMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Bar Graph");
		s = Modeler.getInternationalText("InsertBarGraph");
		menuItem.setText((s != null ? s : "Bar Graph") + "...");
		menuItem.setIcon(IconPool.getIcon("bargraph"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertOutputMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("X-Y Graph");
		s = Modeler.getInternationalText("InsertXYGraph");
		menuItem.setText((s != null ? s : "X-Y Graph") + "...");
		menuItem.setIcon(IconPool.getIcon("linegraph"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertOutputMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Gauge");
		s = Modeler.getInternationalText("InsertGauge");
		menuItem.setText((s != null ? s : "Gauge") + "...");
		menuItem.setIcon(IconPool.getIcon("gauge"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertOutputMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Pie Chart");
		s = Modeler.getInternationalText("InsertPieChart");
		menuItem.setText((s != null ? s : "Pie Chart") + "...");
		menuItem.setIcon(IconPool.getIcon("piechart"));
		menuItem.setEnabled(false);
		insertOutputMenu.add(menuItem);

		s = Modeler.getInternationalText("InsertInstrument");
		insertInstrumentMenu = new JMenu(s != null ? s : "Instrument");
		insertInstrumentMenu.setIcon(new ImageIcon(getClass().getResource("images/Instrument.gif")));
		add(insertInstrumentMenu);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Diffraction Device");
		s = Modeler.getInternationalText("InsertDiffractionDevice");
		menuItem.setText((s != null ? s : "Diffraction Device") + "...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertInstrumentMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Emission and Absorption Spectrometer");
		s = Modeler.getInternationalText("InsertSpectrometer");
		menuItem.setText((s != null ? s : "Emission and Absorption Spectrometer") + "...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertInstrumentMenu.add(menuItem);

		s = Modeler.getInternationalText("InsertStandardController");
		insertControllerMenu = new JMenu(s != null ? s : "Insert Standard Controller");
		insertControllerMenu.setIcon(new ImageIcon(getClass().getResource("images/Controller.gif")));
		add(insertControllerMenu);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Button");
		s = Modeler.getInternationalText("InsertButton");
		menuItem.setText((s != null ? s : "Button") + "...");
		menuItem.setIcon(IconPool.getIcon("button"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertControllerMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Check Box");
		s = Modeler.getInternationalText("InsertCheckBox");
		menuItem.setText((s != null ? s : "Check Box") + "...");
		menuItem.setIcon(IconPool.getIcon("checkbox"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertControllerMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Spinner");
		s = Modeler.getInternationalText("InsertSpinner");
		menuItem.setText((s != null ? s : "Spinner") + "...");
		menuItem.setIcon(IconPool.getIcon("spinner"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertControllerMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Slider");
		s = Modeler.getInternationalText("InsertSlider");
		menuItem.setText((s != null ? s : "Slider") + "...");
		menuItem.setIcon(IconPool.getIcon("slider"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertControllerMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Combo Box");
		s = Modeler.getInternationalText("InsertComboBox");
		menuItem.setText((s != null ? s : "Combo Box") + "...");
		menuItem.setIcon(IconPool.getIcon("combobox"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertControllerMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("A Group of Radio Buttons");
		s = Modeler.getInternationalText("InsertRadioButton");
		menuItem.setText((s != null ? s : "A Group of Radio Buttons") + "...");
		menuItem.setIcon(IconPool.getIcon("radiobutton"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertControllerMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Script Console");
		s = Modeler.getInternationalText("InsertScriptConsole");
		menuItem.setText((s != null ? s : "Script Console") + "...");
		menuItem.setIcon(IconPool.getIcon("console"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		insertControllerMenu.add(menuItem);

		s = Modeler.getInternationalText("InsertSpecialController");
		insertSpecialControllerMenu = new JMenu(s != null ? s : "Insert Special Controller");
		insertSpecialControllerMenu.setIcon(new ImageIcon(getClass().getResource("images/Controller.gif")));
		add(insertSpecialControllerMenu);

		s = Modeler.getInternationalText("InsertChemicalReactionKinetics");
		JMenu subMenu = new JMenu(s != null ? s : "Chemical Reaction Kinetics");
		insertSpecialControllerMenu.add(subMenu);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Bond-Breaking Barrier");
		s = Modeler.getInternationalText("InsertBondBreakingBarrier");
		menuItem.setText((s != null ? s : "Bond-Breaking Barrier") + "...");
		subMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Activation Barrier");
		s = Modeler.getInternationalText("InsertActivationBarrier");
		menuItem.setText((s != null ? s : "Activation Barrier") + "...");
		subMenu.add(menuItem);

		s = Modeler.getInternationalText("InsertProteinAndDNA");
		subMenu = new JMenu(s != null ? s : "Proteins and DNA");
		insertSpecialControllerMenu.add(subMenu);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("DNA Scroller");
		s = Modeler.getInternationalText("InsertDNAScroller");
		menuItem.setText((s != null ? s : "DNA Scroller") + "...");
		subMenu.add(menuItem);

		s = Modeler.getInternationalText("InsertLightMatterInteraction");
		subMenu = new JMenu(s != null ? s : "Light-Matter Interactions");
		insertSpecialControllerMenu.add(subMenu);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Electronic Structure");
		s = Modeler.getInternationalText("InsertElectronicStructure");
		menuItem.setText((s != null ? s : "Electronic Structure") + "...");
		subMenu.add(menuItem);

		s = Modeler.getInternationalText("InsertMiscComponent");
		insertComponentMenu = new JMenu(s != null ? s : "Insert Other Component");
		insertComponentMenu.setIcon(new ImageIcon(getClass().getResource("images/PageComponent.gif")));
		add(insertComponentMenu);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Activity Button");
		s = Modeler.getInternationalText("InsertActivityButton");
		menuItem.setText((s != null ? s : "Activity Button") + "...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		menuItem.setIcon(new ImageIcon(getClass().getResource("images/ActivityButton.gif")));
		insertComponentMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Text Box");
		s = Modeler.getInternationalText("InsertTextBox");
		menuItem.setText((s != null ? s : "Text Box") + "...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		menuItem.setIcon(new ImageIcon(getClass().getResource("images/TextBox.gif")));
		insertComponentMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction("Color Bar"));
		s = Modeler.getInternationalText("ColorBar");
		menuItem.setText((s != null ? s : "Color Bar") + "...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		menuItem.setIcon(new ImageIcon(getClass().getResource("images/ColorBar.gif")));
		insertComponentMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Multiple Choice");
		s = Modeler.getInternationalText("InsertMultipleChoice");
		menuItem.setText((s != null ? s : "Multiple Choice") + "...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		menuItem.setIcon(new ImageIcon(getClass().getResource("images/MultipleChoice.gif")));
		insertComponentMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Image Question");
		s = Modeler.getInternationalText("InsertImageQuestion");
		menuItem.setText((s != null ? s : "Image Question") + "...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		menuItem.setIcon(new ImageIcon(getClass().getResource("images/ImageQuestion.gif")));
		insertComponentMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("User Input Text Field");
		s = Modeler.getInternationalText("InsertTextField");
		menuItem.setText((s != null ? s : "User Input Text Field") + "...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		menuItem.setIcon(new ImageIcon(getClass().getResource("images/TextField.gif")));
		insertComponentMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("User Input Text Area");
		s = Modeler.getInternationalText("InsertTextArea");
		menuItem.setText((s != null ? s : "User Input Text Area") + "...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		menuItem.setIcon(new ImageIcon(getClass().getResource("images/TextArea.gif")));
		insertComponentMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Table");
		s = Modeler.getInternationalText("InsertTable");
		menuItem.setText((s != null ? s : "Table") + "...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		menuItem.setIcon(new ImageIcon(getClass().getResource("images/Table.gif")));
		insertComponentMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Applet");
		s = Modeler.getInternationalText("Applet");
		menuItem.setText((s != null ? s + "(Applet)" : "Applet") + "...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		menuItem.setIcon(new ImageIcon(getClass().getResource("images/Applet.gif")));
		insertComponentMenu.add(menuItem);

		menuItem = new JMenuItem(page.getAction(Page.INSERT_COMPONENT));
		menuItem.setName("Plugin");
		s = Modeler.getInternationalText("Plugin");
		menuItem.setText((s != null ? s : "Plugin") + "...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK, true));
		menuItem.setIcon(new ImageIcon(getClass().getResource("images/plugin.gif")));
		insertComponentMenu.add(menuItem);

	}

	public void show(Component invoker, int x, int y) {
		final boolean b = page.isEditable();
		page.getAction("Font").setEnabled(b);
		page.getAction("Paragraph").setEnabled(b);
		page.getAction("Bullet").setEnabled(b);
		page.getAction("Hyperlink").setEnabled(b);
		insertModelMenu.setEnabled(b);
		insertInstrumentMenu.setEnabled(b);
		insertOutputMenu.setEnabled(b);
		insertControllerMenu.setEnabled(b);
		insertSpecialControllerMenu.setEnabled(b);
		insertComponentMenu.setEnabled(b);
		super.show(invoker, x, y);
	}

}