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
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

public class BorderRectangle extends JComponent {

	public final static String EMPTY_BORDER = "None";
	public final static String RAISED_BEVEL_BORDER = "Raised Bevel";
	public final static String LOWERED_BEVEL_BORDER = "Lowered Bevel";
	public final static String RAISED_ETCHED_BORDER = "Raised Etched";
	public final static String LOWERED_ETCHED_BORDER = "Lowered Etched";
	public final static String LINE_BORDER = "Line";
	public final static String MATTE_BORDER = "Matte";
	public final static Icon TILE_ICON = new Icon() {
		public int getIconWidth() {
			return 3;
		}

		public int getIconHeight() {
			return 1;
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			g.setColor(c.getForeground());
			g.drawRect(x, y, getIconWidth() >> 1, getIconHeight());
		}
	};

	private String type = EMPTY_BORDER;
	private Border border;

	public BorderRectangle() {
		setPreferredSize(new Dimension(60, 20));
		setBackground(Color.lightGray);
	}

	public void setBorderType(String s) {
		if (s == null)
			throw new IllegalArgumentException("border cannot be null");
		type = s;
		if (s.equals(RAISED_BEVEL_BORDER)) {
			border = BorderFactory.createRaisedBevelBorder();
		}
		else if (s.equals(LOWERED_BEVEL_BORDER)) {
			border = BorderFactory.createLoweredBevelBorder();
		}
		else if (s.equals(RAISED_ETCHED_BORDER)) {
			border = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		}
		else if (s.equals(LOWERED_ETCHED_BORDER)) {
			border = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		}
		else if (s.equals(LINE_BORDER)) {
			border = BorderFactory.createLineBorder(getForeground(), 2);
		}
		else if (s.equals(MATTE_BORDER)) {
			border = BorderFactory.createMatteBorder(2, 2, 2, 2, TILE_ICON);
		}
		else {
			border = null;
		}
	}

	public String getBorderType() {
		return type;
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
		if (border != null)
			border.paintBorder(this, g, 5, 2, width - 10, height - 4);
		if (type.equals(EMPTY_BORDER)) {
			g.setColor(getForeground());
			FontMetrics fm = g.getFontMetrics();
			String s = TextComponentPopupMenu.getInternationalText("None");
			if (s == null)
				s = "None";
			g.drawString(s, (width - fm.stringWidth(s)) >> 1, 14);
		}
	}

}
