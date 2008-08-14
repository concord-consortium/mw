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
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.mw3d.models.TBond;

class TBondPropertiesPanel extends PropertiesPanel {

	private JDialog dialog;

	void destroy() {
		if (dialog != null)
			dialog.dispose();
	}

	TBondPropertiesPanel(final TBond tbond) {

		super(new BorderLayout(5, 5));

		String s = MolecularContainer.getInternationalText("TorsionalBond");
		final JLabel nameLabel = createLabel(s != null ? s : "Torsional Bond");
		final JLabel indexLabel = createLabel(tbond.getAtom1().getModel().getTBonds().indexOf(tbond));
		final JLabel atom1Label = createLabel("" + tbond.getAtom1());
		final JLabel atom2Label = createLabel("" + tbond.getAtom2());
		final JLabel atom3Label = createLabel("" + tbond.getAtom3());
		final JLabel atom4Label = createLabel("" + tbond.getAtom4());
		final FloatNumberTextField strengthField = new FloatNumberTextField(tbond.getStrength(), 0.1f, 100);
		final FloatNumberTextField angleField = new FloatNumberTextField(0, 180);
		angleField.setText(MolecularView.FORMAT.format(Math.toDegrees(tbond.getAngle())));

		JButton okButton = new JButton();

		s = MolecularContainer.getInternationalText("Cancel");
		JButton cancelButton = new JButton(s != null ? s : "Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				destroy();
			}
		});

		/* OK listener */

		Action okAction = new ModelAction(tbond.getAtom1().getModel(), new Executable() {

			public void execute() {

				applyBounds(strengthField);
				applyBounds(angleField);

				boolean changed = false;

				if (Math.abs(tbond.getStrength() - strengthField.getValue()) > ZERO) {
					tbond.setStrength(strengthField.getValue());
					changed = true;
				}

				if (Math.abs(tbond.getAngle() - Math.PI / 180 * angleField.getValue()) > ZERO) {
					tbond.setAngle((float) (Math.PI / 180 * angleField.getValue()));
					changed = true;
				}

				if (changed) {
					tbond.getAtom1().getModel().getView().refresh();
					tbond.getAtom1().getModel().getView().repaint();
					tbond.getAtom1().getModel().notifyChange();
				}
				destroy();

			}
		}) {
		};

		strengthField.setAction(okAction);
		angleField.setAction(okAction);
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
		s = MolecularContainer.getInternationalText("Index");
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
		panel.add(new JLabel(s != null ? s : "Atom"));
		panel.add(atom3Label);
		panel.add(new JPanel());

		// row 6
		panel.add(new JLabel(s != null ? s : "Atom"));
		panel.add(atom4Label);
		panel.add(new JPanel());

		// row 7
		s = MolecularContainer.getInternationalText("Strength");
		panel.add(new JLabel(s != null ? s : "Strength", SwingConstants.LEFT));
		panel.add(strengthField);
		panel.add(createSmallerFontLabel("<html>eV/Radian<sup>2</sup></html>"));

		// row 8
		s = MolecularContainer.getInternationalText("EquilibriumTorsionalAngle");
		panel.add(new JLabel(s != null ? s : "Equilibrium Torsional Angle"));
		panel.add(angleField);
		panel.add(createSmallerFontLabel("<html>&#176;</html>"));

		// row 9
		s = MolecularContainer.getInternationalText("CurrentTorsionalAngle");
		panel.add(new JLabel(s != null ? s : "Current Torsional Angle"));
		JTextField tf = new JTextField(MolecularView.FORMAT.format(Math.toDegrees(tbond.getAngle(-1))));
		tf.setEnabled(false);
		panel.add(tf);
		panel.add(createSmallerFontLabel("<html>&#176;</html>"));

		makeCompactGrid(panel, 9, 3, 5, 5, 10, 2);

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