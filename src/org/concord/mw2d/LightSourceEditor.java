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

import javax.swing.AbstractButton;
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
	private JRadioButton singleBeamButton, multiBeamButton;
	private JSlider frequencySlider, intensitySlider;
	private JLabel label, degreeLabel;
	private IntegerTextField angleField;
	private JPanel directionPanel;

	LightSourceEditor() {

		super(new BorderLayout());

		final JPanel p2 = new JPanel(new GridLayout(2, 1));
		p2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Photon Direction"));
		add(p2, BorderLayout.NORTH);

		ButtonGroup bg = new ButtonGroup();

		JPanel p = new JPanel();
		p2.add(p);

		westButton = new JRadioButton("East");
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

		eastButton = new JRadioButton("West");
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

		northButton = new JRadioButton("South");
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

		southButton = new JRadioButton("North");
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

		otherButton = new JRadioButton("Other:");
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
		p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Monochromaticity"));

		bg = new ButtonGroup();

		monochromaticButton = new JRadioButton("Monochromatic");
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

		whiteButton = new JRadioButton("White");
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
		p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Beams"));
		bg = new ButtonGroup();

		singleBeamButton = new JRadioButton("Single");
		singleBeamButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					model.getLightSource().setSingleBeam(singleBeamButton.isSelected());
					model.notifyChange();
				}
			}
		});
		bg.add(singleBeamButton);
		p1.add(singleBeamButton);

		multiBeamButton = new JRadioButton("Multiple");
		multiBeamButton.setSelected(true);
		multiBeamButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					model.getLightSource().setSingleBeam(singleBeamButton.isSelected());
					model.notifyChange();
				}
			}
		});
		bg.add(multiBeamButton);
		p1.add(multiBeamButton);

		p.add(p1, BorderLayout.NORTH);

		p1 = new JPanel();
		p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Frequency"));
		p.add(p1, BorderLayout.CENTER);

		frequencySlider = new JSlider();
		p1.add(frequencySlider);

		p1 = new JPanel();
		p1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Intensity"));
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

		lightSwitch = new JCheckBox("On");
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
		singleBeamButton.setEnabled(b);
		multiBeamButton.setEnabled(b);
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
			selectWithoutNotifyingListeners(lightSwitch, true);
			boolean b1 = model.getLightSource().isMonochromatic();
			selectWithoutNotifyingListeners(monochromaticButton, b1);
			selectWithoutNotifyingListeners(whiteButton, !b1);
			boolean b2 = model.getLightSource().isSingleBeam();
			selectWithoutNotifyingListeners(singleBeamButton, b2);
			selectWithoutNotifyingListeners(multiBeamButton, !b2);
			enableEditor(true);
			frequencySlider.setEnabled(b1);
			setupFrequencySlider(model);
			intensitySlider.setValue(intensitySlider.getMaximum() + 1 - model.getLightSource().getRadiationPeriod()
					/ 100);
			switch (model.getLightSource().getDirection()) {
			case LightSource.WEST:
				selectWithoutNotifyingListeners(westButton, true);
				break;
			case LightSource.EAST:
				selectWithoutNotifyingListeners(eastButton, true);
				break;
			case LightSource.NORTH:
				selectWithoutNotifyingListeners(northButton, true);
				break;
			case LightSource.SOUTH:
				selectWithoutNotifyingListeners(southButton, true);
				break;
			case LightSource.OTHER:
				selectWithoutNotifyingListeners(otherButton, true);
				angleField.setValue((int) Math.round(Math.toDegrees(model.getLightSource().getAngleOfIncidence())));
				directionPanel.add(angleField);
				directionPanel.add(degreeLabel);
				directionPanel.validate();
				directionPanel.repaint();
				break;
			}
		}
		else {
			selectWithoutNotifyingListeners(lightSwitch, false);
			enableEditor(false);
		}
	}

	public JDialog createDialog(Component parent, MolecularModel model) {

		this.model = model;

		if (model == null)
			return null;

		setLightSource();

		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(parent), "Edit Light Source", true);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		d.getContentPane().add(this, BorderLayout.CENTER);

		final JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		d.getContentPane().add(p, BorderLayout.SOUTH);

		p.add(label);
		p.add(lightSwitch);

		JButton b = new JButton("Close");
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

	static void selectWithoutNotifyingListeners(AbstractButton ab, boolean selected) {

		if (ab == null)
			return;

		ItemListener[] il = ab.getItemListeners();
		if (il != null) {
			for (int i = 0; i < il.length; i++)
				ab.removeItemListener(il[i]);
		}
		ActionListener[] al = ab.getActionListeners();
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				ab.removeActionListener(al[i]);
		}

		ab.setSelected(selected);

		if (il != null) {
			for (int i = 0; i < il.length; i++)
				ab.addItemListener(il[i]);
		}
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				ab.addActionListener(al[i]);
		}

	}

}