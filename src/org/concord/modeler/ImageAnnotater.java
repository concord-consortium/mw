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
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.ImageIcon;

import org.concord.modeler.draw.Draw;

class ImageAnnotater extends Draw {

	ImageIcon image;

	public void setImage(ImageIcon image) {
		this.image = image;
		if (image != null)
			setPreferredSize(new Dimension(image.getIconWidth(), image.getIconHeight()));
		setMode(DEFAULT_MODE);
		clear();
	}

	public ImageIcon getImage() {
		return image;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {
		g.setColor(Color.white);
		g.fillRect(0, 0, getWidth(), getHeight());
		if (image != null) {
			image
					.paintIcon(this, g, (getWidth() - image.getIconWidth()) / 2,
							(getHeight() - image.getIconHeight()) / 2);
		}
		super.update(g);
	}

}