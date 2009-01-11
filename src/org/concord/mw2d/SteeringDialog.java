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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.concord.modeler.ui.RealNumberTextField;
import org.concord.mw2d.models.UserField;

class SteeringDialog {

	private MDView view;
	private byte returnedValue;

	SteeringDialog(MDView view) {
		this.view = view;
	}

	byte show(boolean on) {

		JOptionPane op = new JOptionPane();
		op.setLayout(new BorderLayout(5, 5));
		final JDialog d = op.createDialog(view, "Steering Options");

		if (on) {

			JPanel p = new JPanel();
			p.setBorder(BorderFactory.createTitledBorder("Interaction Mode"));
			op.add(p, BorderLayout.NORTH);

			ButtonGroup bg = new ButtonGroup();
			JRadioButton rb = new JRadioButton("Force");
			rb.setSelected(true);
			rb.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED)
						returnedValue = UserField.FORCE_MODE;
				}
			});
			bg.add(rb);
			p.add(rb);

			rb = new JRadioButton("Impulse");
			rb.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED)
						returnedValue = UserField.IMPULSE_MODE;
				}
			});
			bg.add(rb);
			p.add(rb);

			p = new JPanel(new BorderLayout());
			op.add(p, BorderLayout.CENTER);

			String s = "<html>A friction field will be turned on to counterbalance<br>the steering forces to prevent too much energy<br>input into the system. You may specify or change<br>the friction coefficient:";
			p.add(new JLabel(s), BorderLayout.NORTH);

			final RealNumberTextField ntf = new RealNumberTextField(view.steerFriction, 0.0, 100.0, 10);
			ntf.setPreferredSize(new Dimension(100, 20));
			ntf.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					view.steerFriction = ntf.getValue();
					view.getModel().setFriction((float) view.steerFriction);
					d.dispose();
				}
			});
			p.add(ntf, BorderLayout.CENTER);

			p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

			s = MDView.getInternationalText("OKButton");
			JButton b = new JButton(s != null ? s : "OK");
			b.setMnemonic(KeyEvent.VK_O);
			b.setPreferredSize(new Dimension(80, 20));
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					view.steerFriction = ntf.getValue();
					view.getModel().setFriction((float) view.steerFriction);
					d.dispose();
				}
			});
			p.add(b);

			s = MDView.getInternationalText("CancelButton");
			b = new JButton(s != null ? s : "Cancel");
			b.setPreferredSize(new Dimension(80, 20));
			b.setMnemonic(KeyEvent.VK_C);
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					returnedValue = -1;
					d.dispose();
				}
			});
			p.add(b);

			op.add(p, BorderLayout.SOUTH);

		}

		else {

			String s = "<html>The friction field you turned on to counterbalance<br>the steering forces will be turned off.</html>";
			op.add(new JLabel(s), BorderLayout.CENTER);

			JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

			s = MDView.getInternationalText("OKButton");
			JButton b = new JButton(s != null ? s : "OK");
			b.setMnemonic(KeyEvent.VK_O);
			b.setPreferredSize(new Dimension(80, 20));
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					view.getModel().setFriction(0.0f);
					d.dispose();
				}
			});
			p.add(b);

			op.add(p, BorderLayout.SOUTH);

			d.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					view.getModel().setFriction(0.0f);
					d.dispose();
				}
			});

		}

		d.pack();
		d.setVisible(true);

		return returnedValue;

	}
}