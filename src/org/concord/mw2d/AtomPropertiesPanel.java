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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.event.ModelEvent;
import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.HyperlinkLabel;
import org.concord.modeler.ui.RealNumberTextField;
import org.concord.modeler.ui.RestrictedTextField;
import org.concord.modeler.util.DataQueueUtilities;
import org.concord.molbio.engine.Aminoacid;
import org.concord.molbio.engine.Nucleotide;
import org.concord.mw2d.models.Atom;
import org.concord.mw2d.models.AtomicModel;
import org.concord.mw2d.models.Codon;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.Molecule;
import org.concord.mw2d.models.PointRestraint;
import org.concord.mw2d.models.ReactionModel;
import org.concord.mw2d.models.RectangularBoundary;

class AtomPropertiesPanel extends PropertiesPanel {

	private final static char[] NUCLEOTIDE_CHARS = new char[] { 'C', 'G', 'A', 'T', 'U' };

	private JDialog dialog;
	private JLabel typeLabel;
	private JLabel nameLabel;
	private JLabel nameLabel2;
	private JLabel sigmaLabel, epsilonLabel;
	private RealNumberTextField massField;
	private RealNumberTextField chargeField;
	private RealNumberTextField frictionField;
	private RealNumberTextField springField;
	private RealNumberTextField rxField;
	private RealNumberTextField ryField;
	private RealNumberTextField vxField;
	private RealNumberTextField vyField;
	private FloatNumberTextField hxField;
	private FloatNumberTextField hyField;
	private HyperlinkLabel leftXLabel;
	private HyperlinkLabel leftYLabel;
	private HyperlinkLabel leftVxLabel;
	private HyperlinkLabel leftVyLabel;
	private JTextField codonField;
	private Atom atom;

	void destroy() {
		if (dialog != null)
			dialog.dispose();
	}

	AtomPropertiesPanel(Atom a) {

		super(new BorderLayout(5, 5));

		atom = a;

		char[] codon = null;
		if (atom.isAminoAcid()) {
			codon = atom.getCodon().toCharArray();
			atom.setName("" + Codon.expressFromDNA(codon).getAbbreviation());
		}

		nameLabel = createLabel(atom.getName() + " (element " + atom.getID() + ")");

		String s = MDView.getInternationalText("Atom");
		String type = s != null ? s : "Atom";
		if (atom.getName().equals("Sp")) {
			s = MDView.getInternationalText("SugarPhosphate");
			type = s != null ? s : "Sugar-Phosphate";
		}
		else if (atom.isAminoAcid()) {
			s = MDView.getInternationalText("AminoAcid");
			type = s != null ? s : "Amino Acid";
		}
		else if (atom.isNucleotide()) {
			s = MDView.getInternationalText("Nucleotide");
			type = s != null ? s : "Nucleotide";
		}
		typeLabel = createLabel(type);

		type = "Editable element";
		if (atom.getName().equals("Sp")) {
			type = "";
		}
		else if (atom.isAminoAcid()) {
			type = Codon.expressFromDNA(codon).getFullName();
		}
		else if (atom.isNucleotide()) {
			type = Nucleotide.getNucleotide(atom.getName().charAt(0)).getFullName();
		}
		nameLabel2 = createSmallerFontLabel(type);

		RectangularBoundary boundary = atom.getHostModel().getBoundary();
		rxField = new RealNumberTextField(0.1 * atom.getRx(), 0.1 * boundary.getX(), 0.1 * boundary.getWidth(), 10);
		ryField = new RealNumberTextField(0.1 * atom.getRy(), 0.1 * boundary.getX(), 0.1 * boundary.getHeight(), 10);

		double velo = atom.isMovable() ? 10000 * atom.getVx() : 0;
		vxField = new RealNumberTextField(velo, -10000, 10000, 10);
		vxField.setEditable(atom.isMovable());

		velo = atom.isMovable() ? 10000 * atom.getVy() : 0;
		vyField = new RealNumberTextField(velo, -10000, 10000, 10);
		vyField.setEditable(atom.isMovable());

		float hval = atom.isMovable() ? 10 * atom.getHx() : 0;
		hxField = new FloatNumberTextField(hval, -10, 10, 10);
		hxField.setEditable(atom.isMovable());

		hval = atom.isMovable() ? 10 * atom.getHy() : 0;
		hyField = new FloatNumberTextField(hval, -10, 10, 10);
		hyField.setEditable(atom.isMovable());

		massField = new RealNumberTextField(atom.getMass() * 120, 1, 1000000.0, 10);
		massField.setEnabled(!atom.isAminoAcid());

		chargeField = new RealNumberTextField(atom.getCharge(), -50.0, 50.0, 10);
		frictionField = new RealNumberTextField(atom.getFriction(), 0.0, 100.0, 10);
		springField = new RealNumberTextField(atom.getRestraint() == null ? 0.0f : atom.getRestraint().getK() * 100,
				0.0, 100000.0, 10);

		if (atom.isAminoAcid()) {
			codonField = new RestrictedTextField(NUCLEOTIDE_CHARS, 3);
			if (codon != null)
				codonField.setText(new String(codon));
		}

		final boolean b = !atom.isAminoAcid() && !atom.isNucleotide() && atom.getID() != Element.ID_MO;

		s = MDView.getInternationalText("Visible");
		final JCheckBox visibleCheckBox = new JCheckBox(s != null ? s : "Visible");
		visibleCheckBox.setToolTipText("Set whether or not this atom will be visible.");
		visibleCheckBox.setSelected(atom.isVisible());
		visibleCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				atom.setVisible(visibleCheckBox.isSelected());
				((AtomisticView) atom.getHostModel().getView()).refreshJmol();
				atom.getHostModel().getView().repaint();
			}
		});
		visibleCheckBox.setEnabled(b);

		s = MDView.getInternationalText("Movable");
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
		movableCheckBox.setEnabled(b);

		JButton okButton = new JButton();

		s = MDView.getInternationalText("CancelButton");
		JButton cancelButton = new JButton(s != null ? s : "Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				destroy();
			}
		});

		/* OK listener */

		Action okAction = new ModelAction(atom.getHostModel(), new Executable() {

			public void execute() {

				applyBounds(massField);
				applyBounds(chargeField);
				applyBounds(frictionField);
				applyBounds(springField);
				applyBounds(vxField);
				applyBounds(vyField);
				applyBounds(hxField);
				applyBounds(hyField);

				boolean changed = false;

				if (Math.abs(atom.getMass() * 120 - massField.getValue()) > ZERO) {
					JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(atom.getHostModel().getView()),
							"The masses of all atoms of this type will be set to " + massField.getText() + " g/mol.",
							"Setting mass", JOptionPane.INFORMATION_MESSAGE);
					((AtomicModel) atom.getHostModel()).getElement(atom.getID()).setMass(massField.getValue() / 120);
					changed = true;
				}

				if (Math.abs(atom.getCharge() - chargeField.getValue()) > ZERO) {
					atom.setCharge(chargeField.getValue());
					changed = true;
				}

				if (Math.abs(atom.getFriction() - frictionField.getValue()) > ZERO) {
					atom.setFriction((float) frictionField.getValue());
					changed = true;
				}

				if (springField.getValue() > ZERO) {
					double v = springField.getValue() * 0.01;
					if (atom.getRestraint() == null) {
						atom.setRestraint(new PointRestraint(v, atom.getRx(), atom.getRy()));
						changed = true;
					}
					else {
						if (Math.abs(atom.getRestraint().getK() - v) > ZERO) {
							atom.getRestraint().setK(v);
							changed = true;
						}
					}
				}
				else {
					if (atom.getRestraint() != null) {
						atom.setRestraint(null);
						changed = true;
					}
				}

				if (Math.abs(atom.getRx() * 0.1 - rxField.getValue()) > ZERO) {
					atom.setRx(rxField.getValue() * 10);
					changed = true;
				}

				if (Math.abs(atom.getRy() * 0.1 - ryField.getValue()) > ZERO) {
					atom.setRy(ryField.getValue() * 10);
					changed = true;
				}

				if (Math.abs(atom.getVx() * 10000 - vxField.getValue()) > ZERO) {
					atom.setVx(vxField.getValue() * 0.0001);
					changed = true;
				}

				if (Math.abs(atom.getVy() * 10000 - vyField.getValue()) > ZERO) {
					atom.setVy(vyField.getValue() * 0.0001);
					changed = true;
				}

				if (Math.abs(atom.getHx() * 10 - hxField.getValue()) > ZERO) {
					atom.setHx(hxField.getValue() * 0.1f);
					changed = true;
				}

				if (Math.abs(atom.getHy() * 10 - hyField.getValue()) > ZERO) {
					atom.setHy(hyField.getValue() * 0.1f);
					changed = true;
				}

				if (atom.isAminoAcid()) {
					final String s = codonField.getText();
					if (s.trim().equals("") || s.length() != 3) {
						JOptionPane.showMessageDialog(dialog, "A codon must contain 3 letters.");
						codonField.setText(atom.getCodon());
						codonField.requestFocusInWindow();
					}
					else {
						char[] c = s.toCharArray();
						if (Codon.isStopCodon(c)) {
							JOptionPane.showMessageDialog(dialog, s
									+ " is a STOP codon, which cannot be assigned to an amino acid.");
							codonField.setText(atom.getCodon());
							codonField.requestFocusInWindow();
						}
						else {
							if (!s.equalsIgnoreCase(atom.getCodon())) {
								Aminoacid aa = Codon.expressFromDNA(c);
								if (aa != null) {
									MolecularModel mm = (MolecularModel) atom.getHostModel();
									atom.setElement(mm.getElement(aa.getAbbreviation()));
									atom.setCodon(s);
									massField.setValue(atom.getMass() * 120);
									chargeField.setValue(atom.getCharge());
									Molecule m = mm.getMolecules().getMolecule(atom);
									typeLabel.setText("Amino Acid");
									nameLabel.setText(atom.getName());
									nameLabel2.setText(aa.getFullName());
									if (m != null)
										atom.getHostModel().notifyModelListeners(
												new ModelEvent(atom, "Selected index", null, new Integer(m
														.indexOfAtom(atom))));
									atom.getHostModel().getView().paintImmediately(atom.getBounds(10));
									changed = true;
								}
							}
						}
					}
				}

				if (changed)
					atom.getHostModel().notifyChange();
				destroy();

			}
		}) {
		};

		rxField.setAction(okAction);
		ryField.setAction(okAction);
		vxField.setAction(okAction);
		vyField.setAction(okAction);
		hxField.setAction(okAction);
		hyField.setAction(okAction);
		massField.setAction(okAction);
		chargeField.setAction(okAction);
		springField.setAction(okAction);
		frictionField.setAction(okAction);
		if (codonField != null)
			codonField.setAction(okAction);
		okButton.setAction(okAction);
		s = MDView.getInternationalText("OKButton");
		okButton.setText(s != null ? s : "OK");

		/* layout components */

		JPanel panel = new JPanel(new SpringLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(10, 10, 10, 10)));
		add(panel, BorderLayout.CENTER);

		s = MDView.getInternationalText("ObjectTypeLabel");
		panel.add(new JLabel(s != null ? s : "Object type"));
		panel.add(typeLabel);
		panel.add(createSmallerFontLabel(atom.isBonded() ? "Bonded" : "Non-bonded"));

		s = MDView.getInternationalText("NameLabel");
		panel.add(new JLabel(s != null ? s : "Name"));
		panel.add(nameLabel);
		panel.add(nameLabel2);

		s = MDView.getInternationalText("IndexLabel");
		panel.add(new JLabel(s != null ? s : "Index"));
		panel.add(createLabel(atom.getIndex()));
		panel.add(createSmallerFontLabel(atom.getHostModel() instanceof ReactionModel ? (atom.isRadical() ? "Radical"
				: "") : ""));

		s = MDView.getInternationalText("Color");
		panel.add(new JLabel(s != null ? s : "Element color"));
		ColorComboBox ballColorComboBox = new ColorComboBox(atom.getHostModel().getView());
		ballColorComboBox.setRenderer(new ComboBoxRenderer.ColorCell(atom.getColor()));
		ballColorComboBox.setPreferredSize(new Dimension(32, 20));
		setColorComboBox(ballColorComboBox, atom.getColor());
		ballColorComboBox.addActionListener(new ElementColorListener(atom));
		panel.add(ballColorComboBox);
		panel.add(new JPanel());

		if (atom.isAminoAcid()) {
			s = MDView.getInternationalText("Codon");
			panel.add(new JLabel(s != null ? s : "Codon (3 char)"));
			panel.add(codonField);
			panel.add(new JPanel());
		}

		s = MDView.getInternationalText("MassLabel");
		panel.add(new JLabel(s != null ? s : "Mass"));
		panel.add(massField);
		panel.add(createSmallerFontLabel("g/mol"));

		panel.add(new JLabel("<html><i>&#963;</i> (&#197;)</html>", SwingConstants.LEFT));
		sigmaLabel = createLabel(0.1 * atom.getSigma());
		panel.add(sigmaLabel);
		JButton button = new JButton(((AtomisticView) atom.getHostModel().getView()).editElements(atom.getID()));
		s = MDView.getInternationalText("ChangeButton");
		if (s != null)
			button.setText(s);
		button.setEnabled(b);
		panel.add(button);

		panel.add(new JLabel("<html><i>&#949;</i> (eV)</html>", SwingConstants.LEFT));
		epsilonLabel = createLabel(atom.getEpsilon());
		panel.add(epsilonLabel);
		button = new JButton(((AtomisticView) atom.getHostModel().getView()).editElements(atom.getID()));
		if (s != null)
			button.setText(s);
		button.setEnabled(b);
		panel.add(button);

		s = MDView.getInternationalText("ChargeLabel");
		panel.add(new JLabel((s != null ? s : "Charge") + " (e)"));
		panel.add(chargeField);
		ColorComboBox chargeColorComboBox = new ColorComboBox(atom.getHostModel().getView());
		chargeColorComboBox.setRenderer(new ComboBoxRenderer.ColorCell(atom.getChargeColor()));
		chargeColorComboBox.setPreferredSize(new Dimension(32, 18));
		setColorComboBox(chargeColorComboBox, atom.getChargeColor());
		chargeColorComboBox.addActionListener(new ChargeColorListener(atom));
		panel.add(chargeColorComboBox);

		s = MDView.getInternationalText("RestraintLabel");
		panel.add(new JLabel(s != null ? s : "Restraint"));
		panel.add(springField);
		panel.add(createSmallerFontLabel("<html>eV/&#197;<sup>2</sup></html>"));

		s = MDView.getInternationalText("DampingLabel");
		panel.add(new JLabel(s != null ? s : "Damping"));
		panel.add(frictionField);
		panel.add(createSmallerFontLabel("<html>eV*fs/&#197;<sup>2</sup></html>"));

		s = MDView.getInternationalText("ExternalForce");
		panel.add(new JLabel((s != null ? s : "External force") + "-x"));
		panel.add(hxField);
		panel.add(createSmallerFontLabel("<html>eV/&#197;</html>"));

		panel.add(new JLabel((s != null ? s : "External force") + "-y"));
		panel.add(hyField);
		panel.add(createSmallerFontLabel("<html>eV/&#197;</html>"));

		leftXLabel = new HyperlinkLabel(
				atom.isMovable() ? "<html><font color=\"#0000ff\"><u><em>X</em></u></font></html>" : "X");
		leftXLabel.setEnabled(atom.isMovable());
		leftXLabel.setToolTipText("Click to view this variable's graph");
		leftXLabel.setAction(new Runnable() {
			public void run() {
				if (atom.getRxRyQueue() != null) {
					atom.getRxRyQueue().getQueue1().setMultiplier(0.1f);
					DataQueueUtilities.show(atom.getRxRyQueue().getQueue1(), JOptionPane
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

		leftYLabel = new HyperlinkLabel(
				atom.isMovable() ? "<html><font color=\"#0000ff\"><u><em>Y</em></u></font></html>" : "Y");
		leftYLabel.setEnabled(atom.isMovable());
		leftYLabel.setToolTipText("Click to view this variable's graph");
		leftYLabel.setAction(new Runnable() {
			public void run() {
				if (atom.getRxRyQueue() != null) {
					atom.getRxRyQueue().getQueue2().setMultiplier(0.1f);
					DataQueueUtilities.show(atom.getRxRyQueue().getQueue2(), JOptionPane
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

		leftVxLabel = new HyperlinkLabel(
				atom.isMovable() ? "<html><font color=\"#0000ff\"><u><em>V<sub>x</sub></em></u></font></html>" : "Vx");
		leftVxLabel.setEnabled(atom.isMovable());
		leftVxLabel.setToolTipText("Click to view this variable's graph");
		leftVxLabel.setAction(new Runnable() {
			public void run() {
				if (atom.getVxVyQueue() != null) {
					atom.getVxVyQueue().getQueue1().setMultiplier(10000);
					DataQueueUtilities.show(atom.getVxVyQueue().getQueue1(), JOptionPane
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

		leftVyLabel = new HyperlinkLabel(
				atom.isMovable() ? "<html><font color=\"#0000ff\"><u><em>V<sub>y</sub></em></u></font></html>" : "Vy");
		leftVyLabel.setEnabled(atom.isMovable());
		leftVyLabel.setToolTipText("Click to view this variable's graph");
		leftVyLabel.setAction(new Runnable() {
			public void run() {
				if (atom.getVxVyQueue() != null) {
					atom.getVxVyQueue().getQueue2().setMultiplier(10000);
					DataQueueUtilities.show(atom.getVxVyQueue().getQueue2(), JOptionPane
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

		HyperlinkLabel hLabel = new HyperlinkLabel(
				atom.isMovable() ? "<html><font color=\"#0000ff\"><u><em>A<sub>x</sub></em></u></font></html>" : "Ax");
		hLabel.setEnabled(atom.isMovable());
		hLabel.setToolTipText("Click to view this variable's graph");
		hLabel.setAction(new Runnable() {
			public void run() {
				if (atom.getAxAyQueue() != null) {
					atom.getAxAyQueue().getQueue1().setMultiplier(0.1f);
					DataQueueUtilities.show(atom.getAxAyQueue().getQueue1(), JOptionPane
							.getFrameForComponent(AtomPropertiesPanel.this));
				}
				else {
					DataQueueUtilities.showNoDataMessage(JOptionPane.getFrameForComponent(AtomPropertiesPanel.this));
				}
			}
		});
		panel.add(hLabel);
		JLabel label = createLabel2(0.1 * atom.getAx());
		label.setEnabled(atom.isMovable());
		panel.add(label);
		panel.add(createSmallerFontLabel("<html>&#197;/fs<sup>2</sup></html>"));

		hLabel = new HyperlinkLabel(
				atom.isMovable() ? "<html><font color=\"#0000ff\"><u><em>A<sub>y</sub></em></u></font></html>" : "Ay");
		hLabel.setEnabled(atom.isMovable());
		hLabel.setToolTipText("Click to view this variable's graph");
		hLabel.setAction(new Runnable() {
			public void run() {
				if (atom.getAxAyQueue() != null) {
					atom.getAxAyQueue().getQueue2().setMultiplier(0.1f);
					DataQueueUtilities.show(atom.getAxAyQueue().getQueue2(), JOptionPane
							.getFrameForComponent(AtomPropertiesPanel.this));
				}
				else {
					DataQueueUtilities.showNoDataMessage(JOptionPane.getFrameForComponent(AtomPropertiesPanel.this));
				}
			}
		});
		panel.add(hLabel);
		label = createLabel2(0.1 * atom.getAy());
		label.setEnabled(atom.isMovable());
		panel.add(label);
		panel.add(createSmallerFontLabel("<html>&#197;/fs<sup>2</sup></html>"));

		makeCompactGrid(panel, atom.isAminoAcid() ? 19 : 18, 3, 5, 5, 10, 2);

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(panel, BorderLayout.SOUTH);

		panel.add(movableCheckBox);
		panel.add(visibleCheckBox);
		if (Modeler.isMac()) {
			panel.add(cancelButton);
			panel.add(okButton);
		}
		else {
			panel.add(okButton);
			panel.add(cancelButton);
		}

	}

	private void setMovable(boolean b) {
		leftXLabel.setEnabled(b);
		leftYLabel.setEnabled(b);
		leftXLabel.setText(b ? "<html><font color=\"#0000ff\"><u><em>X</em></u></font></html>" : "X");
		leftYLabel.setText(b ? "<html><font color=\"#0000ff\"><u><em>Y</em></u></font></html>" : "Y");
		leftVxLabel.setEnabled(b);
		leftVyLabel.setEnabled(b);
		leftVxLabel.setText(b ? "<html><font color=\"#0000ff\"><u><em>V<sub>x</sub></em></u></font></html>" : "Vx");
		leftVyLabel.setText(b ? "<html><font color=\"#0000ff\"><u><em>V<sub>y</sub></em></u></font></html>" : "Vy");
		rxField.setEnabled(b);
		ryField.setEnabled(b);
		vxField.setEnabled(b);
		vyField.setEnabled(b);
		rxField.setEditable(b);
		ryField.setEditable(b);
		vxField.setEditable(b);
		vyField.setEditable(b);
	}

	void setDialog(JDialog d) {
		dialog = d;
	}

	void windowActivated() {
		sigmaLabel.setText(DECIMAL_FORMAT.format(atom.getSigma() * 0.1));
		epsilonLabel.setText(DECIMAL_FORMAT.format(atom.getEpsilon()));
	}

	static class ElementColorListener implements ActionListener {

		private Color color6 = Color.white;
		private Atom atom;
		private AtomisticView view;

		ElementColorListener(Atom atom) {
			this.atom = atom;
			view = (AtomisticView) atom.getHostModel().getView();
		}

		public void actionPerformed(ActionEvent e) {
			final JComboBox cb = (JComboBox) e.getSource();
			int id = (Integer) cb.getSelectedItem();
			if (id == ColorComboBox.INDEX_COLOR_CHOOSER) {
				String s = MDView.getInternationalText("MoreColors");
				JColorChooser.createDialog(view, s != null ? s : "More Colors", true, ModelerUtilities.colorChooser,
						new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								color6 = ModelerUtilities.colorChooser.getColor();
								view.colorManager.setColor((byte) atom.getID(), color6);
								cb.setSelectedIndex(6);
								ColorRectangle cr = (ColorRectangle) cb.getRenderer();
								cr.setMoreColor(color6);
							}
						}, null).setVisible(true);
			}
			else if (id == ColorComboBox.INDEX_HEX_INPUTTER) {
				if (cb instanceof ColorComboBox) {
					final ColorComboBox colorComboBox = (ColorComboBox) cb;
					colorComboBox.updateColor(new Runnable() {
						public void run() {
							view.colorManager.setColor((byte) atom.getID(), colorComboBox.getMoreColor());
							view.refreshJmol();
							view.repaint();
						}
					});
				}
			}
			else if (id == ColorComboBox.INDEX_MORE_COLOR) {
				view.colorManager.setColor((byte) atom.getID(), color6);
			}
			else {
				view.colorManager.setColor((byte) atom.getID(), ColorRectangle.COLORS[id]);
			}
			view.refreshJmol();
			view.repaint();
		}

	}

}