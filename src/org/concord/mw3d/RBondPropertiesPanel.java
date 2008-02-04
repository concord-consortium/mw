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
import javax.swing.SwingConstants;

import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.mw3d.models.RBond;

class RBondPropertiesPanel extends PropertiesPanel {

	private JDialog dialog;

	void destroy() {
		if (dialog != null)
			dialog.dispose();
	}

	RBondPropertiesPanel(final RBond rbond) {

		super(new BorderLayout(5, 5));

		final JLabel nameLabel = createLabel("Radial Bond");
		final JLabel indexLabel = createLabel(rbond.getAtom1().getModel().getRBonds().indexOf(rbond));
		final JLabel atom1Label = createLabel("" + rbond.getAtom1());
		final JLabel atom2Label = createLabel("" + rbond.getAtom2());
		final FloatNumberTextField strengthField = new FloatNumberTextField(rbond.getStrength(), 0.1f, 100);
		final FloatNumberTextField lengthField = new FloatNumberTextField(0.5f, 100);
		lengthField.setText(MolecularView.FORMAT.format(rbond.getLength()));

		JButton okButton = new JButton();

		String s = MolecularContainer.getInternationalText("CancelButton");
		JButton cancelButton = new JButton(s != null ? s : "Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				destroy();
			}
		});

		/* OK listener */

		Action okAction = new ModelAction(rbond.getAtom1().getModel(), new Executable() {

			public void execute() {

				applyBounds(strengthField);
				applyBounds(lengthField);

				boolean changed = false;

				if (Math.abs(rbond.getStrength() - strengthField.getValue()) > ZERO) {
					rbond.setStrength(strengthField.getValue());
					changed = true;
				}

				if (Math.abs(rbond.getLength() - lengthField.getValue()) > ZERO) {
					rbond.setLength(lengthField.getValue());
					changed = true;
				}

				if (changed) {
					rbond.getAtom1().getModel().getView().refresh();
					rbond.getAtom1().getModel().getView().repaint();
					rbond.getAtom1().getModel().notifyChange();
				}
				destroy();

			}
		}) {
		};

		strengthField.setAction(okAction);
		lengthField.setAction(okAction);
		okButton.setAction(okAction);
		s = MolecularContainer.getInternationalText("OK");
		okButton.setText(s != null ? s : "OK");

		/* layout components */

		JPanel panel = new JPanel(new SpringLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(10, 10, 10, 10)));
		add(panel, BorderLayout.CENTER);

		// row 1
		s = MolecularContainer.getInternationalText("Name");
		panel.add(new JLabel(s != null ? s : "Name"));
		panel.add(nameLabel);
		panel.add(new JPanel());

		// row 2
		s = MolecularContainer.getInternationalText("IndexLabel");
		panel.add(new JLabel(s != null ? s : "Index"));
		panel.add(indexLabel);
		panel.add(new JPanel());

		// row 3
		s = MolecularContainer.getInternationalText("Atom");
		panel.add(new JLabel(s != null ? s : "Atom"));
		panel.add(atom1Label);
		panel.add(new JPanel());

		// row 4
		panel.add(new JLabel(s != null ? s : "Atom"));
		panel.add(atom2Label);
		panel.add(new JPanel());

		// row 5
		s = MolecularContainer.getInternationalText("Strength");
		panel.add(new JLabel("Strength", SwingConstants.LEFT));
		panel.add(strengthField);
		panel.add(createSmallerFontLabel("<html>eV/&#197;<sup>2</sup></html>"));

		// row 6
		s = MolecularContainer.getInternationalText("BondLength");
		panel.add(new JLabel(s != null ? s : "Bond Length"));
		panel.add(lengthField);
		panel.add(createSmallerFontLabel("<html>&#197;</html>"));

		makeCompactGrid(panel, 6, 3, 5, 5, 10, 2);

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(panel, BorderLayout.SOUTH);

		panel.add(okButton);
		panel.add(cancelButton);

	}

	void setDialog(JDialog d) {
		dialog = d;
	}

	void windowActivated() {
	}

}