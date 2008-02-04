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
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;

import org.concord.mw2d.models.Element;

public class DiatomicConfigure extends JDialog {

	public static double distance = 25.0;
	public static double strength = 0.2;
	public static byte typeOfA = Element.ID_PL, typeOfB = Element.ID_PL;

	private JRadioButton rb1A, rb1B, rb1C, rb1D, rb2A, rb2B, rb2C, rb2D;
	private JSlider slider1, slider2;

	public DiatomicConfigure(Frame owner) {

		super(owner, "Customize Diatomic Molecule", true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JPanel p = new JPanel(new GridLayout(1, 2, 5, 5));
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		getContentPane().add(p, BorderLayout.CENTER);

		slider1 = new JSlider(JSlider.HORIZONTAL, 10, 50, (int) distance);
		slider1.setMajorTickSpacing(10);
		slider1.setMinorTickSpacing(5);
		slider1.setPaintTicks(true);
		slider1.setPaintTrack(true);
		slider1.setPaintLabels(true);
		slider1.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		slider1.setBorder(BorderFactory.createTitledBorder("Length (0.1 angstrom)"));
		p.add(slider1);

		slider2 = new JSlider(JSlider.HORIZONTAL, 10, 30, (int) (strength * 100));
		slider2.setMajorTickSpacing(10);
		slider2.setMinorTickSpacing(5);
		slider2.setPaintTicks(true);
		slider2.setPaintTrack(true);
		slider2.setPaintLabels(true);
		slider2.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		slider2.setBorder(BorderFactory.createTitledBorder("Strength (100 x eV/A^2)"));
		p.add(slider2);

		p = new JPanel(new GridLayout(2, 5, 5, 5));
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		getContentPane().add(p, BorderLayout.NORTH);

		rb1A = new JRadioButton("Nt");
		rb1B = new JRadioButton("Pl");
		rb1C = new JRadioButton("Ws");
		rb1D = new JRadioButton("Ck");
		rb2A = new JRadioButton("Nt");
		rb2B = new JRadioButton("Pl");
		rb2C = new JRadioButton("Ws");
		rb2D = new JRadioButton("Ck");

		p.add(new JLabel("Atom A : "));
		p.add(rb1A);
		p.add(rb1B);
		p.add(rb1C);
		p.add(rb1D);
		ButtonGroup group = new ButtonGroup();
		group.add(rb1A);
		group.add(rb1B);
		group.add(rb1C);
		group.add(rb1D);
		rb1B.setSelected(true);

		p.add(new JLabel("Atom B : "));
		p.add(rb2A);
		p.add(rb2B);
		p.add(rb2C);
		p.add(rb2D);
		group = new ButtonGroup();
		group.add(rb2A);
		group.add(rb2B);
		group.add(rb2C);
		group.add(rb2D);
		rb2B.setSelected(true);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(p, BorderLayout.SOUTH);

		String s = MDContainer.getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				dispose();
			}
		});
		p.add(button);

		s = MDContainer.getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		p.add(button);

		s = MDContainer.getInternationalText("ResetButton");
		button = new JButton(s != null ? s : "Reset");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				slider1.setValue(25);
				slider2.setValue(20);
				rb1B.setSelected(true);
				rb2B.setSelected(true);
			}
		});
		p.add(button);

	}

	public void setCurrentValues() {
		switch (typeOfA) {
		case Element.ID_NT:
			rb1A.setSelected(true);
			break;
		case Element.ID_PL:
			rb1B.setSelected(true);
			break;
		case Element.ID_WS:
			rb1C.setSelected(true);
			break;
		case Element.ID_CK:
			rb1D.setSelected(true);
			break;
		}
		switch (typeOfB) {
		case Element.ID_NT:
			rb2A.setSelected(true);
			break;
		case Element.ID_PL:
			rb2B.setSelected(true);
			break;
		case Element.ID_WS:
			rb2C.setSelected(true);
			break;
		case Element.ID_CK:
			rb2D.setSelected(true);
			break;
		}
		slider1.setValue((int) distance);
		slider2.setValue((int) (strength * 100));
	}

	private void confirm() {

		if (rb1A.isSelected())
			typeOfA = Element.ID_NT;
		else if (rb1B.isSelected())
			typeOfA = Element.ID_PL;
		else if (rb1C.isSelected())
			typeOfA = Element.ID_WS;
		else if (rb1D.isSelected())
			typeOfA = Element.ID_CK;

		if (rb2A.isSelected())
			typeOfB = Element.ID_NT;
		else if (rb2B.isSelected())
			typeOfB = Element.ID_PL;
		else if (rb2C.isSelected())
			typeOfB = Element.ID_WS;
		else if (rb2D.isSelected())
			typeOfB = Element.ID_CK;

		distance = slider1.getValue();
		strength = slider2.getValue() * 0.01;

	}

}