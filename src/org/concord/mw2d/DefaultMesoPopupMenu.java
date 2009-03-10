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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.concord.modeler.ui.IconPool;

class DefaultMesoPopupMenu extends JPopupMenu {

	private MesoView view;

	void setCoor(int x, int y) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				view.getActionMap().get(MesoView.PASTE).setEnabled(view.pasteBuffer != null);
				view.clearEditor(true);
			}
		});
	}

	DefaultMesoPopupMenu(MesoView v) {

		super("Default");
		view = v;

		JMenuItem mi = new JMenuItem(view.getActionMap().get(MesoView.PASTE));
		String s = MDView.getInternationalText("Paste");
		if (s != null)
			mi.setText(s);
		add(mi);
		addSeparator();

		mi = new JMenuItem(view.getActionMap().get("Model Reader"));
		s = MDView.getInternationalText("OpenModel");
		if (s != null)
			mi.setText(s);
		add(mi);

		mi = new JMenuItem(view.getActionMap().get("Model Writer"));
		s = MDView.getInternationalText("SaveModel");
		if (s != null)
			mi.setText(s);
		add(mi);
		addSeparator();

		mi = new JMenuItem(view.getActionMap().get("Properties"));
		s = MDView.getInternationalText("Properties");
		if (s != null)
			mi.setText(s);
		add(mi);

		mi = new JMenuItem(view.getActionMap().get("Snapshot"));
		s = MDView.getInternationalText("Snapshot");
		if (s != null)
			mi.setText(s);
		add(mi);

		mi = new JMenuItem(view.getActionMap().get("View Options"));
		s = MDView.getInternationalText("ViewOption");
		if (s != null)
			mi.setText(s);
		add(mi);

		s = MDView.getInternationalText("TaskManager");
		mi = new JMenuItem(s != null ? s : "Task Manager", IconPool.getIcon("taskmanager"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.showTaskManager();
			}
		});
		add(mi);

		pack();

	}

}