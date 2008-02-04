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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.border.Border;

import org.concord.mw2d.models.Element;

public class ChainConfigure extends JDialog {

	public final static byte MAXIMUM = 0x32;
	public final static byte SAWTOOTH = 0x65;
	public final static byte CURLUP = 0x66;
	public final static byte RANDOM = 0x67;
	private final static Border ETCHED_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
			BorderFactory.createEmptyBorder(8, 8, 8, 8));

	public static int distance = 22;
	public static int angle = 60;
	public static int number = 10;
	public static byte typeOfAtom = Element.ID_PL;
	public static byte growMode = SAWTOOTH;

	private JSlider slider1, slider2, slider3;
	private JRadioButton rbGrow1, rbGrow2, rbGrow3;
	private JRadioButton rbBead1, rbBead2, rbBead3, rbBead4;

	public ChainConfigure(Frame owner) {

		super(owner, "Customize Chain Molecule", true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JPanel total = new JPanel(new BorderLayout());
		getContentPane().add(total, BorderLayout.CENTER);

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(ETCHED_BORDER);
		total.add(panel, BorderLayout.CENTER);

		Hashtable<Integer, JLabel> t = new Hashtable<Integer, JLabel>();
		t.put(10, new JLabel("0.1"));
		t.put(20, new JLabel("0.2"));
		t.put(30, new JLabel("0.3"));

		slider1 = new JSlider(JSlider.HORIZONTAL, 8, 30, distance);
		slider1.setPreferredSize(new Dimension(150, 45));
		slider1.setLabelTable(t);
		slider1.setPaintTicks(true);
		slider1.setMajorTickSpacing(10);
		slider1.setMinorTickSpacing(1);
		slider1.setPaintTrack(true);
		slider1.setPaintLabels(true);
		slider1.putClientProperty("JSlider.isFilled", Boolean.TRUE);

		JPanel p = new JPanel(new BorderLayout(5, 5));
		p.add(slider1, BorderLayout.CENTER);
		p.add(new JLabel("Steplength (nm)"), BorderLayout.NORTH);

		JPanel p1 = new JPanel(new BorderLayout());
		p1.add(new JLabel(new ImageIcon(getClass().getResource("images/howLong.gif"))), BorderLayout.WEST);
		p1.add(p, BorderLayout.EAST);
		panel.add(p1, BorderLayout.CENTER);

		slider2 = new JSlider(JSlider.HORIZONTAL, 0, 180, angle);
		slider2.setPreferredSize(new Dimension(150, 45));
		slider2.setPaintTicks(true);
		slider2.setMajorTickSpacing(90);
		slider2.setMinorTickSpacing(15);
		slider2.setPaintTrack(true);
		slider2.setPaintLabels(true);
		slider2.putClientProperty("JSlider.isFilled", Boolean.TRUE);

		p = new JPanel(new BorderLayout(5, 5));
		p.add(slider2, BorderLayout.CENTER);
		p.add(new JLabel("Zigzag angle (deg)"), BorderLayout.NORTH);

		p1 = new JPanel(new BorderLayout());
		p1.add(new JLabel(new ImageIcon(getClass().getResource("images/howBend.gif"))), BorderLayout.WEST);
		p1.add(p, BorderLayout.EAST);
		panel.add(p1, BorderLayout.SOUTH);

		slider3 = new JSlider(JSlider.HORIZONTAL, 5, 85, number);
		slider3.setPreferredSize(new Dimension(150, 45));
		slider3.setPaintTicks(true);
		slider3.setMajorTickSpacing(20);
		slider3.setMinorTickSpacing(2);
		slider3.setPaintTrack(true);
		slider3.setPaintLabels(true);
		slider3.putClientProperty("JSlider.isFilled", Boolean.TRUE);

		p = new JPanel(new BorderLayout(5, 5));
		p.add(slider3, BorderLayout.CENTER);
		p.add(new JLabel("Number of beads"), BorderLayout.NORTH);

		p1 = new JPanel(new BorderLayout());
		p1.add(new JLabel(new ImageIcon(getClass().getResource("images/howMany.gif"))), BorderLayout.WEST);
		p1.add(p, BorderLayout.EAST);
		panel.add(p1, BorderLayout.NORTH);

		p = new JPanel();
		ButtonGroup group = new ButtonGroup();
		rbGrow1 = new JRadioButton("Sawtooth");
		rbGrow2 = new JRadioButton("Curl Up");
		rbGrow3 = new JRadioButton("Random");
		group.add(rbGrow1);
		group.add(rbGrow2);
		group.add(rbGrow3);
		p.add(rbGrow1);
		p.add(rbGrow2);
		p.add(rbGrow3);
		rbGrow1.setSelected(true);

		panel = new JPanel(new BorderLayout());
		panel.setBorder(ETCHED_BORDER);
		total.add(panel, BorderLayout.SOUTH);

		panel.add(new JLabel("Directional growth mode:"), BorderLayout.NORTH);
		panel.add(p, BorderLayout.CENTER);

		panel = new JPanel(new BorderLayout());
		panel.setBorder(ETCHED_BORDER);

		p = new JPanel();
		group = new ButtonGroup();
		rbBead1 = new JRadioButton("Nt");
		rbBead2 = new JRadioButton("Pl");
		rbBead3 = new JRadioButton("Ws");
		rbBead4 = new JRadioButton("Ck");
		p.add(rbBead1);
		p.add(rbBead2);
		p.add(rbBead3);
		p.add(rbBead4);
		group.add(rbBead1);
		group.add(rbBead2);
		group.add(rbBead3);
		group.add(rbBead4);
		rbBead2.setSelected(true);

		panel.add(new JLabel("Bead type"), BorderLayout.NORTH);
		panel.add(p, BorderLayout.CENTER);

		total.add(panel, BorderLayout.NORTH);

		/* add the button panel */

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

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
				slider1.setValue(20);
				slider2.setValue(60);
				slider3.setValue(10);
				rbGrow1.setSelected(true);
				rbBead2.setSelected(true);
			}
		});
		p.add(button);

		getContentPane().add(p, BorderLayout.SOUTH);

	}

	private void confirm() {

		if (rbBead1.isSelected())
			typeOfAtom = Element.ID_NT;
		else if (rbBead2.isSelected())
			typeOfAtom = Element.ID_PL;
		else if (rbBead3.isSelected())
			typeOfAtom = Element.ID_WS;
		else if (rbBead4.isSelected())
			typeOfAtom = Element.ID_CK;

		if (rbGrow1.isSelected())
			growMode = SAWTOOTH;
		else if (rbGrow2.isSelected())
			growMode = CURLUP;
		else if (rbGrow3.isSelected())
			growMode = RANDOM;

		distance = slider1.getValue();
		angle = slider2.getValue();
		number = slider3.getValue();

	}

	public void setCurrentValues() {
		switch (typeOfAtom) {
		case Element.ID_NT:
			rbBead1.setSelected(true);
			break;
		case Element.ID_PL:
			rbBead2.setSelected(true);
			break;
		case Element.ID_WS:
			rbBead3.setSelected(true);
			break;
		case Element.ID_CK:
			rbBead4.setSelected(true);
			break;
		}
		switch (growMode) {
		case SAWTOOTH:
			rbGrow1.setSelected(true);
			break;
		case CURLUP:
			rbGrow2.setSelected(true);
			break;
		case RANDOM:
			rbGrow3.setSelected(true);
			break;
		}
		slider1.setValue(distance);
		slider2.setValue(angle);
		slider3.setValue(number);
	}

}