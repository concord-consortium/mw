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
import java.awt.Font;
import java.io.Serializable;

/**
 * This class defines a serializable label for axis.
 * 
 * @author Qian Xie
 */

public class AxisLabel implements Serializable {

	private String text;
	private Color color;
	private Font font;

	public AxisLabel() {
		text = "Label";
		color = Color.black;
		font = new Font("Times Roman", Font.PLAIN, 9);
	}

	public AxisLabel(String text) {
		this.text = text;
		color = Color.black;
		font = new Font("Times Roman", Font.PLAIN, 9);
	}

	public AxisLabel(String text, Color color, Font font) {
		this.text = text;
		this.color = color;
		this.font = font;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public Color getColor() {
		return color;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public Font getFont() {
		return font;
	}

}
