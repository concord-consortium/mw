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

package org.concord.modeler.text;

public final class PrintParameters {

	private final static int DEFAULT_MARGIN = 16;

	private int topMargin = DEFAULT_MARGIN;
	private int bottomMargin = DEFAULT_MARGIN;
	private int leftMargin = DEFAULT_MARGIN;
	private int rightMargin = DEFAULT_MARGIN;

	private float componentScale = 0.7f;
	private float chracterScale = 1;
	private float imageScale = 0.7f;
	private float indentScale = 0.25f;

	public PrintParameters() {
		if (System.getProperty("os.name").startsWith("Mac")) {
			topMargin = 3 * DEFAULT_MARGIN;
			bottomMargin = 3 * DEFAULT_MARGIN;
			leftMargin = 4 * DEFAULT_MARGIN;
			rightMargin = 2 * DEFAULT_MARGIN;
		}
	}

	public void setTopMargin(int margin) {
		topMargin = margin;
	}

	public int getTopMargin() {
		return topMargin;
	}

	public void setBottomMargin(int margin) {
		bottomMargin = margin;
	}

	public int getBottomMargin() {
		return bottomMargin;
	}

	public void setLeftMargin(int margin) {
		leftMargin = margin;
	}

	public int getLeftMargin() {
		return leftMargin;
	}

	public void setRightMargin(int margin) {
		rightMargin = margin;
	}

	public int getRightMargin() {
		return rightMargin;
	}

	public void setComponentScale(float scale) {
		componentScale = scale;
	}

	public float getComponentScale() {
		return componentScale;
	}

	public void setCharacterScale(float scale) {
		chracterScale = scale;
	}

	public float getCharacterScale() {
		return chracterScale;
	}

	public void setImageScale(float scale) {
		imageScale = scale;
	}

	public float getImageScale() {
		return imageScale;
	}

	public void setIndentScale(float scale) {
		indentScale = scale;
	}

	public float getIndentScale() {
		return indentScale;
	}

}