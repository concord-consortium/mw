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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;

import org.concord.modeler.PageRadioButton;

class RadioButtonConnector {

	private Map<Long, List<PageRadioButton>> map;

	RadioButtonConnector() {
		map = new HashMap<Long, List<PageRadioButton>>();
	}

	void connect() {
		if (map.isEmpty())
			return;
		for (List<PageRadioButton> list : map.values()) {
			ButtonGroup bg = new ButtonGroup();
			for (PageRadioButton rb : list) {
				rb.putClientProperty("button group", bg);
				bg.add(rb);
			}
		}
	}

	void enroll(PageRadioButton rb) {
		if (rb == null)
			return;
		Long id = rb.getGroupID();
		List<PageRadioButton> o = map.get(id);
		if (o != null) {
			o.add(rb);
		}
		else {
			List<PageRadioButton> list = new ArrayList<PageRadioButton>();
			list.add(rb);
			map.put(id, list);
		}
	}

	void clear() {
		map.clear();
	}

}