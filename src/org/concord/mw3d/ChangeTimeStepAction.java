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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.IconPool;

class ChangeTimeStepAction extends AbstractAction {

	private MolecularContainer container;

	ChangeTimeStepAction(MolecularContainer container) {
		super("Change Simulation Time Step");
		this.container = container;
		putValue(SMALL_ICON, IconPool.getIcon("time step"));
	}

	public void actionPerformed(ActionEvent e) {
		createTimeStepDialog().setVisible(true);
	}

	private JDialog createTimeStepDialog() {

		String s = MolecularContainer.getInternationalText("ChangeTimeStep");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(container),
				s != null ? s : "Set Time Step", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setSize(200, 200);

		final FloatNumberTextField stepField = new FloatNumberTextField(container.model.getTimeStep(), 0.00001f, 5.0f,
				4);
		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				container.model.setTimeStep(stepField.getValue());
				container.notifyChange();
				dialog.dispose();
			}
		};
		stepField.addActionListener(okListener);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		s = MolecularContainer.getInternationalText("MolecularDynamicsTimeStep");
		p.add(new JLabel((s != null ? s : "Molecular Dynamics Time Step") + ":"));
		p.add(stepField);
		s = MolecularContainer.getInternationalText("Femtosecond");
		p.add(new JLabel(s != null ? s : "Femtoseconds"));
		dialog.getContentPane().add(p, BorderLayout.CENTER);
		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		s = MolecularContainer.getInternationalText("OK");
		JButton b = new JButton(s != null ? s : "OK");
		b.addActionListener(okListener);
		p.add(b);
		s = MolecularContainer.getInternationalText("Cancel");
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
				stepField.selectAll();
				stepField.requestFocusInWindow();
			}
		});
		return dialog;
	}

}
