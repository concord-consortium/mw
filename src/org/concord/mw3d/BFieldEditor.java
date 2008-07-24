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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.vecmath.Vector3f;

import org.concord.mw3d.models.BField;

/**
 * @author Charles Xie
 * 
 */
class BFieldEditor extends JDialog {

	private BField bField;
	private JSlider xSlider, ySlider, zSlider;
	private Vector3f original;

	BFieldEditor(JDialog owner, BField bf) {

		super(owner, "Magnetic Field Properties", false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		bField = bf;
		original = new Vector3f(bField.getDirection());
		original.scale(bField.getIntensity());

		JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
		getContentPane().add(panel, BorderLayout.CENTER);

		xSlider = createSlider(original.x, "x-component");
		panel.add(xSlider);
		ySlider = createSlider(original.y, "y-component");
		panel.add(ySlider);
		zSlider = createSlider(original.z, "z-component");
		panel.add(zSlider);

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(panel, BorderLayout.SOUTH);

		JButton button = new JButton("OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok();
				dispose();
			}
		});
		panel.add(button);

		button = new JButton("Cancel");
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
		JSlider s = new JSlider(-100, 100, (int) (value * 10));
		s.setMajorTickSpacing(5);
		s.setMinorTickSpacing(1);
		s.setPaintLabels(true);
		Hashtable ht = new Hashtable();
		ht.put(0, new JLabel("0"));
		ht.put(-100, new JLabel("Min"));
		ht.put(100, new JLabel("Max"));
		s.setLabelTable(ht);
		s.setBorder(BorderFactory.createTitledBorder(title));
		return s;
	}

	private void ok() {
		setValues(xSlider.getValue() * 0.1f, ySlider.getValue() * 0.1f, zSlider.getValue() * 0.1f);
	}

	private void cancel() {
		setValues(original.x, original.y, original.z);
	}

	private void setValues(float x, float y, float z) {
		float a = (float) Math.sqrt(x * x + y * y + z * z);
		bField.setIntensity(a);
		bField.setDirection(x / a, y / a, z / a);
	}

}
