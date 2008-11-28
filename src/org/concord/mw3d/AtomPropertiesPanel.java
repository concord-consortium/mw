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

/**
 * @author Charles Xie
 *
 */
package org.concord.mw3d;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.HyperlinkLabel;
import org.concord.modeler.util.DataQueueUtilities;
import org.concord.mw3d.models.Atom;
import org.concord.mw3d.models.MolecularModel;
import org.concord.mw3d.models.Restraint;
import org.myjmol.api.JmolViewer;

class AtomPropertiesPanel extends PropertiesPanel {

	private JDialog dialog;

	private JLabel nameLabel, indexLabel;
	private FloatNumberTextField massField, sigmaField, epsilonField;
	private FloatNumberTextField rxField, ryField, rzField, vxField, vyField, vzField;
	private FloatNumberTextField chargeField, dampField, restraintField;
	private HyperlinkLabel leftXLabel;
	private HyperlinkLabel leftYLabel;
	private HyperlinkLabel leftZLabel;
	private HyperlinkLabel leftVxLabel;
	private HyperlinkLabel leftVyLabel;
	private HyperlinkLabel leftVzLabel;
	private boolean isGenericParticle;

	void destroy() {
		if (dialog != null)
			dialog.dispose();
	}

	AtomPropertiesPanel(final Atom atom) {

		super(new BorderLayout(5, 5));

		isGenericParticle = atom.isGenericParticle();

		nameLabel = createLabel(atom.getSymbol());
		indexLabel = createLabel(atom.getIndex());

		MolecularView view = atom.getModel().getView();
		ColorComboBox ballColorComboBox = new ColorComboBox(view);
		ballColorComboBox.setRenderer(new ComboBoxRenderer.ColorCell(view.getElementColor(atom)));
		ballColorComboBox.setPreferredSize(new Dimension(32, 20));
		setColorComboBox(ballColorComboBox, view.getElementColor(atom));
		ballColorComboBox.addActionListener(new ElementColorListener(atom.getSymbol(), atom.getModel()));

		massField = new FloatNumberTextField(atom.getMass(), 1, 10000, 10);
		sigmaField = new FloatNumberTextField(atom.getSigma(), 1, 100, 10);
		epsilonField = new FloatNumberTextField(atom.getEpsilon(), 0, 10, 10);
		if (!isGenericParticle) {
			massField.setEnabled(false);
			sigmaField.setEnabled(false);
			epsilonField.setEnabled(false);
			ballColorComboBox.setEnabled(false);
		}

		rxField = new FloatNumberTextField(atom.getRx(), -1000, 1000, 10);
		ryField = new FloatNumberTextField(atom.getRy(), -1000, 1000, 10);
		rzField = new FloatNumberTextField(atom.getRz(), -1000, 1000, 10);

		float velo = atom.isMovable() ? 100000 * atom.getVx() : 0;
		vxField = new FloatNumberTextField(velo, -100000, 100000, 10);
		vxField.setEditable(atom.isMovable());

		velo = atom.isMovable() ? 100000 * atom.getVy() : 0;
		vyField = new FloatNumberTextField(velo, -100000, 100000, 10);
		vyField.setEditable(atom.isMovable());

		velo = atom.isMovable() ? 100000 * atom.getVz() : 0;
		vzField = new FloatNumberTextField(velo, -100000, 100000, 10);
		vzField.setEditable(atom.isMovable());

		chargeField = new FloatNumberTextField(atom.getCharge(), -50, 50, 10);
		dampField = new FloatNumberTextField(atom.getDamp(), 0, 10, 10);
		Restraint r = atom.getRestraint();
		restraintField = new FloatNumberTextField(r != null ? r.getStrength() : 0, 0, 1000, 10);

		String s = MolecularContainer.getInternationalText("Movable");
		final JCheckBox movableCheckBox = new JCheckBox(s != null ? s : "Movable");
		movableCheckBox.setToolTipText("Set whether or not this atom will move in the molecular dynamics simulation.");
		movableCheckBox.setSelected(atom.isMovable());
		movableCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean b = movableCheckBox.isSelected();
				atom.setMovable(b);
				setMovable(b);
			}
		});

		s = MolecularContainer.getInternationalText("Visible");
		final JCheckBox visibleCheckBox = new JCheckBox(s != null ? s : "Visible");
		visibleCheckBox.setToolTipText("Set whether or not this atom will be visible.");
		visibleCheckBox.setSelected(atom.isVisible());
		visibleCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean b = visibleCheckBox.isSelected();
				atom.setVisible(b);
				atom.getModel().getView().setVisible(atom, b);
			}
		});

		JButton okButton = new JButton();

		s = MolecularContainer.getInternationalText("Cancel");
		JButton cancelButton = new JButton(s != null ? s : "Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				destroy();
			}
		});

		/* OK listener */

		Action okAction = new ModelAction(atom.getModel(), new Executable() {

			public void execute() {

				applyBounds(chargeField);
				applyBounds(dampField);
				applyBounds(restraintField);
				applyBounds(vxField);
				applyBounds(vyField);

				if (isGenericParticle) {
					applyBounds(massField);
					applyBounds(sigmaField);
					applyBounds(epsilonField);
				}

				boolean changed = false;

				if (Math.abs(atom.getCharge() - chargeField.getValue()) > ZERO) {
					atom.getModel().getView().setCharge(atom.getIndex(), chargeField.getValue());
					changed = true;
				}
				if (Math.abs(atom.getDamp() - dampField.getValue()) > ZERO) {
					atom.setDamp(dampField.getValue());
					changed = true;
				}
				Restraint r = atom.getRestraint();
				if (r != null) {
					if (Math.abs(r.getStrength() - restraintField.getValue()) > ZERO) {
						r.setStrength(restraintField.getValue());
						changed = true;
					}
				}
				else {
					float x = restraintField.getValue();
					if (x > ZERO) {
						r = new Restraint(atom);
						r.setStrength(x);
						changed = true;
					}
				}
				if (Math.abs(atom.getRx() - rxField.getValue()) > ZERO) {
					setRx(atom, rxField.getValue());
					changed = true;
				}
				if (Math.abs(atom.getRy() - ryField.getValue()) > ZERO) {
					setRy(atom, ryField.getValue());
					changed = true;
				}
				if (Math.abs(atom.getRz() - rzField.getValue()) > ZERO) {
					setRz(atom, rzField.getValue());
					changed = true;
				}

				if (Math.abs(atom.getVx() * 100000 - vxField.getValue()) > ZERO) {
					atom.setVx(vxField.getValue() * 0.00001f);
					changed = true;
				}
				if (Math.abs(atom.getVy() * 100000 - vyField.getValue()) > ZERO) {
					atom.setVy(vyField.getValue() * 0.00001f);
					changed = true;
				}
				if (Math.abs(atom.getVz() * 100000 - vzField.getValue()) > ZERO) {
					atom.setVz(vzField.getValue() * 0.00001f);
					changed = true;
				}

				if (isGenericParticle) {
					if (Math.abs(atom.getMass() - massField.getValue()) > ZERO) {
						setMass(atom, massField.getValue());
						changed = true;
					}
					if (Math.abs(atom.getSigma() - sigmaField.getValue()) > ZERO) {
						setSigma(atom, sigmaField.getValue());
						changed = true;
					}
					if (Math.abs(atom.getEpsilon() - epsilonField.getValue()) > ZERO) {
						setEpsilon(atom, epsilonField.getValue());
						changed = true;
					}
				}

				if (changed) {
					atom.getModel().getView().refresh();
					atom.getModel().getView().repaint();
					atom.getModel().notifyChange();
				}

				destroy();

			}
		}) {
		};

		rxField.setAction(okAction);
		ryField.setAction(okAction);
		rzField.setAction(okAction);
		vxField.setAction(okAction);
		vyField.setAction(okAction);
		vzField.setAction(okAction);
		chargeField.setAction(okAction);
		dampField.setAction(okAction);
		restraintField.setAction(okAction);
		okButton.setAction(okAction);
		s = MolecularContainer.getInternationalText("OK");
		okButton.setText(s != null ? s : "OK");

		/* layout components */

		JPanel panel = new JPanel(new SpringLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(10, 10, 10, 10)));
		add(panel, BorderLayout.NORTH);

		// row 1
		s = MolecularContainer.getInternationalText("Name");
		panel.add(new JLabel(s != null ? s : "Name"));
		panel.add(nameLabel);
		panel.add(new JPanel());

		// row 2
		s = MolecularContainer.getInternationalText("Index");
		panel.add(new JLabel(s != null ? s : "Index"));
		panel.add(indexLabel);
		panel.add(new JPanel());

		// row 3
		s = MolecularContainer.getInternationalText("Color");
		panel.add(new JLabel(s != null ? s : "Color"));
		panel.add(ballColorComboBox);
		panel.add(new JPanel());

		// row 4
		s = MolecularContainer.getInternationalText("Mass");
		panel.add(new JLabel(s != null ? s : "Mass"));
		panel.add(massField);
		panel.add(new JLabel("<html>g/mol, <font color=gray>&#10014;</font></html>"));

		// row 5
		panel.add(new JLabel("<html><i>&#963;</i></html>", SwingConstants.LEFT));
		panel.add(sigmaField);
		panel.add(new JLabel("<html>&#197; <font color=gray>&#10014;</font></html>"));

		// row 6
		panel.add(new JLabel("<html><i>&#949;</i></html>", SwingConstants.LEFT));
		panel.add(epsilonField);
		panel.add(new JLabel("<html>eV <font color=gray>&#10014;</font></html>"));

		// row 7
		s = MolecularContainer.getInternationalText("Charge");
		panel.add(new JLabel((s != null ? s : "Charge") + " (e)"));
		panel.add(chargeField);
		panel.add(new JPanel());

		// row 8
		s = MolecularContainer.getInternationalText("Damping");
		panel.add(new JLabel(s != null ? s : "Damping"));
		panel.add(dampField);
		panel.add(new JPanel());

		// row 9
		s = MolecularContainer.getInternationalText("Restraint");
		panel.add(new JLabel(s != null ? s : "Restraint"));
		panel.add(restraintField);
		panel.add(new JPanel());

		// row 10
		leftXLabel = new HyperlinkLabel(
				atom.isMovable() ? "<html><font color=\"#0000ff\"><u><em>X</em></u></font></html>" : "X");
		leftXLabel.setEnabled(atom.isMovable());
		leftXLabel.setToolTipText("Click to view this variable's graph");
		leftXLabel.setAction(new Runnable() {
			public void run() {
				if (atom.getRQ() != null) {
					DataQueueUtilities.show(atom.getRQ().getQueue1(), JOptionPane
							.getFrameForComponent(AtomPropertiesPanel.this));
				}
				else {
					DataQueueUtilities.showNoDataMessage(JOptionPane.getFrameForComponent(AtomPropertiesPanel.this));
				}
			}
		});
		panel.add(leftXLabel);
		panel.add(rxField);
		panel.add(new JLabel("<html>&#197;</html>"));

		// row 11
		leftYLabel = new HyperlinkLabel(
				atom.isMovable() ? "<html><font color=\"#0000ff\"><u><em>Y</em></u></font></html>" : "Y");
		leftYLabel.setEnabled(atom.isMovable());
		leftYLabel.setToolTipText("Click to view this variable's graph");
		leftYLabel.setAction(new Runnable() {
			public void run() {
				if (atom.getRQ() != null) {
					DataQueueUtilities.show(atom.getRQ().getQueue2(), JOptionPane
							.getFrameForComponent(AtomPropertiesPanel.this));
				}
				else {
					DataQueueUtilities.showNoDataMessage(JOptionPane.getFrameForComponent(AtomPropertiesPanel.this));
				}
			}
		});
		panel.add(leftYLabel);
		panel.add(ryField);
		panel.add(new JLabel("<html>&#197;</html>"));

		// row 12
		leftZLabel = new HyperlinkLabel(
				atom.isMovable() ? "<html><font color=\"#0000ff\"><u><em>Z</em></u></font></html>" : "Z");
		leftZLabel.setEnabled(atom.isMovable());
		leftZLabel.setToolTipText("Click to view this variable's graph");
		leftZLabel.setAction(new Runnable() {
			public void run() {
				if (atom.getRQ() != null) {
					DataQueueUtilities.show(atom.getRQ().getQueue3(), JOptionPane
							.getFrameForComponent(AtomPropertiesPanel.this));
				}
				else {
					DataQueueUtilities.showNoDataMessage(JOptionPane.getFrameForComponent(AtomPropertiesPanel.this));
				}
			}
		});
		panel.add(leftZLabel);
		panel.add(rzField);
		panel.add(new JLabel("<html>&#197;</html>"));

		// row 13
		leftVxLabel = new HyperlinkLabel(
				atom.isMovable() ? "<html><font color=\"#0000ff\"><u><em>V<sub>x</sub></em></u></font></html>" : "Vx");
		leftVxLabel.setEnabled(atom.isMovable());
		leftVxLabel.setToolTipText("Click to view this variable's graph");
		leftVxLabel.setAction(new Runnable() {
			public void run() {
				if (atom.getVQ() != null) {
					atom.getVQ().getQueue1().setMultiplier(100000);
					DataQueueUtilities.show(atom.getVQ().getQueue1(), JOptionPane
							.getFrameForComponent(AtomPropertiesPanel.this));
				}
				else {
					DataQueueUtilities.showNoDataMessage(JOptionPane.getFrameForComponent(AtomPropertiesPanel.this));
				}
			}
		});
		panel.add(leftVxLabel);
		panel.add(vxField);
		panel.add(new JLabel("m/s"));

		// row 14
		leftVyLabel = new HyperlinkLabel(
				atom.isMovable() ? "<html><font color=\"#0000ff\"><u><em>V<sub>y</sub></em></u></font></html>" : "Vy");
		leftVyLabel.setEnabled(atom.isMovable());
		leftVyLabel.setToolTipText("Click to view this variable's graph");
		leftVyLabel.setAction(new Runnable() {
			public void run() {
				if (atom.getVQ() != null) {
					atom.getVQ().getQueue2().setMultiplier(100000);
					DataQueueUtilities.show(atom.getVQ().getQueue2(), JOptionPane
							.getFrameForComponent(AtomPropertiesPanel.this));
				}
				else {
					DataQueueUtilities.showNoDataMessage(JOptionPane.getFrameForComponent(AtomPropertiesPanel.this));
				}
			}
		});
		panel.add(leftVyLabel);
		panel.add(vyField);
		panel.add(new JLabel("m/s"));

		// row 15
		leftVzLabel = new HyperlinkLabel(
				atom.isMovable() ? "<html><font color=\"#0000ff\"><u><em>V<sub>z</sub></em></u></font></html>" : "Vz");
		leftVzLabel.setEnabled(atom.isMovable());
		leftVzLabel.setToolTipText("Click to view this variable's graph");
		leftVzLabel.setAction(new Runnable() {
			public void run() {
				if (atom.getVQ() != null) {
					atom.getVQ().getQueue3().setMultiplier(100000);
					DataQueueUtilities.show(atom.getVQ().getQueue3(), JOptionPane
							.getFrameForComponent(AtomPropertiesPanel.this));
				}
				else {
					DataQueueUtilities.showNoDataMessage(JOptionPane.getFrameForComponent(AtomPropertiesPanel.this));
				}
			}
		});
		panel.add(leftVzLabel);
		panel.add(vzField);
		panel.add(new JLabel("m/s"));

		makeCompactGrid(panel, 15, 3, 5, 5, 10, 2);

		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		add(panel, BorderLayout.CENTER);

		s = MolecularContainer.getInternationalText("AppliedToAllParticlesOfThisType");
		panel.add(new JLabel("<html><font color=gray>&#10014; "
				+ (s != null ? s : "Applied to all particles of this type") + ".</font></html>"));

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(panel, BorderLayout.SOUTH);

		panel.add(movableCheckBox);
		panel.add(visibleCheckBox);
		panel.add(okButton);
		panel.add(cancelButton);

	}

	private void setMovable(boolean b) {
		leftXLabel.setEnabled(b);
		leftYLabel.setEnabled(b);
		leftZLabel.setEnabled(b);
		leftXLabel.setText(b ? "<html><font color=\"#0000ff\"><u><em>X</em></u></font></html>" : "X");
		leftYLabel.setText(b ? "<html><font color=\"#0000ff\"><u><em>Y</em></u></font></html>" : "Y");
		leftZLabel.setText(b ? "<html><font color=\"#0000ff\"><u><em>Z</em></u></font></html>" : "Z");
		leftVxLabel.setEnabled(b);
		leftVyLabel.setEnabled(b);
		leftVzLabel.setEnabled(b);
		leftVxLabel.setText(b ? "<html><font color=\"#0000ff\"><u><em>V<sub>x</sub></em></u></font></html>" : "Vx");
		leftVyLabel.setText(b ? "<html><font color=\"#0000ff\"><u><em>V<sub>y</sub></em></u></font></html>" : "Vy");
		leftVzLabel.setText(b ? "<html><font color=\"#0000ff\"><u><em>V<sub>z</sub></em></u></font></html>" : "Vz");
		rxField.setEnabled(b);
		ryField.setEnabled(b);
		rzField.setEnabled(b);
		vxField.setEnabled(b);
		vyField.setEnabled(b);
		vzField.setEnabled(b);
		rxField.setEditable(b);
		ryField.setEditable(b);
		rzField.setEditable(b);
		vxField.setEditable(b);
		vyField.setEditable(b);
		vzField.setEditable(b);
	}

	private static void setRx(Atom a, float x) {
		a.setRx(x);
		updateAtomPosition(a);
	}

	private static void setRy(Atom a, float y) {
		a.setRy(y);
		updateAtomPosition(a);
	}

	private static void setRz(Atom a, float z) {
		a.setRz(z);
		updateAtomPosition(a);
	}

	private static void setMass(Atom atom, float value) {
		MolecularModel model = atom.getModel();
		int n = model.getAtomCount();
		for (int i = 0; i < n; i++) {
			Atom a = model.getAtom(i);
			if (a.getElementNumber() == atom.getElementNumber())
				a.setMass(value);
		}
		if (atom.isGenericParticle())
			model.setElementMass(atom.getSymbol(), value);
	}

	private static void setSigma(Atom atom, float value) {
		MolecularModel m = atom.getModel();
		MolecularView v = m.getView();
		JmolViewer viewer = v.getViewer();
		int n = m.getAtomCount();
		for (int i = 0; i < n; i++) {
			Atom a = m.getAtom(i);
			if (a.getElementNumber() == atom.getElementNumber()) {
				a.setSigma(value);
				viewer.setAtomSize(i, value * 1000);
			}
		}
		if (atom.isGenericParticle())
			m.setElementSigma(atom.getSymbol(), value);
		v.repaint();
	}

	private static void setEpsilon(Atom atom, float value) {
		MolecularModel m = atom.getModel();
		int n = m.getAtomCount();
		for (int i = 0; i < n; i++) {
			Atom a = m.getAtom(i);
			if (a.getElementNumber() == atom.getElementNumber())
				a.setEpsilon(value);
		}
		if (atom.isGenericParticle())
			m.setElementEpsilon(atom.getSymbol(), value);
	}

	private static void updateAtomPosition(Atom a) {
		MolecularView v = a.getModel().getView();
		JmolViewer viewer = v.getViewer();
		viewer.setAtomCoordinates(a.getModel().getAtomIndex(a), a.getRx(), a.getRy(), a.getRz());
	}

	void setDialog(JDialog d) {
		dialog = d;
	}

	void windowActivated() {
	}

}