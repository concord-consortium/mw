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

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.border.EtchedBorder;

/**
 * @author Charles Xie
 * 
 */
public class RoundEtchedBorder extends EtchedBorder {

	private int arcWidth = 10;
	private int arcHeight = 10;

	public RoundEtchedBorder() {
		super();
	}

	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		g.translate(x, y);
		g.setColor(etchType == LOWERED ? getShadowColor(c) : getHighlightColor(c));
		g.drawRoundRect(0, 0, w - 2, h - 2, arcWidth, arcHeight);
		g.setColor(etchType == LOWERED ? getHighlightColor(c) : getShadowColor(c));
		g.drawRoundRect(2, 2, w - 6, h - 6, arcWidth - 3, arcHeight - 3);
		g.translate(-x, -y);
	}

}
