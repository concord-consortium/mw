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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
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

import org.concord.mw2d.models.ElectricField;

class ElectricFieldEditor extends JDialog {

	private final static double FRQ_MIN = 0.0;
	private final static double FRQ_MAX = Math.PI * 0.2;

	private ElectricField ef;
	private int direction = ElectricField.EAST;
	private double fieldIntensity = 1.0;
	private double amplitudeAC;
	private double frequencyAC = FRQ_MIN + 0.1 * (FRQ_MAX - FRQ_MIN);

	private float intensityScale = 100;

	private JRadioButton rbE, rbW, rbN, rbS;
	private JSlider sliderDC, ampSlider, frqSlider;
	private JCheckBox checkBox;
	private JLabel imageLabel;

	private ImageIcon eastIcon, westIcon, northIcon, southIcon;
	private final static Font smallItalicFont = new Font(null, Font.ITALIC, 10);

	private Runnable callbackForAddingField, callbackForRemovingField;

	public ElectricFieldEditor(Frame owner) {

		super(owner, "Electric Field Settings", false);
		String s = MDView.getInternationalText("ChangeElectricField");
		if (s != null)
			setTitle(s);
		setResizable(false);
		setSize(350, 450);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		eastIcon = new ImageIcon(getClass().getResource("images/eastElectricFieldIcon.gif"));
		westIcon = new ImageIcon(getClass().getResource("images/westElectricFieldIcon.gif"));
		northIcon = new ImageIcon(getClass().getResource("images/northElectricFieldIcon.gif"));
		southIcon = new ImageIcon(getClass().getResource("images/southElectricFieldIcon.gif"));

		final ElectricFieldDrawing tf = new ElectricFieldDrawing(fieldIntensity, amplitudeAC, frequencyAC);

		imageLabel = new JLabel(eastIcon);

		final ButtonGroup bg = new ButtonGroup();

		Container container = getContentPane();

		s = MDView.getInternationalText("DCIntensity");
		if (fieldIntensity * 100 > 50)
			fieldIntensity = 0.5;
		sliderDC = new JSlider(JSlider.HORIZONTAL, 0, 50, (int) (fieldIntensity * intensityScale));
		sliderDC.setToolTipText(s != null ? s : "Change the field intensity");
		sliderDC.setBorder(BorderFactory.createTitledBorder(s != null ? s : "D.C. Intensity (GV/m)"));
		sliderDC.setPreferredSize(new Dimension(150, 70));
		sliderDC.setPaintLabels(true);
		sliderDC.setPaintTicks(true);
		sliderDC.setPaintTrack(true);
		sliderDC.setSnapToTicks(true);
		sliderDC.setMajorTickSpacing(10);
		sliderDC.setMinorTickSpacing(1);
		sliderDC.putClientProperty("JSlider.isFilled", Boolean.TRUE);

		Hashtable<Integer, JLabel> tableOfLabels = new Hashtable<Integer, JLabel>();
		JLabel b5 = new JLabel("50");
		JLabel b4 = new JLabel("40");
		JLabel b3 = new JLabel("30");
		JLabel b2 = new JLabel("20");
		JLabel b1 = new JLabel("10");
		JLabel b0 = new JLabel("0");
		b0.setFont(smallItalicFont);
		b1.setFont(smallItalicFont);
		b2.setFont(smallItalicFont);
		b3.setFont(smallItalicFont);
		b4.setFont(smallItalicFont);
		b5.setFont(smallItalicFont);
		tableOfLabels.put(50, b5);
		tableOfLabels.put(40, b4);
		tableOfLabels.put(30, b3);
		tableOfLabels.put(20, b2);
		tableOfLabels.put(10, b1);
		tableOfLabels.put(0, b0);

		sliderDC.setLabelTable(tableOfLabels);
		sliderDC.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					int value = source.getValue();
					fieldIntensity = value / intensityScale;
					if (value > 0) {
						switch (direction) {
						case ElectricField.EAST:
							imageLabel.setIcon(eastIcon);
							break;
						case ElectricField.WEST:
							imageLabel.setIcon(westIcon);
							break;
						case ElectricField.SOUTH:
							imageLabel.setIcon(southIcon);
							break;
						case ElectricField.NORTH:
							imageLabel.setIcon(northIcon);
							break;
						}
					}
					if (direction == ElectricField.EAST || direction == ElectricField.NORTH) {
						tf.setDC(fieldIntensity);
					}
					else if (direction == ElectricField.WEST || direction == ElectricField.SOUTH) {
						tf.setDC(-fieldIntensity);
					}
					tf.repaint();
					if (ef != null)
						ef.setIntensity(fieldIntensity);
				}
			}
		});

		JPanel aPanel = new JPanel(new GridLayout(4, 1));
		s = MDView.getInternationalText("DCDirection");
		aPanel.setBorder(BorderFactory.createTitledBorder(s != null ? s : "D.C.Direction"));

		s = MDView.getInternationalText("Eastward");
		rbE = new JRadioButton(s != null ? s : "Eastward");
		rbE.setSelected(true);
		rbE.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				direction = ElectricField.EAST;
				if (sliderDC.getValue() == 0)
					return;
				imageLabel.setIcon(eastIcon);
				tf.setDC(Math.abs(fieldIntensity));
				tf.repaint();
				if (ef != null)
					ef.setOrientation(direction);
			}
		});
		bg.add(rbE);
		aPanel.add(rbE);

		s = MDView.getInternationalText("Westward");
		rbW = new JRadioButton(s != null ? s : "Westward");
		rbW.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				direction = ElectricField.WEST;
				if (sliderDC.getValue() == 0)
					return;
				imageLabel.setIcon(westIcon);
				tf.setDC(-Math.abs(fieldIntensity));
				tf.repaint();
				if (ef != null)
					ef.setOrientation(direction);
			}
		});
		bg.add(rbW);
		aPanel.add(rbW);

		s = MDView.getInternationalText("Northward");
		rbN = new JRadioButton(s != null ? s : "Northward");
		rbN.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				direction = ElectricField.NORTH;
				if (sliderDC.getValue() == 0)
					return;
				imageLabel.setIcon(northIcon);
				tf.setDC(Math.abs(fieldIntensity));
				tf.repaint();
				if (ef != null)
					ef.setOrientation(direction);
			}
		});
		bg.add(rbN);
		aPanel.add(rbN);

		s = MDView.getInternationalText("Southward");
		rbS = new JRadioButton(s != null ? s : "Southward");
		rbS.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				direction = ElectricField.SOUTH;
				if (sliderDC.getValue() == 0)
					return;
				imageLabel.setIcon(southIcon);
				tf.setDC(-Math.abs(fieldIntensity));
				tf.repaint();
				if (ef != null)
					ef.setOrientation(direction);
			}
		});
		bg.add(rbS);
		aPanel.add(rbS);

		JPanel bPanel = new JPanel(new BorderLayout());
		bPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		bPanel.add(imageLabel);

		if (amplitudeAC * 100 > 50)
			amplitudeAC = 0.5;
		ampSlider = new JSlider(JSlider.HORIZONTAL, 0, 50, (int) (amplitudeAC * intensityScale));
		ampSlider.setToolTipText("Change the Amplitude of AC field");
		s = MDView.getInternationalText("ACAmplitude");
		ampSlider.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Amplitude of A.C. (GV/m)"));
		ampSlider.setPreferredSize(new Dimension(150, 70));
		ampSlider.setPaintLabels(true);
		ampSlider.setPaintTicks(true);
		ampSlider.setPaintTrack(true);
		ampSlider.setSnapToTicks(true);
		ampSlider.setMajorTickSpacing(10);
		ampSlider.setMinorTickSpacing(1);
		ampSlider.setLabelTable(tableOfLabels);
		ampSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		ampSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					int value = source.getValue();
					amplitudeAC = value / intensityScale;
					tf.setAmplitude(amplitudeAC);
					tf.repaint();
					if (ef != null)
						ef.setAmplitude(amplitudeAC);
				}
			}
		});

		s = MDView.getInternationalText("ACFrequency");
		frqSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, 10);
		frqSlider.setToolTipText("Change the Frequency of AC field");
		frqSlider.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Frequency of A.C. (100THz)"));
		frqSlider.setPreferredSize(new Dimension(150, 70));
		frqSlider.setPaintLabels(true);
		frqSlider.setPaintTicks(true);
		frqSlider.setPaintTrack(true);
		frqSlider.setSnapToTicks(true);
		frqSlider.setMajorTickSpacing(20);
		frqSlider.setMinorTickSpacing(2);
		Hashtable<Integer, JLabel> t2 = new Hashtable<Integer, JLabel>();
		JLabel l5 = new JLabel("100");
		JLabel l4 = new JLabel("80");
		JLabel l3 = new JLabel("60");
		JLabel l2 = new JLabel("40");
		JLabel l1 = new JLabel("20");
		JLabel l0 = new JLabel("1");
		l0.setFont(smallItalicFont);
		l1.setFont(smallItalicFont);
		l2.setFont(smallItalicFont);
		l3.setFont(smallItalicFont);
		l4.setFont(smallItalicFont);
		l5.setFont(smallItalicFont);
		t2.put(100, l5);
		t2.put(80, l4);
		t2.put(60, l3);
		t2.put(40, l2);
		t2.put(20, l1);
		t2.put(1, l0);
		frqSlider.setLabelTable(t2);
		frqSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
		frqSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					int value = source.getValue();
					double df = (FRQ_MAX - FRQ_MIN) / frqSlider.getMaximum();
					frequencyAC = FRQ_MIN + value * df;
					tf.setFrequency(frequencyAC);
					tf.repaint();
					if (ef != null)
						ef.setFrequency(frequencyAC);
				}
			}
		});

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(frqSlider, BorderLayout.CENTER);
		panel.add(ampSlider, BorderLayout.EAST);

		JPanel cPanel = new JPanel(new BorderLayout());
		cPanel.add(sliderDC, BorderLayout.WEST);
		cPanel.add(panel, BorderLayout.CENTER);

		JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(panel2, BorderLayout.SOUTH);

		s = MDView.getInternationalText("ApplyField");
		checkBox = new JCheckBox(s != null ? s : "Apply");
		checkBox.setSelected(false);
		checkBox.setFocusPainted(false);
		checkBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enableComponents(checkBox.isSelected());
				if (checkBox.isSelected()) {
					if (callbackForAddingField != null)
						callbackForAddingField.run();
				}
				else {
					if (callbackForRemovingField != null)
						callbackForRemovingField.run();
				}
			}
		});
		panel2.add(checkBox);

		s = MDView.getInternationalText("CloseButton");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ElectricFieldEditor.this.dispose();
			}
		});
		panel2.add(button);
		cPanel.add(panel, BorderLayout.SOUTH);

		tf.setPreferredSize(new Dimension(150, 100));

		cPanel.add(tf, BorderLayout.EAST);

		container.setLayout(new BorderLayout());
		container.add(aPanel, BorderLayout.WEST);
		container.add(cPanel, BorderLayout.SOUTH);
		container.add(bPanel, BorderLayout.CENTER);

	}

	void setCallbackForAddingField(Runnable r) {
		callbackForAddingField = r;
	}

	void setCallbackForRemovingField(Runnable r) {
		callbackForRemovingField = r;
	}

	double getDCIntensity() {
		return fieldIntensity;
	}

	double getACAmplitude() {
		return amplitudeAC;
	}

	double getACFrequency() {
		return frequencyAC;
	}

	int getDirection() {
		return direction;
	}

	public void setField(ElectricField e) {
		ef = e;
		if (ef != null) {
			enableComponents(true);
			direction = ef.getOrientation();
			rbW.setSelected(direction == ElectricField.WEST);
			rbE.setSelected(direction == ElectricField.EAST);
			rbS.setSelected(direction == ElectricField.SOUTH);
			rbN.setSelected(direction == ElectricField.NORTH);
			sliderDC.setValue((int) (100.0 * ef.getIntensity()));
			ampSlider.setValue((int) (100.0 * ef.getAmplitude()));
			double df = (FRQ_MAX - FRQ_MIN) / frqSlider.getMaximum();
			frqSlider.setValue((int) ((ef.getFrequency() - FRQ_MIN) / df));
			enableComponents(true);
			switch (direction) {
			case ElectricField.WEST:
				imageLabel.setIcon(westIcon);
				break;
			case ElectricField.EAST:
				imageLabel.setIcon(eastIcon);
				break;
			case ElectricField.NORTH:
				imageLabel.setIcon(northIcon);
				break;
			case ElectricField.SOUTH:
				imageLabel.setIcon(southIcon);
				break;
			}
		}
		else {
			enableComponents(false);
		}
	}

	private void enableComponents(boolean b) {
		rbW.setEnabled(b);
		rbE.setEnabled(b);
		rbS.setEnabled(b);
		rbN.setEnabled(b);
		sliderDC.setEnabled(b);
		ampSlider.setEnabled(b);
		frqSlider.setEnabled(b);
		checkBox.setSelected(b);
	}

}