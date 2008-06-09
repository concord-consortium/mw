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

package org.concord.mw3d;

import java.awt.BorderLayout;
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
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.HyperlinkLabel;
import org.concord.modeler.util.DataQueueUtilities;
import org.concord.mw3d.models.Atom;
import org.myjmol.api.JmolViewer;

class AtomPropertiesPanel extends PropertiesPanel {

	private JDialog dialog;

	private JLabel nameLabel;
	private JLabel massLabel;
	private JLabel indexLabel;
	private JLabel sigmaLabel;
	private JLabel epsilonLabel;
	private FloatNumberTextField chargeField, rxField, ryField, rzField, vxField, vyField, vzField;
	private HyperlinkLabel leftXLabel;
	private HyperlinkLabel leftYLabel;
	private HyperlinkLabel leftZLabel;
	private HyperlinkLabel leftVxLabel;
	private HyperlinkLabel leftVyLabel;
	private HyperlinkLabel leftVzLabel;

	void destroy() {
		if (dialog != null)
			dialog.dispose();
	}

	AtomPropertiesPanel(final Atom atom) {

		super(new BorderLayout(5, 5));

		massLabel = createLabel(atom.getMass());
		sigmaLabel = createLabel(atom.getSigma());
		epsilonLabel = createLabel(atom.getEpsilon());
		nameLabel = createLabel(atom.getSymbol());
		indexLabel = createLabel(atom.getIndex());
		rxField = new FloatNumberTextField(atom.getRx(), -100, 100, 10);
		ryField = new FloatNumberTextField(atom.getRy(), -100, 100, 10);
		rzField = new FloatNumberTextField(atom.getRz(), -100, 100, 10);

		float velo = atom.isMovable() ? 100000 * atom.getVx() : 0;
		vxField = new FloatNumberTextField(velo, -10000, 10000, 10);
		vxField.setEditable(atom.isMovable());

		velo = atom.isMovable() ? 100000 * atom.getVy() : 0;
		vyField = new FloatNumberTextField(velo, -10000, 10000, 10);
		vyField.setEditable(atom.isMovable());

		velo = atom.isMovable() ? 100000 * atom.getVz() : 0;
		vzField = new FloatNumberTextField(velo, -10000, 10000, 10);
		vzField.setEditable(atom.isMovable());

		chargeField = new FloatNumberTextField(atom.getCharge(), -5, 5, 10);

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
				applyBounds(vxField);
				applyBounds(vyField);

				boolean changed = false;

				if (Math.abs(atom.getCharge() - chargeField.getValue()) > ZERO) {
					atom.getModel().getView().setCharge(atom.getIndex(), chargeField.getValue());
					changed = true;
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
		okButton.setAction(okAction);
		s = MolecularContainer.getInternationalText("OK");
		okButton.setText(s != null ? s : "OK");

		/* layout components */

		JPanel panel = new JPanel(new SpringLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(10, 10, 10, 10)));
		add(panel, BorderLayout.CENTER);

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
		s = MolecularContainer.getInternationalText("Mass");
		panel.add(new JLabel(s != null ? s : "Mass"));
		panel.add(massLabel);
		panel.add(createSmallerFontLabel("g/mol"));

		// row 4
		panel.add(new JLabel("<html><i>&#963;</i> (&#197;)</html>", SwingConstants.LEFT));
		panel.add(sigmaLabel);
		panel.add(new JPanel());

		// row 5
		panel.add(new JLabel("<html><i>&#949;</i> (eV)</html>", SwingConstants.LEFT));
		panel.add(epsilonLabel);
		panel.add(new JPanel());

		// row 6
		s = MolecularContainer.getInternationalText("Charge");
		panel.add(new JLabel((s != null ? s : "Charge") + " (e)"));
		panel.add(chargeField);
		panel.add(new JPanel());

		// row 7
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
		panel.add(createSmallerFontLabel("<html>&#197;</html>"));

		// row 8
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
		panel.add(createSmallerFontLabel("<html>&#197;</html>"));

		// row 9
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
		panel.add(createSmallerFontLabel("<html>&#197;</html>"));

		// row 10
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
		panel.add(createSmallerFontLabel("m/s"));

		// row 11
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
		panel.add(createSmallerFontLabel("m/s"));

		// row 12
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
		panel.add(createSmallerFontLabel("m/s"));

		makeCompactGrid(panel, 12, 3, 5, 5, 10, 2);

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(panel, BorderLayout.SOUTH);

		panel.add(movableCheckBox);
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

	private void setRx(Atom a, float x) {
		a.setRx(x);
		updateAtomPosition(a);
	}

	private void setRy(Atom a, float y) {
		a.setRy(y);
		updateAtomPosition(a);
	}

	private void setRz(Atom a, float z) {
		a.setRz(z);
		updateAtomPosition(a);
	}

	private void updateAtomPosition(Atom a) {
		MolecularView v = a.getModel().getView();
		JmolViewer viewer = v.getViewer();
		viewer.setAtomCoordinates(a.getModel().getAtomIndex(a), a.getRx(), a.getRy(), a.getRz());
		v.repaint();
	}

	void setDialog(JDialog d) {
		dialog = d;
	}

	void windowActivated() {
	}

}