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

package org.concord.mw2d.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.RealNumberTextField;

class RotationAction extends AbstractAction {

	private final static Icon ROTATE_ICON = new ImageIcon(MDContainer.class.getResource("images/RotationAction.gif"));
	private MDContainer container;

	RotationAction(MDContainer container) {
		super();
		this.container = container;
		putValue(NAME, "Rotate All Particles");
		putValue(SHORT_DESCRIPTION, "Rotate all the particles of the model");
		putValue(SMALL_ICON, ROTATE_ICON);
	}

	public void actionPerformed(ActionEvent e) {
		if (ModelerUtilities.stopFiring(e))
			return;
		createModelRotationDialog().setVisible(true);
	}

	private JDialog createModelRotationDialog() {

		String s = MDContainer.getInternationalText("RotateWholeModel");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(container), s != null ? s
				: "Rotate all particles clockwise", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		final RealNumberTextField angleField = new RealNumberTextField(0, -180, 180, 8);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		s = MDContainer.getInternationalText("RotationAngle");
		p.add(new JLabel((s != null ? s : "Rotational angle (input a number between -180 and 180, in degrees)") + ":"));
		p.add(angleField);

		dialog.getContentPane().add(p, BorderLayout.CENTER);

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (angleField.getValue() == 0)
					return;
				if (container.getView().rotateWholeModel(angleField.getValue())) {
					container.getModel().notifyChange();
					dialog.dispose();
				}
				else {
					JOptionPane.showMessageDialog(container, "Some objects would fall outside the boundary, if\n"
							+ "rotated by the specified angle.", "Rotation not allowed", JOptionPane.ERROR_MESSAGE);
				}
			}
		};
		angleField.addActionListener(okListener);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		s = MDContainer.getInternationalText("OK");
		JButton b = new JButton(s != null ? s : "OK");
		b.addActionListener(okListener);
		p.add(b);

		s = MDContainer.getInternationalText("Cancel");
		b = new JButton(s != null ? s : "Cancel");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		p.add(b);

		dialog.getContentPane().add(p, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(container);

		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				angleField.selectAll();
				angleField.requestFocus();
			}
		});

		return dialog;

	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}