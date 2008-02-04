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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.ui.HyperlinkLabel;
import org.concord.modeler.ui.IntegerTextField;
import org.concord.modeler.ui.PastableTextArea;
import org.concord.modeler.ui.RealNumberTextField;
import org.concord.mw2d.AtomisticView;
import org.concord.mw2d.BoundarySetup;
import org.concord.mw2d.MDView;
import org.concord.mw2d.UserAction;
import org.concord.mw2d.models.ElectricField;
import org.concord.mw2d.models.Element;
import org.concord.mw2d.models.GravitationalField;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.MagneticField;
import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.ReactionModel;
import org.concord.mw2d.models.RectangularBoundary;
import org.concord.mw2d.ui.MDContainer;

public class MolecularModelProperties extends ModelProperties {

	private MolecularModel model;
	private JTextArea scriptArea;
	private RealNumberTextField widthField;
	private RealNumberTextField heightField;
	private RealNumberTextField viscosityField;
	private RealNumberTextField moMassField;
	private RealNumberTextField moEpsilonField;
	private RealNumberTextField stepField;
	private IntegerTextField reactionIntervalField;
	private JCheckBox ljCheckBox;
	private JCheckBox interCoulombCheckBox;
	private static BoundarySetup boundarySetup;

	public MolecularModelProperties(Frame owner) {
		super(owner);
	}

	void confirm() {

		if (model == null)
			throw new NullPointerException("model is null");

		Component cp = model.getView().getParent();
		if (cp instanceof MDContainer) {
			int w = (int) Math.round(10.0 * widthField.getValue());
			int h = (int) Math.round(10.0 * heightField.getValue());
			((MDContainer) cp).getView().resize(new Dimension(w, h), false);
		}

		model.setLJBetweenBondPairs(ljCheckBox.isSelected());
		model.setInterCoulomb(interCoulombCheckBox.isSelected());
		model.getUniverse().setViscosity((float) viscosityField.getValue());
		model.setTimeStep(stepField.getValue());
		String s = scriptArea.getText();
		if (s != null && !s.trim().equals("")) {
			model.setInitializationScript(scriptArea.getText());
		}
		else {
			model.setInitializationScript(null);
		}

		if (reactionIntervalField != null && (model instanceof ReactionModel)) {
			int i = reactionIntervalField.getValue();
			model.getJob().getTask(ReactionModel.REACT).setInterval(i);
		}

		model.notifyPageComponentListeners(new PageComponentEvent(model, PageComponentEvent.COMPONENT_CHANGED));

	}

	public void setModel(MDModel m) {
		if (m instanceof ReactionModel) {
			setModel2((ReactionModel) m);
		}
		else {
			setModel2((MolecularModel) m);
		}
	}

	private void setModel2(ReactionModel m) {

		setModel2((MolecularModel) m);
		ljCheckBox.setSelected(true);
		ljCheckBox.setEnabled(false);

		JPanel panel = new JPanel(new GridLayout(1, 3, 5, 3));
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(10, 10, 10, 10)));

		panel.add(new JLabel("Reaction Interval", SwingConstants.LEFT));
		try {
			reactionIntervalField = new IntegerTextField(m.getJob().getTask(ReactionModel.REACT).getInterval(), 1, 100);
		}
		catch (Exception e) {
			reactionIntervalField = new IntegerTextField(10, 1, 100);
		}
		panel.add(reactionIntervalField);

		panel.add(new JLabel("MD steps", SwingConstants.LEFT));

		JPanel p = new JPanel(new BorderLayout());
		p.add(panel, BorderLayout.NORTH);

		String s = MDView.getInternationalText("ReactionTab");
		tabbedPane.add(s != null ? s : "Reaction", p);

	}

	private void setModel2(MolecularModel m) {

		setTitle((m instanceof ReactionModel ? "Chemical Reaction Simulator" : "Basic 2D Molecular Simulator")
				+ " Properties");
		String s = null;
		if (m instanceof ReactionModel) {
			s = MDView.getInternationalText("ChemicalReactorProperties");
			if (s != null)
				setTitle(s);
		}
		else {
			s = MDView.getInternationalText("AtomisticSimulatorProperties");
			if (s != null)
				setTitle(s);
		}

		tabbedPane.removeAll();

		model = m;

		Rectangle2D.Double dim = (Rectangle2D.Double) model.getBoundary().getView();
		widthField = new RealNumberTextField(0.1 * dim.width, 25.0, 0.1 * SCREEN_SIZE.width);
		heightField = new RealNumberTextField(0.1 * dim.height, 10.0, 0.1 * SCREEN_SIZE.height);
		viscosityField = new RealNumberTextField(m.getUniverse().getViscosity(), 0.1, 5.0);
		stepField = new RealNumberTextField(model.getTimeStep(), 0.00001, 5.0, 8);

		/* objects */

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(8, 8, 8, 8)));
		s = MDView.getInternationalText("ObjectTab");
		tabbedPane.add(s != null ? s : "Objects", panel);

		JPanel p = new JPanel(new GridLayout(9, 3, 2, 2));
		panel.add(p, BorderLayout.NORTH);

		s = MDView.getInternationalText("NumberOfAtomsLabel");
		p.add(new JLabel(s != null ? s : "# Atoms", SwingConstants.LEFT));
		JLabel label = new JLabel(Integer.toString(((MolecularModel) model).getNumberOfAtoms()));
		label.setBorder(BUTTON_BORDER);
		p.add(label);
		p.add(new JPanel());

		s = MDView.getInternationalText("NumberOfMoleculesLabel");
		p.add(new JLabel(s != null ? s : "# Molecules", SwingConstants.LEFT));
		label = new JLabel(model.getMolecules().size() + "");
		label.setBorder(BUTTON_BORDER);
		p.add(label);
		p.add(new JPanel());

		s = MDView.getInternationalText("NumberOfObstaclesLabel");
		p.add(new JLabel(s != null ? s : "# Obstacles", SwingConstants.LEFT));
		label = new JLabel(Integer.toString(model.getObstacles().size()));
		label.setBorder(BUTTON_BORDER);
		p.add(label);
		p.add(new JPanel());

		s = MDView.getInternationalText("ViewWidthLabel");
		p.add(new JLabel(s != null ? s : "View Width", SwingConstants.LEFT));
		p.add(widthField);
		p.add(new JLabel(" \u00c5"));

		s = MDView.getInternationalText("ViewHeightLabel");
		p.add(new JLabel(s != null ? s : "View Height", SwingConstants.LEFT));
		p.add(heightField);
		p.add(new JLabel(" \u00c5"));

		s = MDView.getInternationalText("BoundaryLabel");
		label = new HyperlinkLabel("<html><font color=\"#0000ff\"><u>" + (s != null ? s : "Boundary")
				+ "</u></font></html>", SwingConstants.LEFT);
		label.setToolTipText("Click to open the Boundary Tool");
		((HyperlinkLabel) label).setAction(new Runnable() {
			public void run() {
				((MDView) model.getView()).setAction(UserAction.SBOU_ID);
				if (boundarySetup == null) {
					boundarySetup = new BoundarySetup(model);
					boundarySetup.pack();
				}
				else {
					boundarySetup.setModel(model);
				}
				boundarySetup.setCurrentValues();
				boundarySetup.setLocationRelativeTo(model.getView());
				boundarySetup.setVisible(true);
			}
		});
		p.add(label);
		RectangularBoundary bound = model.getBoundary();
		String[] bName = { "Default", "Reflective", "Periodic", "X-ref., Y-per.", "X-per., Y-ref." };
		int k = 0;
		switch (bound.getType()) {
		case RectangularBoundary.RBC_ID:
			k = 1;
			break;
		case RectangularBoundary.PBC_ID:
			k = 2;
			break;
		case RectangularBoundary.XRYPBC_ID:
			k = 3;
			break;
		case RectangularBoundary.XPYRBC_ID:
			k = 4;
			break;
		}
		label = new JLabel(k >= 0 && k < bName.length ? bName[k] : null);
		label.setBorder(BUTTON_BORDER);
		p.add(label);
		p.add(new JPanel());

		s = MDView.getInternationalText("PhysicalBoundLabel");
		p.add(new JLabel(s != null ? s : "Physical Bound", SwingConstants.LEFT));
		label = new JLabel(bound.getType() == RectangularBoundary.DBC_ID ? "(0, 0, " + 0.1f * (float) dim.width + ", "
				+ 0.1f * (float) dim.height + ")" : "(" + 0.1f * (float) bound.x + ", " + 0.1f * (float) bound.y + ", "
				+ 0.1f * (float) bound.width + ", " + 0.1f * (float) bound.height + ")");
		label.setBorder(BUTTON_BORDER);
		p.add(label);
		p.add(new JLabel(" \u00c5"));

		s = MDView.getInternationalText("MediumViscosityLabel");
		p.add(new JLabel(s != null ? s : "Medium Viscosity", SwingConstants.LEFT));
		p.add(viscosityField);
		p.add(new JLabel(" dimensionless"));

		s = MDView.getInternationalText("TimeStepLabel");
		p.add(new JLabel(s != null ? s : "MD Time Steplength", SwingConstants.LEFT));
		p.add(stepField);
		stepField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				model.setTimeStep(stepField.getValue());
				model.notifyPageComponentListeners(new PageComponentEvent(model, PageComponentEvent.COMPONENT_CHANGED));
			}
		});
		p.add(new JLabel(" femtosecond"));

		String info = "<html><body><hr><font size=2>";
		String str = (String) model.getProperty("url");
		s = MDView.getInternationalText("FileLabel");
		if (str != null)
			info += (s != null ? s : "<b>File</b>") + ": " + str + "<br>";
		Date date = (Date) model.getProperty("date");
		s = MDView.getInternationalText("LastModifiedLabel");
		info += (date != null ? (s != null ? s : "<b>Last modified</b>") + ": " + date
				: "The current model has never been saved.")
				+ "</font></body></html>";
		panel.add(new JLabel(info), BorderLayout.SOUTH);

		/* interations */

		panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(8, 8, 8, 8)));
		s = MDView.getInternationalText("InteractionTab");
		tabbedPane.add(s != null ? s : "Interactions", panel);

		p = new JPanel(new GridLayout(9, 2, 2, 2));
		panel.add(p, BorderLayout.NORTH);

		label = new HyperlinkLabel("<html><font color=\"#0000ff\"><u>Lennard-Jones</u></font></html>",
				SwingConstants.LEFT);
		label.setToolTipText("Click to open the Lennard-Jones Potential Editor");
		((HyperlinkLabel) label).setAction(new Runnable() {
			public void run() {
				((AtomisticView) model.getView()).editElements(Element.ID_NT).actionPerformed(null);
			}
		});
		p.add(label);

		ljCheckBox = new JCheckBox("On between covalently-bonded pairs");
		ljCheckBox.setSelected(model.getLJBetweenBondPairs());
		p.add(ljCheckBox);

		label = new JLabel("Electrostatic", SwingConstants.LEFT);
		p.add(label);

		interCoulombCheckBox = new JCheckBox("On between charges");
		interCoulombCheckBox.setSelected(model.getInterCoulomb());
		p.add(interCoulombCheckBox);

		s = MDView.getInternationalText("NumberOfRadialBondsLabel");
		p.add(new JLabel(s != null ? s : "Number of Radial Bonds", SwingConstants.LEFT));

		label = new JLabel(Integer.toString(model.getBonds().size()));
		label.setBorder(BUTTON_BORDER);
		p.add(label);

		s = MDView.getInternationalText("NumberOfAngularBondsLabel");
		p.add(new JLabel(s != null ? s : "Number of Angular Bonds", SwingConstants.LEFT));

		label = new JLabel(Integer.toString(model.getBends().size()));
		label.setBorder(BUTTON_BORDER);
		p.add(label);

		s = MDView.getInternationalText("GravitationalFieldLabel");
		label = new HyperlinkLabel("<html><font color=\"#0000ff\"><u>" + (s != null ? s : "Gravitational Field")
				+ "</u></font></html>", SwingConstants.LEFT);
		label.setToolTipText("Click to open the Gravity Tool");
		((HyperlinkLabel) label).setAction(new Runnable() {
			public void run() {
				model.getView().getActionMap().get("Edit Gravitational Field").actionPerformed(null);
			}
		});
		p.add(label);
		label = new JLabel(model.getNonLocalField(GravitationalField.class.getName()) != null ? "On" : "Off");
		label.setBorder(BUTTON_BORDER);
		p.add(label);

		s = MDView.getInternationalText("ElectricFieldLabel");
		label = new HyperlinkLabel("<html><font color=\"#0000ff\"><u>" + (s != null ? s : "Electric Field")
				+ "</u></font></html>", SwingConstants.LEFT);
		label.setToolTipText("Click to open the Electric Field Tool");
		((HyperlinkLabel) label).setAction(new Runnable() {
			public void run() {
				model.getView().getActionMap().get("Edit Electric Field").actionPerformed(null);
			}
		});
		p.add(label);
		label = new JLabel(model.getNonLocalField(ElectricField.class.getName()) != null ? "On" : "Off");
		label.setBorder(BUTTON_BORDER);
		p.add(label);

		s = MDView.getInternationalText("MagneticFieldLabel");
		label = new HyperlinkLabel("<html><font color=\"#0000ff\"><u>" + (s != null ? s : "Magnetic Field")
				+ "</u></font></html>", SwingConstants.LEFT);
		label.setToolTipText("Click to open the Magnetic Field Tool");
		((HyperlinkLabel) label).setAction(new Runnable() {
			public void run() {
				model.getView().getActionMap().get("Edit Magnetic Field").actionPerformed(null);
			}
		});
		p.add(label);
		label = new JLabel(model.getNonLocalField(MagneticField.class.getName()) != null ? "On" : "Off");
		label.setBorder(BUTTON_BORDER);
		p.add(label);

		label = new JLabel("Epsilon of Mo (eV)", SwingConstants.LEFT);
		p.add(label);
		moEpsilonField = new RealNumberTextField(model.getElement(Element.ID_MO).getEpsilon(), 0, 1);
		moEpsilonField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Element mo = model.getElement(Element.ID_MO);
				mo.setEpsilon(moEpsilonField.getValue());
			}
		});
		p.add(moEpsilonField);

		label = new JLabel("Mass of Mo (g/mol)", SwingConstants.LEFT);
		p.add(label);
		moMassField = new RealNumberTextField(model.getElement(Element.ID_MO).getMass() * 120, 1, 10000);
		moMassField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Element mo = model.getElement(Element.ID_MO);
				mo.setMass(moMassField.getValue() / 120.0);
			}
		});
		p.add(moMassField);

		/* atom profile */
		panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(8, 8, 8, 8)));
		s = MDView.getInternationalText("CompositionTab");
		tabbedPane.add(s != null ? s : "Composition", panel);
		panel.add(new AtomComposition(model), BorderLayout.CENTER);

		/* dielectric constant */

		panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(8, 8, 8, 8)));
		s = MDView.getInternationalText("DielectricsTab");
		tabbedPane.add(s != null ? s : "Dielectics", panel);

		JSlider slider = new JSlider(1, 80);
		slider.setOrientation(JSlider.HORIZONTAL);
		slider.setToolTipText("Change the dielectric constant");
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setPaintTrack(true);
		slider.setSnapToTicks(true);
		slider.setMajorTickSpacing(20);
		slider.setMinorTickSpacing(1);
		slider.setValue((int) model.getUniverse().getDielectricConstant());
		slider.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(), "Dielectric Constant (Dimensionless)", 0,
				0));

		Hashtable<Integer, JLabel> tableOfLabels = new Hashtable<Integer, JLabel>();
		JLabel label4 = new JLabel("80 (water)");
		JLabel label3 = new JLabel("60");
		JLabel label2 = new JLabel("40");
		JLabel label1 = new JLabel("20");
		JLabel label0 = new JLabel("1 (vacuum)");
		Font smallItalicFont = new Font("Arial", Font.ITALIC, 10);
		label0.setFont(smallItalicFont);
		label1.setFont(smallItalicFont);
		label2.setFont(smallItalicFont);
		label3.setFont(smallItalicFont);
		label4.setFont(smallItalicFont);
		tableOfLabels.put(80, label4);
		tableOfLabels.put(60, label3);
		tableOfLabels.put(40, label2);
		tableOfLabels.put(20, label1);
		tableOfLabels.put(1, label0);

		slider.setLabelTable(tableOfLabels);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (model == null)
					return;
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					model.getUniverse().setDielectricConstant(source.getValue());
				}
			}
		});
		panel.add(slider, BorderLayout.CENTER);
		panel.add(new JLabel(new ImageIcon(model.getView().getClass().getResource("images/Dielectrics.gif"))),
				BorderLayout.NORTH);

		/* script */

		panel = new JPanel(new BorderLayout(2, 2));
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory
				.createEmptyBorder(8, 8, 8, 8)));
		s = MDView.getInternationalText("ScriptTab");
		tabbedPane.add(s != null ? s : "Script", panel);

		p = new JPanel(new BorderLayout(10, 10));
		p.setPreferredSize(new Dimension(360, 200));
		s = MDView.getInternationalText("ScriptToRunAfterLoadingModelLabel");
		p.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(), s != null ? s
				: "Script to run right after loading page", 0, 0));
		panel.add(p, BorderLayout.CENTER);

		scriptArea = new PastableTextArea(model.getInitializationScript());
		JScrollPane scrollPane = new JScrollPane(scriptArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		p.add(scrollPane, BorderLayout.CENTER);

		s = MDView.getInternationalText("CautionLabel");
		p
				.add(
						new JLabel(
								"<html><body><font size=2><b>"
										+ (s != null ? s : "Caution")
										+ ":</b><br>(a) These scripts won't run if the model is not loaded via page loading.<br>(b) Long-running scripts can interfer with authoring.<br>(c) Don't use motion scripts if the model won't be seen upon loading.</font></body></html>"),
						BorderLayout.SOUTH);

		pack();

	}

}