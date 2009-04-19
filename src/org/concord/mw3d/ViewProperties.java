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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.draw.FillMode;
import org.concord.modeler.ui.BackgroundComboBox;
import org.concord.modeler.ui.ColorMenu;

class ViewProperties extends JDialog {

	private MolecularView view;
	private BackgroundComboBox bgComboBox;
	private JComboBox styleComboBox;
	private JComboBox axesStyleComboBox;
	private JComboBox vdwLinesRatioComboBox;
	private JComboBox velocityLengthComboBox;
	private JCheckBox fullVdwCheckBox;
	private JCheckBox glassBoxCheckBox;
	private JCheckBox keshadingCheckBox;
	private JCheckBox perspectiveDepthCheckBox;
	private JCheckBox showAxesCheckBox;
	private JCheckBox indexCheckBox;
	private JCheckBox clockCheckBox;
	private JCheckBox chargeCheckBox;
	private JCheckBox vdwLinesCheckBox;
	private JButton closeButton;
	private JLabel displayStyleLabel;

	public ViewProperties(MolecularView v) {

		super(JOptionPane.getFrameForComponent(v), "View Options", false);
		String s = MolecularContainer.getInternationalText("ViewOptions");
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

		s = MolecularContainer.getInternationalText("Close");
		closeButton = new JButton(s != null ? s : "Close");
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.repaint();
				dispose();
			}
		});

		init();
		pack();
		setLocationRelativeTo(view);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				view.repaint();
				dispose();
			}

			public void windowActivated(WindowEvent e) {
				closeButton.requestFocusInWindow();
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

	private void init() {

		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(10, 10, 10, 10)));

		JPanel p = new JPanel(new BorderLayout(5, 5));
		panel.add(p, BorderLayout.NORTH);

		String s = MolecularContainer.getInternationalText("GeneralOptions");
		JPanel p2 = new JPanel(new GridLayout(5, 2, 5, 5));
		p2.setBorder(BorderFactory.createTitledBorder(s != null ? s : "General Options"));
		p.add(p2, BorderLayout.NORTH);

		perspectiveDepthCheckBox = new JCheckBox(view.getActionMap().get("perspective depth"));
		s = MolecularContainer.getInternationalText("PerspectiveDepth");
		if (s != null)
			perspectiveDepthCheckBox.setText(s);
		p2.add(perspectiveDepthCheckBox);

		s = MolecularContainer.getInternationalText("ShowClock");
		clockCheckBox = new JCheckBox(s != null ? s : "Show Clock");
		clockCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				view.setShowClock(clockCheckBox.isSelected());
			}
		});
		p2.add(clockCheckBox);

		s = MolecularContainer.getInternationalText("ShowAxes");
		showAxesCheckBox = new JCheckBox(s != null ? s : "Show Axes");
		showAxesCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				view.setAxesShown(showAxesCheckBox.isSelected());
			}
		});
		p2.add(showAxesCheckBox);

		s = MolecularContainer.getInternationalText("ShowIndex");
		indexCheckBox = new JCheckBox(s != null ? s : "Show Atom Index");
		indexCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				view.setShowAtomIndex(indexCheckBox.isSelected());
			}
		});
		p2.add(indexCheckBox);

		s = MolecularContainer.getInternationalText("ShowCharge");
		chargeCheckBox = new JCheckBox(s != null ? s : "Show Charges");
		chargeCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				view.setShowCharge(chargeCheckBox.isSelected());
			}
		});
		p2.add(chargeCheckBox);

		s = MolecularContainer.getInternationalText("KineticEnergyShading");
		keshadingCheckBox = new JCheckBox(s != null ? s : "K. E. Shading");
		keshadingCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				view.setKeShading(keshadingCheckBox.isSelected());
			}
		});
		p2.add(keshadingCheckBox);

		s = MolecularContainer.getInternationalText("FullSizeForUnbondedAtoms");
		fullVdwCheckBox = new JCheckBox(s != null ? s : "Full-Size Unbonded Atoms");
		fullVdwCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				view.setFullSizeUnbondedAtoms(fullVdwCheckBox.isSelected());
			}
		});
		p2.add(fullVdwCheckBox);

		s = MolecularContainer.getInternationalText("ShowGlassSimulationBox");
		glassBoxCheckBox = new JCheckBox(s != null ? s : "Show Glass Simulation Box");
		glassBoxCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				view.setShowGlassSimulationBox(glassBoxCheckBox.isSelected());
			}
		});
		p2.add(glassBoxCheckBox);

		s = MolecularContainer.getInternationalText("ShowVanderWaalsLines");
		vdwLinesCheckBox = new JCheckBox(s != null ? s : "Show VDW Lines");
		vdwLinesCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean b = e.getStateChange() == ItemEvent.SELECTED;
				view.setShowVdwLines(b);
				vdwLinesRatioComboBox.setEnabled(b);
			}
		});
		p2.add(vdwLinesCheckBox);

		Object[] o = new String[] { "Cutoff at long distance", "Cutoff at medium distance", "Cutoff at short distance" };
		s = MolecularContainer.getInternationalText("CutoffAtLongDistance");
		if (s != null)
			o[0] = s;
		s = MolecularContainer.getInternationalText("CutoffAtMediumDistance");
		if (s != null)
			o[1] = s;
		s = MolecularContainer.getInternationalText("CutoffAtShortDistance");
		if (s != null)
			o[2] = s;
		vdwLinesRatioComboBox = new JComboBox(o);
		vdwLinesRatioComboBox.setSelectedIndex(1);
		vdwLinesRatioComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					switch (vdwLinesRatioComboBox.getSelectedIndex()) {
					case 0:
						view.setVdwLinesRatio(2);
						break;
					case 1:
						view.setVdwLinesRatio(1.67f);
						break;
					case 2:
						view.setVdwLinesRatio(1.33f);
						break;
					}
				}
			}
		});
		p2.add(vdwLinesRatioComboBox);

		p2 = new JPanel(new GridLayout(1, 2, 5, 5));
		s = MolecularContainer.getInternationalText("VectorOptions");
		p2.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Vector Options"));
		p.add(p2, BorderLayout.CENTER);

		s = MolecularContainer.getInternationalText("VelocityVectorDrawingLength");
		p2.add(new JLabel("   " + (s != null ? s : "Drawn Length of Velocity Vector") + ":"));
		velocityLengthComboBox = new JComboBox(new String[] { "100 m/s equiv. 1 angstrom", "50 m/s equiv. 1 angstrom",
				"25 m/s equiv. 1 angstrom", "10 m/s equiv. 1 angstrom" });
		velocityLengthComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					switch (velocityLengthComboBox.getSelectedIndex()) {
					case 0:
						view.setVelocityVectorScalingFactor((short) 1000);
						break;
					case 1:
						view.setVelocityVectorScalingFactor((short) 2000);
						break;
					case 2:
						view.setVelocityVectorScalingFactor((short) 4000);
						break;
					case 3:
						view.setVelocityVectorScalingFactor((short) 10000);
						break;
					}
					view.repaint();
				}
			}
		});
		p2.add(velocityLengthComboBox);

		p2 = new JPanel(new GridLayout(2, 2, 5, 5));
		s = MolecularContainer.getInternationalText("Style");
		p2.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Styles"));
		p.add(p2, BorderLayout.SOUTH);

		s = MolecularContainer.getInternationalText("BackgroundEffect");
		p2.add(new JLabel("  " + (s != null ? s : "Background Filling"), SwingConstants.LEFT));
		p2.add(bgComboBox);

		JPanel p3 = new JPanel(new GridLayout(1, 2, 0, 0));
		p2.add(p3);

		s = MolecularContainer.getInternationalText("Molecule");
		displayStyleLabel = new JLabel("  " + (s != null ? s : "Molecule"), SwingConstants.LEFT);
		p3.add(displayStyleLabel);
		o = new String[] { "Spacefilling", "Ball & stick", "Stick", "Wireframe" };
		styleComboBox = new JComboBox(o);
		styleComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					view.setMolecularStyle((byte) styleComboBox.getSelectedIndex());
				}
			}
		});
		p3.add(styleComboBox);

		p3 = new JPanel(new GridLayout(1, 2, 0, 0));
		p2.add(p3);

		s = MolecularContainer.getInternationalText("AxesDrawing");
		p3.add(new JLabel("  " + (s != null ? s : "Axes Drawing"), SwingConstants.LEFT));
		axesStyleComboBox = new JComboBox(new String[] { "Center", "Corner" });
		axesStyleComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					view.getViewer().setAxisStyle((byte) (((JComboBox) e.getSource()).getSelectedIndex()));
					view.repaint();
				}
			}
		});
		p3.add(axesStyleComboBox);

		p2 = new JPanel(new BorderLayout(5, 5));
		panel.add(p2, BorderLayout.CENTER);

		getContentPane().add(panel, BorderLayout.CENTER);

		panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.add(closeButton);

		getContentPane().add(panel, BorderLayout.SOUTH);

	}

	public void setCurrentValues() {
		if (view == null)
			return;
		bgComboBox.setFillMode(view.getFillMode());
		setStateOfToggleButton(perspectiveDepthCheckBox, view.getViewer().getPerspectiveDepth(), false);
		perspectiveDepthCheckBox.setEnabled(!view.getViewer().getNavigationMode());
		setStateOfToggleButton(clockCheckBox, view.getShowClock(), false);
		setStateOfToggleButton(chargeCheckBox, view.getShowCharge(), false);
		setStateOfToggleButton(keshadingCheckBox, view.getKeShading(), false);
		setStateOfToggleButton(indexCheckBox, view.getShowAtomIndex(), false);
		setStateOfToggleButton(showAxesCheckBox, view.areAxesShown(), false);
		setStateOfToggleButton(vdwLinesCheckBox, view.getShowVdwLines(), false);
		setStateOfToggleButton(fullVdwCheckBox, view.getFullSizeUnbondedAtoms(), false);
		setStateOfToggleButton(glassBoxCheckBox, view.getShowGlassSimulationBox(), false);
		switch (view.getMolecularStyle()) {
		case MolecularView.SPACE_FILLING:
			setStateOfComboBox(styleComboBox, 0);
			break;
		case MolecularView.BALL_AND_STICK:
			setStateOfComboBox(styleComboBox, 1);
			break;
		case MolecularView.STICKS:
			setStateOfComboBox(styleComboBox, 2);
			break;
		case MolecularView.WIREFRAME:
			setStateOfComboBox(styleComboBox, 3);
			break;
		}
		setStateOfComboBox(axesStyleComboBox, view.getViewer().getAxisStyle());
		if (view.getVdwLinesRatio() > MolecularView.DEFAULT_VDW_LINE_RATIO) {
			setStateOfComboBox(vdwLinesRatioComboBox, 0);
		}
		else if (view.getVdwLinesRatio() == MolecularView.DEFAULT_VDW_LINE_RATIO) {
			setStateOfComboBox(vdwLinesRatioComboBox, 1);
		}
		else {
			setStateOfComboBox(vdwLinesRatioComboBox, 2);
		}
		switch (view.getVelocityVectorScalingFactor()) {
		case 1000:
			setStateOfComboBox(velocityLengthComboBox, 0);
			break;
		case 2000:
			setStateOfComboBox(velocityLengthComboBox, 1);
			break;
		case 4000:
			setStateOfComboBox(velocityLengthComboBox, 2);
			break;
		case 10000:
			setStateOfComboBox(velocityLengthComboBox, 3);
			break;
		}
	}

	private void setStateOfToggleButton(JToggleButton cb, boolean b, boolean noText) {

		Action a = cb.getAction();
		ActionListener[] al = cb.getActionListeners();
		ItemListener[] il = cb.getItemListeners();
		String s = cb.getText();

		cb.setAction(null);
		if (al != null) {
			for (ActionListener i : al)
				cb.removeActionListener(i);
		}
		if (il != null) {
			for (ItemListener i : il)
				cb.removeItemListener(i);
		}

		cb.setSelected(b);

		cb.setAction(a);
		if (al != null) {
			for (ActionListener i : al)
				cb.addActionListener(i);
		}
		if (il != null) {
			for (ItemListener i : il)
				cb.addItemListener(i);
		}

		if (noText)
			cb.setText(null);
		else cb.setText(s);

	}

	private void setStateOfComboBox(JComboBox cb, int index) {

		Action a = cb.getAction();
		ActionListener[] al = cb.getActionListeners();
		ItemListener[] il = cb.getItemListeners();

		cb.setAction(null);
		if (al != null) {
			for (ActionListener i : al)
				cb.removeActionListener(i);
		}
		if (il != null) {
			for (ItemListener i : il)
				cb.removeItemListener(i);
		}

		cb.setSelectedIndex(index);

		cb.setAction(a);
		if (al != null) {
			for (ActionListener i : al)
				cb.addActionListener(i);
		}
		if (il != null) {
			for (ItemListener i : il)
				cb.addItemListener(i);
		}

	}

}