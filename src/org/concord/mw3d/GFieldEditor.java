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

import org.concord.mw3d.models.GField;
import org.concord.mw3d.models.MolecularModel;

/**
 * @author Charles Xie
 * 
 */
class GFieldEditor extends JDialog {

	private final static float SLIDER_MAGNIFIER = 100000;
	private JSlider gSlider;
	private float original;
	private MolecularModel model;

	GFieldEditor(JDialog owner, MolecularModel model) {

		super(owner, "Gravitational Field Properties", false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		String s = MolecularContainer.getInternationalText("GravitationalFieldProperties");
		if (s != null)
			setTitle(s);

		this.model = model;
		GField gField = model.getGField();
		if (gField != null)
			original = gField.getIntensity();

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(panel, BorderLayout.CENTER);

		int g = (int) (original * SLIDER_MAGNIFIER);
		gSlider = new JSlider(0, 100, g > 100 ? 100 : g);
		gSlider.setMajorTickSpacing(10);
		gSlider.setMinorTickSpacing(5);
		gSlider.setPaintLabels(true);
		Hashtable ht = new Hashtable();
		ht.put(0, new JLabel("0"));
		ht.put(100, new JLabel("0.001"));
		gSlider.setLabelTable(ht);
		s = MolecularContainer.getInternationalText("GravitationalAcceleration");
		gSlider.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Gravitational Acceleration"));
		panel.add(gSlider, BorderLayout.CENTER);

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

	private void ok() {
		model.setGField(gSlider.getValue() / SLIDER_MAGNIFIER);
	}

	private void cancel() {
		model.setGField(original);
	}

}
