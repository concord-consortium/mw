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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.concord.modeler.ui.IconPool;

class ResizeAction extends AbstractAction {

	private final static short MIN_WIDTH = 300;
	private final static short MAX_WIDTH = 1000;
	private final static short MIN_HEIGHT = 100;
	private final static short MAX_HEIGHT = 800;

	private MolecularContainer container;

	ResizeAction(MolecularContainer container) {
		super("Resize Container");
		this.container = container;
		putValue(SMALL_ICON, IconPool.getIcon("resize"));
	}

	public void actionPerformed(ActionEvent e) {
		createSizeDialog().setVisible(true);
	}

	private JDialog createSizeDialog() {

		String s = MolecularContainer.getInternationalText("ResizeContainer");
		final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(container), s != null ? s
				: "Set Container Size", true);
		dialog.setSize(200, 200);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		final int w0 = container.getWidth();
		final int h0 = container.getHeight();
		final JTextField widthField = new JTextField(w0 + "");
		final JTextField heightField = new JTextField(h0 + "");

		ActionListener okListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int w = 0, h = 0;
				try {
					w = Integer.parseInt(widthField.getText());
					if (w < MIN_WIDTH)
						w = MIN_WIDTH;
					else if (w > MAX_WIDTH)
						w = MAX_WIDTH;
					h = Integer.parseInt(heightField.getText());
					if (h < MIN_HEIGHT)
						h = MIN_HEIGHT;
					else if (h > MAX_HEIGHT)
						h = MAX_HEIGHT;
				}
				catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(dialog, "Please input integers.", "Input error",
							JOptionPane.ERROR_MESSAGE);
					widthField.setText(w0 + "");
					heightField.setText(h0 + "");
				}
				if (w > 0 && h > 0) {
					container.setViewerSize(new Dimension(w, h));
					container.notifyChange();
					dialog.dispose();
				}
			}
		};

		widthField.addActionListener(okListener);
		heightField.addActionListener(okListener);

		JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		s = MolecularContainer.getInternationalText("Width");
		String s2 = MolecularContainer.getInternationalText("Pixels");
		p.add(new JLabel((s != null ? s : "Width") + " (" + MIN_WIDTH + "-" + MAX_WIDTH + " "
				+ (s2 != null ? s2 : "pixels") + "):"));
		p.add(widthField);
		s = MolecularContainer.getInternationalText("Height");
		p.add(new JLabel((s != null ? s : "Height") + " (" + MIN_HEIGHT + "-" + MAX_HEIGHT + " "
				+ (s2 != null ? s2 : "pixels") + "):"));
		p.add(heightField);

		dialog.getContentPane().add(p, BorderLayout.CENTER);

		p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		s = MolecularContainer.getInternationalText("OK");
		JButton b = new JButton(s != null ? s : "OK");
		b.addActionListener(okListener);
		p.add(b);

		s = MolecularContainer.getInternationalText("Cancel");
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

}
