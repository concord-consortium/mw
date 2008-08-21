/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.concord.modeler.draw.LineWidth;
import org.concord.modeler.event.PageComponentEvent;
import org.concord.modeler.ui.ComboBoxRenderer;

class LineOption extends JDialog {

	private MDView view;
	private String type;
	private JComboBox comboBoxLineWidth;

	LineOption(Frame owner) {

		super(owner, "Line Options", false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		String s = MDView.getInternationalText("LineOptions");
		if (s != null)
			setTitle(s);

		JPanel p = new JPanel();

		comboBoxLineWidth = new JComboBox();
		comboBoxLineWidth.setPreferredSize(new Dimension(300, 50));
		comboBoxLineWidth.setRenderer(new ComboBoxRenderer.LineThickness());
		s = MDView.getInternationalText("LineThickness");
		comboBoxLineWidth.setBorder(BorderFactory.createTitledBorder(s != null ? s : "Line Thickness"));
		comboBoxLineWidth.setBackground(p.getBackground());
		comboBoxLineWidth.addItem(new Float(LineWidth.STROKE_WIDTH_1));
		comboBoxLineWidth.addItem(new Float(LineWidth.STROKE_WIDTH_2));
		comboBoxLineWidth.addItem(new Float(LineWidth.STROKE_WIDTH_3));
		comboBoxLineWidth.addItem(new Float(LineWidth.STROKE_WIDTH_4));
		comboBoxLineWidth.addItem(new Float(LineWidth.STROKE_WIDTH_5));

		p.add(comboBoxLineWidth);

		getContentPane().add(p, BorderLayout.CENTER);

		p = new JPanel(new FlowLayout(FlowLayout.CENTER));

		s = MDView.getInternationalText("OKButton");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirm();
				dispose();
			}
		});
		p.add(button);

		s = MDView.getInternationalText("CancelButton");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		p.add(button);

		s = MDView.getInternationalText("Apply");
		button = new JButton(s != null ? s : "Apply");
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

	void setType(String type) {
		this.type = type;
	}

	private void confirm() {
		if (view == null)
			return;
		float w = ((Float) comboBoxLineWidth.getSelectedItem()).floatValue();
		if ("vdw".equals(type)) {
			if (view instanceof AtomisticView) {
				((AtomisticView) view).setVdwLineThickness(w);
			}
		}
		view.repaint();
		view.getModel().notifyPageComponentListeners(
				new PageComponentEvent(view.getModel(), PageComponentEvent.COMPONENT_CHANGED));
	}

}
