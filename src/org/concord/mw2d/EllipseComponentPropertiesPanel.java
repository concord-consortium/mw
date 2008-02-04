/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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

import org.concord.modeler.draw.AbstractEllipse;
import org.concord.modeler.draw.ui.EllipsePropertiesPanel;
import org.concord.mw2d.models.EllipseComponent;

class EllipseComponentPropertiesPanel extends EllipsePropertiesPanel {

	private EllipseComponent.Delegate delegate;

	public EllipseComponentPropertiesPanel(AbstractEllipse r) {
		super(r);
	}

	void destroy() {
	}

	public int getIndex() {
		if (!(ellipse instanceof EllipseComponent))
			return -1;
		return ((MDView) ellipse.getComponent()).getLayeredComponentIndex((EllipseComponent) ellipse);
	}

	public void notifyChange() {
		if (!(ellipse instanceof EllipseComponent))
			return;
		((EllipseComponent) ellipse).getHostModel().notifyChange();
	}

	public void storeSettings() {
		if (ellipse instanceof EllipseComponent) {
			delegate = new EllipseComponent.Delegate((EllipseComponent) ellipse);
		}
	}

	public void restoreSettings() {
		if (ellipse instanceof EllipseComponent) {
			((EllipseComponent) ellipse).set(delegate);
		}
	}

}