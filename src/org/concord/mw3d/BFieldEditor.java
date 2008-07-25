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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SpringLayout;
import javax.vecmath.Vector3f;

import org.concord.modeler.ModelerUtilities;
import org.concord.mw3d.models.BField;
import org.concord.mw3d.models.MolecularModel;

/**
 * @author Charles Xie
 * 
 */
class BFieldEditor extends JDialog {

	private final static float SLIDER_MAGNIFIER = 10;
	private JSlider xSlider, ySlider, zSlider;
	private Vector3f original;
	private MolecularModel model;

	BFieldEditor(JDialog owner, MolecularModel model) {

		super(owner, "Magnetic Field Properties", false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		String s = MolecularContainer.getInternationalText("MagneticFieldProperties");
		if (s != null)
			setTitle(s);

		this.model = model;
		BField bField = model.getBField();
		if (bField != null) {
			original = new Vector3f(bField.getDirection());
			original.scale(bField.getIntensity());
		}

		JPanel panel = new JPanel(new SpringLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(panel, BorderLayout.CENTER);

		s = MolecularContainer.getInternationalText("XComponent");
		xSlider = createSlider(original == null ? 0 : original.x, s != null ? s : "x-component");
		panel.add(createSliderButtonPanel(xSlider));

		s = MolecularContainer.getInternationalText("YComponent");
		ySlider = createSlider(original == null ? 0 : original.y, s != null ? s : "y-component");
		panel.add(createSliderButtonPanel(ySlider));

		s = MolecularContainer.getInternationalText("ZComponent");
		zSlider = createSlider(original == null ? 0 : original.z, s != null ? s : "z-component");
		panel.add(createSliderButtonPanel(zSlider));

		ModelerUtilities.makeCompactGrid(panel, 3, 1, 5, 5, 10, 2);

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(panel, BorderLayout.SOUTH);

		s = MolecularContainer.getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok();
				dispose();
			}
		});
		panel.add(button);

		s = MolecularContainer.getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel();
				dispose();
			}
		});
		panel.add(button);

		pack();

	}

	private JSlider createSlider(float value, String title) {
		JSlider s = new JSlider(-100, 100, (int) (value * SLIDER_MAGNIFIER));
		s.setMajorTickSpacing(10);
		s.setMinorTickSpacing(5);
		s.setPaintLabels(true);
		Hashtable ht = new Hashtable();
		ht.put(0, new JLabel("0"));
		ht.put(-100, new JLabel("-10"));
		ht.put(100, new JLabel("10"));
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

	private void ok() {
		int x = xSlider.getValue();
		int y = ySlider.getValue();
		int z = zSlider.getValue();
		setValues(x / SLIDER_MAGNIFIER, y / SLIDER_MAGNIFIER, z / SLIDER_MAGNIFIER);
	}

	private void cancel() {
		if (original != null)
			setValues(original.x, original.y, original.z);
	}

	private void setValues(float x, float y, float z) {
		float a = (float) Math.sqrt(x * x + y * y + z * z);
		model.setBField(a, a == 0 ? null : new Vector3f(x / a, y / a, z / a));
	}

}
