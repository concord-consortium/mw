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
package org.concord.jmol;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.ColorComboBox;
import org.concord.modeler.ui.PastableTextArea;
import org.jmol.api.SiteAnnotation;
import org.jmol.g3d.Graphics3D;

/**
 * @author Charles Xie
 * 
 */
class AnnotationManager {

	private JmolContainer jmolContainer;
	private SiteAnnotation siteAnnotation;

	AnnotationManager(JmolContainer jmolContainer) {
		this.jmolContainer = jmolContainer;
	}

	JDialog getEditor(final int iHost, final byte hostType) {

		siteAnnotation = jmolContainer.getSiteAnnotation(iHost, hostType);

		String s = JmolContainer.getInternationalText("EditSelectedAnnotation");
		final JDialog d = new JDialog(JOptionPane.getFrameForComponent(jmolContainer), s != null ? s
				: "Annotation Settings", true);
		d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		JPanel contentPane = new JPanel(new BorderLayout(5, 5));
		d.setContentPane(contentPane);

		final ColorComboBox bgComboBox = new ColorComboBox(jmolContainer);
		final ColorComboBox kcComboBox = new ColorComboBox(jmolContainer);
		final JTextArea textArea = new PastableTextArea(siteAnnotation == null ? null : siteAnnotation.getText());
		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (siteAnnotation == null) {
					siteAnnotation = new SiteAnnotation();
					siteAnnotation.setHostType(hostType);
				}
				siteAnnotation.setText(textArea.getText());
				siteAnnotation.setBackgroundRgb(bgComboBox.getSelectedColor().getRGB());
				siteAnnotation.setKeyRgb(kcComboBox.getSelectedColor().getRGB());
				jmolContainer.setSiteAnnotation(iHost, siteAnnotation);
				d.dispose();
			}
		};

		JPanel p = new JPanel(new SpringLayout());
		p.setBorder(BorderFactory.createEmptyBorder(10, 10, 2, 10));
		contentPane.add(p, BorderLayout.NORTH);

		s = JmolContainer.getInternationalText("Background");
		p.add(new JLabel(s != null ? s : "Background Color"));
		bgComboBox.setColor(new Color(siteAnnotation != null ? siteAnnotation.getBackgroundRgb() : 0xffFDEDCB));
		p.add(bgComboBox);

		s = JmolContainer.getInternationalText("AnnotationKeyColor");
		p.add(new JLabel(s != null ? s : "Key Color"));
		kcComboBox.setColor(new Color(siteAnnotation != null ? siteAnnotation.getKeyRgb() : Graphics3D
				.getArgb(Graphics3D.GOLD)));
		p.add(kcComboBox);

		ModelerUtilities.makeCompactGrid(p, 2, 2, 5, 5, 10, 2);

		JScrollPane scroller = new JScrollPane(textArea);
		scroller.setPreferredSize(new Dimension(300, 200));
		scroller.setBorder(BorderFactory.createTitledBorder("Please write annotation text below:"));
		contentPane.add(scroller, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		contentPane.add(buttonPanel, BorderLayout.SOUTH);

		s = JmolContainer.getInternationalText("OK");
		JButton button = new JButton(s != null ? s : "OK");
		button.addActionListener(okListener);
		buttonPanel.add(button);

		s = JmolContainer.getInternationalText("Cancel");
		button = new JButton(s != null ? s : "Cancel");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d.dispose();
			}
		});
		buttonPanel.add(button);

		d.pack();
		d.setLocationRelativeTo(jmolContainer);

		return d;

	}
}
