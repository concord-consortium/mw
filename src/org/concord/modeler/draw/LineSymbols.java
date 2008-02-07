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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

import javax.swing.JComponent;

public class LineSymbols extends JComponent {

	public final static String UP = "up";
	public final static String DOWN = "down";
	public final static String LEFT = "left";
	public final static String RIGHT = "right";
	public final static String ERROR_BAR = "error";

	private final static Ellipse2D CIRCLE = new Ellipse2D.Float();
	private final static Rectangle RECTANGLE = new Rectangle();
	private final static NamedGeneralPath TRIANGLE_UP = new NamedGeneralPath(GeneralPath.WIND_EVEN_ODD, UP);
	private final static NamedGeneralPath TRIANGLE_DOWN = new NamedGeneralPath(GeneralPath.WIND_EVEN_ODD, DOWN);
	private final static NamedGeneralPath TRIANGLE_LEFT = new NamedGeneralPath(GeneralPath.WIND_EVEN_ODD, LEFT);
	private final static NamedGeneralPath TRIANGLE_RIGHT = new NamedGeneralPath(GeneralPath.WIND_EVEN_ODD, RIGHT);
	private final static NamedGeneralPath ERROR_SYMBOL = new NamedGeneralPath(GeneralPath.WIND_EVEN_ODD, ERROR_BAR);

	public final static byte SYMBOL_NUMBER_0 = 0;
	public final static byte SYMBOL_NUMBER_1 = 1;
	public final static byte SYMBOL_NUMBER_2 = 2;
	public final static byte SYMBOL_NUMBER_3 = 3;
	public final static byte SYMBOL_NUMBER_4 = 4;
	public final static byte SYMBOL_NUMBER_5 = 5;
	public final static byte SYMBOL_NUMBER_6 = 6;
	public final static byte SYMBOL_NUMBER_7 = 7;
	public final static byte MAX = SYMBOL_NUMBER_7;

	public final static Object[] SYMBOLS = new Object[8];
	public static int spacing = 10;

	static {
		SYMBOLS[0] = null;
		SYMBOLS[1] = CIRCLE;
		SYMBOLS[2] = RECTANGLE;
		SYMBOLS[3] = TRIANGLE_UP;
		SYMBOLS[4] = TRIANGLE_DOWN;
		SYMBOLS[5] = TRIANGLE_LEFT;
		SYMBOLS[6] = TRIANGLE_RIGHT;
		SYMBOLS[7] = ERROR_SYMBOL;
	}

	private int symbolNumber;
	private final static BasicStroke THIN_STROKE = new BasicStroke(1.0f);

	public LineSymbols() {
		symbolNumber = SYMBOL_NUMBER_0;
		setPreferredSize(new Dimension(60, 20));
	}

	public void setSymbolNumber(int symbolNumber) {
		this.symbolNumber = symbolNumber;
	}

	public int getSymbolNumber() {
		return symbolNumber;
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
		g2.setColor(getForeground());
		g2.drawRect(3, 3, width - 6, height - 6);
		g2.setStroke(THIN_STROKE);
		g2.drawLine(10, height / 2, width - 10, height / 2);

		switch (symbolNumber) {

		case SYMBOL_NUMBER_1:
			if (SYMBOLS[symbolNumber] instanceof Ellipse2D.Float) {
				Ellipse2D.Float circle = (Ellipse2D.Float) SYMBOLS[symbolNumber];
				circle.setFrame(width / 2 - 5, height / 2 - 5, 10, 10);
				g2.setColor(getBackground());
				g2.fill(circle);
				g2.setColor(getForeground());
				g2.draw(circle);
			}
			break;

		case SYMBOL_NUMBER_2:
			if (SYMBOLS[symbolNumber] instanceof Rectangle) {
				Rectangle rect = (Rectangle) SYMBOLS[symbolNumber];
				rect.setRect(width / 2 - 4, height / 2 - 4, 8, 8);
				g2.setColor(getBackground());
				g2.fill(rect);
				g2.setColor(getForeground());
				g2.draw(rect);
			}
			break;

		case SYMBOL_NUMBER_3:
			if (SYMBOLS[symbolNumber] instanceof NamedGeneralPath) {
				int xmid = width / 2;
				int ymid = height / 2;
				GeneralPath triangle = ((NamedGeneralPath) SYMBOLS[symbolNumber]).getGeneralPath();
				triangle.reset();
				triangle.moveTo(xmid, ymid - 4);
				triangle.lineTo(xmid - 4, ymid + 4);
				triangle.lineTo(xmid + 4, ymid + 4);
				triangle.lineTo(xmid, ymid - 4);
				triangle.closePath();
				g2.setColor(getBackground());
				g2.fill(triangle);
				g2.setColor(getForeground());
				g2.draw(triangle);
			}
			break;

		case SYMBOL_NUMBER_4:
			if (SYMBOLS[symbolNumber] instanceof NamedGeneralPath) {
				int xmid = width / 2;
				int ymid = height / 2;
				GeneralPath triangle = ((NamedGeneralPath) SYMBOLS[symbolNumber]).getGeneralPath();
				triangle.reset();
				triangle.moveTo(xmid - 4, ymid - 4);
				triangle.lineTo(xmid + 4, ymid - 4);
				triangle.lineTo(xmid, ymid + 4);
				triangle.lineTo(xmid - 4, ymid - 4);
				triangle.closePath();
				g2.setColor(getBackground());
				g2.fill(triangle);
				g2.setColor(getForeground());
				g2.draw(triangle);
			}
			break;

		case SYMBOL_NUMBER_5:
			if (SYMBOLS[symbolNumber] instanceof NamedGeneralPath) {
				int xmid = width / 2;
				int ymid = height / 2;
				GeneralPath triangle = ((NamedGeneralPath) SYMBOLS[symbolNumber]).getGeneralPath();
				triangle.reset();
				triangle.moveTo(xmid - 4, ymid);
				triangle.lineTo(xmid + 4, ymid - 4);
				triangle.lineTo(xmid + 4, ymid + 4);
				triangle.lineTo(xmid - 4, ymid);
				triangle.closePath();
				g2.setColor(getBackground());
				g2.fill(triangle);
				g2.setColor(getForeground());
				g2.draw(triangle);
			}
			break;

		case SYMBOL_NUMBER_6:
			if (SYMBOLS[symbolNumber] instanceof NamedGeneralPath) {
				int xmid = width / 2;
				int ymid = height / 2;
				GeneralPath triangle = ((NamedGeneralPath) SYMBOLS[symbolNumber]).getGeneralPath();
				triangle.reset();
				triangle.moveTo(xmid + 4, ymid);
				triangle.lineTo(xmid - 4, ymid - 4);
				triangle.lineTo(xmid - 4, ymid + 4);
				triangle.lineTo(xmid + 4, ymid);
				triangle.closePath();
				g2.setColor(getBackground());
				g2.fill(triangle);
				g2.setColor(getForeground());
				g2.draw(triangle);
			}
			break;

		case SYMBOL_NUMBER_7:
			if (SYMBOLS[symbolNumber] instanceof NamedGeneralPath) {
				int xmid = width / 2;
				int ymid = height / 2;
				g2.setColor(getForeground());
				GeneralPath path = ((NamedGeneralPath) SYMBOLS[symbolNumber]).getGeneralPath();
				path.reset();
				path.moveTo(xmid - 2, ymid - 5);
				path.lineTo(xmid + 2, ymid - 5);
				path.lineTo(xmid, ymid - 5);
				path.lineTo(xmid, ymid + 5);
				path.lineTo(xmid + 2, ymid + 5);
				path.lineTo(xmid - 2, ymid + 5);
				g2.draw(path);
				g2.drawOval(xmid - 2, ymid - 2, 4, 4);
			}
			break;

		}

	}

}