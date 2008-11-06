/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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

/**
 * @author Charles Xie
 *
 */
package org.concord.mw3d;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.mw3d.models.Atom;
import org.concord.mw3d.models.MolecularModel;
import org.myjmol.api.JmolViewer;

class GenericParticlePropertiesPanel extends PropertiesPanel {

	private MolecularModel model;
	private JDialog dialog;
	private FloatNumberTextField[] massField, sigmaField, epsilonField;
	private ColorComboBox[] colorComboBox;
	private int[] originalColor;

	void destroy() {
		if (dialog != null)
			dialog.dispose();
	}

	GenericParticlePropertiesPanel(MolecularModel m) {

		super(new BorderLayout(5, 5));

		model = m;

		final byte n = MolecularModel.getGenericParticleTypes();
		massField = new FloatNumberTextField[n];
		sigmaField = new FloatNumberTextField[n];
		epsilonField = new FloatNumberTextField[n];
		colorComboBox = new ColorComboBox[n];
		originalColor = new int[n];
		originalColor[0] = model.getView().getElementArgb("X1");
		originalColor[1] = model.getView().getElementArgb("X2");
		originalColor[2] = model.getView().getElementArgb("X3");
		originalColor[3] = model.getView().getElementArgb("X4");

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(panel, BorderLayout.SOUTH);

		String s = MolecularContainer.getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});
		panel.add(button);

		s = MolecularContainer.getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
		panel.add(button);

		panel = new JPanel(new SpringLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(10, 10, 10, 10)));
		add(panel, BorderLayout.NORTH);

		// add the header row
		s = MolecularContainer.getInternationalText("Name");
		panel.add(new JLabel(s != null ? s : "Name"), SwingConstants.CENTER);

		s = MolecularContainer.getInternationalText("Mass");
		panel.add(new JLabel((s != null ? s : "Mass") + " (g/mol)", SwingConstants.CENTER));

		panel.add(new JLabel("<html><i>&#963;</i> (&#197;)</html>", SwingConstants.CENTER));

		panel.add(new JLabel("<html><i>&#949;</i> (eV)</html>", SwingConstants.CENTER));

		s = MolecularContainer.getInternationalText("Color");
		panel.add(new JLabel(s != null ? s : "Color", SwingConstants.CENTER));

		createPanel("X1", panel);
		createPanel("X2", panel);
		createPanel("X3", panel);
		createPanel("X4", panel);

		makeCompactGrid(panel, 5, 5, 5, 5, 10, 2);

	}

	private void ok() {
		setElement("X1");
		setElement("X2");
		setElement("X3");
		setElement("X4");
		destroy();
	}

	private void setElement(String symbol) {
		setMass(symbol, getMassField(symbol).getValue());
		setSigma(symbol, getSigmaField(symbol).getValue());
		setEpsilon(symbol, getEpsilonField(symbol).getValue());
	}

	private void cancel() {
		model.getView().setElementColor("X1", new Color(originalColor[0]));
		model.getView().setElementColor("X2", new Color(originalColor[1]));
		model.getView().setElementColor("X3", new Color(originalColor[2]));
		model.getView().setElementColor("X4", new Color(originalColor[3]));
		model.getView().repaint();
		destroy();
	}

	private FloatNumberTextField getMassField(String symbol) {
		if ("X1".equals(symbol))
			return massField[0];
		if ("X2".equals(symbol))
			return massField[1];
		if ("X3".equals(symbol))
			return massField[2];
		if ("X4".equals(symbol))
			return massField[3];
		return null;
	}

	private FloatNumberTextField getSigmaField(String symbol) {
		if ("X1".equals(symbol))
			return sigmaField[0];
		if ("X2".equals(symbol))
			return sigmaField[1];
		if ("X3".equals(symbol))
			return sigmaField[2];
		if ("X4".equals(symbol))
			return sigmaField[3];
		return null;
	}

	private FloatNumberTextField getEpsilonField(String symbol) {
		if ("X1".equals(symbol))
			return epsilonField[0];
		if ("X2".equals(symbol))
			return epsilonField[1];
		if ("X3".equals(symbol))
			return epsilonField[2];
		if ("X4".equals(symbol))
			return epsilonField[3];
		return null;
	}

	private void createPanel(String symbol, JPanel panel) {
		int i = -1;
		if ("X1".equals(symbol))
			i = 0;
		else if ("X2".equals(symbol))
			i = 1;
		else if ("X3".equals(symbol))
			i = 2;
		else if ("X4".equals(symbol))
			i = 3;
		if (i == -1)
			return;
		panel.add(new JLabel(symbol));
		massField[i] = new FloatNumberTextField(model.getElementMass(symbol), 1, 10000, 10);
		panel.add(massField[i]);
		sigmaField[i] = new FloatNumberTextField(model.getElementSigma(symbol), 1, 100, 10);
		panel.add(sigmaField[i]);
		epsilonField[i] = new FloatNumberTextField(model.getElementEpsilon(symbol), 0, 10, 10);
		panel.add(epsilonField[i]);
		MolecularView view = model.getView();
		colorComboBox[i] = new ColorComboBox(view);
		colorComboBox[i].setRenderer(new ComboBoxRenderer.ColorCell(new Color(view.getElementArgb(symbol))));
		colorComboBox[i].setPreferredSize(new Dimension(100, 24));
		setColorComboBox(colorComboBox[i], new Color(view.getElementArgb(symbol)));
		colorComboBox[i].addActionListener(new ElementColorListener(symbol, model));
		panel.add(colorComboBox[i]);
	}

	private void setMass(String symbol, float value) {
		int n = model.getAtomCount();
		for (int i = 0; i < n; i++) {
			Atom a = model.getAtom(i);
			if (a.getSymbol().equals(symbol))
				a.setMass(value);
		}
		model.setElementMass(symbol, value);
	}

	private void setSigma(String symbol, float value) {
		MolecularView v = model.getView();
		JmolViewer viewer = v.getViewer();
		int n = model.getAtomCount();
		for (int i = 0; i < n; i++) {
			Atom a = model.getAtom(i);
			if (a.getSymbol().equals(symbol)) {
				a.setSigma(value);
				viewer.setAtomSize(i, value * 1000);
			}
		}
		model.setElementSigma(symbol, value);
		v.repaint();
	}

	private void setEpsilon(String symbol, float value) {
		int n = model.getAtomCount();
		for (int i = 0; i < n; i++) {
			Atom a = model.getAtom(i);
			if (a.getSymbol().equals(symbol))
				a.setEpsilon(value);
		}
		model.setElementEpsilon(symbol, value);
	}

	void setDialog(JDialog d) {
		dialog = d;
	}

	void windowActivated() {
	}

}