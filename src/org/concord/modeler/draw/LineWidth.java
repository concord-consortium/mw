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

package org.concord.modeler.draw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;

public class LineWidth extends JComponent {

	public final static byte STROKE_WIDTH_0 = 0;
	public final static byte STROKE_WIDTH_1 = 1;
	public final static byte STROKE_WIDTH_2 = 2;
	public final static byte STROKE_WIDTH_3 = 3;
	public final static byte STROKE_WIDTH_4 = 4;
	public final static byte STROKE_WIDTH_5 = 5;

	private BasicStroke stroke;
	private float strokeNumber;

	public LineWidth() {
		setBackground(Color.white);
		strokeNumber = STROKE_WIDTH_1;
		stroke = new BasicStroke(strokeNumber);
		setPreferredSize(new Dimension(60, 20));
	}

	public void setStrokeNumber(float strokeNumber) {
		this.strokeNumber = strokeNumber;
		if (strokeNumber > 0) {
			stroke = new BasicStroke(strokeNumber);
		}
	}

	public float getStrokeNumber() {
		return strokeNumber;
	}

	public BasicStroke getStroke() {
		return stroke;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		update(g);
	}

	public void update(Graphics g) {

		Graphics2D g2 = (Graphics2D) g;

		int width = getWidth();
		int height = getHeight();

		g2.setColor(getBackground());
		g2.fillRect(0, 0, width, height);
		g2.setColor(Color.gray);
		g2.drawRect(3, 3, width - 6, height - 6);
		g2.setColor(getForeground());
		if (strokeNumber > 0.0f) {
			g2.setStroke(stroke);
			g2.drawLine(10, height / 2, width - 10, height / 2);
		}

	}

}