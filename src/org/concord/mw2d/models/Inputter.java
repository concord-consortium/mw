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

package org.concord.mw2d.models;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.concord.modeler.ui.RealNumberTextField;
import org.concord.mw2d.MDView;

class Inputter {

	final static byte RESTRAINT = 0x01;
	final static byte CHARGE = 0x02;
	final static byte DIPOLE = 0x03;

	private Particle particle;

	Inputter(Particle p) {
		particle = p;
	}

	void input(byte property) {

		JOptionPane op = new JOptionPane();
		op.setLayout(new BorderLayout(5, 5));
		final JDialog d = op.createDialog(particle.getView(), "");
		final RealNumberTextField textField = new RealNumberTextField();
		ActionListener okListener = null;
		JPanel p = new JPanel();

		switch (property) {

		case CHARGE:
			d.setTitle("Input Charge");
			textField.setColumns(5);
			textField.setValue(particle.charge);
			textField.setMinValue(-5);
			textField.setMaxValue(5);
			okListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					particle.setCharge(textField.getValue());
					particle.getView().repaint();
					d.dispose();
				}
			};
			p.add(new JLabel("Please input a number in [-5, 5]: "));
			break;

		case DIPOLE:
			d.setTitle("Input Dipole Moment");
			textField.setColumns(5);
			if (particle instanceof GayBerneParticle)
				textField.setValue(((GayBerneParticle) particle).dipoleMoment);
			textField.setMinValue(-1000);
			textField.setMaxValue(1000);
			okListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (particle instanceof GayBerneParticle) {
						((GayBerneParticle) particle).dipoleMoment = textField.getValue();
					}
					particle.getView().repaint();
					d.dispose();
				}
			};
			p.add(new JLabel("Please input a number in [-100, 100]: "));
			break;

		case RESTRAINT:
			d.setTitle("Input Restraint");
			textField.setColumns(5);
			textField.setValue(particle.restraint == null ? 0 : particle.restraint.getK() * 100);
			textField.setMinValue(0);
			textField.setMaxValue(100000);
			okListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					double x = textField.getValue() * 0.01;
					if (x > Particle.ZERO) {
						if (particle.restraint == null) {
							particle.restraint = new PointRestraint(x, particle.rx, particle.ry);
						}
						else {
							particle.restraint.setK(x);
						}
					}
					else {
						particle.restraint = null;
					}
					particle.getView().repaint();
					d.dispose();
				}
			};
			p.add(new JLabel("Please specify or change the restraint strength:"));
			break;

		}

		textField.addActionListener(okListener);
		p.add(textField);
		op.add(p, BorderLayout.CENTER);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		String s = MDView.getInternationalText("OKButton");
		JButton b = new JButton(s != null ? s : "OK");
		b.setMnemonic(KeyEvent.VK_O);
		b.addActionListener(okListener);
		p.add(b);

		s = MDView.getInternationalText("CancelButton");
		b = new JButton(s != null ? s : "Cancel");
		b.setMnemonic(KeyEvent.VK_C);
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.dispose();
			}
		});
		p.add(b);

		op.add(p, BorderLayout.SOUTH);

		d.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				d.dispose();
			}

			public void windowActivated(WindowEvent e) {
				textField.selectAll();
				textField.requestFocusInWindow();
			}
		});

		d.pack();
		d.setLocationRelativeTo(particle.getView());
		d.setVisible(true);

	}

}