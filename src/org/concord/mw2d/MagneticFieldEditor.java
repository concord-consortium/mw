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
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.concord.mw2d.models.MagneticField;

class MagneticFieldEditor extends JDialog {

	private double b = 0.1;
	private JSlider slider;
	private JRadioButton rbIn, rbOut;
	private JCheckBox checkBox;
	private MagneticField mf;
	private Runnable callbackForAddingField, callbackForRemovingField;
	private final static Font smallItalicFont = new Font(null, Font.ITALIC, 10);

	private ChangeListener slideListener = new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider) e.getSource();
			if (!source.getValueIsAdjusting()) {
				b = source.getValue() * 0.01;
				if (mf != null)
					mf.setIntensity(rbIn.isSelected() ? b : -b);
			}
		}
	};

	MagneticFieldEditor(Frame owner) {

		super(owner, "Change Magnetic Field", false);
		String s = MDView.getInternationalText("ChangeMagneticField");
		if (s != null)
			setTitle(s);
		setResizable(false);
		setSize(160, 240);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		Container container = getContentPane();
		container.setLayout(new BorderLayout());

		slider = new JSlider(JSlider.VERTICAL, 0, 100, (int) (b * 10.0));
		slider.setToolTipText(s != null ? s : "Change magnetic field intensity");
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setPaintTrack(true);
		slider.setSnapToTicks(true);
		slider.setMajorTickSpacing(20);
		slider.setMinorTickSpacing(1);
		slider.setBorder(BorderFactory.createTitledBorder("Tesla"));
		slider.putClientProperty("JSlider.isFilled", Boolean.TRUE);

		Hashtable<Integer, JLabel> tableOfLabels = new Hashtable<Integer, JLabel>();
		JLabel label5 = new JLabel("1.0");
		JLabel label4 = new JLabel("0.8");
		JLabel label3 = new JLabel("0.6");
		JLabel label2 = new JLabel("0.4");
		JLabel label1 = new JLabel("0.2");
		JLabel label0 = new JLabel("0.0");
		label0.setFont(smallItalicFont);
		label1.setFont(smallItalicFont);
		label2.setFont(smallItalicFont);
		label3.setFont(smallItalicFont);
		label4.setFont(smallItalicFont);
		label5.setFont(smallItalicFont);
		tableOfLabels.put(100, label5);
		tableOfLabels.put(80, label4);
		tableOfLabels.put(60, label3);
		tableOfLabels.put(40, label2);
		tableOfLabels.put(20, label1);
		tableOfLabels.put(0, label0);

		slider.setLabelTable(tableOfLabels);
		slider.addChangeListener(slideListener);

		JPanel p = new JPanel(new BorderLayout());
		p.add(slider, BorderLayout.CENTER);

		container.add(p, BorderLayout.WEST);

		JLabel imageLabel = new JLabel(new ImageIcon(getClass().getResource("images/Magnet.gif")));

		p = new JPanel(new BorderLayout());

		p.add(imageLabel, BorderLayout.CENTER);

		JPanel p1 = new JPanel();
		p1.setBorder(BorderFactory.createEtchedBorder());
		ButtonGroup bg = new ButtonGroup();

		s = MDView.getInternationalText("Inward");
		rbIn = new JRadioButton(s != null ? s : "Inward");
		rbIn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (mf != null)
					mf.setIntensity(Math.abs(mf.getIntensity()));
			}
		});
		bg.add(rbIn);
		p1.add(rbIn);

		s = MDView.getInternationalText("Outward");
		rbOut = new JRadioButton(s != null ? s : "Outward");
		rbOut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (mf != null)
					mf.setIntensity(-Math.abs(mf.getIntensity()));
			}
		});
		bg.add(rbOut);
		p1.add(rbOut);

		p.add(p1, BorderLayout.SOUTH);

		container.add(p, BorderLayout.EAST);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		s = MDView.getInternationalText("ApplyField");
		checkBox = new JCheckBox(s != null ? s : "Apply");
		checkBox.setSelected(false);
		checkBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				slider.setEnabled(checkBox.isSelected());
				if (checkBox.isSelected()) {
					if (callbackForAddingField != null)
						callbackForAddingField.run();
					rbIn.setSelected(true);
					rbIn.setEnabled(true);
					rbOut.setEnabled(true);
				}
				else {
					if (callbackForRemovingField != null)
						callbackForRemovingField.run();
					rbIn.setEnabled(false);
					rbOut.setEnabled(false);
				}
			}
		});
		p.add(checkBox, BorderLayout.SOUTH);

		s = MDView.getInternationalText("CloseButton");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		p.add(button);

		container.add(p, BorderLayout.SOUTH);

	}

	void setCallbackForAddingField(Runnable r) {
		callbackForAddingField = r;
	}

	void setCallbackForRemovingField(Runnable r) {
		callbackForRemovingField = r;
	}

	double getB() {
		return b;
	}

	int getDirection() {
		if (rbIn.isSelected())
			return MagneticField.INWARD;
		return MagneticField.OUTWARD;
	}

	void setField(MagneticField m) {
		mf = m;
		if (mf != null) {
			slider.setEnabled(true);
			slider.removeChangeListener(slideListener);
			slider.setValue((int) (100.0 * Math.abs(mf.getIntensity())));
			slider.addChangeListener(slideListener);
			rbIn.setEnabled(true);
			rbOut.setEnabled(true);
			if (mf.getIntensity() > 0.0) {
				rbIn.setSelected(true);
				rbOut.setSelected(false);
			}
			else {
				rbIn.setSelected(false);
				rbOut.setSelected(true);
			}
			checkBox.setSelected(true);
		}
		else {
			slider.setEnabled(false);
			rbIn.setSelected(true);
			rbIn.setEnabled(false);
			rbOut.setEnabled(false);
			checkBox.setSelected(false);
		}
	}

}