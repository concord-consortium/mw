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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;

import org.concord.mw2d.models.Element;

public class TriatomicConfigure extends JDialog {

	public static int angle = 120;
	public static int strength = 2;
	public static int d12 = 20, d23 = 20;
	public static double s12 = 0.2, s23 = 0.2;
	public static byte typeOfA = Element.ID_PL, typeOfB = Element.ID_WS, typeOfC = Element.ID_PL;

	private JSlider angleValue, angleStrength;
	private JSlider sliderArm1, sliderArm1s, sliderArm2, sliderArm2s;
	private JRadioButton rb1A, rb1B, rb1C, rb1D;
	private JRadioButton rb2A, rb2B, rb2C, rb2D;
	private JRadioButton rb3A, rb3B, rb3C, rb3D;

	public TriatomicConfigure(Frame owner) {

		super(owner, "Customize Triatomic Molecule", true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		Dimension dim = new Dimension(100, 80);

		JPanel p = new JPanel(new GridLayout(1, 3, 5, 5));
		getContentPane().add(p, BorderLayout.CENTER);

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setBorder(BorderFactory.createTitledBorder("Angular Bond"));
		p.add(tabbedPane);

		angleValue = new JSlider(JSlider.VERTICAL, 60, 180, angle);
		angleValue.setPreferredSize(dim);
		angleValue.setMajorTickSpacing(40);
		angleValue.setMinorTickSpacing(20);
		angleValue.setPaintTicks(true);
		angleValue.setPaintTrack(true);
		angleValue.setPaintLabels(true);
		angleValue.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		angleValue.setBorder(BorderFactory.createTitledBorder("degree"));
		tabbedPane.addTab("Angle", angleValue);

		angleStrength = new JSlider(JSlider.VERTICAL, 0, 20, strength);
		angleStrength.setPreferredSize(dim);
		angleStrength.setMajorTickSpacing(5);
		angleStrength.setMinorTickSpacing(5);
		angleStrength.setPaintTicks(true);
		angleStrength.setPaintTrack(true);
		angleStrength.setPaintLabels(true);
		angleStrength.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		angleStrength.setBorder(BorderFactory.createTitledBorder("eV/radian^2"));
		tabbedPane.addTab("Strength", angleStrength);

		tabbedPane = new JTabbedPane();
		tabbedPane.setBorder(BorderFactory.createTitledBorder("Radial Bond A-B"));
		p.add(tabbedPane);

		sliderArm1 = new JSlider(JSlider.VERTICAL, 15, 30, d12);
		sliderArm1.setPreferredSize(dim);
		sliderArm1.setMajorTickSpacing(5);
		sliderArm1.setMinorTickSpacing(5);
		sliderArm1.setPaintTicks(true);
		sliderArm1.setPaintTrack(true);
		sliderArm1.setPaintLabels(true);
		sliderArm1.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		sliderArm1.setBorder(BorderFactory.createTitledBorder("0.01 nm"));
		tabbedPane.addTab("Length", sliderArm1);

		sliderArm1s = new JSlider(JSlider.VERTICAL, 10, 30, (int) (s12 * 100));
		sliderArm1s.setPreferredSize(dim);
		sliderArm1s.setMajorTickSpacing(10);
		sliderArm1s.setMinorTickSpacing(10);
		sliderArm1s.setPaintTicks(true);
		sliderArm1s.setPaintTrack(true);
		sliderArm1s.setPaintLabels(true);
		sliderArm1s.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		sliderArm1s.setBorder(BorderFactory.createTitledBorder("10^4 eV/nm^2"));
		tabbedPane.addTab("Strength", sliderArm1s);

		tabbedPane = new JTabbedPane();
		tabbedPane.setBorder(BorderFactory.createTitledBorder("Radial Bond B-C"));
		p.add(tabbedPane);

		sliderArm2 = new JSlider(JSlider.VERTICAL, 15, 30, d23);
		sliderArm2.setPreferredSize(dim);
		sliderArm2.setMajorTickSpacing(5);
		sliderArm2.setMinorTickSpacing(5);
		sliderArm2.setPaintTicks(true);
		sliderArm2.setPaintTrack(true);
		sliderArm2.setPaintLabels(true);
		sliderArm2.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		sliderArm2.setBorder(BorderFactory.createTitledBorder("0.01 nm"));
		tabbedPane.addTab("Length", sliderArm2);

		sliderArm2s = new JSlider(JSlider.VERTICAL, 10, 30, (int) (s12 * 100));
		sliderArm2s.setPreferredSize(dim);
		sliderArm2s.setMajorTickSpacing(10);
		sliderArm2s.setMinorTickSpacing(10);
		sliderArm2s.setPaintTicks(true);
		sliderArm2s.setPaintTrack(true);
		sliderArm2s.setPaintLabels(true);
		sliderArm2s.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		sliderArm2s.setBorder(BorderFactory.createTitledBorder("10^4 eV/nm^2"));
		tabbedPane.addTab("Strength", sliderArm2s);

		JPanel p2 = new JPanel();
		p2.add(new JLabel(new ImageIcon(getClass().getResource("images/TriatomicMolecule.gif"))));
		getContentPane().add(p2, BorderLayout.NORTH);

		p = new JPanel(new GridLayout(3, 5, 5, 5));
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory
				.createEmptyBorder(5, 5, 5, 5)));
		p2.add(p);

		ButtonGroup group = new ButtonGroup();
		p.add(new JLabel("Atom A :"));
		rb1A = new JRadioButton("Nt");
		rb1B = new JRadioButton("Pl");
		rb1C = new JRadioButton("Ws");
		rb1D = new JRadioButton("Ck");
		p.add(rb1A);
		p.add(rb1B);
		p.add(rb1C);
		p.add(rb1D);
		group.add(rb1A);
		group.add(rb1B);
		group.add(rb1C);
		group.add(rb1D);
		rb1B.setSelected(true);

		group = new ButtonGroup();
		p.add(new JLabel("Atom B :"));
		rb2A = new JRadioButton("Nt");
		rb2B = new JRadioButton("Pl");
		rb2C = new JRadioButton("Ws");
		rb2D = new JRadioButton("Ck");
		p.add(rb2A);
		p.add(rb2B);
		p.add(rb2C);
		p.add(rb2D);
		group.add(rb2A);
		group.add(rb2B);
		group.add(rb2C);
		group.add(rb2D);
		rb2C.setSelected(true);

		group = new ButtonGroup();
		p.add(new JLabel("Atom C :"));
		rb3A = new JRadioButton("Nt");
		rb3B = new JRadioButton("Pl");
		rb3C = new JRadioButton("Ws");
		rb3D = new JRadioButton("Ck");
		p.add(rb3A);
		p.add(rb3B);
		p.add(rb3C);
		p.add(rb3D);
		group.add(rb3A);
		group.add(rb3B);
		group.add(rb3C);
		group.add(rb3D);
		rb3B.setSelected(true);

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
				rb1B.setSelected(true);
				rb2C.setSelected(true);
				rb3B.setSelected(true);
				angleValue.setValue(120);
				angleStrength.setValue(200);
				sliderArm1.setValue(20);
				sliderArm1s.setValue(20);
				sliderArm2.setValue(20);
				sliderArm2s.setValue(20);
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
		switch (typeOfC) {
		case Element.ID_NT:
			rb3A.setSelected(true);
			break;
		case Element.ID_PL:
			rb3B.setSelected(true);
			break;
		case Element.ID_WS:
			rb3C.setSelected(true);
			break;
		case Element.ID_CK:
			rb3D.setSelected(true);
			break;
		}

		angleValue.setValue(angle);
		angleStrength.setValue(strength);

		sliderArm1.setValue(d12);
		sliderArm2.setValue(d23);
		sliderArm1s.setValue((int) (s12 * 100));
		sliderArm2s.setValue((int) (s23 * 100));

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

		if (rb3A.isSelected())
			typeOfC = Element.ID_NT;
		else if (rb3B.isSelected())
			typeOfC = Element.ID_PL;
		else if (rb3C.isSelected())
			typeOfC = Element.ID_WS;
		else if (rb3D.isSelected())
			typeOfC = Element.ID_CK;

		angle = angleValue.getValue();
		strength = angleStrength.getValue();

		d12 = sliderArm1.getValue();
		s12 = sliderArm1s.getValue() * 0.01;
		d23 = sliderArm2.getValue();
		s23 = sliderArm2s.getValue() * 0.01;

	}

}