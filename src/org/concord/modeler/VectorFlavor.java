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

package org.concord.modeler;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.io.Serializable;

import org.concord.modeler.draw.LineStyle;

/**
 * This class defines the flavor of vector displaying
 * 
 * @author Charles Xie
 */

public class VectorFlavor implements Serializable {

	private Color color;
	private Stroke stroke;
	private int length;
	private int style;
	private float width = 1.0f;

	public VectorFlavor() {
		color = Color.black;
		stroke = new BasicStroke(1.0f);
		length = 100;
		style = LineStyle.STROKE_NUMBER_1;
	}

	public VectorFlavor(Color c, Stroke s, int l) {
		set(c, s, l);
		style = LineStyle.STROKE_NUMBER_1;
	}

	public VectorFlavor(Color c, Stroke s, int l, int i) {
		set(c, s, l);
		style = i;
	}

	public void set(Color c, Stroke s, int l) {
		color = c;
		stroke = s;
		length = l;
	}

	public void setColor(Color c) {
		color = c;
	}

	public Color getColor() {
		return color;
	}

	public void setLength(int i) {
		length = i;
	}

	public int getLength() {
		return length;
	}

	public void setWidth(float i) {
		width = i;
	}

	public float getWidth() {
		return width;
	}

	public void setStroke(Stroke s) {
		stroke = s;
		if (s instanceof BasicStroke)
			setWidth(((BasicStroke) s).getLineWidth());
	}

	public Stroke getStroke() {
		return stroke;
	}

	public void setStyle(int i) {
		style = i;
	}

	public int getStyle() {
		return style;
	}

}