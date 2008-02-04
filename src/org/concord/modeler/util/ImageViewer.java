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

package org.concord.modeler.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.concord.modeler.ui.PrintableComponent;

public class ImageViewer extends PrintableComponent {

	static int xoffset = 200;
	static int yoffset = 200;

	ImageIcon image;

	public ImageViewer(ImageIcon image) {
		super();
		setImage(image);
	}

	public void setImage(ImageIcon image) {
		this.image = image;
		if (image != null)
			setPreferredSize(new Dimension(image.getIconWidth(), image.getIconHeight()));
	}

	public ImageIcon getImage() {
		return image;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {
		if (image != null)
			image
					.paintIcon(this, g, (getWidth() - image.getIconWidth()) / 2,
							(getHeight() - image.getIconHeight()) / 2);
	}

	public static JDialog createDialog(Frame owner, Image image, boolean modal) {
		return createDialog(owner, new ImageIcon(image), modal);
	}

	public static JDialog createDialog(Frame owner, ImageIcon image, boolean modal) {

		final JDialog dialog = new JDialog(owner, "Image Viewer", modal);
		dialog.setLocation(xoffset, yoffset);
		xoffset += 50;
		yoffset += 20;
		int w = Toolkit.getDefaultToolkit().getScreenSize().width;
		int h = Toolkit.getDefaultToolkit().getScreenSize().height;
		if (xoffset > w - 250)
			xoffset = w - 250;
		if (yoffset > h - 150)
			yoffset = h - 150;
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		ImageViewer viewer = new ImageViewer(image);
		dialog.getContentPane().add(viewer, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JButton button = new JButton("Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				xoffset -= 50;
				if (xoffset < 100)
					xoffset = 100;
				yoffset -= 20;
				if (yoffset < 100)
					yoffset = 100;
				dialog.dispose();
			}
		});
		buttonPanel.add(button);
		dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		dialog.pack();

		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				xoffset -= 50;
				if (xoffset < 100)
					xoffset = 100;
				yoffset -= 20;
				if (yoffset < 100)
					yoffset = 100;
				dialog.dispose();
			}
		});

		return dialog;

	}

}