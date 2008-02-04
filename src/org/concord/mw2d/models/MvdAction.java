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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.mw2d.AtomisticView;

class MvdAction extends AbstractAction {

	private final static Icon HISTOGRAM_ICON = new ImageIcon(AtomisticView.class.getResource("images/Histogram.gif"));

	private AtomicModel model;
	private byte element;
	private boolean scalar;

	private float defaultVmaxMaxwell = 2000;
	private JDialog vmaxMaxwellDialog;

	MvdAction(AtomicModel model, boolean scalar, byte element) {
		this.model = model;
		this.scalar = scalar;
		this.element = element;
		putValue(SMALL_ICON, HISTOGRAM_ICON);
		String t = scalar ? "Maxwell Speed Distribution Function" : "Maxwell Velocity Distribution Function";
		putValue(NAME, "Show " + t + ": " + model.getElement(element).getName());
		t = scalar ? "Maxwell speed distribution function" : "Maxwell velocity distribution function";
		putValue(SHORT_DESCRIPTION, "Show " + t + ": " + model.getElement(element).getName());

	}

	public void actionPerformed(ActionEvent e) {
		if (ModelerUtilities.stopFiring(e))
			return;
		askForVmaxMaxwell();
		model.showMVD(new Mvd.Parameter[] { new Mvd.Parameter(scalar, "all", defaultVmaxMaxwell, element,
				model.boundary) });
	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

	private void askForVmaxMaxwell() {
		if (vmaxMaxwellDialog == null) {
			vmaxMaxwellDialog = new JDialog(JOptionPane.getFrameForComponent(model.view), "Input", true);
			vmaxMaxwellDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			JPanel p = new JPanel(new BorderLayout(10, 10));
			p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			vmaxMaxwellDialog.getContentPane().add(p, BorderLayout.CENTER);
			p.add(new JLabel(
					"<html>Please specify the maximum speed or velocity<br>to be counted (the unit is m/s) :</html>"),
					BorderLayout.NORTH);
			final IntegerTextField vmaxField = new IntegerTextField((int) defaultVmaxMaxwell, 100, 5000);
			vmaxField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					defaultVmaxMaxwell = vmaxField.getValue();
					vmaxMaxwellDialog.dispose();
				}
			});
			p.add(vmaxField, BorderLayout.CENTER);
			JPanel p1 = new JPanel();
			p.add(p1, BorderLayout.SOUTH);
			JButton button = new JButton("OK");
			p1.add(button);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					defaultVmaxMaxwell = vmaxField.getValue();
					vmaxMaxwellDialog.dispose();
				}
			});
			vmaxMaxwellDialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					vmaxMaxwellDialog.dispose();
				}

				public void windowActivated(WindowEvent e) {
					vmaxField.selectAll();
					vmaxField.requestFocus();
				}
			});
			vmaxMaxwellDialog.pack();
			vmaxMaxwellDialog.setLocation(400, 300);
		}
		vmaxMaxwellDialog.setVisible(true);
	}

}