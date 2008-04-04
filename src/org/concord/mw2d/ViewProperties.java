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
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.ui.BackgroundComboBox;
import org.concord.modeler.ui.ColorMenu;
import org.concord.modeler.ui.HyperlinkLabel;

class ViewProperties extends JDialog {

	private MDView view;
	private VectorDisplay vd;
	private BackgroundComboBox bgComboBox;
	private JComboBox qualityComboBox, antialiasComboBox;
	private JComboBox styleComboBox;
	private JComboBox restraintStyleComboBox;
	private JComboBox trajectoryStyleComboBox;
	private JComboBox vdwLinesRatioComboBox;
	private JComboBox vdwCirclesStyleComboBox;
	private JCheckBox useJmolCheckBox;
	private JCheckBox indexCheckBox;
	private JCheckBox clockCheckBox;
	private JCheckBox chargeCheckBox;
	private JCheckBox dipoleCheckBox;
	private JCheckBox vdwLinesCheckBox;
	private JCheckBox vdwCirclesCheckBox;
	private JCheckBox externalForceCheckBox;
	private JCheckBox velocityCheckBox;
	private JCheckBox momentumCheckBox;
	private JCheckBox angularMomentumCheckBox;
	private JCheckBox accelerationCheckBox;
	private JCheckBox forceCheckBox;
	private JRadioButton keshadingRadioButton;
	private JRadioButton chargeShadingRadioButton;
	private JRadioButton noShadingRadioButton;
	private JButton closeButton;
	private JLabel displayStyleLabel;

	public ViewProperties(MDView v) {

		super(JOptionPane.getFrameForComponent(v), "View Options", false);
		String s = MDView.getInternationalText("ViewOptionMenuItem");
		if (s != null)
			setTitle(s);
		setResizable(true);

		view = v;

		bgComboBox = new BackgroundComboBox(view, ModelerUtilities.colorChooser, ModelerUtilities.fillEffectChooser);
		bgComboBox.getColorMenu().addNoFillListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.getFillMode() == FillMode.getNoFillMode())
					return;
				FillMode fm = FillMode.getNoFillMode();
				view.setFillMode(fm);
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
				view.getModel().notifyChange();
			}
		});
		bgComboBox.getColorMenu().addColorArrayListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.getFillMode() instanceof FillMode.ColorFill
						&& ((FillMode.ColorFill) view.getFillMode()).getColor().equals(
								bgComboBox.getColorMenu().getColor()))
					return;
				view.setFillMode(new FillMode.ColorFill(bgComboBox.getColorMenu().getColor()));
				view.getModel().notifyChange();
			}
		});
		bgComboBox.getColorMenu().addMoreColorListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (view.getFillMode() instanceof FillMode.ColorFill
						&& ((FillMode.ColorFill) view.getFillMode()).getColor().equals(
								bgComboBox.getColorMenu().getColorChooser().getColor()))
					return;
				FillMode fm = new FillMode.ColorFill(bgComboBox.getColorMenu().getColorChooser().getColor());
				view.setFillMode(fm);
				view.getModel().notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().addHexColorListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color c = bgComboBox.getColorMenu().getHexInputColor(
						view.getFillMode() instanceof FillMode.ColorFill ? ((FillMode.ColorFill) view.getFillMode())
								.getColor() : null);
				if (c == null)
					return;
				if (view.getFillMode() instanceof FillMode.ColorFill
						&& ((FillMode.ColorFill) view.getFillMode()).getColor().equals(c))
					return;
				FillMode fm = new FillMode.ColorFill(c);
				view.setFillMode(fm);
				view.getModel().notifyChange();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, fm);
			}
		});
		bgComboBox.getColorMenu().addFillEffectListeners(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (bgComboBox.getColorMenu().getFillEffectChooser().getFillMode().equals(view.getFillMode()))
					return;
				view.setFillMode(bgComboBox.getColorMenu().getFillEffectChooser().getFillMode());
				view.getModel().notifyChange();
				view.repaint();
				bgComboBox.getColorMenu().firePropertyChange(ColorMenu.FILLING, null, view.getFillMode());
			}
		}, null);
		bgComboBox.setPreferredSize(new Dimension(50, 22));

		s = MDView.getInternationalText("CloseButton");
		closeButton = new JButton(s != null ? s : "Close");
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.repaint();
				dispose();
			}
		});

		if (v instanceof AtomisticView)
			init((AtomisticView) v);
		else if (v instanceof MesoView)
			init((MesoView) v);
		pack();
		setLocationRelativeTo(view);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				view.repaint();
				dispose();
			}

			public void windowActivated(WindowEvent e) {
				closeButton.requestFocus();
			}
		});

	}

	void destroy() {
		bgComboBox.destroy();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				dispose();
			}
		});
		view = null;
	}

	private void init(final AtomisticView av) {

		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(10, 10, 10, 10)));

		JPanel p = new JPanel(new BorderLayout(5, 5));
		panel.add(p, BorderLayout.NORTH);

		String s = MDView.getInternationalText("GeneralOptions");
		JPanel p2 = new JPanel(new GridLayout(5, 2, 5, 5));
		p2.setBorder(BorderFactory.createTitledBorder(s != null ? s : "General Options"));
		p.add(p2, BorderLayout.NORTH);

		s = MDView.getInternationalText("3DEffect");
		useJmolCheckBox = new JCheckBox(s != null ? s : "3D Effect (Jmol)");
		useJmolCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = useJmolCheckBox.isSelected();
				((AtomisticView) view).setUseJmol(b);
				styleComboBox.setEnabled(b);
				displayStyleLabel.setEnabled(b);
				vdwCirclesCheckBox.setEnabled(b);
				if (b)
					((AtomisticView) view).notifyJmol();
			}
		});
		p2.add(useJmolCheckBox);

		clockCheckBox = new JCheckBox(view.getActionMap().get("Show Clock"));
		s = MDView.getInternationalText("ShowClock");
		if (s != null)
			clockCheckBox.setText(s);
		p2.add(clockCheckBox);

		indexCheckBox = new JCheckBox(view.getSwitches().get("Show Particle Index"));
		s = MDView.getInternationalText("ShowIndex");
		if (s != null)
			indexCheckBox.setText(s);
		p2.add(indexCheckBox);

		chargeCheckBox = new JCheckBox(view.getSwitches().get("Show Charge"));
		s = MDView.getInternationalText("ShowCharge");
		if (s != null)
			chargeCheckBox.setText(s);
		p2.add(chargeCheckBox);

		externalForceCheckBox = new JCheckBox(view.getSwitches().get("Show External Force"));
		s = MDView.getInternationalText("ShowExternalForce");
		if (s != null)
			externalForceCheckBox.setText(s);
		p2.add(externalForceCheckBox);
		p2.add(new JPanel());

		s = MDView.getInternationalText("ShowVanderWaalsSpheres");
		vdwCirclesCheckBox = new JCheckBox(s != null ? s : "Show VDW Spheres");
		vdwCirclesCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = vdwCirclesCheckBox.isSelected();
				av.showVDWCircles(b);
				vdwCirclesStyleComboBox.setEnabled(b);
			}
		});
		p2.add(vdwCirclesCheckBox);

		Object[] o = new String[] { "Dotted circle", "Radial color gradient", "Color gradient & dotted circle" };
		s = MDView.getInternationalText("DottedCircle");
		if (s != null)
			o[0] = s;
		s = MDView.getInternationalText("RadialColorGradient");
		if (s != null)
			o[1] = s;
		s = MDView.getInternationalText("RadialColorGradientInsideDottedCircle");
		if (s != null)
			o[2] = s;
		vdwCirclesStyleComboBox = new JComboBox(o);
		vdwCirclesStyleComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					switch (vdwCirclesStyleComboBox.getSelectedIndex()) {
					case 0:
						av.setVDWCircleStyle(StyleConstant.VDW_DOTTED_CIRCLE);
						break;
					case 1:
						av.setVDWCircleStyle(StyleConstant.VDW_RADIAL_COLOR_GRADIENT);
						break;
					case 2:
						byte i = StyleConstant.VDW_DOTTED_CIRCLE | StyleConstant.VDW_RADIAL_COLOR_GRADIENT;
						av.setVDWCircleStyle(i);
						break;
					}
					av.repaint();
				}
			}
		});
		p2.add(vdwCirclesStyleComboBox);

		vdwLinesCheckBox = new JCheckBox(view.getSwitches().get("Show van der Waals interactions"));
		vdwLinesCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				vdwLinesRatioComboBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		s = MDView.getInternationalText("ShowVanderWaalsLines");
		if (s != null)
			vdwLinesCheckBox.setText(s);
		p2.add(vdwLinesCheckBox);

		o = new String[] { "Cutoff at long distance", "Cutoff at medium distance", "Cutoff at short distance" };
		s = MDView.getInternationalText("CutoffAtLongDistance");
		if (s != null)
			o[0] = s;
		s = MDView.getInternationalText("CutoffAtMediumDistance");
		if (s != null)
			o[1] = s;
		s = MDView.getInternationalText("CutoffAtShortDistance");
		if (s != null)
			o[2] = s;
		vdwLinesRatioComboBox = new JComboBox(o);
		vdwLinesRatioComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					switch (vdwLinesRatioComboBox.getSelectedIndex()) {
					case 0:
						av.setVDWLinesRatio(2);
						break;
					case 1:
						av.setVDWLinesRatio(1.67f);
						break;
					case 2:
						av.setVDWLinesRatio(1.33f);
						break;
					}
				}
			}
		});
		p2.add(vdwLinesRatioComboBox);

		p2 = new JPanel(new GridLayout(3, 2, 5, 5));
		s = MDView.getInternationalText("Style");
		p2.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Styles"));
		p.add(p2, BorderLayout.CENTER);

		s = MDView.getInternationalText("DisplayStyle");
		displayStyleLabel = new JLabel("  " + (s != null ? s : "Display Style"), SwingConstants.LEFT);
		p2.add(displayStyleLabel);
		Action a = view.getChoices().get("Display Style");
		styleComboBox = new JComboBox((String[]) a.getValue("options"));
		styleComboBox.setAction(a);
		p2.add(styleComboBox);

		s = MDView.getInternationalText("BackgroundEffect");
		p2.add(new JLabel("  " + (s != null ? s : "Background Style"), SwingConstants.LEFT));
		p2.add(bgComboBox);

		JPanel p3 = new JPanel(new GridLayout(1, 2, 0, 0));
		p2.add(p3);

		s = MDView.getInternationalText("RestraintStyle");
		p3.add(new JLabel("  " + (s != null ? s : "Restraint"), SwingConstants.LEFT));
		restraintStyleComboBox = new JComboBox(new String[] { "Cross", "Heavy Spring", "Light Spring", "Ghost" });
		restraintStyleComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					switch (((JComboBox) e.getSource()).getSelectedIndex()) {
					case 0:
						av.setRestraintStyle(StyleConstant.RESTRAINT_CROSS_STYLE);
						break;
					case 1:
						av.setRestraintStyle(StyleConstant.RESTRAINT_HEAVY_SPRING_STYLE);
						break;
					case 2:
						av.setRestraintStyle(StyleConstant.RESTRAINT_LIGHT_SPRING_STYLE);
						break;
					case 3:
						av.setRestraintStyle(StyleConstant.RESTRAINT_GHOST_STYLE);
						break;
					}
					av.repaint();
				}
			}
		});
		p3.add(restraintStyleComboBox);

		p3 = new JPanel(new GridLayout(1, 2, 0, 0));
		p2.add(p3);

		s = MDView.getInternationalText("TrajectoryStyle");
		p3.add(new JLabel("  " + (s != null ? s : "Trajectory"), SwingConstants.LEFT));
		trajectoryStyleComboBox = new JComboBox(new String[] { "Line", "Dotted Line", "Circles" });
		trajectoryStyleComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					switch (((JComboBox) e.getSource()).getSelectedIndex()) {
					case 0:
						av.setTrajectoryStyle(StyleConstant.TRAJECTORY_LINE_STYLE);
						break;
					case 1:
						av.setTrajectoryStyle(StyleConstant.TRAJECTORY_DOTTEDLINE_STYLE);
						break;
					case 2:
						av.setTrajectoryStyle(StyleConstant.TRAJECTORY_CIRCLES_STYLE);
						break;
					}
					av.repaint();
				}
			}
		});
		p3.add(trajectoryStyleComboBox);

		p2 = new JPanel(new BorderLayout(5, 5));
		panel.add(p2, BorderLayout.CENTER);

		p = new JPanel(new GridLayout(2, 2, 5, 5));
		s = MDView.getInternationalText("ShadingMode");
		p.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Shading Mode"));
		p2.add(p, BorderLayout.NORTH);

		ButtonGroup group = new ButtonGroup();
		s = MDView.getInternationalText("None");
		noShadingRadioButton = new JRadioButton(s != null ? s : "No Shading");
		noShadingRadioButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					av.showShading(false);
					av.showChargeShading(false);
				}
			}
		});
		group.add(noShadingRadioButton);
		p.add(noShadingRadioButton);

		keshadingRadioButton = new JRadioButton(view.getSwitches().get("K. E. Shading"));
		s = MDView.getInternationalText("KineticEnergyShading");
		if (s != null)
			keshadingRadioButton.setText(s);
		p.add(keshadingRadioButton);
		group.add(keshadingRadioButton);

		chargeShadingRadioButton = new JRadioButton("Charge Shading");
		s = MDView.getInternationalText("ChargeShading");
		if (s != null)
			chargeShadingRadioButton.setText(s);
		chargeShadingRadioButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				av.showChargeShading(b);
				av.showShading(!b);
			}
		});
		p.add(chargeShadingRadioButton);
		group.add(chargeShadingRadioButton);

		s = MDView.getInternationalText("VectorMode");
		p = new JPanel(new GridLayout(2, 2, 0, 0));
		p.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Vector Mode"));
		p2.add(p, BorderLayout.CENTER);

		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		p.add(p1);

		velocityCheckBox = new JCheckBox(view.getSwitches().get("Velocity Vector"));
		velocityCheckBox.setText(null);
		p1.add(velocityCheckBox);
		s = MDView.getInternationalText("VelocityVector");
		JLabel label = new HyperlinkLabel("<html><u><font color=\"#0000ff\">" + (s != null ? s : "Velocity Vector")
				+ "</font></u></html>", SwingConstants.LEFT);
		label.setToolTipText("Customize velocity vector display");
		((HyperlinkLabel) label).setAction(new Runnable() {
			public void run() {
				if (vd == null)
					vd = new VectorDisplay(JOptionPane.getFrameForComponent(view));
				vd.setView(view);
				vd.setType(VectorDisplay.V_VECTOR);
				vd.setLocationRelativeTo(ViewProperties.this);
				vd.setVisible(true);
			}
		});
		p1.add(label);

		p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		p.add(p1);

		momentumCheckBox = new JCheckBox(view.getSwitches().get("Momentum Vector"));
		momentumCheckBox.setText(null);
		p1.add(momentumCheckBox);
		s = MDView.getInternationalText("MomentumVector");
		label = new HyperlinkLabel("<html><u><font color=\"#0000ff\">" + (s != null ? s : "Momentum Vector")
				+ "</font></u></html>", SwingConstants.LEFT);
		label.setToolTipText("Customize momentum vector display");
		((HyperlinkLabel) label).setAction(new Runnable() {
			public void run() {
				if (vd == null)
					vd = new VectorDisplay(JOptionPane.getFrameForComponent(view));
				vd.setView(view);
				vd.setType(VectorDisplay.P_VECTOR);
				vd.setLocationRelativeTo(ViewProperties.this);
				vd.setVisible(true);
			}
		});
		p1.add(label);

		p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		p.add(p1);

		accelerationCheckBox = new JCheckBox(view.getSwitches().get("Acceleration Vector"));
		accelerationCheckBox.setText(null);
		p1.add(accelerationCheckBox);
		s = MDView.getInternationalText("AccelerationVector");
		label = new HyperlinkLabel("<html><u><font color=\"#0000ff\">" + (s != null ? s : "Acceleration Vector")
				+ "</font></u></html>", SwingConstants.LEFT);
		label.setToolTipText("Customize acceleration vector display");
		((HyperlinkLabel) label).setAction(new Runnable() {
			public void run() {
				if (vd == null)
					vd = new VectorDisplay(JOptionPane.getFrameForComponent(view));
				vd.setView(view);
				vd.setType(VectorDisplay.A_VECTOR);
				vd.setLocationRelativeTo(ViewProperties.this);
				vd.setVisible(true);
			}
		});
		p1.add(label);

		p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		p.add(p1);

		forceCheckBox = new JCheckBox(view.getSwitches().get("Force Vector"));
		forceCheckBox.setText(null);
		p1.add(forceCheckBox);
		s = MDView.getInternationalText("ForceVector");
		label = new HyperlinkLabel("<html><u><font color=\"#0000ff\">" + (s != null ? s : "Force Vector")
				+ "</font></u></html>", SwingConstants.LEFT);
		label.setToolTipText("Customize force vector display");
		((HyperlinkLabel) label).setAction(new Runnable() {
			public void run() {
				if (vd == null)
					vd = new VectorDisplay(JOptionPane.getFrameForComponent(view));
				vd.setView(view);
				vd.setType(VectorDisplay.F_VECTOR);
				vd.setLocationRelativeTo(ViewProperties.this);
				vd.setVisible(true);
			}
		});
		p1.add(label);

		p = new JPanel(new GridLayout(1, 2, 5, 5));
		s = MDView.getInternationalText("GraphicsOptions");
		p.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Graphics Options"));
		p2.add(p, BorderLayout.SOUTH);

		o = new String[] { "Quality rendering", "Speed rendering" };
		qualityComboBox = new JComboBox(o);
		qualityComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					switch (qualityComboBox.getSelectedIndex()) {
					case 0:
						view.setRenderingMethod(view.getRenderingMethod() ^ MDView.SPEED_RENDERING);
						break;
					case 1:
						view.setRenderingMethod(view.getRenderingMethod() | MDView.SPEED_RENDERING);
						break;
					}
					view.repaint();
				}
			}
		});
		p.add(qualityComboBox);

		o = new String[] { "Anti-aliasing on", "Anti-aliasing off" };
		antialiasComboBox = new JComboBox(o);
		antialiasComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					switch (antialiasComboBox.getSelectedIndex()) {
					case 0:
						view.setRenderingMethod(view.getRenderingMethod() ^ MDView.ANTIALIASING_OFF);
						break;
					case 1:
						view.setRenderingMethod(view.getRenderingMethod() | MDView.ANTIALIASING_OFF);
						break;
					}
					view.repaint();
				}
			}
		});
		p.add(antialiasComboBox);

		getContentPane().add(panel, BorderLayout.CENTER);

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(closeButton);

		getContentPane().add(panel, BorderLayout.SOUTH);

	}

	private void init(final MesoView mv) {

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10),
				BorderFactory.createRaisedBevelBorder()));

		JPanel p = new JPanel(new GridLayout(2, 2, 5, 5));
		String s = MDView.getInternationalText("GeneralOptions");
		p.setBorder(BorderFactory.createTitledBorder(s != null ? s : "General Options"));
		panel.add(p, BorderLayout.NORTH);

		s = MDView.getInternationalText("BackgroundEffect");
		p.add(new JLabel("  " + (s != null ? s : "Background")));
		p.add(bgComboBox);

		clockCheckBox = new JCheckBox(view.getActionMap().get("Show Clock"));
		s = MDView.getInternationalText("ShowClock");
		if (s != null)
			clockCheckBox.setText(s);
		p.add(clockCheckBox);
		p.add(new JPanel());

		p = new JPanel(new GridLayout(6, 1, 5, 5));
		s = MDView.getInternationalText("Style");
		p.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Particle Options"));
		panel.add(p, BorderLayout.CENTER);

		indexCheckBox = new JCheckBox(view.getSwitches().get("Show Particle Index"));
		s = MDView.getInternationalText("ShowIndex");
		if (s != null)
			indexCheckBox.setText(s);
		p.add(indexCheckBox);

		chargeCheckBox = new JCheckBox(view.getSwitches().get("Show Charge"));
		s = MDView.getInternationalText("ShowCharge");
		if (s != null)
			chargeCheckBox.setText(s);
		p.add(chargeCheckBox);

		dipoleCheckBox = new JCheckBox(view.getActionMap().get("Show Dipole Moment"));
		s = MDView.getInternationalText("ShowDipole");
		if (s != null)
			dipoleCheckBox.setText(s);
		p.add(dipoleCheckBox);

		externalForceCheckBox = new JCheckBox(view.getSwitches().get("Show External Force"));
		s = MDView.getInternationalText("ShowExternalForce");
		if (s != null)
			externalForceCheckBox.setText(s);
		p.add(externalForceCheckBox);

		momentumCheckBox = new JCheckBox(view.getActionMap().get("Momentum Vector of Center of Mass"));
		s = MDView.getInternationalText("MomentumVectorOfCenterOfMass");
		if (s != null)
			momentumCheckBox.setText(s);
		p.add(momentumCheckBox);

		angularMomentumCheckBox = new JCheckBox(view.getActionMap().get("Angular Momentum"));
		s = MDView.getInternationalText("AngularMomentum");
		if (s != null)
			angularMomentumCheckBox.setText(s);
		p.add(angularMomentumCheckBox);

		p = new JPanel(new GridLayout(1, 2, 5, 5));
		s = MDView.getInternationalText("GraphicsOptions");
		p.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Graphics Options"));
		panel.add(p, BorderLayout.SOUTH);

		String[] o = new String[] { "Quality rendering", "Speed rendering" };
		qualityComboBox = new JComboBox(o);
		qualityComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					switch (qualityComboBox.getSelectedIndex()) {
					case 0:
						view.setRenderingMethod(view.getRenderingMethod() ^ MDView.SPEED_RENDERING);
						break;
					case 1:
						view.setRenderingMethod(view.getRenderingMethod() | MDView.SPEED_RENDERING);
						break;
					}
					view.repaint();
				}
			}
		});
		p.add(qualityComboBox);

		o = new String[] { "Anti-aliasing on", "Anti-aliasing off" };
		antialiasComboBox = new JComboBox(o);
		antialiasComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					switch (antialiasComboBox.getSelectedIndex()) {
					case 0:
						view.setRenderingMethod(view.getRenderingMethod() ^ MDView.ANTIALIASING_OFF);
						break;
					case 1:
						view.setRenderingMethod(view.getRenderingMethod() | MDView.ANTIALIASING_OFF);
						break;
					}
					view.repaint();
				}
			}
		});
		p.add(antialiasComboBox);

		getContentPane().add(panel, BorderLayout.CENTER);

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(closeButton);

		getContentPane().add(panel, BorderLayout.SOUTH);

	}

	public void setCurrentValues() {
		if (view == null)
			return;
		if ((view.getRenderingMethod() & MDView.SPEED_RENDERING) == MDView.SPEED_RENDERING) {
			setStateOfComboBox(qualityComboBox, 1);
		}
		else {
			setStateOfComboBox(qualityComboBox, 0);
		}
		if ((view.getRenderingMethod() & MDView.ANTIALIASING_OFF) == MDView.ANTIALIASING_OFF) {
			setStateOfComboBox(antialiasComboBox, 1);
		}
		else {
			setStateOfComboBox(antialiasComboBox, 0);
		}
		bgComboBox.setFillMode(view.getFillMode());
		setStateOfToggleButton(indexCheckBox, view.getShowParticleIndex(), false);
		setStateOfToggleButton(clockCheckBox, view.getShowClock(), false);
		setStateOfToggleButton(chargeCheckBox, view.getDrawCharge(), false);
		setStateOfToggleButton(externalForceCheckBox, view.getDrawExternalForce(), false);
		if (view instanceof AtomisticView) {
			AtomisticView av = (AtomisticView) view;
			switch (av.getDisplayStyle()) {
			case StyleConstant.SPACE_FILLING:
				setStateOfComboBox(styleComboBox, 0);
				break;
			case StyleConstant.BALL_AND_STICK:
				setStateOfComboBox(styleComboBox, 1);
				break;
			case StyleConstant.WIRE_FRAME:
				setStateOfComboBox(styleComboBox, 2);
				break;
			case StyleConstant.STICK:
				setStateOfComboBox(styleComboBox, 3);
				break;
			case StyleConstant.DELAUNAY:
				setStateOfComboBox(styleComboBox, 4);
				break;
			case StyleConstant.VORONOI:
				setStateOfComboBox(styleComboBox, 5);
				break;
			}
			setStateOfToggleButton(useJmolCheckBox, av.getUseJmol(), false);
			setStateOfToggleButton(vdwLinesCheckBox, av.vdwLinesShown(), false);
			setStateOfToggleButton(vdwCirclesCheckBox, av.vdwCirclesShown(), false);
			setStateOfToggleButton(velocityCheckBox, av.velocityVectorShown(), true);
			setStateOfToggleButton(momentumCheckBox, av.momentumVectorShown(), true);
			setStateOfToggleButton(accelerationCheckBox, av.accelerationVectorShown(), true);
			setStateOfToggleButton(forceCheckBox, av.forceVectorShown(), true);
			switch (view.getRestraintStyle()) {
			case StyleConstant.RESTRAINT_CROSS_STYLE:
				setStateOfComboBox(restraintStyleComboBox, 0);
				break;
			case StyleConstant.RESTRAINT_HEAVY_SPRING_STYLE:
				setStateOfComboBox(restraintStyleComboBox, 1);
				break;
			case StyleConstant.RESTRAINT_LIGHT_SPRING_STYLE:
				setStateOfComboBox(restraintStyleComboBox, 2);
				break;
			case StyleConstant.RESTRAINT_GHOST_STYLE:
				setStateOfComboBox(restraintStyleComboBox, 3);
				break;
			}
			switch (view.getTrajectoryStyle()) {
			case StyleConstant.TRAJECTORY_LINE_STYLE:
				setStateOfComboBox(trajectoryStyleComboBox, 0);
				break;
			case StyleConstant.TRAJECTORY_DOTTEDLINE_STYLE:
				setStateOfComboBox(trajectoryStyleComboBox, 1);
				break;
			case StyleConstant.TRAJECTORY_CIRCLES_STYLE:
				setStateOfComboBox(trajectoryStyleComboBox, 2);
				break;
			}
			if (av.getVDWLinesRatio() > 1.8)
				setStateOfComboBox(vdwLinesRatioComboBox, 0);
			else if (av.getVDWLinesRatio() > 1.5)
				setStateOfComboBox(vdwLinesRatioComboBox, 1);
			else setStateOfComboBox(vdwLinesRatioComboBox, 2);
			if (av.getVDWCircleStyle() == StyleConstant.VDW_DOTTED_CIRCLE) {
				setStateOfComboBox(vdwCirclesStyleComboBox, 0);
			}
			else if (av.getVDWCircleStyle() == StyleConstant.VDW_RADIAL_COLOR_GRADIENT) {
				setStateOfComboBox(vdwCirclesStyleComboBox, 1);
			}
			else {
				setStateOfComboBox(vdwCirclesStyleComboBox, 2);
			}
			styleComboBox.setEnabled(av.getUseJmol());
			displayStyleLabel.setEnabled(av.getUseJmol());
			vdwCirclesCheckBox.setEnabled(av.getUseJmol());
			boolean b = !av.chargeShadingShown() && !av.shadingShown();
			if (b) {
				setStateOfToggleButton(noShadingRadioButton, true, false);
			}
			else {
				setStateOfToggleButton(keshadingRadioButton, av.shadingShown(), false);
				setStateOfToggleButton(chargeShadingRadioButton, av.chargeShadingShown(), false);
			}
		}
		else if (view instanceof MesoView) {
			MesoView mv = (MesoView) view;
			setStateOfToggleButton(dipoleCheckBox, mv.getDrawDipole(), false);
			setStateOfToggleButton(momentumCheckBox, mv.linearMomentaShown(), false);
			setStateOfToggleButton(angularMomentumCheckBox, mv.angularMomentaShown(), false);
		}
	}

	void setStateOfToggleButton(JToggleButton cb, boolean b, boolean noText) {

		Action a = cb.getAction();
		ActionListener[] al = cb.getActionListeners();
		ItemListener[] il = cb.getItemListeners();
		String s = cb.getText();

		cb.setAction(null);
		if (al != null) {
			for (ActionListener x : al)
				cb.removeActionListener(x);
		}
		if (il != null) {
			for (ItemListener x : il)
				cb.removeItemListener(x);
		}

		cb.setSelected(b);

		cb.setAction(a);
		if (al != null) {
			for (ActionListener x : al)
				cb.addActionListener(x);
		}
		if (il != null) {
			for (ItemListener x : il)
				cb.addItemListener(x);
		}

		if (noText)
			cb.setText(null);
		else cb.setText(s);

	}

	void setStateOfComboBox(JComboBox cb, int index) {

		Action a = cb.getAction();
		ActionListener[] al = cb.getActionListeners();
		ItemListener[] il = cb.getItemListeners();

		cb.setAction(null);
		if (al != null) {
			for (ActionListener x : al)
				cb.removeActionListener(x);
		}
		if (il != null) {
			for (ItemListener x : il)
				cb.removeItemListener(x);
		}

		cb.setSelectedIndex(index);

		cb.setAction(a);
		if (al != null) {
			for (ActionListener x : al)
				cb.addActionListener(x);
		}
		if (il != null) {
			for (ItemListener x : il)
				cb.addItemListener(x);
		}

	}

}