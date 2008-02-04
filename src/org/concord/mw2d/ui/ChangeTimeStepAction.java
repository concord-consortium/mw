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
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.ui.RealNumberTextField;

class ChangeTimeStepAction extends AbstractAction {

	private MDContainer container;

	ChangeTimeStepAction(MDContainer container) {
		super();
		this.container = container;
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
		putValue(NAME, "Change Time Step");
		putValue(SHORT_DESCRIPTION, "Change time step for molecular dynamics");
		putValue(SMALL_ICON, IconPool.getIcon("time step"));
	}

	public void actionPerformed(ActionEvent e) {
		if (ModelerUtilities.stopFiring(e))
			return;
		createTimeStepDialog().setVisible(true);
	}

	private JDialog createTimeStepDialog() {

		String s = MDContainer.getInternationalText("ChangeTimeStep");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(container),
				s != null ? s : "Set Time Step", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setSize(200, 200);

		final RealNumberTextField stepField = new RealNumberTextField(container.getModel().getTimeStep(), 0.00001, 5.0,
				10);
		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				container.getModel().setTimeStepAndAdjustReminder(stepField.getValue());
				container.getModel().notifyChange();
				dialog.dispose();
			}
		};
		stepField.addActionListener(okListener);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		s = MDContainer.getInternationalText("MolecularDynamicsTimeStep");
		p.add(new JLabel((s != null ? s : "Molecular Dynamics Time Step") + ":"));
		p.add(stepField);

		dialog.getContentPane().add(p, BorderLayout.CENTER);

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

		final JButton b2 = new JButton("What's This?");
		b2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				container
						.showMessageWithPopupMenu(
								b2,
								"<html><p><b>Molecular dynamics time step</b></p><br><p>specifies the basic unit of temporal resolution in molecular dynamics simulations.<br>The molecular dynamics method is a numerical model that discretizes time to<br>approximate the equations of motion. In other words, the model time advances<br>by a discrete value, which is called the time step. Note that the time step<br>does not model anything in the real world, which assumes continuum of time.</p><br><p>The time step is critical to a simulation. If it is set too high, the model<br>will crash, due to the drastic increasing of numerical errors called divergency.<br>If it is set too low, the model will appear to run slowly. The choice of time<br>step depends on a number of factors, such as the strength of intermolecular<br>forces and the temperature. If you do not know much about this, use a value<br>between 0.5 and 2.0 femtoseconds.</p></html>");
			}
		});
		p.add(b2);

		dialog.getContentPane().add(p, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(container);

		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				stepField.selectAll();
				stepField.requestFocus();
			}
		});

		return dialog;

	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}