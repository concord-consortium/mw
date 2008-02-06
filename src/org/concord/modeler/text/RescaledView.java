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

package org.concord.modeler.text;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.border.MatteBorder;

/**
 * A rescaled view of this <code>PrintPage</code>. This is an expensive manipulation because it involves creating and
 * scaling the buffered image representing the <code>PrintPage</code>.
 */

class RescaledView extends JComponent {

	protected int rescaledWidth;
	protected int rescaledHeight;
	protected Image sourceImage;
	protected Image rescaledImage;

	private int x0, y0, w0, h0, w1, h1;
	private float t1, t2, s1, s2;

	RescaledView(int w, int h, PrintPage pp) {

		rescaledWidth = w;
		rescaledHeight = h;
		Component c = pp.getComponent(0);
		x0 = c.getBounds().x;
		y0 = c.getBounds().y;
		w0 = c.getBounds().width;
		h0 = c.getBounds().height;
		w1 = (int) PrintPreview.pageFormat.getWidth();
		h1 = (int) PrintPreview.pageFormat.getHeight();
		s1 = (float) w / (float) w1;
		s2 = (float) h / (float) h1;
		t1 = (float) w0 / (float) w1;
		t2 = (float) h0 / (float) h1;

		BufferedImage bi = new BufferedImage(w0, h0, BufferedImage.TYPE_INT_RGB);
		Graphics g = bi.getGraphics();
		c.print(g);
		g.dispose();
		sourceImage = Toolkit.getDefaultToolkit().createImage(bi.getSource());

		rescaledImage = sourceImage.getScaledInstance((int) (rescaledWidth * t1), (int) (rescaledHeight * t2),
				Image.SCALE_SMOOTH);
		rescaledImage.flush();
		setBackground(Color.white);
		setBorder(new MatteBorder(1, 1, 3, 3, Color.black));

	}

	public void setScaledSize(int w, int h) {
		rescaledWidth = w;
		rescaledHeight = h;
		s1 = (float) w / (float) w1;
		s2 = (float) h / (float) h1;
		rescaledImage = sourceImage.getScaledInstance((int) (rescaledWidth * t1), (int) (rescaledHeight * t2),
				Image.SCALE_SMOOTH);
		repaint();
	}

	public Dimension getPreferredSize() {
		Insets ins = getInsets();
		return new Dimension(rescaledWidth + ins.left + ins.right, rescaledHeight + ins.top + ins.bottom);
	}

	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	public void paint(Graphics g) {
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		g.drawImage(rescaledImage, (int) (x0 * s1), (int) (y0 * s2), this);
		paintBorder(g);
	}

}