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

package org.concord.mw2d;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.concord.mw2d.models.MolecularModel;
import org.concord.mw2d.models.StructureFactor;

class DiffractionPattern extends JComponent {

	private StructureFactor sf;
	private int halfWidth = 100, halfHeight = 100;
	private int width, height;
	private BufferedImage bi;
	private String msg;
	private int zoom = 8;

	public DiffractionPattern() {
		width = halfWidth + halfWidth + 1;
		height = halfHeight + halfHeight + 1;
		Dimension dim = new Dimension(width, height);
		setPreferredSize(dim);
		setMaximumSize(dim);
		setMinimumSize(dim);
		sf = new StructureFactor(halfWidth, halfHeight);
	}

	public void setLevelOfDetails(int i) {
		sf.setLevelOfDetails(i);
	}

	public int getLevelOfDetails() {
		return sf.getLevelOfDetails();
	}

	public boolean zoomIn() {
		if (zoom <= 1)
			return false;
		zoom--;
		sf.setZooming(zoom);
		return true;
	}

	public boolean zoomOut() {
		if (zoom >= 50)
			return false;
		zoom++;
		sf.setZooming(zoom);
		return true;
	}

	int getZoom() {
		return zoom;
	}

	void setZoom(int i) {
		zoom = i;
		sf.setZooming(zoom);
	}

	public void computeImage(MolecularModel model, int type) {
		if (bi == null)
			bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		synchronized (model) {
			sf.compute(model, type);
		}
		bi.setRGB(0, 0, width, height, sf.getDiffractionImage(), 0, width);
	}

	public BufferedImage getBufferedImage() {
		return bi;
	}

	public void setBufferedImage(BufferedImage bi) {
		this.bi = bi;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {
		if (msg != null) {
			g.setColor(Color.black);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.white);
			int sw = SwingUtilities.computeStringWidth(g.getFontMetrics(), msg);
			g.drawString(msg, (width - sw) >> 1, height >> 1);
		}
		else {
			if (bi != null)
				g.drawImage(bi, 0, 0, width, height, this);
		}
		g.setColor(Color.black);
		g.drawLine(0, height >> 1, width, height >> 1);
		g.drawLine(width >> 1, 0, width >> 1, height);
	}

	public void setMessage(String msg) {
		this.msg = msg;
	}

}