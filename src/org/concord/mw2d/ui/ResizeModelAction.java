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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.ui.IconPool;
import org.concord.modeler.ui.IntegerTextField;

class ResizeModelAction extends AbstractAction {

	private MDContainer container;

	ResizeModelAction(MDContainer container) {
		super();
		this.container = container;
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		putValue(NAME, "Change Model Size");
		putValue(SHORT_DESCRIPTION, "Change size of model");
		putValue(SMALL_ICON, IconPool.getIcon("resize"));
	}

	public void actionPerformed(ActionEvent e) {
		if (ModelerUtilities.stopFiring(e))
			return;
		createSizeDialog().setVisible(true);
	}

	private JDialog createSizeDialog() {

		String s = MDContainer.getInternationalText("Resize");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(container), s != null ? s
				: "Set Model Size", true);
		dialog.setSize(200, 200);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		Rectangle2D.Double dim = (Rectangle2D.Double) container.getModel().getBoundary().getView();
		final IntegerTextField widthField = new IntegerTextField((int) dim.width, 350, 1000, 5), heightField = new IntegerTextField(
				(int) dim.height, 100, 1000, 5);

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int w = widthField.getValue();
				if (w < widthField.getMinValue())
					w = widthField.getMinValue();
				else if (w > widthField.getMaxValue())
					w = widthField.getMaxValue();
				int h = heightField.getValue();
				if (h < heightField.getMinValue())
					h = heightField.getMinValue();
				else if (h > heightField.getMaxValue())
					h = heightField.getMaxValue();
				container.getView().resize(new Dimension(w, h), false);
				dialog.dispose();
			}
		};

		widthField.addActionListener(okListener);
		heightField.addActionListener(okListener);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		s = MDContainer.getInternationalText("Width");
		String pix = MDContainer.getInternationalText("Pixels");
		p.add(new JLabel((s != null ? s : "Width") + " (" + widthField.getMinValue() + "-" + widthField.getMaxValue()
				+ (pix != null ? pix : " pixels") + "):"));
		p.add(widthField);
		s = MDContainer.getInternationalText("Height");
		p.add(new JLabel((s != null ? s : "Height") + " (" + heightField.getMinValue() + "-"
				+ heightField.getMaxValue() + (pix != null ? pix : " pixels") + "):"));
		p.add(heightField);

		dialog.getContentPane().add(p, BorderLayout.CENTER);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		s = MDContainer.getInternationalText("OK");
		JButton b = new JButton(s != null ? s : "OK");
		b.addActionListener(okListener);
		p.add(b);

		s = MDContainer.getInternationalText("Cancel");
		b = new JButton(s != null ? s : "Cancel");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		p.add(b);

		dialog.getContentPane().add(p, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(container);

		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.dispose();
			}

			public void windowActivated(WindowEvent e) {
				widthField.selectAll();
				widthField.requestFocusInWindow();
			}
		});

		return dialog;

	}

	public String toString() {
		return (String) getValue(SHORT_DESCRIPTION);
	}

}