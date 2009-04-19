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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.RealNumberTextField;
import org.concord.mw2d.models.RadialBond;
import org.concord.mw2d.models.ReactionModel;

class RadialBondPropertiesPanel extends PropertiesPanel {

	private JDialog dialog;

	private RealNumberTextField lengthField;
	private RealNumberTextField strengthField;
	private ColorComboBox colorComboBox;
	private JComboBox styleComboBox;
	private JLabel leftColorLabel;
	private FloatNumberTextField torqueField;
	private JComboBox torqueTypeComboBox;
	private FloatNumberTextField amplitudeField;
	private IntegerTextField periodField, phaseField;

	void destroy() {
		removeListenersForTextField(lengthField);
		removeListenersForTextField(strengthField);
		removeListenersForComboBox(colorComboBox);
		if (dialog != null)
			dialog.dispose();
	}

	RadialBondPropertiesPanel(final RadialBond bond) {

		super(new BorderLayout());

		JPanel p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), BorderFactory
				.createTitledBorder("")));
		add(p, BorderLayout.NORTH);

		String s = null;

		// row 1
		s = MDView.getInternationalText("IndexLabel");
		p.add(new JLabel(s != null ? s : "Index"));
		p.add(createLabel(bond.getIndex()));
		p.add(new JPanel());

		// row 2
		s = MDView.getInternationalText("Atom1");
		p.add(new JLabel(s != null ? s : "Atom 1"));
		p.add(createLabel(bond.getAtom1().getIndex()));
		p.add(createSmallerFontLabel(bond.getAtom1().getName()));

		// row 3
		s = MDView.getInternationalText("Atom2");
		p.add(new JLabel(s != null ? s : "Atom 2"));
		p.add(createLabel(bond.getAtom2().getIndex()));
		p.add(createSmallerFontLabel(bond.getAtom2().getName()));

		// row 4
		s = MDView.getInternationalText("BondStyle");
		p.add(new JLabel(s != null ? s : "Style"));
		styleComboBox = new JComboBox(new String[] { "Standard Stick", "Long Spring", "Solid Line", "Invisible",
				"Unicolor Stick", "Short Spring" });
		setComboBox(styleComboBox, bond.getBondStyle() - RadialBond.STANDARD_STICK_STYLE);
		styleComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int i = styleComboBox.getSelectedIndex();
				colorComboBox.setEnabled(i != 0 && i != 3);
				leftColorLabel.setEnabled(colorComboBox.isEnabled());
				bond.setBondStyle((byte) (i + RadialBond.STANDARD_STICK_STYLE));
				bond.getHostModel().getView().repaint();
				bond.getHostModel().notifyChange();
			}
		});
		p.add(styleComboBox);
		p.add(new JPanel());

		// row 5
		s = MDView.getInternationalText("Color");
		leftColorLabel = new JLabel(s != null ? s : "Color");
		p.add(leftColorLabel);
		colorComboBox = new ColorComboBox(bond.getHostModel().getView());
		colorComboBox.setRenderer(new ComboBoxRenderer.ColorCell(bond.getBondColor()));
		colorComboBox.setPreferredSize(new Dimension(50, 18));
		int x = styleComboBox.getSelectedIndex();
		colorComboBox.setEnabled(x != 0 && x != 3);
		leftColorLabel.setEnabled(colorComboBox.isEnabled());
		if (bond.getBondColor() != null)
			setColorComboBox(colorComboBox, bond.getBondColor());
		colorComboBox.addActionListener(new BondColorListener(bond));
		p.add(colorComboBox);
		p.add(new JPanel());

		// row 6
		s = MDView.getInternationalText("BondLength");
		p.add(new JLabel(s != null ? s : "Bond Length"));
		double length = 0.1 * bond.getBondLength();
		lengthField = new RealNumberTextField(length, length - 0.5, length + 0.5);
		p.add(lengthField);
		p.add(createSmallerFontLabel("\u00c5"));

		// row 7
		s = MDView.getInternationalText("BondStrength");
		p.add(new JLabel(s != null ? s : "Bond Strength"));
		strengthField = new RealNumberTextField(bond.getBondStrength(), 1, 1000);
		p.add(strengthField);
		p.add(createSmallerFontLabel("<html>eV/&#197;<sup>2</sup></html>"));

		// row 8
		s = MDView.getInternationalText("CurrentLength");
		p.add(new JLabel(s != null ? s : "Current Length"));
		double x12 = bond.getAtom1().getRx() - bond.getAtom2().getRx();
		double y12 = bond.getAtom1().getRy() - bond.getAtom2().getRy();
		p.add(createLabel(DECIMAL_FORMAT.format(0.1 * Math.sqrt(x12 * x12 + y12 * y12))));
		p.add(createSmallerFontLabel("\u00c5"));

		if (bond.getHostModel() instanceof ReactionModel) {

			// row 9
			s = MDView.getInternationalText("BondEnergy");
			p.add(new JLabel(s != null ? s : "Bond Energy"));
			p.add(createLabel(DECIMAL_FORMAT.format(bond.getChemicalEnergy())));
			p.add(createSmallerFontLabel("eV"));

			makeCompactGrid(p, 9, 3, 5, 5, 10, 2);

		}
		else {

			makeCompactGrid(p, 8, 3, 5, 5, 10, 2);

		}

		if (!(bond.getHostModel() instanceof ReactionModel)) {

			JPanel p2 = new JPanel(new BorderLayout(2, 2));
			p2.setBorder(BorderFactory.createEmptyBorder(2, 10, 10, 10));
			add(p2, BorderLayout.CENTER);

			s = MDView.getInternationalText("Torque");
			p = new JPanel(new SpringLayout());
			p.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Torque"));
			p2.add(p, BorderLayout.NORTH);

			// row 1
			torqueField = new FloatNumberTextField(bond.getTorque() * 10000, -50f, 50f);
			p.add(new JLabel((s != null ? s : "Torque Force") + " [" + (int) torqueField.getMinValue() + ","
					+ (int) torqueField.getMaxValue() + "]"));
			torqueField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					bond.setTorque(torqueField.getValue() * 0.0001f);
					bond.getHostModel().getView().repaint();
					bond.getHostModel().notifyChange();
					destroy();
				}
			});
			torqueField.addFocusListener(new FocusAdapter() {
				public void focusLost(FocusEvent e) {
					bond.setTorque(torqueField.getValue() * 0.0001f);
					bond.getHostModel().getView().repaint();
				}
			});
			p.add(torqueField);
			p.add(createSmallerFontLabel("<html>10<sup>-5</sup>eV/&#197;</html>"));

			// row 2
			s = MDView.getInternationalText("TorqueType");
			p.add(new JLabel(s != null ? s : "Torque Type"));
			torqueTypeComboBox = new JComboBox(new String[] { "Atom 2 around atom 1", "Atom 1 around atom 2",
					"Both atoms around center" });
			setComboBox(torqueTypeComboBox, bond.getTorqueType() - RadialBond.TORQUE_AROUND_ATOM1);
			torqueTypeComboBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int i = torqueTypeComboBox.getSelectedIndex();
					bond.setTorqueType((byte) (i + RadialBond.TORQUE_AROUND_ATOM1));
					bond.getHostModel().getView().repaint();
					bond.getHostModel().notifyChange();
				}
			});
			p.add(torqueTypeComboBox);
			p.add(new JPanel());

			makeCompactGrid(p, 2, 3, 5, 5, 10, 2);

			s = MDView.getInternationalText("ForcedVibration");
			p = new JPanel(new SpringLayout());
			p.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Forced vibration"));
			p2.add(p, BorderLayout.CENTER);

			// row 1
			amplitudeField = new FloatNumberTextField(bond.getAmplitude() * 1000, 0f, 100f);
			s = MDView.getInternationalText("Amplitude");
			p.add(new JLabel((s != null ? s : "Amplitude") + " [" + (int) amplitudeField.getMinValue() + ","
					+ (int) amplitudeField.getMaxValue() + "]"));
			amplitudeField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					bond.setAmplitude(amplitudeField.getValue() * 0.001f);
					bond.getHostModel().getView().repaint();
					bond.getHostModel().notifyChange();
					destroy();
				}
			});
			amplitudeField.addFocusListener(new FocusAdapter() {
				public void focusLost(FocusEvent e) {
					bond.setAmplitude(amplitudeField.getValue() * 0.001f);
					bond.getHostModel().getView().repaint();
				}
			});
			p.add(amplitudeField);
			p.add(createSmallerFontLabel("<html>10<sup>-3</sup>eV/&#197;</html>"));

			// row 2
			periodField = new IntegerTextField(bond.getPeriod(), 1, 1000000);
			s = MDView.getInternationalText("Period");
			p.add(new JLabel((s != null ? s : "Period") + " [" + periodField.getMinValue() + ","
					+ periodField.getMaxValue() + "]"));
			periodField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					bond.setPeriod(periodField.getValue());
					bond.getHostModel().getView().repaint();
					bond.getHostModel().notifyChange();
					destroy();
				}
			});
			periodField.addFocusListener(new FocusAdapter() {
				public void focusLost(FocusEvent e) {
					bond.setPeriod(periodField.getValue());
					bond.getHostModel().getView().repaint();
				}
			});
			p.add(periodField);
			p.add(createSmallerFontLabel("fs"));

			// row 3
			phaseField = new IntegerTextField((int) (bond.getPhase() * 180f / (float) Math.PI), 0, 360);
			s = MDView.getInternationalText("Phase");
			p.add(new JLabel((s != null ? s : "Phase") + " [0, 360]"));
			phaseField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					bond.setPhase(phaseField.getValue() * (float) Math.PI / 180f);
					bond.getHostModel().getView().repaint();
					bond.getHostModel().notifyChange();
					destroy();
				}
			});
			phaseField.addFocusListener(new FocusAdapter() {
				public void focusLost(FocusEvent e) {
					bond.setPhase(phaseField.getValue() * (float) Math.PI / 180f);
					bond.getHostModel().getView().repaint();
				}
			});
			p.add(phaseField);
			p.add(createSmallerFontLabel("<html>Degrees</html>"));

			makeCompactGrid(p, 3, 3, 5, 5, 10, 2);

		}

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(p, BorderLayout.SOUTH);

		Action okAction = new ModelAction(bond.getHostModel(), new Executable() {
			public void execute() {
				boolean changed = false;
				if (Math.abs(bond.getBondLength() - lengthField.getValue() * 10) > ZERO) {
					bond.setBondLength(lengthField.getValue() * 10);
					changed = true;
				}
				if (Math.abs(bond.getBondStrength() - strengthField.getValue()) > ZERO) {
					bond.setBondStrength(strengthField.getValue());
					changed = true;
				}
				if (changed)
					bond.getHostModel().notifyChange();
				destroy();
			}
		}) {
		};

		lengthField.setAction(okAction);
		strengthField.setAction(okAction);

		JButton okButton = new JButton(okAction);
		s = MDView.getInternationalText("OKButton");
		okButton.setText(s != null ? s : "OK");

		s = MDView.getInternationalText("CancelButton");
		JButton cancelButton = new JButton(s != null ? s : "Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				destroy();
			}
		});

		if (Modeler.isMac()) {
			p.add(cancelButton);
			p.add(okButton);
		}
		else {
			p.add(okButton);
			p.add(cancelButton);
		}

	}

	void setDialog(JDialog d) {
		dialog = d;
	}

	void windowActivated() {
		strengthField.selectAll();
		strengthField.requestFocusInWindow();
	}

	static class BondColorListener implements ActionListener {

		private Color color6 = Color.white;
		private RadialBond bond;

		BondColorListener(RadialBond bond) {
			this.bond = bond;
		}

		public void actionPerformed(ActionEvent e) {
			final JComboBox cb = (JComboBox) e.getSource();
			int id = ((Integer) cb.getSelectedItem()).intValue();
			if (id == ColorComboBox.INDEX_COLOR_CHOOSER) {
				JColorChooser.createDialog(bond.getHostModel().getView(), "More Colors", true,
						ModelerUtilities.colorChooser, new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								color6 = ModelerUtilities.colorChooser.getColor();
								bond.setBondColor(color6);
								cb.setSelectedIndex(ColorComboBox.INDEX_MORE_COLOR);
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
							bond.setBondColor(colorComboBox.getMoreColor());
						}
					});
				}
			}
			else if (id == ColorComboBox.INDEX_MORE_COLOR) {
				bond.setBondColor(color6);
			}
			else {
				bond.setBondColor(ColorRectangle.COLORS[id]);
			}
			bond.getHostModel().getView().repaint();
			bond.getHostModel().notifyChange();
		}

	}

}