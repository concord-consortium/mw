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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.concord.modeler.ui.TextBox;

class HTMLComponentConnector {

	private List<TextBox> htmlPaneList;

	HTMLComponentConnector() {
		htmlPaneList = Collections.synchronizedList(new ArrayList<TextBox>());
	}

	void enroll(TextBox textBox) {
		if (textBox == null)
			return;
		htmlPaneList.add(textBox);
	}

	void connect() {

		if (htmlPaneList.isEmpty())
			return;

		synchronized (htmlPaneList) {
			for (TextBox box : htmlPaneList)
				box.setEmbeddedComponentAttributes();
		}

	}

	void clear() {
		htmlPaneList.clear();
	}

}