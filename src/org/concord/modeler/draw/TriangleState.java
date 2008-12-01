/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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
 * This class defines a serializable object for the state of a triangle.
 * 
 * @author Charles Xie
 * @see org.concord.modeler.draw.AbstractTriangle
 */
public class TriangleState {

	private float xA, yA, xB, yB, xC, yC;
	private short alpha = 255;
	private float angle;
	private FillMode fillMode = FillMode.getNoFillMode();
	private Color lineColor = Color.black;
	private byte lineWeight = 1;
	private byte lineStyle = LineStyle.STROKE_NUMBER_1;

	public TriangleState() {
	}

	public TriangleState(AbstractTriangle t) {
		xA = t.getVertext(0).x;
		yA = t.getVertext(0).y;
		xB = t.getVertext(1).x;
		yB = t.getVertext(1).y;
		xC = t.getVertext(2).x;
		yC = t.getVertext(2).y;
		angle = t.getAngle();
		alpha = t.getAlpha();
		fillMode = t.getFillMode();
		lineColor = t.getLineColor();
		lineWeight = t.getLineWeight();
		lineStyle = t.getLineStyle();
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

	public void setXa(float x) {
		xA = x;
	}

	public float getXa() {
		return xA;
	}

	public void setYa(float y) {
		yA = y;
	}

	public float getYa() {
		return yA;
	}

	public void setXb(float x) {
		xB = x;
	}

	public float getXb() {
		return xB;
	}

	public void setYb(float y) {
		yB = y;
	}

	public float getYb() {
		return yB;
	}

	public void setXc(float x) {
		xC = x;
	}

	public float getXc() {
		return xC;
	}

	public void setYc(float y) {
		yC = y;
	}

	public float getYc() {
		return yC;
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