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
package org.concord.mw3d;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

/**
 * @author Charles Xie
 * 
 */
final class RightClickToggleButton extends JToggleButton {

	private int[] xPoints, yPoints;
	private boolean firstTime = true;

	RightClickToggleButton(ImageIcon icon, Dimension preferredSize) {
		super(icon);
		setPoints(preferredSize);
	}

	RightClickToggleButton(Action action, Dimension preferredSize) {
		super(action);
		setPoints(preferredSize);
	}

	private void setPoints(Dimension size) {
		xPoints = new int[3];
		yPoints = new int[3];
		xPoints[0] = size.width;
		yPoints[0] = size.height;
		xPoints[1] = xPoints[0];
		yPoints[1] = yPoints[0] - 7;
		xPoints[2] = xPoints[0] - 7;
		yPoints[2] = yPoints[0];
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (firstTime) {
			setPoints(getSize());
			firstTime = false;
		}
		g.setColor(Color.darkGray);
		g.fillPolygon(xPoints, yPoints, 3);
	}

}