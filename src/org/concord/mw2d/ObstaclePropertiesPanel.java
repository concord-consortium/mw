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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.process.Executable;
import org.concord.modeler.ui.BackgroundComboBox;
import org.concord.modeler.ui.ColorMenu;
import org.concord.modeler.ui.FloatNumberTextField;
import org.concord.modeler.ui.HyperlinkLabel;
import org.concord.modeler.ui.RealNumberTextField;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.RectangularObstacle;

class ObstaclePropertiesPanel extends PropertiesPanel {

	private static final String LINK_OPEN = "<html><u><font color=\"#0000ff\">";
	private static final String LINK_CLOSE = "</font></u></html>";

	private JDialog dialog;

	private HyperlinkLabel westProbeLabel;
	private HyperlinkLabel eastProbeLabel;
	private HyperlinkLabel southProbeLabel;
	private HyperlinkLabel northProbeLabel;
	private PressureProbePanel probePanel;

	void destroy() {
		if (dialog != null)
			dialog.dispose();
	}

	private void setWestProbeLabel(boolean b) {
		westProbeLabel.setEnabled(b);
		String s = MDView.getInternationalText("West");
		westProbeLabel.setText(b ? LINK_OPEN + (s != null ? s : "West") + LINK_CLOSE : (s != null ? s : "West"));
	}

	private void setEastProbeLabel(boolean b) {
		eastProbeLabel.setEnabled(b);
		String s = MDView.getInternationalText("East");
		eastProbeLabel.setText(b ? LINK_OPEN + (s != null ? s : "East") + LINK_CLOSE : (s != null ? s : "East"));
	}

	private void setSouthProbeLabel(boolean b) {
		southProbeLabel.setEnabled(b);
		String s = MDView.getInternationalText("South");
		southProbeLabel.setText(b ? LINK_OPEN + (s != null ? s : "South") + LINK_CLOSE : (s != null ? s : "South"));
	}

	private void setNorthProbeLabel(boolean b) {
		northProbeLabel.setEnabled(b);
		String s = MDView.getInternationalText("North");
		northProbeLabel.setText(b ? LINK_OPEN + (s != null ? s : "North") + LINK_CLOSE : (s != null ? s : "North"));
	}

	ObstaclePropertiesPanel(final RectangularObstacle obs) {

		super(new BorderLayout(5, 5));

		String s = null;

		final JCheckBox ntPassCheckBox = new JCheckBox("Nt");
		ntPassCheckBox.setSelected(obs.isPermeable(Element.ID_NT));
		ntPassCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				obs.setPermeable(Element.ID_NT, e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		ntPassCheckBox.setSelected(obs.isPermeable(Element.ID_NT));

		final JCheckBox plPassCheckBox = new JCheckBox("Pl");
		ntPassCheckBox.setSelected(obs.isPermeable(Element.ID_PL));
		plPassCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				obs.setPermeable(Element.ID_PL, e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		plPassCheckBox.setSelected(obs.isPermeable(Element.ID_PL));

		final JCheckBox wsPassCheckBox = new JCheckBox("Ws");
		ntPassCheckBox.setSelected(obs.isPermeable(Element.ID_WS));
		wsPassCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				obs.setPermeable(Element.ID_WS, e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		wsPassCheckBox.setSelected(obs.isPermeable(Element.ID_WS));

		final JCheckBox ckPassCheckBox = new JCheckBox("Ck");
		ntPassCheckBox.setSelected(obs.isPermeable(Element.ID_CK));
		ckPassCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				obs.setPermeable(Element.ID_CK, e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		ckPassCheckBox.setSelected(obs.isPermeable(Element.ID_CK));

		s = MDView.getInternationalText("West");
		westProbeLabel = new HyperlinkLabel(s != null ? s : "West");
		westProbeLabel.setEnabled(false);
		westProbeLabel.setToolTipText("Customize the pressure gauge on this side");

		s = MDView.getInternationalText("East");
		eastProbeLabel = new HyperlinkLabel(s != null ? s : "East");
		eastProbeLabel.setEnabled(false);
		eastProbeLabel.setToolTipText("Customize the pressure gauge on this side");

		s = MDView.getInternationalText("South");
		southProbeLabel = new HyperlinkLabel(s != null ? s : "South");
		southProbeLabel.setEnabled(false);
		southProbeLabel.setToolTipText("Customize the pressure gauge on this side");

		s = MDView.getInternationalText("North");
		northProbeLabel = new HyperlinkLabel(s != null ? s : "North");
		northProbeLabel.setEnabled(false);
		northProbeLabel.setToolTipText("Customize the pressure gauge on this side");

		final JCheckBox westProbeCheckBox = new JCheckBox();
		westProbeCheckBox.setSelected(obs.isWestProbe());
		westProbeCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				setWestProbeLabel(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		setWestProbeLabel(obs.isWestProbe());

		final JCheckBox eastProbeCheckBox = new JCheckBox();
		eastProbeCheckBox.setSelected(obs.isEastProbe());
		eastProbeCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				setEastProbeLabel(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		setEastProbeLabel(obs.isEastProbe());

		final JCheckBox southProbeCheckBox = new JCheckBox();
		southProbeCheckBox.setSelected(obs.isSouthProbe());
		southProbeCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				setSouthProbeLabel(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		setSouthProbeLabel(obs.isSouthProbe());

		final JCheckBox northProbeCheckBox = new JCheckBox();
		northProbeCheckBox.setSelected(obs.isNorthProbe());
		northProbeCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				setNorthProbeLabel(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		setNorthProbeLabel(obs.isNorthProbe());

		final RealNumberTextField widthField = new RealNumberTextField(0.1 * obs.getWidth(), 1, 100);
		widthField.setMaximumFractionDigits(6);

		final RealNumberTextField heightField = new RealNumberTextField(0.1 * obs.getHeight(), 1, 100);
		heightField.setMaximumFractionDigits(6);

		final RealNumberTextField x0Field = new RealNumberTextField(0.1 * obs.getX(), 0, 100);
		x0Field.setMaximumFractionDigits(6);

		final RealNumberTextField y0Field = new RealNumberTextField(0.1 * obs.getY(), 0, 100);
		y0Field.setMaximumFractionDigits(6);

		final RealNumberTextField vxField = new RealNumberTextField(10000 * obs.getVx(), -10000, 10000);
		vxField.setMaximumFractionDigits(6);

		final RealNumberTextField vyField = new RealNumberTextField(10000 * obs.getVy(), -10000, 10000);
		vyField.setMaximumFractionDigits(6);

		final FloatNumberTextField fxField = new FloatNumberTextField(obs.getExternalFx() * 1000, -1000, 1000);
		fxField.setMaximumFractionDigits(6);

		final FloatNumberTextField fyField = new FloatNumberTextField(obs.getExternalFy() * 1000, -1000, 1000);
		fyField.setMaximumFractionDigits(6);

		final RealNumberTextField frictionField = new RealNumberTextField(obs.getFriction(), 0.0, 100.0, 10);
		final FloatNumberTextField elasticityField = new FloatNumberTextField(obs.getElasticity(), 0.0f, 1.0f);
		final RealNumberTextField densityField = new RealNumberTextField(100 * obs.getDensity(), 0.01, 100000000);

		final JComboBox boundaryComboBox = new JComboBox(new String[] { "Bounce back", "Stop" });
		boundaryComboBox.setSelectedIndex(obs.isBounced() ? 0 : 1);
		boundaryComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.DESELECTED)
					return;
				if (obs.isBounced() != (boundaryComboBox.getSelectedIndex() == 0))
					obs.getHostModel().notifyChange();
				obs.setBounced(boundaryComboBox.getSelectedIndex() == 0);
			}
		});

		final BackgroundComboBox bgComboBox = new BackgroundComboBox(obs.getHostModel().getView(),
				ModelerUtilities.colorChooser, ModelerUtilities.fillEffectChooser);
		bgComboBox.setPreferredSize(new Dimension(80, 20));
		bgComboBox.setFillMode(obs.getFillMode());
		bgComboBox.getColorMenu().setNoFillAction(new AbstractAction("Default") {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = FillMode.getNoFillMode();
				if (fm.equals(obs.getFillMode()))
					return;
				obs.setFillMode(fm);
				obs.getHostModel().getView().repaint();
				obs.getHostModel().notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().setColorArrayAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = new FillMode.ColorFill(bgComboBox.getColorMenu().getColor());
				if (fm.equals(obs.getFillMode()))
					return;
				obs.setFillMode(fm);
				obs.getHostModel().getView().repaint();
				obs.getHostModel().notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().setMoreColorAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = new FillMode.ColorFill(bgComboBox.getColorMenu().getColorChooser().getColor());
				if (fm.equals(obs.getFillMode()))
					return;
				obs.setFillMode(fm);
				obs.getHostModel().getView().repaint();
				obs.getHostModel().notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().addHexColorListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = bgComboBox.getColorMenu().getHexInputColor(
						obs.getFillMode() instanceof FillMode.ColorFill ? ((FillMode.ColorFill) obs.getFillMode())
								.getColor() : null);
				if (c == null)
					return;
				FillMode fm = new FillMode.ColorFill(c);
				if (fm.equals(obs.getFillMode()))
					return;
				obs.setFillMode(fm);
				obs.getHostModel().getView().repaint();
				obs.getHostModel().notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().setFillEffectActions(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FillMode fm = bgComboBox.getColorMenu().getFillEffectChooser().getFillMode();
				if (fm.equals(obs.getFillMode()))
					return;
				obs.setFillMode(fm);
				obs.getHostModel().getView().repaint();
				obs.getHostModel().notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		}, null);

		final JComboBox visibleComboBox = new JComboBox(new String[] { "Visible", "Invisible" });
		visibleComboBox.setSelectedIndex(obs.isVisible() ? 0 : 1);
		visibleComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.DESELECTED)
					return;
				if (obs.isVisible() != (visibleComboBox.getSelectedIndex() == 0)) {
					obs.getHostModel().notifyChange();
					obs.setVisible(visibleComboBox.getSelectedIndex() == 0);
					obs.getHostModel().getView().repaint();
				}
			}
		});

		final JComboBox roundedComboBox = new JComboBox(new String[] { "Straight-cornered", "Round-cornered" });
		roundedComboBox.setSelectedIndex(obs.getRoundCornerRadius() <= 0 ? 0 : 1);
		roundedComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.DESELECTED)
					return;
				if (obs.getRoundCornerRadius() <= 0 != (roundedComboBox.getSelectedIndex() == 0)) {
					obs.getHostModel().notifyChange();
					obs.setRoundCornerRadius(roundedComboBox.getSelectedIndex() == 0 ? 0 : RectangularObstacle
							.getDefaultRoundCornerRadius());
					obs.getHostModel().getView().repaint();
				}
			}
		});

		/* OK action */

		Action okAction = new ModelAction(obs.getHostModel(), new Executable() {
			public void execute() {
				boolean changed = false;
				double oldX = obs.getX();
				double oldY = obs.getY();
				double oldW = obs.getWidth();
				double oldH = obs.getHeight();
				if (Math.abs(oldX - x0Field.getValue() * 10) > ZERO || Math.abs(oldY - y0Field.getValue() * 10) > ZERO
						|| Math.abs(oldW - widthField.getValue() * 10) > ZERO
						|| Math.abs(oldH - heightField.getValue() * 10) > ZERO) {
					obs.setRect(x0Field.getValue() * 10, y0Field.getValue() * 10, widthField.getValue() * 10,
							heightField.getValue() * 10);
					if (obs.getHostModel().getView() instanceof AtomisticView) {
						if (((AtomisticView) obs.getHostModel().getView()).intersects(obs)) {
							JOptionPane
									.showMessageDialog(
											JOptionPane.getFrameForComponent(obs.getHostModel().getView()),
											"The selected obstacle cannot be moved\nto the specified position. Click OK to\nreset it to the original position.",
											"Overlap Error", JOptionPane.ERROR_MESSAGE);
							obs.setRect(oldX, oldY, oldW, oldH);
						}
						else {
							changed = true;
						}
					}
					obs.getHostModel().getView().repaint();
				}
				if (Math.abs(obs.getDensity() * 100 - densityField.getValue()) > ZERO) {
					obs.setDensity(densityField.getValue() * 0.01);
					changed = true;
				}
				if (Math.abs(obs.getVx() * 10000 - vxField.getValue()) > ZERO) {
					obs.setVx(vxField.getValue() * 0.0001);
					changed = true;
				}
				if (Math.abs(obs.getVy() * 10000 - vyField.getValue()) > ZERO) {
					obs.setVy(vyField.getValue() * 0.0001);
					changed = true;
				}
				if (Math.abs(obs.getExternalFx() * 1000 - fxField.getValue()) > ZERO) {
					obs.setExternalFx((float) (fxField.getValue() * 0.001));
					changed = true;
				}
				if (Math.abs(obs.getExternalFy() * 1000 - fyField.getValue()) > ZERO) {
					obs.setExternalFy((float) (fyField.getValue() * 0.001));
					changed = true;
				}
				if (Math.abs(obs.getElasticity() - elasticityField.getValue()) > ZERO) {
					obs.setElasticity(elasticityField.getValue());
					changed = true;
				}
				if (Math.abs(obs.getFriction() - frictionField.getValue()) > ZERO) {
					obs.setFriction((float) frictionField.getValue());
					changed = true;
				}
				if (obs.isWestProbe() != westProbeCheckBox.isSelected()) {
					obs.setWestProbe(westProbeCheckBox.isSelected());
					changed = true;
					if (!obs.getHostModel().getRecorderDisabled()) {
						obs.initializePWQ(obs.isWestProbe() ? obs.getHostModel().getMovie().getCapacity() : -1);
					}
				}
				if (obs.isEastProbe() != eastProbeCheckBox.isSelected()) {
					obs.setEastProbe(eastProbeCheckBox.isSelected());
					changed = true;
					if (!obs.getHostModel().getRecorderDisabled()) {
						obs.initializePEQ(obs.isEastProbe() ? obs.getHostModel().getMovie().getCapacity() : -1);
					}
				}
				if (obs.isNorthProbe() != northProbeCheckBox.isSelected()) {
					obs.setNorthProbe(northProbeCheckBox.isSelected());
					changed = true;
					if (!obs.getHostModel().getRecorderDisabled()) {
						obs.initializePNQ(obs.isNorthProbe() ? obs.getHostModel().getMovie().getCapacity() : -1);
					}
				}
				if (obs.isSouthProbe() != southProbeCheckBox.isSelected()) {
					obs.setSouthProbe(southProbeCheckBox.isSelected());
					changed = true;
					if (!obs.getHostModel().getRecorderDisabled()) {
						obs.initializePSQ(obs.isSouthProbe() ? obs.getHostModel().getMovie().getCapacity() : -1);
					}
				}
				if (changed) {
					obs.getHostModel().notifyChange();
					obs.getHostModel().getView().repaint();
				}
				destroy();
			}
		}) {
		};

		densityField.setAction(okAction);
		widthField.setAction(okAction);
		heightField.setAction(okAction);
		x0Field.setAction(okAction);
		y0Field.setAction(okAction);
		vxField.setAction(okAction);
		vyField.setAction(okAction);
		fxField.setAction(okAction);
		fyField.setAction(okAction);
		frictionField.setAction(okAction);
		elasticityField.setAction(okAction);

		/* lay out components */

		JPanel panel = new JPanel(new SpringLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(10, 10, 10, 10)));
		add(panel, BorderLayout.NORTH);

		// row 1
		panel.add(new JLabel("Type and index"));
		panel.add(createLabel(obs.toString()));
		panel.add(roundedComboBox);

		// row 2
		panel.add(new JLabel("Elasticity of collision with particles"));
		panel.add(elasticityField);
		panel.add(new JLabel("[0, 1]"));

		// row 3
		panel.add(new JLabel("Density"));
		panel.add(densityField);
		panel.add(createSmallerFontLabel("<html>kg/(mol*&#197;<sup>2</sup>)</html>"));

		// row 4
		panel.add(new JLabel("When hitting boundary"));
		panel.add(boundaryComboBox);
		panel.add(new JPanel());

		// row 5
		s = MDView.getInternationalText("Width");
		panel.add(new JLabel(s != null ? s : "Width"));
		panel.add(widthField);
		panel.add(createSmallerFontLabel("\u00c5"));

		// row 6
		s = MDView.getInternationalText("Height");
		panel.add(new JLabel(s != null ? s : "Height"));
		panel.add(heightField);
		panel.add(createSmallerFontLabel("\u00c5"));

		// row 7
		panel.add(new JLabel("<html><em>X</em><sub>0</sub> (left side)</html>"));
		panel.add(x0Field);
		panel.add(createSmallerFontLabel("\u00c5"));

		// row 8
		panel.add(new JLabel("<html><em>Y</em><sub>0</sub> (upper side)</html>"));
		panel.add(y0Field);
		panel.add(createSmallerFontLabel("\u00c5"));

		// row 9
		panel.add(new JLabel("<html><em>V<sub>x</sub></em></html>"));
		panel.add(vxField);
		panel.add(createSmallerFontLabel("m/s"));

		// row 10
		panel.add(new JLabel("<html><em>V<sub>y</sub></em></html>"));
		panel.add(vyField);
		panel.add(createSmallerFontLabel("m/s"));

		// row 11
		panel.add(new JLabel("Horizontal force (per mass unit)"));
		panel.add(fxField);
		panel.add(createSmallerFontLabel("<html>1000&#197;/fs<sup>2</sup></html>"));

		// row 12
		panel.add(new JLabel("Vertical force factor (per mass unit)"));
		panel.add(fyField);
		panel.add(createSmallerFontLabel("<html>1000&#197;/fs<sup>2</sup></html>"));

		// row 13
		panel.add(new JLabel("Damping force (per mass unit)"));
		panel.add(frictionField);
		panel.add(createSmallerFontLabel("<html>eV*fs/&#197;<sup>2</sup></html>"));

		// row 14
		s = MDView.getInternationalText("FillingColorAndPattern");
		panel.add(new JLabel(s != null ? s : "Filling color and pattern"));
		panel.add(bgComboBox);
		panel.add(visibleComboBox);

		makeCompactGrid(panel, 14, 3, 5, 5, 10, 2);

		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		add(panel, BorderLayout.CENTER);

		s = MDView.getInternationalText("Permeability");
		panel.add(new JLabel((s != null ? s : "Permeability") + ":"));

		panel.add(ntPassCheckBox);
		panel.add(plPassCheckBox);
		panel.add(wsPassCheckBox);
		panel.add(ckPassCheckBox);

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(panel, BorderLayout.SOUTH);

		s = MDView.getInternationalText("PressureProbeOnSurface");
		panel.add(new JLabel((s != null ? s : "Pressure gauge on surface") + ":"));

		panel.add(westProbeCheckBox);
		westProbeLabel.setAction(new Runnable() {
			public void run() {
				if (probePanel == null)
					probePanel = new PressureProbePanel();
				probePanel.createDialog(obs, RectangularObstacle.WEST).setVisible(true);
			}
		});
		panel.add(westProbeLabel);

		panel.add(eastProbeCheckBox);
		eastProbeLabel.setAction(new Runnable() {
			public void run() {
				if (probePanel == null)
					probePanel = new PressureProbePanel();
				probePanel.createDialog(obs, RectangularObstacle.EAST).setVisible(true);
			}
		});
		panel.add(eastProbeLabel);

		panel.add(northProbeCheckBox);
		northProbeLabel.setAction(new Runnable() {
			public void run() {
				if (probePanel == null)
					probePanel = new PressureProbePanel();
				probePanel.createDialog(obs, RectangularObstacle.NORTH).setVisible(true);
			}
		});
		panel.add(northProbeLabel);

		panel.add(southProbeCheckBox);
		southProbeLabel.setAction(new Runnable() {
			public void run() {
				if (probePanel == null)
					probePanel = new PressureProbePanel();
				probePanel.createDialog(obs, RectangularObstacle.SOUTH).setVisible(true);
			}
		});
		panel.add(southProbeLabel);

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

}