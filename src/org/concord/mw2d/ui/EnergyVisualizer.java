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

package org.concord.mw2d.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import org.concord.modeler.draw.LineSymbols;
import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.g2d.AxisLabel;
import org.concord.modeler.g2d.Curve;
import org.concord.modeler.g2d.CurveFlavor;
import org.concord.modeler.g2d.CurveGroup;
import org.concord.modeler.g2d.Legend;
import org.concord.modeler.g2d.Symbol;
import org.concord.modeler.g2d.XYGrapher;
import org.concord.modeler.ui.PrintableComponent;
import org.concord.mw2d.MDView;
import org.concord.mw2d.models.MDModel;

class EnergyVisualizer extends PrintableComponent {

	private JLabel[] tf;
	private JLabel timeField;
	private NumberFormat format;
	private CurveGroup cg;
	private XYGrapher grapher;
	private MDModel model;
	private CurveFlavor kecf, pecf, tecf;
	private Curve keCurve, peCurve, teCurve;
	private int maxLength;

	public EnergyVisualizer(MDModel m) {

		model = m;

		String s = MDView.getInternationalText("Energy");
		String s1 = MDView.getInternationalText("TimeInFemtoseconds");
		cg = new CurveGroup(s != null ? s : "Energies", new AxisLabel(s1 != null ? s1 : "Time (fs)"), new AxisLabel(
				(s != null ? s : "Energies") + " (eV)"));

		setPreferredSize(new Dimension(600, 350));
		setLayout(new BorderLayout());

		format = NumberFormat.getNumberInstance();
		format.setMaximumFractionDigits(4);

		grapher = new XYGrapher();
		grapher.addSnapshotListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				model.notifyPageComponentListeners(new PageComponentEvent(grapher.getGraph(),
						PageComponentEvent.SNAPSHOT_TAKEN));
			}
		});
		add(grapher, BorderLayout.CENTER);

		tf = new JLabel[3];

		JPanel p = new JPanel(new GridLayout(2, 4));
		p.setBorder(new LineBorder(Color.blue, 4));

		p.add(new JLabel(s1 != null ? s1 : "Time (fs)"));
		s = MDView.getInternationalText("KineticEnergyPerParticle");
		p.add(new JLabel(s != null ? s : model.getKinTS().getName()));
		s = MDView.getInternationalText("PotentialEnergyPerParticle");
		p.add(new JLabel(s != null ? s : model.getPotTS().getName()));
		s = MDView.getInternationalText("TotalEnergyPerParticle");
		p.add(new JLabel(s != null ? s : model.getTotTS().getName()));

		timeField = new JLabel("Unknown");
		p.add(timeField);

		for (int i = 0; i < 3; i++) {
			tf[i] = new JLabel("Unknown");
			p.add(tf[i]);
		}

		add(p, BorderLayout.SOUTH);

	}

	public void refresh() {

		if (kecf == null) {
			kecf = new CurveFlavor();
			kecf.setColor(Color.red);
			Symbol sym = new Symbol(LineSymbols.SYMBOL_NUMBER_1, 1);
			sym.setSpacing(10);
			sym.setSize(4);
			kecf.setSymbol(sym);
		}
		if (keCurve == null) {
			keCurve = new Curve(model.getModelTimeQueue(), model.getKinTS(), kecf, new Legend(model.getKinTS()
					.getName()));
		}
		else {
			keCurve.setCurve(model.getModelTimeQueue(), model.getKinTS());
		}
		if (!cg.containsCurve(keCurve))
			cg.addCurve(keCurve);

		if (pecf == null) {
			pecf = new CurveFlavor();
			pecf.setColor(Color.blue);
			Symbol sym = new Symbol(LineSymbols.SYMBOL_NUMBER_2, 1);
			sym.setSpacing(10);
			sym.setSize(4);
			pecf.setSymbol(sym);
		}
		if (peCurve == null) {
			peCurve = new Curve(model.getModelTimeQueue(), model.getPotTS(), pecf, new Legend(model.getPotTS()
					.getName()));
		}
		else {
			peCurve.setCurve(model.getModelTimeQueue(), model.getPotTS());
		}
		if (!cg.containsCurve(peCurve))
			cg.addCurve(peCurve);

		if (tecf == null) {
			tecf = new CurveFlavor();
			tecf.setColor(Color.magenta);
			Symbol sym = new Symbol(LineSymbols.SYMBOL_NUMBER_3, 1);
			sym.setSpacing(10);
			sym.setSize(4);
			tecf.setSymbol(sym);
		}
		if (teCurve == null) {
			teCurve = new Curve(model.getModelTimeQueue(), model.getTotTS(), tecf, new Legend(model.getTotTS()
					.getName()));
		}
		else {
			teCurve.setCurve(model.getModelTimeQueue(), model.getTotTS());
		}
		if (!cg.containsCurve(teCurve))
			cg.addCurve(teCurve);

		grapher.input(cg);

		if (maxLength == 0) {
			int l = 0;
			if (grapher.getFontMetrics() != null) {
				l = grapher.getFontMetrics().stringWidth(model.getKinTS().getName());
				if (l > maxLength)
					maxLength = l;
				l = grapher.getFontMetrics().stringWidth(model.getPotTS().getName());
				if (l > maxLength)
					maxLength = l;
				l = grapher.getFontMetrics().stringWidth(model.getTotTS().getName());
				if (l > maxLength)
					maxLength = l;
			}
		}
		Insets insets = grapher.getGraphInsets();
		grapher.setLegendLocation(grapher.getWidth() - insets.right - maxLength - 20, insets.top + 20);

	}

	public void setText(int i, double d) {
		if (i < 0 && i >= tf.length)
			throw new IllegalArgumentException("No such curve");
		tf[i].setText(format.format(d));
	}

	public void setTime(double time) {
		timeField.setText(format.format(time));
	}

}