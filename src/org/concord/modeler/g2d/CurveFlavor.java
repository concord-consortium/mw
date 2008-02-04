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

package org.concord.modeler.g2d;

import java.awt.Color;
import java.io.Serializable;

import org.concord.modeler.draw.LineStyle;
import org.concord.modeler.draw.LineWidth;

public class CurveFlavor implements Serializable {

	private Color color;
	private float thickness;
	private int lineStyle;
	private Symbol symbol;

	public CurveFlavor(Color color, float thickness, int lineStyle, Symbol symbol) {
		this.color = color;
		this.thickness = thickness;
		this.lineStyle = lineStyle;
		this.symbol = symbol;
	}

	public CurveFlavor() {
		this(Color.black, LineWidth.STROKE_WIDTH_1, LineStyle.STROKE_NUMBER_1, new Symbol());
	}

	public CurveFlavor(Color color) {
		this(color, LineWidth.STROKE_WIDTH_1, LineStyle.STROKE_NUMBER_1, new Symbol());
	}

	public CurveFlavor(float thickness) {
		this(Color.black, thickness, LineStyle.STROKE_NUMBER_1, new Symbol());
	}

	public CurveFlavor(int symbol) {
		this(Color.black, LineWidth.STROKE_WIDTH_1, LineStyle.STROKE_NUMBER_1, new Symbol(symbol));
	}

	public CurveFlavor(Color color, Symbol symbol) {
		this(color, LineWidth.STROKE_WIDTH_1, LineStyle.STROKE_NUMBER_1, symbol);
	}

	public void setColor(Color c) {
		color = c;
	}

	public Color getColor() {
		return color;
	}

	public void setThickness(float f) {
		thickness = f;
	}

	public float getThickness() {
		return thickness;
	}

	public void setLineStyle(int i) {
		lineStyle = i;
	}

	public int getLineStyle() {
		return lineStyle;
	}

	public void setSymbol(Symbol s) {
		symbol = s;
	}

	public Symbol getSymbol() {
		return symbol;
	}

}
