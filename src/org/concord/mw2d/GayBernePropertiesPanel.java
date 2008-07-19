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
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ColorRectangle;
import org.concord.modeler.ui.ComboBoxRenderer;
import org.concord.modeler.ui.HyperlinkLabel;
import org.concord.modeler.ui.RealNumberTextField;
import org.concord.modeler.util.DataQueueUtilities;
import org.concord.mw2d.models.GayBerneParticle;
import org.concord.mw2d.models.PointRestraint;
import org.concord.mw2d.models.UnitedAtom;

class GayBernePropertiesPanel extends PropertiesPanel {

	private JDialog dialog;

	void destroy() {
		if (dialog != null)
			dialog.dispose();
	}

	GayBernePropertiesPanel(final GayBerneParticle gb) {

		super(new BorderLayout(5, 5));

		/* reuse text fields */

		final RealNumberTextField massField = createTextField(gb.getMass() * 120, 1, 10000.0);
		final RealNumberTextField inertiaField = createTextField(gb.getInertia(), 100, 10000);
		final RealNumberTextField epsilon0Field = createTextField(gb.getEpsilon0(), 0, 1);
		final RealNumberTextField eeesField = createTextField(gb.getEeVsEs(), 0.001, 100);
		final RealNumberTextField chargeField = createTextField(gb.getCharge(), -50, 50);
		final RealNumberTextField dipoleField = createTextField(gb.getDipoleMoment(), -50, 50);
		final RealNumberTextField frictionField = new RealNumberTextField(gb.getFriction(), 0.0, 100.0, 10);
		final RealNumberTextField springField = new RealNumberTextField(gb.getRestraint() == null ? 0.0f : gb
				.getRestraint().getK() * 100, 0.0, 100000.0, 10);
		final RealNumberTextField vxField = new RealNumberTextField(10000 * gb.getVx(), -10000, 10000, 10);
		final RealNumberTextField vyField = new RealNumberTextField(10000 * gb.getVy(), -10000, 10000, 10);
		final RealNumberTextField omegaField = new RealNumberTextField(gb.getOmega() * 180 / Math.PI, -100, 100, 10);

		/* OK action */

		Action okAction = new ModelAction(gb.getHostModel(), new Executable() {
			public void execute() {

				applyBounds(massField);
				applyBounds(inertiaField);
				applyBounds(chargeField);
				applyBounds(dipoleField);
				applyBounds(springField);
				applyBounds(vxField);
				applyBounds(vyField);
				applyBounds(omegaField);
				applyBounds(epsilon0Field);
				applyBounds(eeesField);

				boolean changed = false;

				if (Math.abs(gb.getMass() * 120 - massField.getValue()) > ZERO) {
					gb.setMass(massField.getValue() / 120);
					changed = true;
				}

				if (Math.abs(gb.getInertia() - inertiaField.getValue()) > ZERO) {
					gb.setInertia(inertiaField.getValue());
					changed = true;
				}

				if (Math.abs(gb.getCharge() - chargeField.getValue()) > ZERO) {
					gb.setCharge(chargeField.getValue());
					changed = true;
				}

				if (Math.abs(gb.getDipoleMoment() - dipoleField.getValue()) > ZERO) {
					gb.setDipoleMoment(dipoleField.getValue());
					changed = true;
				}

				if (Math.abs(gb.getEeVsEs() - eeesField.getValue()) > ZERO) {
					gb.setEeVsEs(eeesField.getValue());
					changed = true;
				}

				if (Math.abs(gb.getEpsilon0() - epsilon0Field.getValue()) > ZERO) {
					gb.setEpsilon0(epsilon0Field.getValue());
					changed = true;
				}

				if (Math.abs(gb.getFriction() - frictionField.getValue()) > ZERO) {
					gb.setFriction((float) frictionField.getValue());
					changed = true;
				}

				if (springField.getValue() > ZERO) {
					double v = springField.getValue() * 0.01;
					if (gb.getRestraint() == null) {
						gb.setRestraint(new PointRestraint(v, gb.getRx(), gb.getRy()));
						changed = true;
					}
					else {
						if (Math.abs(gb.getRestraint().getK() - v) > ZERO) {
							gb.getRestraint().setK(v);
							changed = true;
						}
					}
				}
				else {
					if (gb.getRestraint() != null) {
						gb.setRestraint(null);
						changed = true;
					}
				}

				if (Math.abs(gb.getVx() * 10000 - vxField.getValue()) > ZERO) {
					gb.setVx(vxField.getValue() * 0.0001);
					changed = true;
				}

				if (Math.abs(gb.getVy() * 10000 - vyField.getValue()) > ZERO) {
					gb.setVy(vyField.getValue() * 0.0001);
					changed = true;
				}

				if (Math.abs(gb.getOmega() - omegaField.getValue()) > ZERO) {
					gb.setOmega(omegaField.getValue() * Math.PI / 180);
					changed = true;
				}

				if (changed)
					gb.getHostModel().notifyChange();
				gb.getHostModel().getView().repaint();
				destroy();

			}
		}) {
		};

		massField.setAction(okAction);
		inertiaField.setAction(okAction);
		epsilon0Field.setAction(okAction);
		eeesField.setAction(okAction);
		chargeField.setAction(okAction);
		dipoleField.setAction(okAction);
		frictionField.setAction(okAction);
		springField.setAction(okAction);
		vxField.setAction(okAction);
		vyField.setAction(okAction);
		omegaField.setAction(okAction);

		/* lay down components */

		JPanel panel = new JPanel(new SpringLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(10, 10, 10, 10)));
		JScrollPane sp = new JScrollPane(panel);
		sp.setPreferredSize(new Dimension(380, 360));
		add(sp, BorderLayout.CENTER);

		String s = MDView.getInternationalText("ObjectTypeLabel");
		panel.add(new JLabel(s != null ? s : "Object type & color"));
		panel.add(createLabel("Gay-Berne"));
		JComboBox cb = new ColorComboBox(gb.getHostModel().getView());
		cb.setRenderer(new ComboBoxRenderer.ColorCell(gb.getColor()));
		setColorComboBox(cb, gb.getColor());
		cb.setPreferredSize(new Dimension(32, 18));
		cb.addActionListener(new ColorListener(gb));
		panel.add(cb);

		s = MDView.getInternationalText("IndexLabel");
		panel.add(new JLabel(s != null ? s : "Index"));
		JLabel label = new JLabel(Integer.toString(gb.getIndex()));
		label.setBorder(BUTTON_BORDER);
		panel.add(label);
		panel.add(createSmallerFontLabel("Non-bonded"));

		s = MDView.getInternationalText("MassLabel");
		panel.add(new JLabel(s != null ? s : "Mass"));
		panel.add(massField);
		panel.add(createSmallerFontLabel("g/mol"));

		s = MDView.getInternationalText("BreadthLabel");
		panel.add(new JLabel(s != null ? s : "Breadth"));
		panel.add(createLabel(gb.getBreadth() * 0.1));
		panel.add(createSmallerFontLabel("\u00c5"));

		s = MDView.getInternationalText("LengthLabel");
		panel.add(new JLabel(s != null ? s : "Length"));
		panel.add(createLabel(gb.getLength() * 0.1));
		panel.add(createSmallerFontLabel("\u00c5"));

		s = MDView.getInternationalText("InertiaLabel");
		panel.add(new JLabel(s != null ? s : "Inertia"));
		panel.add(inertiaField);
		panel.add(createSmallerFontLabel("<html>kg/mol*&#197;<sup>2</sup></html>"));

		panel.add(new JLabel("<html><i>&#949;</i><sub>0</sub></html>", SwingConstants.LEFT));
		panel.add(epsilon0Field);
		panel.add(createSmallerFontLabel("eV"));

		panel.add(new JLabel("<html><i>&#949;</i><sub>e</sub>/<i>&#949;</i><sub>s</sub></html>", SwingConstants.LEFT));
		panel.add(eeesField);
		panel.add(createSmallerFontLabel("dimensionless"));

		s = MDView.getInternationalText("ChargeLabel");
		panel.add(new JLabel(s != null ? s : "Charge (e)"));
		panel.add(chargeField);
		cb = new ColorComboBox(gb.getHostModel().getView());
		cb.setRenderer(new ComboBoxRenderer.ColorCell(gb.getChargeColor()));
		setColorComboBox(cb, gb.getChargeColor());
		cb.setPreferredSize(new Dimension(32, 18));
		cb.addActionListener(new ChargeColorListener(gb));
		panel.add(cb);

		s = MDView.getInternationalText("DipoleMomentLabel");
		panel.add(new JLabel((s != null ? s : "Dipole Moment ") + "(e*A)"));
		panel.add(dipoleField);
		cb = new ColorComboBox(gb.getHostModel().getView());
		cb.setRenderer(new ComboBoxRenderer.ColorCell(gb.getDipoleColor()));
		setColorComboBox(cb, gb.getDipoleColor());
		cb.setPreferredSize(new Dimension(32, 18));
		cb.addActionListener(new DipoleColorListener(gb));
		panel.add(cb);

		s = MDView.getInternationalText("RestraintLabel");
		panel.add(new JLabel(s != null ? s : "Restraint"));
		panel.add(springField);
		panel.add(createSmallerFontLabel("<html>eV/&#197;<sup>2</sup></html>"));

		s = MDView.getInternationalText("DampingLabel");
		panel.add(new JLabel(s != null ? s : "Damping"));
		panel.add(frictionField);
		panel.add(createSmallerFontLabel("<html>eV*fs/&#197;<sup>2</sup></html>"));

		HyperlinkLabel hLabel = new HyperlinkLabel("<html><font color=\"#0000ff\"><u><em>X</em></u></font></html>");
		hLabel.setToolTipText("Click to view this variable's graph");
		hLabel.setAction(new Runnable() {
			public void run() {
				if (gb.getRxRyQueue() != null) {
					gb.getRxRyQueue().getQueue1().setMultiplier(0.1f);
					DataQueueUtilities.show(gb.getRxRyQueue().getQueue1(), JOptionPane
							.getFrameForComponent(GayBernePropertiesPanel.this));
				}
				else {
					DataQueueUtilities
							.showNoDataMessage(JOptionPane.getFrameForComponent(GayBernePropertiesPanel.this));
				}
			}
		});
		panel.add(hLabel);
		panel.add(createLabel(gb.getRx() * 0.1));
		panel.add(createSmallerFontLabel("\u00c5"));

		hLabel = new HyperlinkLabel("<html><font color=\"#0000ff\"><u><em>Y</em></u></font></html>");
		hLabel.setToolTipText("Click to view this variable's graph");
		hLabel.setAction(new Runnable() {
			public void run() {
				if (gb.getRxRyQueue() != null) {
					gb.getRxRyQueue().getQueue2().setMultiplier(0.1f);
					DataQueueUtilities.show(gb.getRxRyQueue().getQueue2(), JOptionPane
							.getFrameForComponent(GayBernePropertiesPanel.this));
				}
				else {
					DataQueueUtilities
							.showNoDataMessage(JOptionPane.getFrameForComponent(GayBernePropertiesPanel.this));
				}
			}
		});
		panel.add(hLabel);
		panel.add(createLabel(gb.getRy() * 0.1));
		panel.add(createSmallerFontLabel("\u00c5"));

		hLabel = new HyperlinkLabel("<html><font color=\"#0000ff\"><i><u>&#952;</u></i></font></html>",
				SwingConstants.LEFT);
		hLabel.setToolTipText("Click to view this variable's graph");
		hLabel.setAction(new Runnable() {
			public void run() {
				if (gb.getThetaQ() != null) {
					gb.getThetaQ().setMultiplier((float) (180 / Math.PI));
					DataQueueUtilities.show(gb.getThetaQ(), JOptionPane
							.getFrameForComponent(GayBernePropertiesPanel.this));
				}
				else {
					DataQueueUtilities
							.showNoDataMessage(JOptionPane.getFrameForComponent(GayBernePropertiesPanel.this));
				}
			}
		});
		panel.add(hLabel);
		panel.add(createLabel(gb.getTheta() * 180.0 / Math.PI));
		panel.add(createSmallerFontLabel("deg (ACW +)"));

		hLabel = new HyperlinkLabel("<html><font color=\"#0000ff\"><u><em>V<sub>x</sub></em></u></font></html>");
		hLabel.setToolTipText("Click to view this variable's graph");
		hLabel.setAction(new Runnable() {
			public void run() {
				if (gb.getVxVyQueue() != null) {
					gb.getVxVyQueue().getQueue1().setMultiplier(10000);
					DataQueueUtilities.show(gb.getVxVyQueue().getQueue1(), JOptionPane
							.getFrameForComponent(GayBernePropertiesPanel.this));
				}
				else {
					DataQueueUtilities
							.showNoDataMessage(JOptionPane.getFrameForComponent(GayBernePropertiesPanel.this));
				}
			}
		});
		panel.add(hLabel);
		panel.add(vxField);
		panel.add(createSmallerFontLabel("m/s"));

		hLabel = new HyperlinkLabel("<html><font color=\"#0000ff\"><u><em>V<sub>y</sub></em></u></font></html>");
		hLabel.setToolTipText("Click to view this variable's graph");
		hLabel.setAction(new Runnable() {
			public void run() {
				if (gb.getVxVyQueue() != null) {
					gb.getVxVyQueue().getQueue2().setMultiplier(10000);
					DataQueueUtilities.show(gb.getVxVyQueue().getQueue2(), JOptionPane
							.getFrameForComponent(GayBernePropertiesPanel.this));
				}
				else {
					DataQueueUtilities
							.showNoDataMessage(JOptionPane.getFrameForComponent(GayBernePropertiesPanel.this));
				}
			}
		});
		panel.add(hLabel);
		panel.add(vyField);
		panel.add(createSmallerFontLabel("m/s"));

		hLabel = new HyperlinkLabel("<html><font color=\"#0000ff\"><i><u>&#969;</u></i></font></html>",
				SwingConstants.LEFT);
		hLabel.setToolTipText("Click to view this variable's graph");
		hLabel.setAction(new Runnable() {
			public void run() {
				if (gb.getOmegaQ() != null) {
					gb.getOmegaQ().setMultiplier((float) (180 / Math.PI));
					DataQueueUtilities.show(gb.getOmegaQ(), JOptionPane
							.getFrameForComponent(GayBernePropertiesPanel.this));
				}
				else {
					DataQueueUtilities
							.showNoDataMessage(JOptionPane.getFrameForComponent(GayBernePropertiesPanel.this));
				}
			}
		});
		panel.add(hLabel);
		panel.add(omegaField);
		panel.add(createSmallerFontLabel("deg/fs"));

		hLabel = new HyperlinkLabel("<html><font color=\"#0000ff\"><u><em>A<sub>x</sub></em></u></font></html>");
		hLabel.setToolTipText("Click to view this variable's graph");
		hLabel.setAction(new Runnable() {
			public void run() {
				if (gb.getAxAyQueue() != null) {
					gb.getAxAyQueue().getQueue1().setMultiplier(0.01f);
					DataQueueUtilities.show(gb.getAxAyQueue().getQueue1(), JOptionPane
							.getFrameForComponent(GayBernePropertiesPanel.this));
				}
				else {
					DataQueueUtilities
							.showNoDataMessage(JOptionPane.getFrameForComponent(GayBernePropertiesPanel.this));
				}
			}
		});
		panel.add(hLabel);
		panel.add(createLabel2(0.01 * gb.getAx()));
		panel.add(createSmallerFontLabel("<html>&#197;/fs<sup>2</sup></html>"));

		hLabel = new HyperlinkLabel("<html><font color=\"#0000ff\"><u><em>A<sub>y</sub></em></u></font></html>");
		hLabel.setToolTipText("Click to view this variable's graph");
		hLabel.setAction(new Runnable() {
			public void run() {
				if (gb.getAxAyQueue() != null) {
					gb.getAxAyQueue().getQueue2().setMultiplier(0.01f);
					DataQueueUtilities.show(gb.getAxAyQueue().getQueue2(), JOptionPane
							.getFrameForComponent(GayBernePropertiesPanel.this));
				}
				else {
					DataQueueUtilities
							.showNoDataMessage(JOptionPane.getFrameForComponent(GayBernePropertiesPanel.this));
				}
			}
		});
		panel.add(hLabel);
		panel.add(createLabel2(0.01 * gb.getAy()));
		panel.add(createSmallerFontLabel("<html>&#197;/fs<sup>2</sup></html>"));

		hLabel = new HyperlinkLabel("<html><font color=\"#0000ff\"><i><u>&#945;</u></i></font></html>",
				SwingConstants.LEFT);
		hLabel.setToolTipText("Click to view this variable's graph");
		hLabel.setAction(new Runnable() {
			public void run() {
				if (gb.getAlphaQ() != null) {
					gb.getAlphaQ().setMultiplier((float) (180 / Math.PI));
					DataQueueUtilities.show(gb.getAlphaQ(), JOptionPane
							.getFrameForComponent(GayBernePropertiesPanel.this));
				}
				else {
					DataQueueUtilities
							.showNoDataMessage(JOptionPane.getFrameForComponent(GayBernePropertiesPanel.this));
				}
			}
		});
		panel.add(hLabel);
		panel.add(createLabel2(gb.getAlpha() * 180 / Math.PI));
		panel.add(createSmallerFontLabel("eV/deg"));

		makeCompactGrid(panel, 21, 3, 5, 5, 10, 2);

		/* button panel */

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(panel, BorderLayout.SOUTH);

		JButton okButton = new JButton(okAction);
		s = MDView.getInternationalText("OKButton");
		okButton.setText(s != null ? s : "OK");

		s = MDView.getInternationalText("CancelButton");
		JButton cancelButton = new JButton(s != null ? s : "Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (gb.getHostModel() != null)
					gb.getHostModel().getView().repaint();
				destroy();
			}
		});

		if (Modeler.isMac()) {
			panel.add(cancelButton);
			panel.add(okButton);
		}
		else {
			panel.add(okButton);
			panel.add(cancelButton);
		}

	}

	void setDialog(JDialog d) {
		dialog = d;
	}

	void windowActivated() {
	}

	private static RealNumberTextField createTextField(double d, double min, double max) {
		return new RealNumberTextField(d, min, max, 10);
	}

	static class DipoleColorListener implements ActionListener {

		private Color color6 = Color.white;
		private UnitedAtom unitedAtom;

		DipoleColorListener(UnitedAtom ua) {
			this.unitedAtom = ua;
		}

		public void actionPerformed(ActionEvent e) {
			final JComboBox cb = (JComboBox) e.getSource();
			int id = ((Integer) cb.getSelectedItem()).intValue();
			if (id == ColorComboBox.INDEX_COLOR_CHOOSER) {
				javax.swing.JColorChooser.createDialog(unitedAtom.getHostModel().getView(), "More Colors", true,
						ModelerUtilities.colorChooser, new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								color6 = ModelerUtilities.colorChooser.getColor();
								unitedAtom.setDipoleColor(color6);
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
							unitedAtom.setDipoleColor(colorComboBox.getMoreColor());
						}
					});
				}
			}
			else if (id == ColorComboBox.INDEX_MORE_COLOR) {
				unitedAtom.setDipoleColor(color6);
			}
			else {
				unitedAtom.setDipoleColor(ColorRectangle.COLORS[id]);
			}
			unitedAtom.getHostModel().getView().repaint();
		}

	}

}