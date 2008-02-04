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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import javax.swing.Icon;

class RemovalIcon implements Icon {

	private final static Stroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f,
			new float[] { 1.0f }, 0.0f);
	private Icon icon0;

	RemovalIcon(Icon icon0) {
		this.icon0 = icon0;
	}

	public int getIconWidth() {
		return icon0.getIconWidth();
	}

	public int getIconHeight() {
		return icon0.getIconHeight();
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		icon0.paintIcon(c, g, x, y);
		Color oldColor = g.getColor();
		Stroke oldStroke = ((Graphics2D) g).getStroke();
		g.setColor(Color.black);
		((Graphics2D) g).setStroke(dashed);
		g.drawRect(x, y, getIconWidth(), getIconHeight());
		g.drawLine(x, y + getIconHeight(), x + getIconWidth(), y);
		g.drawLine(x, y, x + getIconWidth(), y + getIconHeight());
		g.setColor(oldColor);
		((Graphics2D) g).setStroke(oldStroke);
	}

}