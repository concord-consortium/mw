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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.IntegerTextField;
import org.jmol.api.InteractionCenter;
import org.jmol.g3d.Graphics3D;

/**
 * @author Charles Xie
 * 
 */
class InteractionManager {

	private JmolContainer jmolContainer;
	private InteractionCenter interactionCenter;

	InteractionManager(JmolContainer jmolContainer) {
		this.jmolContainer = jmolContainer;
	}

	JDialog getEditor(final int iHost, final byte hostType) {

		interactionCenter = jmolContainer.getInteraction(iHost, hostType);

		String s = JmolContainer.getInternationalText("EditSelectedInteraction");
		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(jmolContainer), s != null ? s
				: "Interaction Settings", true);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		JPanel contentPane = new JPanel(new BorderLayout(5, 5));
		d.setContentPane(contentPane);

		final ColorComboBox kcComboBox = new ColorComboBox(jmolContainer);
		final IntegerTextField chargeField = new IntegerTextField(interactionCenter == null ? 1 : interactionCenter
				.getCharge(), -10, 10, 10);
		final FloatNumberTextField radiusField = new FloatNumberTextField(interactionCenter == null ? 0.5f
				: interactionCenter.getRadius(), 0.1f, 10000, 10);

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (interactionCenter == null) {
					interactionCenter = new InteractionCenter();
					interactionCenter.setHostType(hostType);
				}
				interactionCenter.setKeyRgb(kcComboBox.getSelectedColor().getRGB());
				interactionCenter.setCharge(chargeField.getValue());
				interactionCenter.setRadius(radiusField.getValue());
				jmolContainer.setInteraction(iHost, interactionCenter);
				jmolContainer.blinkInteractions();
				d.dispose();
			}
		};

		JPanel p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 2, 10));
		contentPane.add(p, BorderLayout.NORTH);

		// row 1
		s = JmolContainer.getInternationalText("HostType");
		p.add(new JLabel(s != null ? s : "Host Type"));
		JTextField field = new JTextField();
		field.setEditable(false);
		switch (hostType) {
		case InteractionCenter.ATOM_HOST:
			field.setText("Atom");
			break;
		case InteractionCenter.BOND_HOST:
			field.setText("Bond");
			break;
		}
		p.add(field);

		// row 2
		s = JmolContainer.getInternationalText("HostIndex");
		p.add(new JLabel(s != null ? s : "Host Index"));
		field = new JTextField();
		field.setEditable(false);
		field.setText("" + iHost);
		p.add(field);

		// row 3
		s = JmolContainer.getInternationalText("InteractionKeyColor");
		p.add(new JLabel(s != null ? s : "Color"));
		kcComboBox.setColor(new Color(interactionCenter != null ? interactionCenter.getKeyRgb() : Graphics3D
				.getArgb(Graphics3D.OLIVE)));
		p.add(kcComboBox);

		// row 4
		s = JmolContainer.getInternationalText("InteractionCenterCharge");
		p.add(new JLabel((s != null ? s : "Charge") + " ([" + chargeField.getMinValue() + "e, "
				+ chargeField.getMaxValue() + "e])"));
		chargeField.addActionListener(okListener);
		p.add(chargeField);

		// row 5
		s = JmolContainer.getInternationalText("InteractionCenterRepulsionRadius");
		p.add(new JLabel("<html>" + (s != null ? s : "Repulsion Radius") + " ([" + radiusField.getMinValue()
				+ "&#197;, " + radiusField.getMaxValue() + "&#197;])</html>"));
		radiusField.addActionListener(okListener);
		p.add(radiusField);

		ModelerUtilities.makeCompactGrid(p, 5, 2, 5, 5, 10, 2);

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
