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

package org.concord.modeler.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.border.LineBorder;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.util.FileUtilities;

public class ImagePreview extends JComponent implements PropertyChangeListener {

	private ImageIcon thumbnail;
	private String path;
	private boolean lockRatio = true;

	public ImagePreview(JFileChooser fc) {
		setPreferredSize(new Dimension(200, 100));
		if (fc != null)
			fc.addPropertyChangeListener(this);
		setBorder(new LineBorder(Color.black));
	}

	public void setLockRatio(boolean b) {
		lockRatio = b;
	}

	public boolean getLockRatio() {
		return lockRatio;
	}

	public void setPath(String s) {
		path = s;
		if (path == null) {
			thumbnail = null;
		}
		else {
			loadImage();
		}
	}

	public void setFile(File file) {
		if (file != null) {
			path = file.getPath();
		}
		else {
			path = null;
		}
		if (path == null) {
			thumbnail = null;
		}
		else {
			loadImage();
		}
	}

	/** use the cached image if there is one */
	public void loadImage() {
		if (path == null)
			return;
		if (getWidth() <= 0 || getHeight() <= 0)
			return; // not ready yet
		ImageIcon tmpIcon = null;
		if (FileUtilities.isRemote(path)) {
			URL u = null;
			try {
				u = new URL(path);
			}
			catch (MalformedURLException e) {
				e.printStackTrace(System.err);
			}
			if (u == null)
				return;
			tmpIcon = ConnectionManager.sharedInstance().loadImage(u);
		}
		else {
			tmpIcon = new ImageIcon(path);
		}
		if (getLockRatio()) {
			float rx = (float) tmpIcon.getIconWidth() / (float) (getWidth() - 10);
			float ry = (float) tmpIcon.getIconHeight() / (float) (getHeight() - 10);
			if (rx > 1.f || ry > 1.f) {
				if (rx > ry) {
					thumbnail = new ImageIcon(tmpIcon.getImage().getScaledInstance(getWidth() - 10, -1,
							Image.SCALE_DEFAULT));
				}
				else {
					thumbnail = new ImageIcon(tmpIcon.getImage().getScaledInstance(-1, getHeight() - 10,
							Image.SCALE_DEFAULT));
				}
			}
			else {
				thumbnail = tmpIcon;
			}
		}
		else {
			thumbnail = new ImageIcon(tmpIcon.getImage()
					.getScaledInstance(getWidth(), getHeight(), Image.SCALE_DEFAULT));
		}
	}

	public void propertyChange(PropertyChangeEvent e) {
		String name = e.getPropertyName();
		if (name.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
			File file = (File) e.getNewValue();
			if (file != null) {
				path = file.getPath();
				if (isShowing()) {
					loadImage();
					repaint();
				}
			}
		}
	}

	public void paintComponent(Graphics g) {
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		if (thumbnail == null) {
			loadImage();
		}
		else {
			int x = (getWidth() >> 1) - (thumbnail.getIconWidth() >> 1);
			int y = (getHeight() >> 1) - (thumbnail.getIconHeight() >> 1);
			if (y < 0) {
				y = 0;
			}
			if (x < 5) {
				x = 5;
			}
			thumbnail.paintIcon(this, g, x, y);
		}
	}

}
