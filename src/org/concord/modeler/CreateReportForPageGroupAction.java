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

import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.JOptionPane;

import org.concord.modeler.text.PageNameGroup;

class CreateReportForPageGroupAction extends AbstractCreateReportAction {

	private PageNameGroup png;

	CreateReportForPageGroupAction(Modeler modeler) {
		super(modeler);
		putValue(NAME, "Create Multipage Report");
		putValue(SHORT_DESCRIPTION, "Create a report for a page group in a new window");
	}

	public void actionPerformed(ActionEvent e) {
		if (ModelerUtilities.stopFiring(e))
			return;
		Object src = e.getSource();
		if (!(src instanceof ActivityButton))
			return;
		ActivityButton ab = (ActivityButton) src;
		String pageGroup = ab.getPageNameGroup();
		if (pageGroup == null)
			return;
		if (question()) {
			if (!justPrint && Modeler.user.isEmpty()) {
				String s = Modeler.getInternationalText("YouAreNotLoggedInYetPleaseTryAgain");
				JOptionPane.showMessageDialog(modeler, s != null ? s : "You are not logged in yet. Please try again.",
						"Message", JOptionPane.WARNING_MESSAGE);
				return;
			}
			if (png == null)
				png = new PageNameGroup();
			png.setNameGroup(pageGroup);
			Map map = modeler.editor.getPage().prepareReportForPageGroup(png);
			if (map != null)
				openReportPage(map, png);
		}
	}

}