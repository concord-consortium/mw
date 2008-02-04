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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.concord.modeler.VectorFlavor;
import org.concord.modeler.draw.LineStyle;
import org.concord.modeler.draw.LineWidth;
import org.concord.modeler.draw.StrokeFactory;
import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.ComboBoxRenderer;

class VectorDisplay extends JDialog {

	final static byte V_VECTOR = 101;
	final static byte P_VECTOR = 102;
	final static byte A_VECTOR = 103;
	final static byte F_VECTOR = 104;

	private final static short A_SCALE = 10;

	private byte type = V_VECTOR;
	private MDView view;

	private JComboBox comboBoxLineWidth;
	private JComboBox comboBoxLineStyle;
	private ColorComboBox comboBoxLineColor;
	private JSlider sliderLineLength;

	VectorDisplay(Frame owner) {

		super(owner, "Customize Vector Display", false);
		setResizable(false);
		setSize(400, 400);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JPanel p = new JPanel(new GridLayout(2, 2, 5, 5));

		comboBoxLineWidth = new JComboBox();
		comboBoxLineWidth.setRenderer(new ComboBoxRenderer.LineThickness());
		String s = MDView.getInternationalText("LineThickness");
		comboBoxLineWidth.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Line Thickness"));
		comboBoxLineWidth.setBackground(p.getBackground());
		comboBoxLineWidth.addItem(new Float(LineWidth.STROKE_WIDTH_1));
		comboBoxLineWidth.addItem(new Float(LineWidth.STROKE_WIDTH_2));
		comboBoxLineWidth.addItem(new Float(LineWidth.STROKE_WIDTH_3));
		comboBoxLineWidth.addItem(new Float(LineWidth.STROKE_WIDTH_4));
		comboBoxLineWidth.addItem(new Float(LineWidth.STROKE_WIDTH_5));

		comboBoxLineStyle = new JComboBox();
		comboBoxLineStyle.setRenderer(new ComboBoxRenderer.LineStyles());
		s = MDView.getInternationalText("Style");
		comboBoxLineStyle.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Style"));
		comboBoxLineStyle.setBackground(p.getBackground());
		comboBoxLineStyle.addItem(new Integer(LineStyle.STROKE_NUMBER_1));
		comboBoxLineStyle.addItem(new Integer(LineStyle.STROKE_NUMBER_2));
		comboBoxLineStyle.addItem(new Integer(LineStyle.STROKE_NUMBER_3));
		comboBoxLineStyle.addItem(new Integer(LineStyle.STROKE_NUMBER_4));
		comboBoxLineStyle.addItem(new Integer(LineStyle.STROKE_NUMBER_5));

		sliderLineLength = new JSlider(JSlider.HORIZONTAL, 1, 200, 50);
		sliderLineLength.setPaintLabels(false);
		sliderLineLength.setPaintTicks(false);
		sliderLineLength.setPaintTrack(true);
		sliderLineLength.setSnapToTicks(true);
		sliderLineLength.setMajorTickSpacing(2);
		sliderLineLength.setMinorTickSpacing(1);
		sliderLineLength.setPreferredSize(new Dimension(200, 50));
		s = MDView.getInternationalText("LineLength");
		sliderLineLength.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Line Length"));

		comboBoxLineColor = new ColorComboBox(owner);
		s = MDView.getInternationalText("Color");
		comboBoxLineColor.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Color"));
		comboBoxLineColor.setBackground(p.getBackground());

		p.add(comboBoxLineWidth);
		p.add(comboBoxLineStyle);
		p.add(comboBoxLineColor);
		p.add(sliderLineLength);

		getContentPane().add(p, BorderLayout.CENTER);

		p = new JPanel(new FlowLayout(FlowLayout.CENTER));

		Dimension dim = new Dimension(100, 20);

		s = MDView.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.setPreferredSize(dim);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				VectorDisplay.this.dispose();
			}
		});
		p.add(button);

		s = MDView.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.setPreferredSize(dim);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				VectorDisplay.this.dispose();
			}
		});
		p.add(button);

		s = MDView.getInternationalText("Apply");
		button = new JButton(s != null ? s : "Apply");
		button.setPreferredSize(dim);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
			}
		});
		p.add(button);

		getContentPane().add(p, BorderLayout.SOUTH);

		pack();

	}

	public void setView(MDView view) {
		this.view = view;
	}

	public void setType(byte i) {
		type = i;
		switch (type) {
		case V_VECTOR:
			String s = MDView.getInternationalText("CustomizeVelocityVector");
			setTitle(s != null ? s : "Customize Velocity Vectors");
			if (view != null) {
				VectorFlavor vf = view.getVelocityFlavor();
				comboBoxLineColor.setColor(vf.getColor());
				comboBoxLineWidth.setSelectedIndex(Math.round(vf.getWidth()) - 1);
				comboBoxLineStyle.setSelectedIndex(vf.getStyle());
				sliderLineLength.setValue(vf.getLength());
			}
			break;
		case P_VECTOR:
			s = MDView.getInternationalText("CustomizeMomentumVector");
			setTitle(s != null ? s : "Customize Momentum Vectors");
			if (view != null) {
				VectorFlavor vf = view.getMomentumFlavor();
				comboBoxLineColor.setColor(vf.getColor());
				comboBoxLineWidth.setSelectedIndex(Math.round(vf.getWidth()) - 1);
				comboBoxLineStyle.setSelectedIndex(vf.getStyle());
				sliderLineLength.setValue(vf.getLength());
			}
			break;
		case A_VECTOR:
			s = MDView.getInternationalText("CustomizeAccelerationVector");
			setTitle(s != null ? s : "Customize Acceleration Vectors");
			if (view != null) {
				VectorFlavor vf = view.getAccelerationFlavor();
				comboBoxLineColor.setColor(vf.getColor());
				comboBoxLineWidth.setSelectedIndex(Math.round(vf.getWidth()) - 1);
				comboBoxLineStyle.setSelectedIndex(vf.getStyle());
				sliderLineLength.setValue(vf.getLength() / A_SCALE);
			}
			break;
		case F_VECTOR:
			s = MDView.getInternationalText("CustomizeForceVector");
			setTitle(s != null ? s : "Customize Force Vectors");
			if (view != null) {
				VectorFlavor vf = view.getForceFlavor();
				comboBoxLineColor.setColor(vf.getColor());
				comboBoxLineWidth.setSelectedIndex(Math.round(vf.getWidth()) - 1);
				comboBoxLineStyle.setSelectedIndex(vf.getStyle());
				sliderLineLength.setValue(vf.getLength() / A_SCALE);
			}
			break;
		}
	}

	public byte getType() {
		return type;
	}

	private void confirm() {
		if (view == null)
			return;
		float w = ((Float) comboBoxLineWidth.getSelectedItem()).floatValue();
		int length = sliderLineLength.getValue();
		int style = ((Integer) comboBoxLineStyle.getSelectedItem()).intValue();
		switch (type) {
		case V_VECTOR:
			view.velocityFlavor.setLength(length);
			view.velocityFlavor.setColor(comboBoxLineColor.getSelectedColor());
			view.velocityFlavor.setStyle(style);
			view.velocityFlavor.setStroke(StrokeFactory.createStroke(w, LineStyle.STROKES[style].getDashArray()));
			break;
		case P_VECTOR:
			view.momentumFlavor.setLength(length);
			view.momentumFlavor.setColor(comboBoxLineColor.getSelectedColor());
			view.momentumFlavor.setStyle(style);
			view.momentumFlavor.setStroke(StrokeFactory.createStroke(w, LineStyle.STROKES[style].getDashArray()));
			break;
		case A_VECTOR:
			view.accelerationFlavor.setLength(length * A_SCALE);
			view.accelerationFlavor.setColor(comboBoxLineColor.getSelectedColor());
			view.accelerationFlavor.setStyle(style);
			view.accelerationFlavor.setStroke(StrokeFactory.createStroke(w, LineStyle.STROKES[style].getDashArray()));
			break;
		case F_VECTOR:
			view.forceFlavor.setLength(length * A_SCALE);
			view.forceFlavor.setColor(comboBoxLineColor.getSelectedColor());
			view.forceFlavor.setStyle(style);
			view.forceFlavor.setStroke(StrokeFactory.createStroke(w, LineStyle.STROKES[style].getDashArray()));
			break;
		}
		view.repaint();
		view.getModel().notifyPageComponentListeners(
				new PageComponentEvent(view.getModel(), PageComponentEvent.COMPONENT_CHANGED));
	}

}
