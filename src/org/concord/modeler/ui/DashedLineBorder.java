/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import javax.swing.border.AbstractBorder;

/**
 * @author Charles Xie
 * 
 */
public class DashedLineBorder extends AbstractBorder {

	private final static Stroke THIN_DASHED = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f,
			new float[] { 2.0f }, 0.0f);

	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		int w = width;
		int h = height;
		g.translate(x, y);
		((Graphics2D) g).setStroke(THIN_DASHED);
		g.setColor(getShadowColor(c));
		g.drawRect(0, 0, w - 2, h - 2);
		g.setColor(getHighlightColor(c));
		g.drawLine(1, h - 3, 1, 1);
		g.drawLine(1, 1, w - 3, 1);
		g.drawLine(0, h - 1, w - 1, h - 1);
		g.drawLine(w - 1, h - 1, w - 1, 0);
		g.translate(-x, -y);
	}

	private Color getHighlightColor(Component c) {
		return c.getBackground().brighter();
	}

	private Color getShadowColor(Component c) {
		return c.getBackground().darker();
	}

}
