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
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.concord.modeler.event.PageComponentEvent;
import org.concord.mw2d.MDView;

class EngineWorker extends JDialog {

	public NeighborListAdjustor listAdjustor;
	private JRadioButton rbNone, rbShift;
	private final static Font littleFont = new Font("Arial", Font.PLAIN, 9);
	private final static NumberFormat decimalFormat = NumberFormat.getNumberInstance();

	public EngineWorker(final AtomicModel model) {

		super(JOptionPane.getFrameForComponent(model.getView()), "Potential Cutoff and Neighbor List", true);
		setSize(400, 300);
		setResizable(false);
		String s = MDView.getInternationalText("PotentialCutoffAndNeighborList");
		if (s != null)
			setTitle(s);

		/* cutoff and skin radius adjustor */
		listAdjustor = new NeighborListAdjustor(model);
		listAdjustor.setBorder(BorderFactory.createEtchedBorder());

		decimalFormat.setMaximumFractionDigits(2);

		JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.add(listAdjustor, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		ButtonGroup bg = new ButtonGroup();

		rbNone = new JRadioButton("None");
		rbNone.setSelected(true);
		rbNone.setFont(littleFont);
		rbNone.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				model.setCutOffShift(false);
				model.notifyPageComponentListeners(new PageComponentEvent(model, PageComponentEvent.COMPONENT_CHANGED));
			}
		});
		bg.add(rbNone);
		panel.add(rbNone);

		rbShift = new JRadioButton("Shift Potential");
		rbShift.setSelected(false);
		rbShift.setFont(littleFont);
		rbShift.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				model.setCutOffShift(true);
				model.notifyPageComponentListeners(new PageComponentEvent(model, PageComponentEvent.COMPONENT_CHANGED));
			}
		});
		bg.add(rbShift);
		panel.add(rbShift);

		JRadioButton radioButton = new JRadioButton("Use Smoothing Function");
		radioButton.setFont(littleFont);
		radioButton.setEnabled(false);
		bg.add(radioButton);
		panel.add(radioButton);

		listPanel.add(panel, BorderLayout.SOUTH);

		Container container = getContentPane();
		container.setLayout(new BorderLayout());
		container.add(listPanel, BorderLayout.CENTER);

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		s = MDView.getInternationalText("CloseButton");
		final JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		panel.add(button);

		container.add(panel, BorderLayout.SOUTH);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}

			public void windowActivated(WindowEvent e) {
				button.requestFocus();
			}
		});

	}

	public void setCutOffShift(boolean b) {
		if (b) {
			rbShift.setSelected(true);
		}
		else {
			rbNone.setSelected(true);
		}
	}

}