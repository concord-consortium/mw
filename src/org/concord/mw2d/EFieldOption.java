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

package org.concord.mw2d;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.concord.modeler.event.PageComponentEvent;
import org.concord.mw2d.models.ElectricForceField;

class EFieldOption extends JDialog {

	private AtomisticView view;
	private JComboBox comboBoxMode;
	private JSpinner cellSizeSpinner;

	EFieldOption(final AtomisticView view) {

		super(JOptionPane.getFrameForComponent(view), "Electric Field Options", true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		String s = MDView.getInternationalText("ElectricFieldOptions");
		if (s != null)
			setTitle(s);
		this.view = view;

		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		s = MDView.getInternationalText("ElectricFieldShadingMode");
		JLabel label = new JLabel((s != null ? s : "Shading mode") + ":", JLabel.LEFT);
		p.add(label);
		comboBoxMode = new JComboBox(new String[] { "Transparency", "Two colors" });
		switch (view.getEFShadingMode()) {
		case ElectricForceField.TRANSPARENCY_SHADING_MODE:
			comboBoxMode.setSelectedIndex(0);
			break;
		case ElectricForceField.TWO_COLORS_SHADING_MODE:
			comboBoxMode.setSelectedIndex(1);
			break;
		}
		p.add(comboBoxMode);

		s = MDView.getInternationalText("ElectricFieldCellSize");
		label = new JLabel((s != null ? s : "Cell size") + ":", JLabel.LEFT);
		p.add(label);
		cellSizeSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 50, 1));
		cellSizeSpinner.setValue(view.getEFCellSize());
		p.add(cellSizeSpinner);

		getContentPane().add(p, BorderLayout.CENTER);

		p = new JPanel(new FlowLayout(FlowLayout.CENTER));

		s = MDView.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				dispose();
			}
		});
		p.add(button);

		s = MDView.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		p.add(button);

		s = MDView.getInternationalText("Apply");
		button = new JButton(s != null ? s : "Apply");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
			}
		});
		p.add(button);

		getContentPane().add(p, BorderLayout.SOUTH);

		pack();

	}

	private void confirm() {
		switch (comboBoxMode.getSelectedIndex()) {
		case 0:
			view.setEFShadingMode(ElectricForceField.TRANSPARENCY_SHADING_MODE);
			break;
		case 1:
			view.setEFShadingMode(ElectricForceField.TWO_COLORS_SHADING_MODE);
			break;
		}
		view.setEFCellSize(((Integer) cellSizeSpinner.getValue()).intValue());
		view.repaint();
		view.getModel().notifyPageComponentListeners(
				new PageComponentEvent(view.getModel(), PageComponentEvent.COMPONENT_CHANGED));
	}

}
