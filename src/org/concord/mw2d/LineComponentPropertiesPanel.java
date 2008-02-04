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

package org.concord.mw2d;

import org.concord.modeler.draw.AbstractLine;
import org.concord.modeler.draw.ui.LinePropertiesPanel;
import org.concord.mw2d.models.LineComponent;

class LineComponentPropertiesPanel extends LinePropertiesPanel {

	private LineComponent.Delegate delegate;

	public LineComponentPropertiesPanel(AbstractLine l) {
		super(l);
	}

	void destroy() {
	}

	public int getLineIndex() {
		if (!(line instanceof LineComponent))
			return -1;
		return ((MDView) line.getComponent()).getLayeredComponentIndex((LineComponent) line);
	}

	public void notifyChange() {
		if (!(line instanceof LineComponent))
			return;
		((LineComponent) line).getHostModel().notifyChange();
	}

	public void storeSettings() {
		if (line instanceof LineComponent) {
			delegate = new LineComponent.Delegate((LineComponent) line);
		}
	}

	public void restoreSettings() {
		if (line instanceof LineComponent) {
			((LineComponent) line).set(delegate);
		}
	}

}