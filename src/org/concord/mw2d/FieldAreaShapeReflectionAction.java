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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.concord.mw2d.models.FieldArea;

/**
 * @author Charles Xie
 * 
 */
class FieldAreaShapeReflectionAction {

	private FieldArea area;

	public FieldAreaShapeReflectionAction(FieldArea area) {
		this.area = area;
	}

	JDialog createDialog(JComponent parent) {

		String s = MDView.getInternationalText("Reflection");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(parent), s != null ? s : "Reflection", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		ButtonGroup bg = new ButtonGroup();
		s = MDView.getInternationalText("None");
		final JRadioButton rb1 = new JRadioButton(s != null ? s : "None");
		bg.add(rb1);
		p.add(rb1);
		s = MDView.getInternationalText("ExternalReflection");
		final JRadioButton rb2 = new JRadioButton(s != null ? s : "External Reflection");
		bg.add(rb2);
		p.add(rb2);
		s = MDView.getInternationalText("InternalReflection");
		final JRadioButton rb3 = new JRadioButton(s != null ? s : "Internal Reflection");
		bg.add(rb3);
		p.add(rb3);

		switch (area.getReflectionType()) {
		case FieldArea.NO_REFLECTION:
			rb1.setSelected(true);
			break;
		case FieldArea.EXTERNAL_REFLECTION:
			rb2.setSelected(true);
			break;
		case FieldArea.INTERNAL_REFLECTION:
			rb3.setSelected(true);
			break;
		}

		dialog.getContentPane().add(p, BorderLayout.CENTER);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		s = MDView.getInternationalText("OKButton");
		JButton b = new JButton(s != null ? s : "OK");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (rb1.isSelected()) {
					area.setReflectionType(FieldArea.NO_REFLECTION);
				}
				else if (rb2.isSelected()) {
					area.setReflectionType(FieldArea.EXTERNAL_REFLECTION);
				}
				else if (rb3.isSelected()) {
					area.setReflectionType(FieldArea.INTERNAL_REFLECTION);
				}
				dialog.dispose();
			}
		});
		p.add(b);

		s = MDView.getInternationalText("CancelButton");
		b = new JButton(s != null ? s : "Cancel");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		p.add(b);

		dialog.getContentPane().add(p, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(parent);

		return dialog;

	}

}
