/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.SpringLayout;
import javax.vecmath.Vector3f;

import org.concord.modeler.ModelerUtilities;
import org.concord.mw3d.models.GField;
import org.concord.mw3d.models.MolecularModel;

/**
 * @author Charles Xie
 * 
 */
class GFieldEditor extends JDialog {

	private final static float SLIDER_MAGNIFIER = 100000;
	private JSlider gSlider;
	private float originalValue;
	private Vector3f originalVector;
	private MolecularModel model;
	private JRadioButton defaultButton, customButton;
	private JSlider xSlider, ySlider, zSlider;
	private JPanel oneSliderPanel, triSliderPanel;

	GFieldEditor(JDialog owner, MolecularModel model) {

		super(owner, "Gravitational Field Properties", true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		String s = MolecularContainer.getInternationalText("GravitationalFieldProperties");
		if (s != null)
			setTitle(s);

		this.model = model;
		GField gField = model.getGField();
		boolean alwaysDown = true;
		if (gField != null) {
			alwaysDown = gField.isAlwaysDown();
			if (alwaysDown) {
				originalValue = gField.getIntensity();
			}
			else {
				originalVector = new Vector3f(gField.getDirection());
				originalVector.scale(gField.getIntensity());
			}
		}

		JPanel topPanel = new JPanel();
		getContentPane().add(topPanel, BorderLayout.NORTH);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(panel, BorderLayout.CENTER);

		defaultButton = new JRadioButton("Default");
		topPanel.add(defaultButton);

		customButton = new JRadioButton("Custom");
		topPanel.add(customButton);

		ButtonGroup bg = new ButtonGroup();
		bg.add(defaultButton);
		bg.add(customButton);
		if (alwaysDown)
			defaultButton.setSelected(true);
		else customButton.setSelected(true);

		defaultButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					panel.removeAll();
					createGSlider();
					panel.add(oneSliderPanel, BorderLayout.CENTER);
					validate();
					panel.repaint();
				}
			}
		});
		customButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					panel.removeAll();
					createThreeSliders();
					panel.add(triSliderPanel, BorderLayout.CENTER);
					validate();
					panel.repaint();
				}
			}
		});

		if (alwaysDown) {
			createGSlider();
			panel.add(oneSliderPanel, BorderLayout.CENTER);
		}
		else {
			createThreeSliders();
			panel.add(triSliderPanel, BorderLayout.CENTER);
		}

		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		s = MolecularContainer.getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok();
				dispose();
			}
		});
		buttonPanel.add(button);

		s = MolecularContainer.getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel();
				dispose();
			}
		});
		buttonPanel.add(button);

		pack();

	}

	private JSlider createSlider(float value, String title) {
		int x = (int) (value * SLIDER_MAGNIFIER);
		if (x > 100)
			x = 100;
		else if (x < -100)
			x = -100;
		JSlider s = new JSlider(-100, 100, x);
		s.setMajorTickSpacing(10);
		s.setMinorTickSpacing(5);
		s.setPaintLabels(true);
		Hashtable ht = new Hashtable();
		ht.put(0, new JLabel("0"));
		ht.put(-100, new JLabel("-0.001"));
		ht.put(100, new JLabel("0.001"));
		s.setLabelTable(ht);
		s.setBorder(BorderFactory.createTitledBorder(title));
		return s;
	}

	private JPanel createSliderButtonPanel(final JSlider slider) {
		JPanel p = new JPanel();
		p.add(slider);
		String s = MolecularContainer.getInternationalText("SetToZero");
		JButton button = new JButton(s != null ? s : "Set to Zero");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				slider.setValue(0);
			}
		});
		p.add(button);
		return p;
	}

	private void createThreeSliders() {
		if (triSliderPanel != null)
			return;
		triSliderPanel = new JPanel(new SpringLayout());
		String s = MolecularContainer.getInternationalText("XComponent");
		xSlider = createSlider(originalVector == null ? 0 : originalVector.x, s != null ? s : "x-component");
		triSliderPanel.add(createSliderButtonPanel(xSlider));
		s = MolecularContainer.getInternationalText("YComponent");
		ySlider = createSlider(originalVector == null ? 0 : originalVector.y, s != null ? s : "y-component");
		triSliderPanel.add(createSliderButtonPanel(ySlider));
		s = MolecularContainer.getInternationalText("ZComponent");
		zSlider = createSlider(originalVector == null ? 0 : originalVector.z, s != null ? s : "z-component");
		triSliderPanel.add(createSliderButtonPanel(zSlider));
		ModelerUtilities.makeCompactGrid(triSliderPanel, 3, 1, 5, 5, 10, 2);
	}

	private void createGSlider() {
		if (gSlider != null)
			return;
		int g = (int) (originalValue * SLIDER_MAGNIFIER);
		gSlider = new JSlider(0, 100, g > 100 ? 100 : g);
		gSlider.setMajorTickSpacing(10);
		gSlider.setMinorTickSpacing(5);
		gSlider.setPaintLabels(true);
		Hashtable ht = new Hashtable();
		ht.put(0, new JLabel("0"));
		ht.put(100, new JLabel("0.001"));
		gSlider.setLabelTable(ht);
		String s = MolecularContainer.getInternationalText("GravitationalAcceleration");
		gSlider.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Gravitational Acceleration"));
		oneSliderPanel = new JPanel(new BorderLayout(5, 5));
		oneSliderPanel.setPreferredSize(new Dimension(400, 300));
		oneSliderPanel.add(gSlider, BorderLayout.NORTH);
		JLabel label = new JLabel("Always pointing downward on the screen.");
		label.setBorder(BorderFactory.createTitledBorder("Direction"));
		oneSliderPanel.add(label, BorderLayout.SOUTH);
	}

	private void ok() {
		if (defaultButton.isSelected()) {
			model.setGField(gSlider.getValue() / SLIDER_MAGNIFIER, null);
		}
		else {
			int x = xSlider.getValue();
			int y = ySlider.getValue();
			int z = zSlider.getValue();
			setValues(x / SLIDER_MAGNIFIER, y / SLIDER_MAGNIFIER, z / SLIDER_MAGNIFIER);
		}
	}

	private void cancel() {
		if (defaultButton.isSelected()) {
			model.setGField(originalValue, null);
		}
		else {
			if (originalVector != null)
				setValues(originalVector.x, originalVector.y, originalVector.z);
		}
	}

	private void setValues(float x, float y, float z) {
		float a = (float) Math.sqrt(x * x + y * y + z * z);
		model.setGField(a, a == 0 ? null : new Vector3f(x / a, y / a, z / a));
	}

}
