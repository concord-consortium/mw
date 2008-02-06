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
package org.concord.modeler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

class ImageContainer extends JPanel implements SnapshotListener {

	private static String str = "Click the Open Button,\nand then drag a thumbnail here.";
	private Image image;
	private String imageNameCopy;
	private int imageX, imageY;
	private int w, h;
	private Point pressPoint;

	ImageContainer() {

		super(new BorderLayout());
		setBackground(Color.white);
		pressPoint = new Point();
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				processMousePressedEvent(e);
			}

			public void mouseReleased(MouseEvent e) {
				processMouseReleasedEvent(e);
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				processMouseDraggedEvent(e);
			}
		});
		setTransferHandler(new ImageIconTransferHandler());
		String s = Modeler.getInternationalText("ClickOpenButtonAndThenDragThumbnailHere");
		if (s != null)
			str = s;

	}

	void destroy() {
		image = null;
	}

	void setScaledSize(int w, int h) {
		this.w = w;
		this.h = h;
	}

	public void setImage(ImageIcon icon) {
		if (icon == null) {
			image = null;
			imageNameCopy = null;
			return;
		}
		imageNameCopy = icon.getDescription();
		int w0 = icon.getIconWidth();
		int h0 = icon.getIconHeight();
		if (w0 >= h0) {
			int h2 = (int) ((float) w / (float) w0 * h0);
			image = icon.getImage().getScaledInstance(w, h2, Image.SCALE_SMOOTH);
			imageX = 0;
			imageY = (h - h2) / 2;
		}
		else {
			int w2 = (int) ((float) h / (float) h0 * w0);
			image = icon.getImage().getScaledInstance(w2, h, Image.SCALE_SMOOTH);
			imageY = 0;
			imageX = (w - w2) / 2;
		}
	}

	void setString(String s) {
		str = s;
	}

	public Image getImage() {
		return image;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {
		if (image != null) {
			g.drawImage(image, imageX, imageY, this);
		}
		else {
			if (str != null) {
				g.setColor(Color.black);
				String[] s = str.split("\n");
				int n = s.length;
				if (n > 0) {
					for (int i = 0; i < s.length; i++) {
						g.drawString(s[i], (getWidth() - g.getFontMetrics().stringWidth(s[i])) / 2, getHeight() / 2
								- 20 * (n / 2 - i));
					}
				}
			}
		}
	}

	protected void processMousePressedEvent(MouseEvent e) {
		if (image == null)
			return;
		if (imageNameCopy != null) {
			SnapshotGallery.sharedInstance().setSelectedImageName(imageNameCopy);
		}
		pressPoint.setLocation(e.getX() - imageX, e.getY() - imageY);
	}

	protected void processMouseReleasedEvent(MouseEvent e) {
		if (image == null)
			return;
		if (e.getClickCount() >= 2) {
			SnapshotGallery.sharedInstance().invokeSnapshotEditor(this, true, false);
		}
	}

	protected void processMouseDraggedEvent(MouseEvent e) {
		if (image == null)
			return;
		imageX = e.getX() - pressPoint.x;
		imageY = e.getY() - pressPoint.y;
		repaint();
	}

	public void snapshotAdded(SnapshotEvent e) {
		// do nothing
	}

	public void snapshotChanged(SnapshotEvent e) {
		if (e.getSource() != this)
			return;
		if (image == null)
			return;
		if (e.getCurrentImageName() != null) {
			setImage(SnapshotGallery.sharedInstance().loadAnnotatedImage(e.getCurrentImageName()));
			repaint();
		}
	}

	public void snapshotRemoved(SnapshotEvent e) {
		image = null;
		repaint();
	}

}