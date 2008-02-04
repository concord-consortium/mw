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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.mw3d.models.HeatBath;
import org.concord.mw3d.models.MolecularModel;

class HeatBathPanel {

	private FloatNumberTextField field1;
	private IntegerTextField field2;
	private MolecularModel model;

	HeatBathPanel(MolecularModel model) {
		this.model = model;
		field1 = new FloatNumberTextField(model.heatBathActivated() ? model.getHeatBath().getExpectedTemperature()
				: model.getTemperature(), 0, 100000);
		field2 = new IntegerTextField(model.heatBathActivated() ? model.getHeatBath().getInterval() : 10, 1, 1000);
	}

	JDialog createDialog() {

		String s = MolecularContainer.getInternationalText("HeatBathSettings");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(model.getView()), s != null ? s
				: "Heat Bath Settings", false);

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HeatBath hb = model.getHeatBath();
				if (hb != null) {
					boolean changed = false;
					if (Math.abs(hb.getExpectedTemperature() - field1.getValue()) > 0.001) {
						hb.setExpectedTemperature(field1.getValue());
						changed = true;
					}
					if (hb.getInterval() != field2.getValue()) {
						hb.setInterval(field2.getValue());
						changed = true;
					}
					if (changed) {
						model.setTemperature(hb.getExpectedTemperature());
						model.getView().repaint();
						model.notifyChange();
					}
				}
				dialog.dispose();
			}
		};

		JPanel p = new JPanel(new GridLayout(2, 2, 5, 5));
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), BorderFactory
				.createEtchedBorder()));
		p.setPreferredSize(new Dimension(250, 65));
		dialog.getContentPane().add(p, BorderLayout.CENTER);

		s = MolecularContainer.getInternationalText("HeatBathTemperature");
		JLabel label = new JLabel((s != null ? s : "Temperature") + " (K)");
		p.add(label);

		field1.addActionListener(okListener);
		p.add(field1);

		s = MolecularContainer.getInternationalText("HeatBathInterval");
		label = new JLabel((s != null ? s : "Interval") + " (MD steps)");
		p.add(label);

		field2.addActionListener(okListener);
		p.add(field2);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		s = MolecularContainer.getInternationalText("Apply");
		final JCheckBox checkBox = new JCheckBox(s != null ? s : "Apply");
		if (!model.heatBathActivated()) {
			field1.setEnabled(false);
			field2.setEnabled(false);
			checkBox.setSelected(false);
		}
		else {
			field1.setEnabled(true);
			field2.setEnabled(true);
			checkBox.setSelected(true);
		}
		checkBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (checkBox.isSelected()) {
					field1.setEnabled(true);
					field2.setEnabled(true);
					model.activateHeatBath(true);
					if (model.heatBathActivated())
						model.getHeatBath().setExpectedTemperature(field1.getValue());
				}
				else {
					field1.setEnabled(false);
					field2.setEnabled(false);
					model.activateHeatBath(false);
				}
				model.getView().repaint();
				model.notifyChange();
			}
		});
		p.add(checkBox);

		s = MolecularContainer.getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(okListener);
		p.add(button);

		s = MolecularContainer.getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		p.add(button);

		dialog.getContentPane().add(p, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(model.getView());

		return dialog;

	}

}