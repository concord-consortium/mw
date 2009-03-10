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

import org.concord.modeler.ui.IconPool;

class AngularBondPopupMenu extends ViewPopupMenu {

	private AtomisticView view;

	void setCoor(int ex, int ey) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				view.clearEditor(false);
			}
		});
	}

	AngularBondPopupMenu(AtomisticView v) {

		super("Angular Bond", v);
		view = v;

		String s = MDView.getInternationalText("Properties");
		JMenuItem mi = new JMenuItem(s != null ? s : "Properties", IconPool.getIcon("properties"));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DialogFactory.showDialog(view.selectedComponent);
			}
		});
		add(mi);
		addSeparator();

		mi = new JMenuItem(view.getActionMap().get(AtomisticView.CUT));
		s = MDView.getInternationalText("Cut");
		if (s != null)
			mi.setText(s);
		add(mi);

		pack();

	}

}