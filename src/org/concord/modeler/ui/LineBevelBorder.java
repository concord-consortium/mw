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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;
import javax.swing.border.BevelBorder;

/**
 * This is a simpler bevel border than <code>javax.swing.border.BevelBorder</code> in that it is only one pixel width
 * and therefore does not have inner/outer lines.
 * 
 * @author Qian Xie
 */

public class LineBevelBorder extends AbstractBorder {

	public final static int LOWERED = BevelBorder.LOWERED;
	public final static int RAISED = BevelBorder.RAISED;

	protected int bevelType = LOWERED;
	protected Color highlightColor = Color.white;
	protected Color shadowColor = Color.black;

	public LineBevelBorder(int type) {
		bevelType = type;
	}

	public Insets getBorderInsets(Component c) {
		return new Insets(0, 0, 0, 0);
	}

	public boolean isBorderOpaque() {
		return true;
	}

	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		if (bevelType == RAISED) {
			paintRaisedBevel(c, g, x, y, w, h);
		}
		else if (bevelType == LOWERED) {
			paintLoweredBevel(c, g, x, y, w, h);
		}
	}

	public Color getHighlightColor() {
		return highlightColor;
	}

	public Color getShadowColor() {
		return shadowColor;
	}

	public Color getHighlightColor(Component c) {
		Color highlight = getHighlightColor();
		return highlight != null ? highlight : c.getBackground().brighter();
	}

	public Color getShadowColor(Component c) {
		Color shadow = getShadowColor();
		return shadow != null ? shadow : c.getBackground().darker();
	}

	protected void paintRaisedBevel(Component c, Graphics g, int x, int y, int width, int height) {
		Color oldColor = g.getColor();
		int h = height;
		int w = width;

		g.translate(x, y);

		g.setColor(getHighlightColor(c));
		g.drawLine(0, 0, 0, h);
		g.drawLine(0, 0, w, 0);

		g.setColor(getShadowColor(c));
		g.drawLine(1, h - 1, w - 1, h - 1);
		g.drawLine(w - 1, 1, w - 1, h - 1);

		g.translate(-x, -y);
		g.setColor(oldColor);

	}

	protected void paintLoweredBevel(Component c, Graphics g, int x, int y, int width, int height) {
		Color oldColor = g.getColor();
		int h = height;
		int w = width;

		g.translate(x, y);

		g.setColor(getShadowColor(c));
		g.drawLine(0, 0, 0, h);
		g.drawLine(0, 0, w, 0);

		g.setColor(getHighlightColor(c));
		g.drawLine(1, h - 1, w - 1, h - 1);
		g.drawLine(w - 1, 1, w - 1, h - 1);

		g.translate(-x, -y);
		g.setColor(oldColor);

	}

}