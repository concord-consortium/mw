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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.concord.mw2d.models.MolecularModel;

/** This class should not be used by anyone */

public class GrowthModeDialog extends JDialog {

	public final static byte ZIGZAG = 0;
	public final static byte SPIRAL = 1;

	private static byte mode = ZIGZAG;
	private JRadioButton[] modeButton;

	public static int getMode() {
		return mode;
	}

	GrowthModeDialog(Frame parent, final MolecularModel model) {

		super(parent, "Option", true);
		setResizable(false);
		setSize(320, 360);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		modeButton = new JRadioButton[2];

		JPanel panel = new JPanel(new GridLayout(1, modeButton.length, 10, 10)) {
			public Insets getInsets() {
				return new Insets(10, 10, 10, 10);
			}
		};

		ButtonGroup buttonGroup = new ButtonGroup();

		modeButton[0] = new JRadioButton("Zigzagged in a line");
		modeButton[0].setSelected(true);
		modeButton[0].addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					mode = ZIGZAG;
				}
			}
		});
		buttonGroup.add(modeButton[0]);
		panel.add(modeButton[0]);

		modeButton[1] = new JRadioButton("Close-packed (spacing-filling)");
		modeButton[1].addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					mode = SPIRAL;
				}
			}
		});
		buttonGroup.add(modeButton[1]);
		panel.add(modeButton[1]);

		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
				"When contineously adding, protein grows in mode:"));
		p.add(panel, BorderLayout.CENTER);
		getContentPane().add(p, BorderLayout.CENTER);

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		String s = MDContainer.getInternationalText("Close");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GrowthModeDialog.this.dispose();
			}
		});
		panel.add(button);

		getContentPane().add(panel, BorderLayout.SOUTH);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				GrowthModeDialog.this.dispose();
			}
		});

	}

}