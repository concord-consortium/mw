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

package org.concord.modeler.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.concord.modeler.PageApplet;
import org.concord.modeler.PageJContainer;

class JavaStarter {

	private List<Object> list;

	JavaStarter() {
		list = Collections.synchronizedList(new ArrayList<Object>());
	}

	void enroll(PageApplet applet) {
		if (applet == null)
			return;
		list.add(applet);
	}

	void enroll(PageJContainer plugin) {
		if (plugin == null)
			return;
		list.add(plugin);
	}

	void start() {

		if (list.isEmpty())
			return;

		Thread t = new Thread("Applet Starter") {
			public void run() {
				synchronized (list) {
					for (Object a : list) {
						if (a instanceof PageApplet)
							((PageApplet) a).start();
						else if (a instanceof PageJContainer)
							((PageJContainer) a).start();
					}
				}
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();

	}

	void clear() {
		list.clear();
	}

}