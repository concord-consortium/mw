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
import java.util.Arrays;

import javax.swing.JComponent;

public class LineStyle extends JComponent {

	public final static byte STROKE_NUMBER_1 = 0;
	public final static byte STROKE_NUMBER_2 = 1;
	public final static byte STROKE_NUMBER_3 = 2;
	public final static byte STROKE_NUMBER_4 = 3;
	public final static byte STROKE_NUMBER_5 = 4;

	public final static BasicStroke[] STROKES = new BasicStroke[5];

	private final static float[] SOLID = { 1.0f, 0.0f };
	private final static float[] DOTTED = { 2.0f, 2.0f };
	private final static float[] DASHED = { 4.0f, 4.0f };
	private final static float[] LONGDASHED = { 8.0f, 4.0f };
	private final static float[] DOTTEDANDDASHED = { 8.0f, 4.0f, 2.0f, 4.0f };

	static {
		STROKES[0] = StrokeFactory.createStroke(2.0f, SOLID);
		STROKES[1] = StrokeFactory.createStroke(2.0f, DOTTED);
		STROKES[2] = StrokeFactory.createStroke(2.0f, DASHED);
		STROKES[3] = StrokeFactory.createStroke(2.0f, LONGDASHED);
		STROKES[4] = StrokeFactory.createStroke(2.0f, DOTTEDANDDASHED);
	}

	private int strokeNumber;

	public LineStyle() {
		setBackground(Color.white);
		strokeNumber = STROKE_NUMBER_1;
		setPreferredSize(new Dimension(60, 20));
	}

	public static byte getStrokeNumber(BasicStroke stroke) {
		float[] dashArray = stroke.getDashArray();
		if (Arrays.equals(DOTTED, dashArray))
			return STROKE_NUMBER_2;
		if (Arrays.equals(DASHED, dashArray))
			return STROKE_NUMBER_3;
		if (Arrays.equals(LONGDASHED, dashArray))
			return STROKE_NUMBER_4;
		if (Arrays.equals(DOTTEDANDDASHED, dashArray))
			return STROKE_NUMBER_5;
		return STROKE_NUMBER_1;
	}

	public static float[] getDashArray(int strokeNumber) {
		switch (strokeNumber) {
		case STROKE_NUMBER_1:
			return SOLID;
		case STROKE_NUMBER_2:
			return DOTTED;
		case STROKE_NUMBER_3:
			return DASHED;
		case STROKE_NUMBER_4:
			return LONGDASHED;
		case STROKE_NUMBER_5:
			return DOTTEDANDDASHED;
		}
		return null;
	}

	public void setStrokeNumber(int strokeNumber) {
		this.strokeNumber = strokeNumber;
	}

	public int getStrokeNumber() {
		return strokeNumber;
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
		g2.setStroke(STROKES[strokeNumber]);
		g2.drawLine(10, height / 2, width - 10, height / 2);

	}

}
