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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.PieChart;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.QuantumRule;

class QuantumDynamicsRuleEditor extends JPanel {

	private MolecularModel model;
	private PieChart pieChart1;
	private PieChart pieChart2;
	private JComboBox whatIsComboBox;
	private JCheckBox ionizationCheckBox;
	private JSlider scatterProbSlider;

	QuantumDynamicsRuleEditor() {

		super(new BorderLayout());
		setPreferredSize(new Dimension(400, 360));

		String[] str = { "Select an item", "Spontaneous Emission", "Stimulated Emission", "Radiationless Transition" };

		whatIsComboBox = new JComboBox(str);
		whatIsComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						switch (whatIsComboBox.getSelectedIndex()) {
						case 1:
							showMessageWithPopupMenu(
									whatIsComboBox,
									"<html><p><b>Spontaneous emission</b></p><p>is a process of an excited electron dropping to a lower-lying state and emitting a photon<br>in the absence of a perturbing external electromagnetic field.<br><br>The spontaneous transition between two states is determined by Einstein's coefficient<br>of spontaneous emission, <i><b>A<sub>21</sub></b></i>.</p></html>");
							break;
						case 2:
							showMessageWithPopupMenu(
									whatIsComboBox,
									"<html><p><b>Stimulated emission</b></p><p>is a process of an excited electron dropping to a lower-lying state and emitting a photon<br>in the presence of an incident photon that has energy identical to the difference between<br>the two energy levels involved in the electronic transition.<br><br>The stimulated transition between two states is determined by Einstein's coefficient<br>of stimulated emission, <i><b>B<sub>12</sub></b></i>.</p></html>");
							break;
						case 3:
							showMessageWithPopupMenu(
									whatIsComboBox,
									"<html><p><b>Radiationless transition</b></p><p>is a transition between two quantum states without emitting or obsorbing photons.<br>If an electron is de-excited through a process of radiationless transition, the<br>excess energy will be converted into the form of kinetic energy, resulting in<br>the increasing of temperature of the system.</p></html>");
							break;
						}
					}
				});
			}
		});

		JTabbedPane tabbedPane = new JTabbedPane();
		add(tabbedPane, BorderLayout.CENTER);

		// create panel for photon-atom interaction

		JPanel panel = new JPanel(new BorderLayout());
		String s = MDView.getInternationalText("PhotonAtomInteraction");
		tabbedPane.addTab(s != null ? s : "Photon-Atom Interaction", panel);

		pieChart2 = new PieChart(2);
		s = MDView.getInternationalText("Absorption");
		pieChart2.setText(0, "1 - " + (s != null ? s : "Absorption") + ".");
		s = MDView.getInternationalText("StimulatedEmission");
		pieChart2.setText(1, "2 - " + (s != null ? s : "Stimulated emission") + ".");
		pieChart2.setColor(0, Color.green);
		pieChart2.setColor(1, Color.magenta);
		panel.add(pieChart2, BorderLayout.CENTER);

		JPanel optionPanel = new JPanel();
		s = MDView.getInternationalText("WhenAPhotonIsNotAbsorbed");
		optionPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10),
				BorderFactory.createTitledBorder(s != null ? s : "When a photon isn't absorbed")));
		panel.add(optionPanel, BorderLayout.SOUTH);
		scatterProbSlider = new JSlider(0, 100, 0);
		Hashtable<Integer, JLabel> tableOfLabels = new Hashtable<Integer, JLabel>();
		s = MDView.getInternationalText("CompletelyPassThrough");
		tableOfLabels.put(1, new JLabel(s != null ? s : "Completely pass through"));
		s = MDView.getInternationalText("CompletelyScatter");
		tableOfLabels.put(100, new JLabel(s != null ? s : "Completely scatter"));
		scatterProbSlider.setLabelTable(tableOfLabels);
		scatterProbSlider.setPaintLabels(true);
		scatterProbSlider.setPreferredSize(new Dimension(320, 80));
		scatterProbSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (scatterProbSlider.getValueIsAdjusting())
					return;
				model.getQuantumRule().setScatterProbability(
						(float) scatterProbSlider.getValue() / (float) scatterProbSlider.getMaximum());
				model.notifyChange();
			}
		});
		optionPanel.add(scatterProbSlider);

		// create panel for de-excitation

		panel = new JPanel(new BorderLayout());
		s = MDView.getInternationalText("Deexcitation");
		tabbedPane.addTab(s != null ? s : "De-excitation", panel);

		pieChart1 = new PieChart(2);
		s = MDView.getInternationalText("SpontaneousEmission");
		pieChart1.setText(0, "1 - " + (s != null ? s : "Spontaneous emission") + ".");
		s = MDView.getInternationalText("RadiationlessTransition");
		pieChart1.setText(1, "2 - " + (s != null ? s : "Radiationless transition") + " (only through collision).");
		pieChart1.setColor(0, Color.red);
		pieChart1.setColor(1, Color.blue);
		panel.add(pieChart1, BorderLayout.CENTER);

		panel = new JPanel(new BorderLayout());
		s = MDView.getInternationalText("Ionization");
		tabbedPane.addTab(s != null ? s : "Ionization", panel);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		s = MDView.getInternationalText("AllowIonization");
		ionizationCheckBox = new JCheckBox(s != null ? s : "Allow ionization");
		ionizationCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				model.getQuantumRule().setIonizationDisallowed(!cb.isSelected());
			}
		});
		p.add(ionizationCheckBox);
		panel.add(p, BorderLayout.NORTH);

		panel = new JPanel(new BorderLayout());
		s = MDView.getInternationalText("ElectronTransfer");
		tabbedPane.addTab(s != null ? s : "Electron Transfer", panel);

	}

	void showMessageWithPopupMenu(Component parent, String message) {
		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.setBackground(SystemColor.info);
		popupMenu.setBorder(BorderFactory.createLineBorder(Color.black));
		JLabel descriptionLabel = new JLabel(message);
		descriptionLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		popupMenu.add(descriptionLabel);
		popupMenu.pack();
		popupMenu.show(parent, 10, 10);
	}

	public JDialog createDialog(Component parent, MolecularModel m) {

		model = m;

		if (model == null)
			return null;

		final QuantumRule rule = model.getQuantumRule();

		float x = rule.getProbability(QuantumRule.RADIATIONLESS_TRANSITION);
		pieChart1.setPercent(0, 1.0f - x);
		pieChart1.setPercent(1, x);

		x = rule.getProbability(QuantumRule.STIMULATED_EMISSION);
		pieChart2.setPercent(0, 1.0f - x);
		pieChart2.setPercent(1, x);

		scatterProbSlider.setValue((int) (rule.getScatterProbability() * 100));
		ModelerUtilities.selectWithoutNotifyingListeners(ionizationCheckBox, !model.getQuantumRule()
				.isIonizationDisallowed());

		String s = MDView.getInternationalText("QuantumDynamicsRules");
		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(parent), s != null ? s
				: "Quantum Dynamics Rules", true);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		d.getContentPane().add(this, BorderLayout.CENTER);

		final JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		d.getContentPane().add(p, BorderLayout.SOUTH);

		p.add(new JLabel("Explain:"));
		p.add(whatIsComboBox);

		s = MDView.getInternationalText("OKButton");
		JButton okButton = new JButton(s != null ? s : "OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rule.setProbability(QuantumRule.RADIATIONLESS_TRANSITION, pieChart1.getPercent(1));
				rule.setProbability(QuantumRule.STIMULATED_EMISSION, pieChart2.getPercent(1));
				model.notifyChange();
				d.dispose();
			}
		});

		s = MDView.getInternationalText("CancelButton");
		JButton cancelButton = new JButton(s != null ? s : "Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.dispose();
			}
		});

		if (Modeler.isMac()) {
			p.add(cancelButton);
			p.add(okButton);
		}
		else {
			p.add(okButton);
			p.add(cancelButton);
		}

		d.pack();

		Point origin = model.getView().getLocationOnScreen();
		Dimension dim0 = model.getView().getPreferredSize();
		Dimension dim1 = d.getPreferredSize();
		d.setLocation(origin.x + (dim0.width - dim1.width) / 2, origin.y + (dim0.height - dim1.height) / 2);

		return d;

	}

}