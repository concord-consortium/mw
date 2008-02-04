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
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.concord.mw2d.models.GravitationalField;
import org.concord.mw2d.models.MDModel;

class GravityEditor extends JDialog {

	public static double gravity = 0.01;

	private JSlider slider;
	private JCheckBox checkBox;
	private MDModel model;
	private final static Font smallItalicFont = new Font("Arial", Font.ITALIC, 10);

	public GravityEditor(Frame owner) {

		super(owner, "Change Gravity", false);
		String s = MDView.getInternationalText("ChangeGravity");
		if (s != null)
			setTitle(s);
		setResizable(false);
		setSize(160, 240);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		Container container = getContentPane();
		container.setLayout(new BorderLayout());

		slider = new JSlider(JSlider.VERTICAL, 0, 25, (int) (gravity * 1000));
		slider.setToolTipText(s != null ? s : "Change the gravity");
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setPaintTrack(true);
		slider.setSnapToTicks(true);
		slider.setMajorTickSpacing(5);
		slider.setMinorTickSpacing(1);
		slider.setBorder(BorderFactory.createTitledBorder("g (m/s^2)"));
		slider.putClientProperty("JSlider.isFilled", Boolean.TRUE);

		Hashtable<Integer, JLabel> tableOfLabels = new Hashtable<Integer, JLabel>();
		JLabel label5 = new JLabel("500");
		JLabel label4 = new JLabel("400");
		JLabel label3 = new JLabel("300");
		JLabel label2 = new JLabel("200");
		JLabel label1 = new JLabel("100");
		JLabel label0 = new JLabel("0");
		label0.setFont(smallItalicFont);
		label1.setFont(smallItalicFont);
		label2.setFont(smallItalicFont);
		label3.setFont(smallItalicFont);
		label4.setFont(smallItalicFont);
		label5.setFont(smallItalicFont);
		tableOfLabels.put(25, label5);
		tableOfLabels.put(20, label4);
		tableOfLabels.put(15, label3);
		tableOfLabels.put(10, label2);
		tableOfLabels.put(5, label1);
		tableOfLabels.put(0, label0);

		slider.setLabelTable(tableOfLabels);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (model == null)
					return;
				GravitationalField gf = (GravitationalField) model.getNonLocalField(GravitationalField.class.getName());
				if (gf == null)
					return;
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					gravity = source.getValue() * 0.001;
					gf.setIntensity(gravity);
				}
			}
		});

		JPanel p = new JPanel(new BorderLayout(5, 5));
		p.add(slider, BorderLayout.CENTER);

		s = MDView.getInternationalText("Downward");
		p.add(new JLabel(s != null ? s : "Always downward"), BorderLayout.SOUTH);

		container.add(p, BorderLayout.WEST);

		JLabel imageLabel = new JLabel(new ImageIcon(getClass().getResource("images/gravityIcon.gif")));

		p = new JPanel(new BorderLayout());
		p.add(imageLabel, BorderLayout.CENTER);

		container.add(p, BorderLayout.EAST);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		s = MDView.getInternationalText("ApplyField");
		checkBox = new JCheckBox(s != null ? s : "Apply");
		checkBox.setSelected(false);
		checkBox.setFocusPainted(false);
		checkBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				slider.setEnabled(checkBox.isSelected());
				if (checkBox.isSelected()) {
					model.addNonLocalField(new GravitationalField(gravity, model.getView().getBounds()));
				}
				else {
					model.removeField(GravitationalField.class.getName());
				}
			}
		});
		p.add(checkBox, BorderLayout.CENTER);

		s = MDView.getInternationalText("CloseButton");
		JButton button = new JButton(s != null ? s : "Close");
		button.setFocusPainted(false);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GravityEditor.this.dispose();
			}
		});
		p.add(button);

		container.add(p, BorderLayout.SOUTH);

	}

	public void destroy() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				dispose();
			}
		});
		model = null;
	}

	public void setModel(MDModel model) {
		this.model = model;
	}

	public MDModel getModel() {
		return model;
	}

	public void setCurrentValues() {
		if (model != null) {
			GravitationalField gf = (GravitationalField) model.getNonLocalField(GravitationalField.class.getName());
			if (gf != null) {
				checkBox.setSelected(true);
				slider.setEnabled(true);
				slider.setValue((int) (1000 * gf.getIntensity()));
			}
			else {
				checkBox.setSelected(false);
				slider.setEnabled(false);
			}
		}
		else {
			slider.setEnabled(false);
		}
	}

}