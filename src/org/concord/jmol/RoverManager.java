/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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
package org.concord.jmol;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.SpringLayout;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.FloatNumberTextField;

/**
 * @author Charles Xie
 * 
 */
class RoverManager {

	private JmolContainer jmolContainer;

	RoverManager(JmolContainer jmolContainer) {
		this.jmolContainer = jmolContainer;
	}

	JDialog getEditor() {

		String s = JmolContainer.getInternationalText("RoverSettings");
		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(jmolContainer),
				s != null ? s : "Rover Settings", true);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		JPanel contentPane = new JPanel(new BorderLayout(5, 5));
		d.setContentPane(contentPane);

		final ColorComboBox colorComboBox = new ColorComboBox(jmolContainer);
		final FloatNumberTextField chargeField = new FloatNumberTextField(jmolContainer.rover.getCharge(), -5, 5, 10);
		final FloatNumberTextField massField = new FloatNumberTextField(jmolContainer.rover.getMass(), 1, 10, 10);
		final FloatNumberTextField frictionField = new FloatNumberTextField(jmolContainer.rover.getFriction(), 0.001f,
				10, 10);
		final FloatNumberTextField moiField = new FloatNumberTextField(jmolContainer.rover.getMomentOfInertia(), 1,
				10000, 10);
		final FloatNumberTextField cpdField = new FloatNumberTextField(jmolContainer.rover.getChasePlaneDistance(), 0,
				100, 10);
		s = JmolContainer.getInternationalText("Velocity");
		String s2 = JmolContainer.getInternationalText("Force");
		final JComboBox orientComboBox = new JComboBox(new String[] { s != null ? s : "Velocity",
				s2 != null ? s2 : "Force" });
		switch (jmolContainer.rover.getTurningOption()) {
		case Rover.TURN_TO_FORCE:
			orientComboBox.setSelectedIndex(1);
			break;
		}
		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int c = colorComboBox.getSelectedColor().getRGB();
				jmolContainer.rover.setColor(c);
				jmolContainer.jmol.viewer.setRoverColor(c);
				jmolContainer.rover.setCharge(chargeField.getValue());
				jmolContainer.rover.setMass(massField.getValue());
				jmolContainer.rover.setMomentOfInertia(moiField.getValue());
				jmolContainer.rover.setFriction(frictionField.getValue());
				jmolContainer.rover.setChasePlaneDistance(cpdField.getValue());
				switch (orientComboBox.getSelectedIndex()) {
				case 0:
					jmolContainer.rover.setTurningOption(Rover.TURN_TO_VELOCITY);
					break;
				case 1:
					jmolContainer.rover.setTurningOption(Rover.TURN_TO_FORCE);
					break;
				}
				d.dispose();
			}
		};

		JPanel p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 2, 10));
		contentPane.add(p, BorderLayout.NORTH);

		// row 1
		s = JmolContainer.getInternationalText("RoverMass");
		p.add(new JLabel((s != null ? s : "Mass") + " ([" + massField.getMinValue() + ", " + massField.getMaxValue()
				+ "])"));
		massField.addActionListener(okListener);
		p.add(massField);

		// row 2
		s = JmolContainer.getInternationalText("RoverMomentOfInertia");
		p.add(new JLabel(s != null ? s : "Moment of inertia"));
		moiField.addActionListener(okListener);
		p.add(moiField);

		// row 3
		s = JmolContainer.getInternationalText("RoverCharge");
		p.add(new JLabel((s != null ? s : "Charge") + " ([" + chargeField.getMinValue() + "e, "
				+ chargeField.getMaxValue() + "e])"));
		chargeField.addActionListener(okListener);
		p.add(chargeField);

		// row 4
		s = JmolContainer.getInternationalText("RoverFriction");
		p.add(new JLabel(s != null ? s : "Friction"));
		frictionField.addActionListener(okListener);
		p.add(frictionField);

		// row 5
		s = JmolContainer.getInternationalText("RoverTurnTo");
		p.add(new JLabel(s != null ? s : "Turn to"));
		orientComboBox.addActionListener(okListener);
		p.add(orientComboBox);

		// row 6
		s = JmolContainer.getInternationalText("RoverChasePlaneDistance");
		p.add(new JLabel("<html>" + (s != null ? s : "Chase plane distance") + " (&#197;)</html>"));
		cpdField.addActionListener(okListener);
		p.add(cpdField);

		// row 7
		s = JmolContainer.getInternationalText("Color");
		p.add(new JLabel(s != null ? s : "Color"));
		colorComboBox.setColor(new Color(jmolContainer.rover.getColor()));
		p.add(colorComboBox);

		ModelerUtilities.makeCompactGrid(p, 7, 2, 5, 5, 10, 2);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		contentPane.add(buttonPanel, BorderLayout.SOUTH);

		s = JmolContainer.getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(okListener);
		buttonPanel.add(button);

		s = JmolContainer.getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.dispose();
			}
		});
		buttonPanel.add(button);

		d.pack();
		d.setLocationRelativeTo(jmolContainer);

		return d;

	}
}
