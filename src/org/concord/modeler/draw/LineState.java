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

import java.awt.Color;

/**
 * This class defines the serializable state of a line.
 * 
 * @author Charles Xie
 * @see org.concord.modeler.draw.AbstractLine;
 * 
 */
public class LineState {

	private float x1, x2, y1, y2;
	private byte weight = 1;
	private byte lineStyle = LineStyle.STROKE_NUMBER_1;
	private Color color = Color.black;
	private byte beginStyle = ArrowRectangle.NO_ARROW;
	private byte endStyle = ArrowRectangle.NO_ARROW;
	private byte option = AbstractLine.DEFAULT;

	public LineState() {
	}

	public LineState(AbstractLine line) {
		x1 = line.getX1();
		y1 = line.getY1();
		x2 = line.getX2();
		y2 = line.getY2();
		weight = line.getLineWeight();
		lineStyle = line.getLineStyle();
		color = line.getColor();
		beginStyle = line.getBeginStyle();
		endStyle = line.getEndStyle();
		option = line.getOption();
	}

	public void setOption(byte option) {
		this.option = option;
	}

	public byte getOption() {
		return option;
	}

	public void setBeginStyle(byte style) {
		beginStyle = style;
	}

	public byte getBeginStyle() {
		return beginStyle;
	}

	public void setEndStyle(byte style) {
		endStyle = style;
	}

	public byte getEndStyle() {
		return endStyle;
	}

	public void setLineStyle(byte style) {
		lineStyle = style;
	}

	public byte getLineStyle() {
		return lineStyle;
	}

	public void setWeight(byte weight) {
		this.weight = weight;
	}

	public byte getWeight() {
		return weight;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Color getColor() {
		return color;
	}

	public void setX1(float x1) {
		this.x1 = x1;
	}

	public float getX1() {
		return x1;
	}

	public void setY1(float y1) {
		this.y1 = y1;
	}

	public float getY1() {
		return y1;
	}

	public void setX2(float x2) {
		this.x2 = x2;
	}

	public float getX2() {
		return x2;
	}

	public void setY2(float y2) {
		this.y2 = y2;
	}

	public float getY2() {
		return y2;
	}

}