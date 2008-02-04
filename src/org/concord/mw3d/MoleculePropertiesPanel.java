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
import javax.vecmath.Point3f;

import org.concord.modeler.process.Executable;
import org.concord.mw3d.models.Molecule;

class MoleculePropertiesPanel extends PropertiesPanel {

	private JDialog dialog;

	void destroy() {
		if (dialog != null)
			dialog.dispose();
	}

	MoleculePropertiesPanel(final Molecule molecule) {

		super(new BorderLayout(5, 5));

		final JLabel nameLabel = createLabel("Undefined");
		final JLabel indexLabel = createLabel(molecule.getAtom(0).getModel().getMoleculeIndex(molecule));
		final JLabel atomCountLabel = createLabel("" + molecule.getAtomCount());

		Point3f com = molecule.getCenterOfMass();
		final JLabel centerOfMassLabel = createLabel("( " + MolecularView.FORMAT.format(com.x) + ", "
				+ MolecularView.FORMAT.format(com.y) + ", " + MolecularView.FORMAT.format(com.z) + " )");

		JButton okButton = new JButton();

		String s = MolecularContainer.getInternationalText("Cancel");
		JButton cancelButton = new JButton(s != null ? s : "Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				destroy();
			}
		});

		/* OK listener */

		Action okAction = new ModelAction(molecule.getAtom(0).getModel(), new Executable() {
			public void execute() {
				destroy();
			}
		}) {
		};

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
		s = MolecularContainer.getInternationalText("AtomCount");
		panel.add(new JLabel(s != null ? s : "Atom Count"));
		panel.add(atomCountLabel);
		panel.add(new JPanel());

		// row 4
		s = MolecularContainer.getInternationalText("CenterOfMass");
		panel.add(new JLabel(s != null ? s : "Center of Mass"));
		panel.add(centerOfMassLabel);
		panel.add(createSmallerFontLabel("<html>&#197;</html>"));

		makeCompactGrid(panel, 4, 3, 5, 5, 10, 2);

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