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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import org.concord.modeler.Modeler;
import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.RealNumberTextField;
import org.concord.mw2d.models.AngularBond;

class AngularBondPropertiesPanel extends PropertiesPanel {

	private JDialog dialog;
	private RealNumberTextField angleField;
	private RealNumberTextField strengthField;

	void destroy() {
		removeListenersForTextField(angleField);
		removeListenersForTextField(strengthField);
		if (dialog != null)
			dialog.dispose();
	}

	AngularBondPropertiesPanel(final AngularBond bond) {

		super(new BorderLayout(5, 5));

		JPanel p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(10, 10, 10, 10)));
		add(p, BorderLayout.CENTER);

		// row 1
		String s = MDView.getInternationalText("IndexLabel");
		p.add(new JLabel(s != null ? s : "Index"));
		p.add(createLabel(bond.getIndex()));
		p.add(new JPanel());

		// row 2
		s = MDView.getInternationalText("AtomAtEnd");
		p.add(new JLabel(s != null ? s : "Atom (end)"));
		p.add(createLabel(bond.getAtom1().getIndex()));
		p.add(createSmallerFontLabel(bond.getAtom1().getName()));

		// row 3
		p.add(new JLabel(s != null ? s : "Atom (end)"));
		p.add(createLabel(bond.getAtom2().getIndex()));
		p.add(createSmallerFontLabel(bond.getAtom2().getName()));

		// row 4
		s = MDView.getInternationalText("AtomAtJoint");
		p.add(new JLabel(s != null ? s : "Atom (joint)"));
		p.add(createLabel(bond.getAtom3().getIndex()));
		p.add(createSmallerFontLabel(bond.getAtom3().getName()));

		// row 5
		s = MDView.getInternationalText("BondAngle");
		p.add(new JLabel(s != null ? s : "Bond Angle"));
		float ba = (float) Math.toDegrees(bond.getBondAngle());
		angleField = new RealNumberTextField(ba, ba - 20, ba + 20, 5);
		p.add(angleField);
		p.add(createSmallerFontLabel("degree"));

		// row 6
		s = MDView.getInternationalText("BondStrength");
		p.add(new JLabel(s != null ? s : "Bond Strength"));
		strengthField = new RealNumberTextField(bond.getBondStrength(), 1, 1000, 5);
		p.add(strengthField);
		p.add(createSmallerFontLabel("<html>eV/radian<sup>2</sup></html>"));

		// row 7
		s = MDView.getInternationalText("CurrentAngle");
		p.add(new JLabel(s != null ? s : "Current Angle"));
		p.add(createLabel(DECIMAL_FORMAT.format(Math.abs(bond.getAngleExtent()))));
		p.add(createSmallerFontLabel("degree"));

		makeCompactGrid(p, 7, 3, 5, 5, 15, 2);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(p, BorderLayout.SOUTH);

		Action okAction = new ModelAction(bond.getHostModel(), new Executable() {
			public void execute() {
				boolean changed = false;
				if (Math.abs(bond.getBondAngle() - angleField.getValue() / 180.0 * Math.PI) > ZERO) {
					bond.setBondAngle(angleField.getValue() / 180.0 * Math.PI);
					changed = true;
				}
				if (Math.abs(bond.getBondStrength() - strengthField.getValue()) > ZERO) {
					bond.setBondStrength(strengthField.getValue());
					changed = true;
				}
				if (changed)
					bond.getHostModel().notifyChange();
				destroy();
			}
		}) {
		};

		strengthField.setAction(okAction);
		angleField.setAction(okAction);

		JButton okButton = new JButton();
		okButton.setAction(okAction);
		s = MDView.getInternationalText("OKButton");
		okButton.setText(s != null ? s : "OK");

		s = MDView.getInternationalText("CancelButton");
		JButton cancelButton = new JButton(s != null ? s : "Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				destroy();
			}
		});

		if (Modeler.isMac()) {
			p.add(cancelButton);
			p.add(okButton);
		}
		else {
			p.add(okButton);
			p.add(cancelButton);
		}

	}

	void setDialog(JDialog d) {
		dialog = d;
	}

	void windowActivated() {
		strengthField.selectAll();
		strengthField.requestFocus();
	}

}