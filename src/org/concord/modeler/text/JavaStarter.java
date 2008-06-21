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

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import org.concord.modeler.PagePlugin;

class JavaStarter {

	private List<PagePlugin> list;

	JavaStarter() {
		list = Collections.synchronizedList(new ArrayList<PagePlugin>());
	}

	void enroll(PagePlugin pi) {
		if (pi == null)
			return;
		list.add(pi);
	}

	void start() {

		if (list.isEmpty())
			return;

		Thread t = new Thread("Plugin Starter") {
			public void run() {
				synchronized (list) {
					for (PagePlugin a : list) {
						a.start();
					}
				}
			}
		};
		t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, final Throwable e) {
				e.printStackTrace();
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						PagePlugin pi = list.get(0);
						JOptionPane
								.showMessageDialog(
										pi == null ? null : JOptionPane.getFrameForComponent(pi),
										"Fatal error in starting plugin, thrown by: "
												+ e
												+ "\nPlease reload the page. If the error persists, please restart the software.",
										"Error", JOptionPane.ERROR_MESSAGE);
					}
				});
			}
		});
		t.setPriority(Thread.MIN_PRIORITY + 1);
		t.start();

	}

	void clear() {
		list.clear();
	}

}