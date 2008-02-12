/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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
package org.concord.molbio.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.border.BevelBorder;

class CustomBevelBorder extends BevelBorder {

	private String hidenSide;

	public CustomBevelBorder(int bevelType) {
		super(bevelType);
	}

	/**
	 * set the side to be hiden. Must be BorderLayout.WEST, BorderLayout.EAST, BorderLayout.NORTH, or
	 * BorderLayout.SOUTH.
	 */
	public void hideSide(String side) {
		hidenSide = side;
	}

	protected void paintRaisedBevel(Component c, Graphics g, int x, int y, int width, int height) {

		Color oldColor = g.getColor();
		int h = height;
		int w = width;

		g.translate(x, y);

		g.setColor(getHighlightOuterColor(c));
		if (hidenSide != BorderLayout.WEST)
			g.drawLine(0, 0, 0, h - 2);
		if (hidenSide != BorderLayout.NORTH)
			g.drawLine(1, 0, w - 2, 0);

		g.setColor(getHighlightInnerColor(c));
		if (hidenSide != BorderLayout.WEST)
			g.drawLine(1, 1, 1, h - 3);
		if (hidenSide != BorderLayout.NORTH)
			g.drawLine(2, 1, w - 3, 1);

		g.setColor(getShadowOuterColor(c));
		if (hidenSide != BorderLayout.SOUTH)
			g.drawLine(0, h - 1, w - 1, h - 1);
		if (hidenSide != BorderLayout.EAST)
			g.drawLine(w - 1, 0, w - 1, h - 2);

		g.setColor(getShadowInnerColor(c));
		if (hidenSide != BorderLayout.SOUTH)
			g.drawLine(1, h - 2, w - 2, h - 2);
		if (hidenSide != BorderLayout.EAST)
			g.drawLine(w - 2, 1, w - 2, h - 3);

		g.translate(-x, -y);
		g.setColor(oldColor);

	}

	protected void paintLoweredBevel(Component c, Graphics g, int x, int y, int width, int height) {

		Color oldColor = g.getColor();
		int h = height;
		int w = width;

		g.translate(x, y);

		g.setColor(getShadowInnerColor(c));
		if (hidenSide != BorderLayout.WEST)
			g.drawLine(0, 0, 0, h - 1);
		if (hidenSide != BorderLayout.NORTH)
			g.drawLine(1, 0, w - 1, 0);

		g.setColor(getShadowOuterColor(c));
		if (hidenSide != BorderLayout.WEST)
			g.drawLine(1, 1, 1, h - 2);
		if (hidenSide != BorderLayout.NORTH)
			g.drawLine(2, 1, w - 2, 1);

		g.setColor(getHighlightOuterColor(c));
		if (hidenSide != BorderLayout.SOUTH)
			g.drawLine(1, h - 1, w - 1, h - 1);
		if (hidenSide != BorderLayout.EAST)
			g.drawLine(w - 1, 1, w - 1, h - 2);

		g.setColor(getHighlightInnerColor(c));
		if (hidenSide != BorderLayout.SOUTH)
			g.drawLine(2, h - 2, w - 2, h - 2);
		if (hidenSide != BorderLayout.EAST)
			g.drawLine(w - 2, 2, w - 2, h - 3);

		g.translate(-x, -y);
		g.setColor(oldColor);

	}

}