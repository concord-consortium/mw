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

import org.concord.modeler.draw.LineSymbols;

/**
 * This wrapper class defines a symbol for decorating curve. There are several kind of symbols provided in
 * <tt>LineSymbols</tt>.
 * 
 * @see org.concord.modeler.draw.LineSymbols
 * @author Qian Xie
 */

public class Symbol implements Serializable {

	private int type;
	private int size;
	private int spacing;
	private Color color;

	public Symbol() {
		type = LineSymbols.SYMBOL_NUMBER_0;
		size = 6;
		spacing = 5;
		color = Color.white;
	}

	public Symbol(int type) {
		this.type = Math.min(type, LineSymbols.MAX);
		size = 6;
		spacing = 5;
		color = Color.white;
	}

	public Symbol(int type, int spacing) {
		if (spacing <= 0)
			throw new IllegalArgumentException("spacing must be greater than 1");
		this.type = type;
		size = 6;
		this.spacing = spacing;
		color = Color.white;
	}

	public Symbol(int type, int size, int spacing, Color color) {
		if (spacing <= 0)
			throw new IllegalArgumentException("spacing must be greater than 1");
		this.type = type;
		this.size = size;
		this.spacing = spacing;
		if (color != null)
			this.color = color;
	}

	public void setType(int i) {
		type = i;
	}

	public int getType() {
		return type;
	}

	public void setSize(int i) {
		size = i;
	}

	public int getSize() {
		return size;
	}

	public void setSpacing(int i) {
		spacing = i;
	}

	public int getSpacing() {
		return spacing;
	}

	public void setColor(Color c) {
		color = c;
	}

	public Color getColor() {
		return color;
	}

}
