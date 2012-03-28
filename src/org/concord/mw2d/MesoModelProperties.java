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
import java.util.Date;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
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
import org.concord.modeler.ui.PastableTextArea;
import org.concord.modeler.ui.RealNumberTextField;
import org.concord.mw2d.MDView;
import org.concord.mw2d.models.ElectricField;
import org.concord.mw2d.models.GravitationalField;
import org.concord.mw2d.models.MDModel;
import org.concord.mw2d.models.MagneticField;
import org.concord.mw2d.models.MesoModel;
import org.concord.mw2d.models.RectangularBoundary;
import org.concord.mw2d.ui.MDContainer;

class MesoModelProperties extends ModelProperties {

	private MesoModel model;
	private JTextArea scriptArea;
	private RealNumberTextField widthField, heightField, viscosityField, stepField;
	private JPanel scriptPanel;

	public MesoModelProperties(Frame owner) {
		super(owner);
	}

	void confirm() {

		Component cp = model.getView().getParent();
		if (cp instanceof MDContainer) {
			int w = (int) Math.round(10.0 * widthField.getValue());
			int h = (int) Math.round(10.0 * heightField.getValue());
			((MDContainer) cp).getView().resize(new Dimension(w, h), false);
		}

		model.getUniverse().setViscosity((float) viscosityField.getValue());
		model.setTimeStep(stepField.getValue());
		String s = scriptArea.getText();
		if (s != null && !s.trim().equals("")) {
			model.setInitializationScript(scriptArea.getText());
		}
		else {
			model.setInitializationScript(null);
		}

		model.notifyPageComponentListeners(new PageComponentEvent(model, PageComponentEvent.COMPONENT_CHANGED));

	}

	public void setModel(MDModel m) {
		setModel2((MesoModel) m);
	}

	void selectInitializationScriptTab() {
		tabbedPane.setSelectedComponent(scriptPanel);
	}

	private void setModel2(MesoModel m) {

		String s = MDView.getInternationalText("MesoscaleParticleSimulatorProperties");
		setTitle(s != null ? s : "Mesoscale Particle Simulator (Gay-Berne Model)");

		tabbedPane.removeAll();

		model = m;

		Dimension dim = model.getView().getSize();
		widthField = new RealNumberTextField(dim.width * 0.1, 10.0, 4000);
		heightField = new RealNumberTextField(dim.height * 0.1, 10.0, 4000);
		viscosityField = new RealNumberTextField(m.getUniverse().getViscosity(), 0.1, 5.0);
		stepField = new RealNumberTextField(model.getTimeStep(), 0.00001, 5.0, 8);

		/* objects */

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
		s = MDView.getInternationalText("ObjectTab");
		tabbedPane.add(s != null ? s : "Objects", panel);

		JPanel p = new JPanel(new GridLayout(7, 3, 2, 2));
		panel.add(p, BorderLayout.NORTH);

		s = MDView.getInternationalText("NumberOfParticlesLabel");
		p.add(new JLabel(s != null ? s : "# Particles", SwingConstants.LEFT));
		JLabel label = new JLabel(Integer.toString(model.getNumberOfParticles()));
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
		p.add(new JLabel(s != null ? s : "Boundary", SwingConstants.LEFT));
		s = "";
		switch (model.getBoundary().getType()) {
		case RectangularBoundary.DBC_ID:
			s = "Default";
			break;
		case RectangularBoundary.RBC_ID:
			s = "Reflective";
			break;
		case RectangularBoundary.PBC_ID:
			s = "Periodic";
			break;
		case RectangularBoundary.XRYPBC_ID:
			s = "X-ref.,Y-per.";
			break;
		case RectangularBoundary.XPYRBC_ID:
			s = "X-per.,Y-ref.";
			break;
		}
		label = new JLabel(s);
		label.setBorder(BUTTON_BORDER);
		p.add(label);
		p.add(new JPanel());

		s = MDView.getInternationalText("PhysicalBoundLabel");
		p.add(new JLabel(s != null ? s : "Physical Bound", SwingConstants.LEFT));
		label = new JLabel(model.getBoundary().getType() == RectangularBoundary.DBC_ID ? "(0 / 0 / " + format(0.1 * dim.width) + " / "
				+ format(0.1 * dim.height) + ")" : "(" + format(0.1 * model.getBoundary().x) + " / " + format(0.1 * model.getBoundary().y) + " / "
				+ format(0.1 * model.getBoundary().width) + " / " + format(0.1 * model.getBoundary().height) + ")");
		label.setBorder(BUTTON_BORDER);
		p.add(label);
		p.add(new JLabel(" \u00c5"));

		s = MDView.getInternationalText("MediumViscosityLabel");
		p.add(new JLabel(s != null ? s : "Medium Viscosity", SwingConstants.LEFT));
		p.add(viscosityField);
		p.add(new JLabel(" dimensionless"));

		s = MDView.getInternationalText("TimeStepLabel");
		p.add(new JLabel(s != null ? s : "MD Time Step", SwingConstants.LEFT));
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
		info += (date != null ? (s != null ? s : "<b>Last modified</b>") + ": " + date : "The current model has never been saved.")
				+ "</font></body></html>";
		panel.add(new JLabel(info), BorderLayout.SOUTH);

		/* interations */

		panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
		s = MDView.getInternationalText("InteractionTab");
		tabbedPane.add(s != null ? s : "Interactions", panel);

		p = new JPanel(new GridLayout(5, 2, 2, 2));
		panel.add(p, BorderLayout.NORTH);

		p.add(new JLabel("Gay-Berne", SwingConstants.LEFT));
		label = new JLabel("Always on");
		label.setBorder(BUTTON_BORDER);
		p.add(label);

		s = MDView.getInternationalText("GravitationalFieldLabel");
		label = new HyperlinkLabel("<html><font color=\"#0000ff\"><u>" + (s != null ? s : "Gravitational Field") + "</u></font></html>",
				SwingConstants.LEFT);
		label.setToolTipText("Click to open the Gravity Tool");
		((HyperlinkLabel) label).setAction(new Runnable() {
			public void run() {
				((MDView) model.getView()).getActionMap().get("Edit Gravitational Field").actionPerformed(null);
			}
		});
		p.add(label);
		label = new JLabel(model.getNonLocalField(GravitationalField.class.getName()) != null ? "On" : "Off");
		label.setBorder(BUTTON_BORDER);
		p.add(label);

		s = MDView.getInternationalText("ElectricFieldLabel");
		s = MDView.getInternationalText("ElectricFieldLabel");
		label = new HyperlinkLabel("<html><font color=\"#0000ff\"><u>" + (s != null ? s : "Electric Field") + "</u></font></html>",
				SwingConstants.LEFT);
		label.setToolTipText("Click to open the Electric Field Tool");
		((HyperlinkLabel) label).setAction(new Runnable() {
			public void run() {
				((MDView) model.getView()).getActionMap().get("Edit Electric Field").actionPerformed(null);
			}
		});
		p.add(label);
		label = new JLabel(model.getNonLocalField(ElectricField.class.getName()) != null ? "On" : "Off");
		label.setBorder(BUTTON_BORDER);
		p.add(label);

		s = MDView.getInternationalText("MagneticFieldLabel");
		s = MDView.getInternationalText("MagneticFieldLabel");
		label = new HyperlinkLabel("<html><font color=\"#0000ff\"><u>" + (s != null ? s : "Magnetic Field") + "</u></font></html>",
				SwingConstants.LEFT);
		label.setToolTipText("Click to open the Magnetic Field Tool");
		((HyperlinkLabel) label).setAction(new Runnable() {
			public void run() {
				((MDView) model.getView()).getActionMap().get("Edit Magnetic Field").actionPerformed(null);
			}
		});
		p.add(label);
		label = new JLabel(model.getNonLocalField(MagneticField.class.getName()) != null ? "On" : "Off");
		label.setBorder(BUTTON_BORDER);
		p.add(label);

		/* dielectric constant */

		panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
		s = MDView.getInternationalText("DielectricsTab");
		tabbedPane.add(s != null ? s : "Dielectics", panel);

		JSlider slider = new JSlider(JSlider.HORIZONTAL, 1, 80, (int) model.getUniverse().getDielectricConstant());
		slider.setToolTipText("Change the dielectric constant");
		slider.setPaintLabels(true);
		slider.setPaintTicks(true);
		slider.setPaintTrack(true);
		slider.setSnapToTicks(true);
		slider.setMajorTickSpacing(20);
		slider.setMinorTickSpacing(5);
		slider.setBorder(new TitledBorder(null, "Dielectric Constant (Dimensionless)", 0, 0));

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
		panel.add(new JLabel(new ImageIcon(model.getView().getClass().getResource("images/Dielectrics.gif"))), BorderLayout.NORTH);

		/* script */

		scriptPanel = new JPanel(new BorderLayout(5, 5));
		scriptPanel
				.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
		s = MDView.getInternationalText("ScriptTab");
		tabbedPane.add(s != null ? s : "Script", scriptPanel);

		p = new JPanel(new BorderLayout(10, 10));
		p.setPreferredSize(new Dimension(400, 150));
		s = MDView.getInternationalText("ScriptToRunAfterLoadingModelLabel");
		p.setBorder(new TitledBorder(BorderFactory.createEtchedBorder(), s != null ? s : "Script to run right after loading page", 0, 0));

		scriptArea = new PastableTextArea(model.getInitializationScript());
		JScrollPane scrollPane = new JScrollPane(scriptArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		p.add(scrollPane, BorderLayout.CENTER);

		s = MDView.getInternationalText("CautionLabel");
		p.add(new JLabel(
				"<html><body><font size=2><b>"
						+ (s != null ? s : "Caution")
						+ ":</b><br>(a) These scripts won't run if the model is not loaded via page loading.<br>(b) Long-running scripts can interfer with authoring.<br>(c) Don't use motion scripts if the model won't be seen upon loading.</font></body></html>"),
				BorderLayout.SOUTH);

		scriptPanel.add(p, BorderLayout.CENTER);

		pack();

	}

}