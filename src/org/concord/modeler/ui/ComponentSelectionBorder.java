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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Stroke;
import javax.swing.border.Border;

public class ComponentSelectionBorder implements Border {

	private final static Stroke DASHED = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f,
			new float[] { 2.0f }, 0.0f);

	public Insets getBorderInsets(Component c) {
		return new Insets(2, 2, 2, 2);
	}

	public boolean isBorderOpaque() {
		return true;
	}

	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Color bg = c.getBackground();
		Graphics2D g2 = (Graphics2D) g;
		Stroke oldStroke = g2.getStroke();
		g2.setColor(new Color(0xfffff ^ bg.getRGB()));
		g2.setStroke(DASHED);
		g2.drawLine(x + 1, y + 1, x + width - 1, y + 1);
		g2.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
		g2.drawLine(x + 1, y + 1, x + 1, y + height - 1);
		g2.drawLine(x + width - 1, y + 1, x + width - 1, y + height - 1);
		g2.setStroke(oldStroke);
	}

}
