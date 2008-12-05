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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

/**
 * @author Charles Xie
 * 
 */
class ThumbnailImagePanel extends JPanel implements SnapshotListener {

	final static int IMAGE_HEIGHT = 60;
	private final static int IMAGE_GAP = 8;

	boolean draggable;
	private List<TransferListener> transferListeners;

	ThumbnailImagePanel() {
		super();
		SnapshotGallery.sharedInstance().addSnapshotListener(this);
		setLayout(new FlowLayout(FlowLayout.LEFT, IMAGE_GAP, 0));
		setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				processKeyPressedEvent(e);
			}
		});
		setTransferHandler(new ImageIconTransferHandler());
	}

	ThumbnailImagePanel(boolean draggable) {
		this();
		this.draggable = draggable;
	}

	void destroy() {
		SnapshotGallery.sharedInstance().removeSnapshotListener(this);
	}

	void addTransferListener(TransferListener listener) {
		if (listener == null)
			return;
		if (transferListeners == null)
			transferListeners = new ArrayList<TransferListener>();
		transferListeners.add(listener);
	}

	void removeTransferListener(TransferListener listener) {
		if (listener == null)
			return;
		if (transferListeners == null)
			return;
		transferListeners.remove(listener);
	}

	private void notifyListeners(TransferEvent e) {
		if (transferListeners == null || transferListeners.isEmpty())
			return;
		for (TransferListener l : transferListeners) {
			switch (e.getType()) {
			case TransferEvent.EXPORT_DONE:
				l.exportDone(e);
				break;
			}
		}
	}

	void exportDone(Transferable data, int action) {
		notifyListeners(new TransferEvent(this, TransferEvent.EXPORT_DONE, data));
	}

	protected void processKeyPressedEvent(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_ENTER:
			SnapshotGallery.sharedInstance().invokeSnapshotEditor(this, true, true);
			break;
		case KeyEvent.VK_LEFT:
			SnapshotGallery.sharedInstance().stepBack();
			break;
		case KeyEvent.VK_RIGHT:
			SnapshotGallery.sharedInstance().stepForward();
			break;
		}
		repaint();
	}

	protected void processMousePressedEvent(MouseEvent e) {

		requestFocus();

		int ex = e.getX();
		int ey = e.getY();
		Image image = null;
		float w;
		Insets insets = getInsets();
		int x = insets.left;
		int n = SnapshotGallery.sharedInstance().size();
		for (int i = n - 1; i >= 0; i--) {
			image = SnapshotGallery.sharedInstance().getThumbnail(i);
			w = image.getWidth(this);
			if (ex > x && ex < x + w && ey > insets.top && ey < insets.top + IMAGE_HEIGHT) {
				SnapshotGallery.sharedInstance().setSelectedIndex(i);
				repaint();
				break;
			}
			if (getLayout() instanceof FlowLayout) {
				x += (int) w + ((FlowLayout) getLayout()).getHgap();
			}
			else {
				x += (int) w + IMAGE_GAP;
			}
		}

		if (draggable) {
			if (!ModelerUtilities.isRightClick(e) && e.getClickCount() < 2) {
				JComponent c = (JComponent) e.getSource();
				TransferHandler handler = c.getTransferHandler();
				handler.exportAsDrag(c, e, TransferHandler.COPY);
			}
		}
		else {
			if (!ModelerUtilities.isRightClick(e) && e.getClickCount() >= 2)
				SnapshotGallery.sharedInstance().invokeSnapshotEditor(this, true, true);
		}

	}

	void scaleImages(String nameOfChangedImage) {

		if (SnapshotGallery.sharedInstance().isEmpty()) {
			return;
		}

		if (EventQueue.isDispatchThread())
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		float w = 0, h = 0;
		Insets insets = getInsets();
		int w1 = insets.left + insets.right;
		int h1 = insets.top + insets.bottom + IMAGE_HEIGHT;
		int n = SnapshotGallery.sharedInstance().size();
		String name = null;
		ImageIcon icon = null;
		Image image = null;
		float r = 1;
		for (int i = n - 1; i >= 0; i--) {
			name = SnapshotGallery.sharedInstance().getImageName(i);
			image = SnapshotGallery.sharedInstance().getThumbnail(i);
			if (image == null || name.equals(nameOfChangedImage)) {
				icon = SnapshotGallery.sharedInstance().loadAnnotatedImage(i);
				w = icon.getIconWidth();
				h = icon.getIconHeight();
				r = IMAGE_HEIGHT / h;
				w *= r;
				// This scaling method causes out-of-memory error:
				// icon.getImage().getScaledInstance((int) w, IMAGE_HEIGHT, Image.SCALE_SMOOTH);
				image = ModelerUtilities.scale((BufferedImage) icon.getImage(), r, r);
				SnapshotGallery.sharedInstance().putThumbnail(icon.getDescription(), image);
				image.flush();
				icon.getImage().flush();
			}
			else {
				w = image.getWidth(this);
				h = image.getHeight(this);
			}
			if (getLayout() instanceof FlowLayout) {
				w1 += (int) w + ((FlowLayout) getLayout()).getHgap();
			}
			else {
				w1 += (int) w + IMAGE_GAP;
			}
		}
		setPreferredSize(new Dimension(w1, h1));
		if (EventQueue.isDispatchThread())
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

	}

	public void paintComponent(Graphics g) {

		super.paintComponent(g);

		if (SnapshotGallery.sharedInstance().isEmpty()) {
			FontMetrics fm = g.getFontMetrics();
			String s = Modeler.getInternationalText("NoSnapshotAvailable");
			if (s == null)
				s = "No snapshot is available.";
			g.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, (getHeight() - fm.getHeight()) / 2);
			return;
		}

		float w, h;
		Insets insets = getInsets();
		int x = insets.left;
		int n = SnapshotGallery.sharedInstance().size();
		Image image;
		for (int i = n - 1; i >= 0; i--) {
			image = SnapshotGallery.sharedInstance().getThumbnail(i);
			if (image == null)
				continue;
			w = image.getWidth(this);
			h = image.getHeight(this);
			w *= IMAGE_HEIGHT / h;
			if (i == SnapshotGallery.sharedInstance().getSelectedIndex()) {
				g.setColor(Color.red);
				g.drawRect(x - 3, insets.top - 3, (int) w + 6, IMAGE_HEIGHT + 6);
				g.drawRect(x - 2, insets.top - 2, (int) w + 4, IMAGE_HEIGHT + 4);
			}
			g.drawImage(image, x, insets.top, this);
			if (getLayout() instanceof FlowLayout) {
				x += (int) w + ((FlowLayout) getLayout()).getHgap();
			}
			else {
				x += (int) w + IMAGE_GAP;
			}
		}

	}

	public void snapshotAdded(SnapshotEvent e) {
		respondToSnapshotEvent(null);
	}

	public void snapshotChanged(SnapshotEvent e) {
		respondToSnapshotEvent(e.getCurrentImageName());
	}

	public void snapshotRemoved(SnapshotEvent e) {
		respondToSnapshotEvent(null);
	}

	void respondToSnapshotEvent(final String name) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				scaleImages(name);
				repaint();
				getParent().doLayout();
			}
		});
	}

}