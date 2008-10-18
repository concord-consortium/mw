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

package org.concord.mw2d.models;

public abstract class LayeredComponentDelegate extends ComponentDelegate {

	double x = 20, y = 20;
	int layer;
	byte layerPosition;
	String hostType;
	int hostIndex;
	boolean draggable = true;
	boolean visible = true;

	public void setDraggable(boolean b) {
		draggable = b;
	}

	public boolean isDraggable() {
		return draggable;
	}

	public void setVisible(boolean b) {
		visible = b;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setHostType(String s) {
		hostType = s;
	}

	public String getHostType() {
		return hostType;
	}

	public void setHostIndex(int i) {
		hostIndex = i;
	}

	public int getHostIndex() {
		return hostIndex;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getX() {
		return x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getY() {
		return y;
	}

	public void setLayer(int l) {
		layer = l;
	}

	public int getLayer() {
		return layer;
	}

	public void setLayerPosition(byte l) {
		layerPosition = l;
	}

	public byte getLayerPosition() {
		return layerPosition;
	}

}