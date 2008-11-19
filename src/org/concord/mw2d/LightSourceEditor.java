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
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.event.AbstractChange;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.mw2d.models.LightSource;
import org.concord.mw2d.models.MolecularModel;

class LightSourceEditor extends JPanel {

	private final static Icon LIGHT_ICON = new ImageIcon(LightSourceEditor.class.getResource("images/light.gif"));

	private MolecularModel model;

	private JCheckBox lightSwitch;
	private JRadioButton westButton, eastButton, northButton, southButton, otherButton;
	private JRadioButton monochromaticButton, whiteButton;
	private JSlider nbeamSlider, frequencySlider, intensitySlider;
	private JLabel label, degreeLabel;
	private IntegerTextField angleField;
	private JPanel directionPanel;

	LightSourceEditor() {

		super(new BorderLayout());

		final JPanel p2 = new JPanel(new GridLayout(2, 1));
		String s = MDView.getInternationalText("PhotonDirection");
		p2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s != null ? s
				: "Photon Direction"));
		add(p2, BorderLayout.NORTH);

		ButtonGroup bg = new ButtonGroup();

		JPanel p = new JPanel();
		p2.add(p);

		s = MDView.getInternationalText("East");
		westButton = new JRadioButton(s != null ? s : "East");
		westButton.setSelected(true);
		westButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					model.getLightSource().setDirection(LightSource.WEST);
					model.notifyChange();
				}
			}
		});
		bg.add(westButton);
		p.add(westButton);

		s = MDView.getInternationalText("West");
		eastButton = new JRadioButton(s != null ? s : "West");
		eastButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					model.getLightSource().setDirection(LightSource.EAST);
					model.notifyChange();
				}
			}
		});
		bg.add(eastButton);
		p.add(eastButton);

		s = MDView.getInternationalText("South");
		northButton = new JRadioButton(s != null ? s : "South");
		northButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					model.getLightSource().setDirection(LightSource.NORTH);
					model.notifyChange();
				}
			}
		});
		bg.add(northButton);
		p.add(northButton);

		s = MDView.getInternationalText("North");
		southButton = new JRadioButton(s != null ? s : "North");
		southButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					model.getLightSource().setDirection(LightSource.SOUTH);
					model.notifyChange();
				}
			}
		});
		bg.add(southButton);
		p.add(southButton);

		directionPanel = new JPanel();
		p2.add(directionPanel);

		s = MDView.getInternationalText("Other");
		otherButton = new JRadioButton((s != null ? s : "Other") + ":");
		otherButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					directionPanel.add(angleField);
					directionPanel.add(degreeLabel);
					model.getLightSource().setDirection(LightSource.OTHER);
				}
				else {
					directionPanel.remove(angleField);
					directionPanel.remove(degreeLabel);
				}
				p2.validate();
				p2.repaint();
				model.notifyChange();
			}
		});
		bg.add(otherButton);
		directionPanel.add(otherButton);

		angleField = new IntegerTextField(0, -179, 180, 5);
		angleField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				model.getLightSource().setAngleOfIncidence((float) Math.toRadians(angleField.getValue()));
				model.notifyChange();
			}
		});
		angleField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				model.getLightSource().setAngleOfIncidence((float) Math.toRadians(angleField.getValue()));
				model.notifyChange();
			}
		});
		degreeLabel = new JLabel("\u00B0");

		p = new JPanel();
		s = MDView.getInternationalText("Monochromaticity");
		p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s != null ? s
				: "Monochromaticity"));

		bg = new ButtonGroup();

		s = MDView.getInternationalText("Monochromatic");
		monochromaticButton = new JRadioButton(s != null ? s : "Monochromatic");
		monochromaticButton.setSelected(true);
		monochromaticButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					frequencySlider.setEnabled(true);
					model.getLightSource().setMonochromatic(true);
					model.notifyChange();
				}
			}
		});
		bg.add(monochromaticButton);
		p.add(monochromaticButton);

		s = MDView.getInternationalText("White");
		whiteButton = new JRadioButton(s != null ? s : "White");
		whiteButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					frequencySlider.setEnabled(false);
					model.getLightSource().setMonochromatic(false);
					model.notifyChange();
				}
			}
		});
		bg.add(whiteButton);
		p.add(whiteButton);

		add(p, BorderLayout.CENTER);

		p = new JPanel(new BorderLayout());

		JPanel p1 = new JPanel();
		s = MDView.getInternationalText("NumberOfBeams");
		p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s != null ? s
				: "Number of Beams"));
		nbeamSlider = new JSlider(1, 20, 10);
		Hashtable<Integer, JLabel> tableOfLabels = new Hashtable<Integer, JLabel>();
		tableOfLabels.put(1, new JLabel("1"));
		tableOfLabels.put(20, new JLabel("20"));
		nbeamSlider.setLabelTable(tableOfLabels);
		nbeamSlider.setPaintLabels(true);
		nbeamSlider.setSnapToTicks(true);
		nbeamSlider.setPaintTicks(true);
		nbeamSlider.setMajorTickSpacing(2);
		nbeamSlider.setMinorTickSpacing(1);
		nbeamSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (intensitySlider.getValueIsAdjusting())
					return;
				model.getLightSource().setNumberOfBeams(nbeamSlider.getValue());
				model.notifyChange();
			}
		});
		p1.add(nbeamSlider);

		p.add(p1, BorderLayout.NORTH);

		p1 = new JPanel();
		s = MDView.getInternationalText("Frequency");
		p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s != null ? s : "Frequency"));
		p.add(p1, BorderLayout.CENTER);

		frequencySlider = new JSlider();
		p1.add(frequencySlider);

		p1 = new JPanel();
		s = MDView.getInternationalText("Intensity");
		p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), s != null ? s : "Intensity"));
		p.add(p1, BorderLayout.SOUTH);

		intensitySlider = new JSlider(0, 20, 10);
		intensitySlider.setSnapToTicks(true);
		intensitySlider.setPaintTicks(true);
		intensitySlider.setMajorTickSpacing(2);
		intensitySlider.setMinorTickSpacing(1);
		intensitySlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (intensitySlider.getValueIsAdjusting())
					return;
				model.setLightSourceInterval((intensitySlider.getMaximum() + 1 - intensitySlider.getValue()) * 100);
				model.notifyChange();
			}
		});
		p1.add(intensitySlider);

		add(p, BorderLayout.SOUTH);

		s = MDView.getInternationalText("On");
		lightSwitch = new JCheckBox(s != null ? s : "On");
		lightSwitch.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				if (b)
					setupFrequencySlider(model);
				enableEditor(b);
				model.setLightSourceEnabled(b);
				model.notifyChange();
			}
		});

		label = new JLabel(LIGHT_ICON);

	}

	private void enableEditor(boolean b) {
		westButton.setEnabled(b);
		eastButton.setEnabled(b);
		northButton.setEnabled(b);
		southButton.setEnabled(b);
		monochromaticButton.setEnabled(b);
		whiteButton.setEnabled(b);
		nbeamSlider.setEnabled(b);
		frequencySlider.setEnabled(b);
		intensitySlider.setEnabled(b);
		label.setEnabled(b);
	}

	private void setupFrequencySlider(MolecularModel model) {
		removeAllListeners(frequencySlider);
		AbstractChange c = (AbstractChange) model.getChanges().get("Frequency of Light Source");
		frequencySlider.setToolTipText((String) c.getProperty(AbstractChange.SHORT_DESCRIPTION));
		double fmax = c.getMaximum();
		double fmin = c.getMinimum();
		double scaleFactor = 1.0 / c.getStepSize();
		frequencySlider.setMinimum(Math.round((float) (fmin * scaleFactor)));
		frequencySlider.setMaximum(Math.round((float) (fmax * scaleFactor)));
		frequencySlider.setValue(Math.round((float) (model.getLightSource().getFrequency() * scaleFactor)));
		frequencySlider.setPaintTicks(true);
		frequencySlider.setMajorTickSpacing(((frequencySlider.getMaximum() - frequencySlider.getMinimum()) / 10));
		// frequencySlider.setMinorTickSpacing(1);
		frequencySlider.addChangeListener(c);
	}

	private void setLightSource() {
		if (model.getLightSource().isOn()) {
			ModelerUtilities.selectWithoutNotifyingListeners(lightSwitch, true);
			boolean b1 = model.getLightSource().isMonochromatic();
			ModelerUtilities.selectWithoutNotifyingListeners(monochromaticButton, b1);
			ModelerUtilities.selectWithoutNotifyingListeners(whiteButton, !b1);
			int nbeam = model.getLightSource().getNumberOfBeams();
			boolean b2 = model.getLightSource().isSingleBeam(); // backward compatible
			if (b2)
				nbeam = 1;
			nbeamSlider.setValue(nbeam);
			enableEditor(true);
			frequencySlider.setEnabled(b1);
			setupFrequencySlider(model);
			intensitySlider.setValue(intensitySlider.getMaximum() + 1 - model.getLightSource().getRadiationPeriod()
					/ 100);
			switch (model.getLightSource().getDirection()) {
			case LightSource.WEST:
				ModelerUtilities.selectWithoutNotifyingListeners(westButton, true);
				break;
			case LightSource.EAST:
				ModelerUtilities.selectWithoutNotifyingListeners(eastButton, true);
				break;
			case LightSource.NORTH:
				ModelerUtilities.selectWithoutNotifyingListeners(northButton, true);
				break;
			case LightSource.SOUTH:
				ModelerUtilities.selectWithoutNotifyingListeners(southButton, true);
				break;
			case LightSource.OTHER:
				ModelerUtilities.selectWithoutNotifyingListeners(otherButton, true);
				angleField.setValue((int) Math.round(Math.toDegrees(model.getLightSource().getAngleOfIncidence())));
				directionPanel.add(angleField);
				directionPanel.add(degreeLabel);
				directionPanel.validate();
				directionPanel.repaint();
				break;
			}
		}
		else {
			ModelerUtilities.selectWithoutNotifyingListeners(lightSwitch, false);
			enableEditor(false);
		}
	}

	public JDialog createDialog(Component parent, MolecularModel model) {

		this.model = model;

		if (model == null)
			return null;

		setLightSource();

		String s = MDView.getInternationalText("EditLightSource");
		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(parent), s != null ? s : "Edit Light Source",
				true);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		d.getContentPane().add(this, BorderLayout.CENTER);

		final JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		d.getContentPane().add(p, BorderLayout.SOUTH);

		p.add(label);
		p.add(lightSwitch);

		s = MDView.getInternationalText("CloseButton");
		JButton b = new JButton(s != null ? s : "Close");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.dispose();
			}
		});
		p.add(b);

		d.pack();
		d.setLocationRelativeTo(model.getView());

		return d;

	}

	static void removeAllListeners(JSlider slider) {
		ChangeListener[] cl = slider.getChangeListeners();
		if (cl == null)
			return;
		for (int i = 0; i < cl.length; i++)
			slider.removeChangeListener(cl[i]);
	}

}