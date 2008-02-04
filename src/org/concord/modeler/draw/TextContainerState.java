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
import java.awt.Font;
import java.awt.Point;

/**
 * @author Charles Xie
 * 
 */
public class TextContainerState {

	private float x = 20;
	private float y = 20;
	private float angle;
	private String text;
	private Font font = new Font(null, Font.PLAIN, 12);
	private Color fgColor = Color.black;
	private byte borderType;
	private byte shadowType;
	private FillMode fillMode = FillMode.getNoFillMode();
	private byte attachmentPosition = TextContainer.BOX_CENTER;
	private boolean callOut;
	private Point callOutPoint = new Point(20, 20);

	public TextContainerState() {
	}

	public TextContainerState(TextContainer t) {
		x = (float) t.getRx();
		y = (float) t.getRy();
		angle = t.getAngle();
		text = t.getText();
		font = t.getFont();
		fgColor = t.getForegroundColor();
		fillMode = t.getFillMode();
		callOut = t.isCallOut();
		callOutPoint = t.getCallOutPoint();
		attachmentPosition = t.getAttachmentPosition();
		borderType = t.getBorderType();
		shadowType = t.getShadowType();
	}

	public void setFillMode(FillMode m) {
		fillMode = m;
	}

	public FillMode getFillMode() {
		return fillMode;
	}

	public void setForegroundColor(Color c) {
		fgColor = c;
	}

	public Color getForegroundColor() {
		return fgColor;
	}

	public void setCallOutPoint(Point p) {
		callOutPoint = p;
	}

	public Point getCallOutPoint() {
		return callOutPoint;
	}

	public void setCallOut(boolean b) {
		callOut = b;
	}

	public boolean isCallOut() {
		return callOut;
	}

	public void setAttachmentPosition(byte position) {
		attachmentPosition = position;
	}

	public byte getAttachmentPosition() {
		return attachmentPosition;
	}

	public void setShadowType(byte type) {
		shadowType = type;
	}

	public byte getShadowType() {
		return shadowType;
	}

	public void setBorderType(byte type) {
		borderType = type;
	}

	public byte getBorderType() {
		return borderType;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public Font getFont() {
		return font;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
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

	public void setAngle(float angle) {
		this.angle = angle;
	}

	public float getAngle() {
		return angle;
	}

}
