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
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SpringLayout;
import javax.swing.border.Border;

import org.concord.modeler.ModelerUtilities;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.RectangularBoundary;

public class BoundarySetup extends JDialog {

	private JRadioButton dbcButton, pbcButton, rbcButton, xrypbcButton, xpyrbcButton;
	private JCheckBox showBorder, showMirror;
	private MDModel model;
	private boolean lastIsDefault = true;

	public BoundarySetup(MDModel m) {

		super(JOptionPane.getFrameForComponent(m.getView()), "Boundary Setup", true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		String s = MDView.getInternationalText("BoundarySetup");
		if (s != null)
			setTitle(s);

		model = m;

		JPanel panel = new JPanel(new SpringLayout());
		s = MDView.getInternationalText("BoundaryOptions");
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8), BorderFactory
				.createTitledBorder(s != null ? s : "Options:")));
		getContentPane().add(panel, BorderLayout.NORTH);

		ButtonGroup buttonGroup = new ButtonGroup();

		/* dbc */

		s = MDView.getInternationalText("DefaultBoundary");
		dbcButton = new JRadioButton(s != null ? s : "Default: wall reflection");
		dbcButton.setToolTipText("Reflecting from the container wall");
		dbcButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showBorder.setSelected(false);
				showBorder.setEnabled(false);
				lastIsDefault = true;
				if (model == null)
					return;
				model.getBoundary().setType(RectangularBoundary.DBC_ID);
				model.getView().repaint();
				resetBoundaryToElastic(model);
				model.notifyChange();
			}
		});
		dbcButton.setSelected(true);
		buttonGroup.add(dbcButton);
		panel.add(dbcButton);

		Border blackLineBorder = BorderFactory.createLineBorder(Color.black);
		JLabel label = new JLabel(new ImageIcon(getClass().getResource("images/dbc.gif")));
		label.setBorder(blackLineBorder);
		panel.add(label);

		/* rbc */

		s = MDView.getInternationalText("ReflectoryBoundary");
		rbcButton = new JRadioButton(s != null ? s : "X-reflectory, Y-reflectory");
		rbcButton.setToolTipText("Reflecting within a specified box");
		rbcButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showBorder.setSelected(false);
				showBorder.setEnabled(false);
				lastIsDefault = true;
				if (model == null)
					return;
				model.getBoundary().setBorderAlwaysOn(true);
				model.getBoundary().setType(RectangularBoundary.RBC_ID);
				model.getView().repaint();
				resetBoundaryToElastic(model);
				model.notifyChange();
			}
		});
		buttonGroup.add(rbcButton);
		panel.add(rbcButton);

		label = new JLabel(new ImageIcon(getClass().getResource("images/rbc.gif")));
		label.setBorder(blackLineBorder);
		panel.add(label);

		/* pbc */

		s = MDView.getInternationalText("PeriodicBoundary");
		pbcButton = new JRadioButton(s != null ? s : "X-periodic, Y-periodic");
		pbcButton.setToolTipText("Periodic boundary in both X and Y directions");
		pbcButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (lastIsDefault) {
					showBorder.setSelected(true);
					lastIsDefault = false;
				}
				showBorder.setEnabled(true);
				if (model == null)
					return;
				// model.getBoundary().setBorderAlwaysOn(true);
				model.getBoundary().setType(RectangularBoundary.PBC_ID);
				model.getView().repaint();
				resetBoundaryToElastic(model);
				model.notifyChange();
			}
		});
		buttonGroup.add(pbcButton);
		panel.add(pbcButton);

		label = new JLabel(new ImageIcon(getClass().getResource("images/pbc.gif")));
		label.setBorder(blackLineBorder);
		panel.add(label);

		/* xrypbc */

		s = MDView.getInternationalText("XReflectoryYPeriodicBoundary");
		xrypbcButton = new JRadioButton(s != null ? s : "X-reflectory, Y-periodic");
		xrypbcButton.setToolTipText("Use reflectory boundary in X direction and periodic boundary in Y direction");
		xrypbcButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (lastIsDefault) {
					showBorder.setSelected(true);
					lastIsDefault = false;
				}
				showBorder.setEnabled(true);
				if (model == null)
					return;
				// model.getBoundary().setBorderAlwaysOn(true);
				model.getBoundary().setType(RectangularBoundary.XRYPBC_ID);
				model.getView().repaint();
				resetBoundaryToElastic(model);
				model.notifyChange();
			}
		});
		xrypbcButton.setSelected(true);
		buttonGroup.add(xrypbcButton);
		panel.add(xrypbcButton);

		label = new JLabel(new ImageIcon(getClass().getResource("images/xrypbc.gif")));
		label.setBorder(blackLineBorder);
		panel.add(label);

		/* xpyrbc */

		s = MDView.getInternationalText("XPeriodicYReflectoryBoundary");
		xpyrbcButton = new JRadioButton(s != null ? s : "X-periodic, Y-reflectory");
		xpyrbcButton.setToolTipText("Use periodic boundary in X direction and reflectory boundary in Y direction");
		xpyrbcButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (lastIsDefault) {
					showBorder.setSelected(true);
					lastIsDefault = false;
				}
				showBorder.setEnabled(true);
				if (model == null)
					return;
				// model.getBoundary().setBorderAlwaysOn(true);
				model.getBoundary().setType(RectangularBoundary.XPYRBC_ID);
				model.getView().repaint();
				resetBoundaryToElastic(model);
				model.notifyChange();
			}
		});
		xpyrbcButton.setSelected(true);
		buttonGroup.add(xpyrbcButton);
		panel.add(xpyrbcButton);

		label = new JLabel(new ImageIcon(getClass().getResource("images/xpyrbc.gif")));
		label.setBorder(blackLineBorder);
		panel.add(label);

		ModelerUtilities.makeCompactGrid(panel, 5, 2, 5, 5, 10, 2);

		JPanel p = new JPanel(new FlowLayout());
		s = MDView.getInternationalText("ForPeriodicBoundaryShow");
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8), BorderFactory
				.createTitledBorder(s != null ? s : "For periodic boundary, show:")));
		getContentPane().add(p, BorderLayout.CENTER);

		s = MDView.getInternationalText("BorderLines");
		showBorder = new JCheckBox(s != null ? s : "Border Lines");
		showBorder.setSelected(true);
		showBorder.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (model == null)
					return;
				model.getBoundary().setBorderAlwaysOn(e.getStateChange() == ItemEvent.SELECTED);
				model.getView().repaint();
				model.notifyChange();
			}
		});
		p.add(showBorder);

		s = MDView.getInternationalText("MirrorImages");
		showMirror = new JCheckBox(s != null ? s : "Mirror Images");
		showMirror.setSelected(true);
		showMirror.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (model == null)
					return;
				((MDView) model.getView()).setShowMirrorImages(e.getStateChange() == ItemEvent.SELECTED);
				model.getView().repaint();
				model.notifyChange();
			}
		});
		p.add(showMirror);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(p, BorderLayout.SOUTH);

		s = MDView.getInternationalText("CloseButton");
		JButton button = new JButton(s != null ? s : "Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		p.add(button);

	}

	void resetBoundaryToElastic(MDModel m) {
		for (byte i = 0; i < 4; i++)
			m.getBoundary().getWall().setElastic(i, true);
	}

	public void setModel(MDModel m) {
		model = m;
	}

	public void setCurrentValues() {
		if (model != null) {
			setButtons(model.getBoundary().getType());
		}
		else {
			setButtons(RectangularBoundary.DBC_ID);
		}
	}

	private void setButtons(int boundaryType) {
		switch (boundaryType) {
		case RectangularBoundary.DBC_ID:
			dbcButton.setSelected(true);
			dbcButton.requestFocus();
			break;
		case RectangularBoundary.PBC_ID:
			pbcButton.setSelected(true);
			pbcButton.requestFocus();
			break;
		case RectangularBoundary.RBC_ID:
			rbcButton.setSelected(true);
			rbcButton.requestFocus();
			break;
		case RectangularBoundary.XRYPBC_ID:
			xrypbcButton.setSelected(true);
			xrypbcButton.requestFocus();
			break;
		case RectangularBoundary.XPYRBC_ID:
			xpyrbcButton.setSelected(true);
			xpyrbcButton.requestFocus();
			break;
		}
		showBorder.setSelected(model.getBoundary().borderIsAlwaysOn());
		showMirror.setSelected(((MDView) model.getView()).getShowMirrorImages());
	}

}