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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.concord.modeler.SnapshotGallery;
import org.concord.modeler.UserData;
import org.concord.modeler.util.FileUtilities;

/*
 * @author Charles Xie
 * 
 */
class MultiplePageReportBuilder {

	private Page page;
	private Map<String, Object> map;

	MultiplePageReportBuilder(Page page) {
		this.page = page;
		map = new LinkedHashMap<String, Object>();
	}

	Map<String, Object> prepare(String reportTitle, PageNameGroup pageNameGroup) {

		map.clear();
		map.put("Page Title", reportTitle == null ? page.getTitle() : reportTitle);
		map.put("Page Address", page.getAddress());

		String[] snapshot = SnapshotGallery.sharedInstance().getImageNames();

		int n = pageNameGroup.size();
		String name = null;

		String parentPath = FileUtilities.getCodeBase(page.getAddress());
		List<String> list = new ArrayList<String>();

		for (int i = 0; i < n; i++) {

			name = pageNameGroup.getPageName(i);

			// obtain all the keys related to the current page
			list.clear();
			for (String key : UserData.sharedInstance().keySet()) {
				if (key.indexOf(parentPath + name) != -1) {
					list.add(key);
				}
			}
			if (!list.isEmpty()) {
				// sort the found keys into the ascending order
				Collections.sort(list);
				// now push the key-value pairs into the map
				int k = 0;
				for (String key : list) {
					if (key.indexOf("PageMultipleChoice") != -1) {
						map.put(name + "_PageMultipleChoice" + k, UserData.sharedInstance().getData(key));
					}
					else if (key.indexOf("PageTextField") != -1) {
						map.put(name + "_PageTextField" + k, UserData.sharedInstance().getData(key));
					}
					else if (key.indexOf("PageTextArea") != -1) {
						map.put(name + "_PageTextArea" + k, UserData.sharedInstance().getData(key));
					}
					else if (key.indexOf("PageTextBox") != -1) {
						map.put(name + "_PageTextBox" + k, UserData.sharedInstance().getData(key));
					}
					else if (key.indexOf("ImageQuestion") != -1) {
						map.put(name + "_ImageQuestion" + k, UserData.sharedInstance().getData(key));
					}
					k++;
				}
			}

			String pathBase = page.getPathBase();
			if (snapshot != null && snapshot.length > 0) {
				for (int k = 0; k < snapshot.length; k++) {
					if ((pathBase + name).equals(SnapshotGallery.sharedInstance().getOwnerPage(snapshot[k]))) {
						map.put(name + "_image" + k, new SnapshotImageWrapper(snapshot[k]));
					}
				}
			}

		}

		return map;

	}

}