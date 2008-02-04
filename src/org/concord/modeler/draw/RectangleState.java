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
 * This class defines a serializable object for the state of a rectangle.
 * 
 * @author Charles Xie
 * @see org.concord.modeler.draw.AbstractRectangle
 */
public class RectangleState {

	private float x, y, w, h, arcWidth, arcHeight;
	private short alpha = 255;
	private float angle;
	private FillMode fillMode = FillMode.getNoFillMode();
	private Color lineColor = Color.black;
	private byte lineWeight = 1;
	private byte lineStyle = LineStyle.STROKE_NUMBER_1;

	public RectangleState() {
	}

	public RectangleState(AbstractRectangle r) {
		x = r.getX();
		y = r.getY();
		w = r.getWidth();
		h = r.getHeight();
		arcWidth = r.getArcWidth();
		arcHeight = r.getArcHeight();
		angle = r.getAngle();
		alpha = r.getAlpha();
		fillMode = r.getFillMode();
		lineColor = r.getLineColor();
		lineWeight = r.getLineWeight();
		lineStyle = r.getLineStyle();
	}

	public void setLineStyle(byte style) {
		lineStyle = style;
	}

	public byte getLineStyle() {
		return lineStyle;
	}

	public void setLineWeight(byte weight) {
		lineWeight = weight;
	}

	public byte getLineWeight() {
		return lineWeight;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getX() {
		return x;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getY() {
		return y;
	}

	public void setWidth(float w) {
		this.w = w;
	}

	public float getWidth() {
		return w;
	}

	public void setHeight(float h) {
		this.h = h;
	}

	public float getHeight() {
		return h;
	}

	public void setArcWidth(float arcWidth) {
		this.arcWidth = arcWidth;
	}

	public float getArcWidth() {
		return arcWidth;
	}

	public void setArcHeight(float arcHeight) {
		this.arcHeight = arcHeight;
	}

	public float getArcHeight() {
		return arcHeight;
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}

	public float getAngle() {
		return angle;
	}

	public void setAlpha(short alpha) {
		this.alpha = alpha;
	}

	public short getAlpha() {
		return alpha;
	}

	public void setFillMode(FillMode fillMode) {
		this.fillMode = fillMode;
	}

	public FillMode getFillMode() {
		return fillMode;
	}

	public void setLineColor(Color color) {
		lineColor = color;
	}

	public Color getLineColor() {
		return lineColor;
	}

}