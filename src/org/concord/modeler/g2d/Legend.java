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
import java.awt.Point;
import java.io.Serializable;

public class Legend implements Serializable {

	private String text;
	private Color color;
	private Point location;
	private Font font;
	private int sampleLineLength;

	public Legend() {
		this("Legend");
	}

	public Legend(String text) {
		this(text, 50, 50);
	}

	public Legend(String text, int x, int y) {
		this(text, Color.black, new Font("Arial", Font.PLAIN, 9), new Point(x, y), 20);
	}

	public Legend(String text, Color color, Font font, Point location, int sampleLineLength) {
		this.text = text;
		this.color = color;
		this.font = font;
		this.location = location;
		this.sampleLineLength = sampleLineLength;
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

	public void setLocation(Point p) {
		location.x = p.x;
		location.y = p.y;
	}

	public Point getLocation() {
		return location;
	}

	public void setSampleLineLength(int i) {
		sampleLineLength = i;
	}

	public int getSampleLineLength() {
		return sampleLineLength;
	}

}
