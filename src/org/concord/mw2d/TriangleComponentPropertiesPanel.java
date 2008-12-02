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

package org.concord.mw2d;

import org.concord.modeler.draw.AbstractTriangle;
import org.concord.modeler.draw.ui.TrianglePropertiesPanel;
import org.concord.mw2d.models.TriangleComponent;

class TriangleComponentPropertiesPanel extends TrianglePropertiesPanel {

	private TriangleComponent.Delegate delegate;

	public TriangleComponentPropertiesPanel(AbstractTriangle r) {
		super(r);
	}

	void destroy() {
	}

	public int getIndex() {
		if (!(triangle instanceof TriangleComponent))
			return -1;
		return ((MDView) triangle.getComponent()).getLayeredComponentIndex((TriangleComponent) triangle);
	}

	public void notifyChange() {
		if (!(triangle instanceof TriangleComponent))
			return;
		((TriangleComponent) triangle).getHostModel().notifyChange();
	}

	public void storeSettings() {
		if (triangle instanceof TriangleComponent) {
			delegate = new TriangleComponent.Delegate((TriangleComponent) triangle);
		}
	}

	public void restoreSettings() {
		if (triangle instanceof TriangleComponent) {
			((TriangleComponent) triangle).set(delegate);
		}
	}

}