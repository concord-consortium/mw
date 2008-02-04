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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.SystemColor;

import javax.swing.JComponent;

public class ColorRectangle extends JComponent {

	public final static Color[] COLORS = { Color.black, Color.gray, Color.blue, Color.red, Color.green, Color.magenta };
	private int colorID = 0;
	private Color moreColor = Color.white;

	ColorRectangle() {
		setPreferredSize(new Dimension(60, 20));
		setBackground(Color.white);
	}

	public ColorRectangle(int id, Color c) {
		setColorID(id);
		setMoreColor(c);
		setBackground(Color.white);
	}

	boolean isDefaultColor(Color c) {
		for (Color x : COLORS) {
			if (x.equals(c))
				return true;
		}
		return false;
	}

	public void setMoreColor(Color c) {
		moreColor = c;
	}

	public void setMoreColor(int r, int g, int b) {
		moreColor = new Color(r, g, b);
	}

	public Color getMoreColor() {
		return moreColor;
	}

	public void setColorID(int id) {
		colorID = id;
		if (colorID < 0)
			colorID = 0;
	}

	public int getColorID() {
		return colorID;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {

		int width = getWidth();
		int height = getHeight();

		g.setColor(getBackground());
		g.fillRect(0, 0, width, height);
		g.setColor(Color.gray);
		g.drawRect(3, 3, width - 6, height - 6);

		if (colorID < COLORS.length) {
			g.setColor(COLORS[colorID]);
			g.fillRect(4, 4, width - 7, height - 7);
		}
		else if (colorID == ColorComboBox.INDEX_MORE_COLOR) {
			g.setColor(moreColor);
			g.fillRect(4, 4, width - 7, height - 7);
		}
		else if (colorID == ColorComboBox.INDEX_COLOR_CHOOSER) {
			g.setColor(getBackground().equals(SystemColor.textHighlight) ? SystemColor.textHighlightText
					: SystemColor.textText);
			FontMetrics fm = g.getFontMetrics();
			String s = TextComponentPopupMenu.getInternationalText("More");
			if (s == null)
				s = "More";
			s += "...";
			int w = fm.stringWidth(s);
			g.drawString(s, (width - w) >> 1, 14);
		}
		else if (colorID == ColorComboBox.INDEX_HEX_INPUTTER) {
			g.setColor(getBackground().equals(SystemColor.textHighlight) ? SystemColor.textHighlightText
					: SystemColor.textText);
			FontMetrics fm = g.getFontMetrics();
			String s = TextComponentPopupMenu.getInternationalText("Hex");
			if (s == null)
				s = "Hex";
			s += "...";
			int w = fm.stringWidth(s);
			g.drawString(s, (width - w) >> 1, 14);
		}

	}

}
