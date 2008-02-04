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

package org.concord.modeler.ui;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.concord.modeler.Model;

/**
 * More responsive menu bar for a simulator. If a simulation is running in a simulator equipped with such a menu bar,
 * the simulation will yield (stop) to menu actions.
 */

public class SimulatorMenuBar extends JMenuBar {

	private Model model;

	public SimulatorMenuBar() {
		super();
	}

	public SimulatorMenuBar(Model inputModel) {
		this();
		model = inputModel;
	}

	/** destroy this menu bar to reclaim memory. */
	public void destroy() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				for (int i = 0, n = getMenuCount(); i < n; i++)
					destroyMenu(getMenu(i));
				removeAll();
			}
		});
		model = null;
	}

	private void destroyMenu(JMenu menu) {
		if (menu == null)
			return;
		PopupMenuListener[] pml = menu.getPopupMenu().getPopupMenuListeners();
		if (pml != null) {
			for (int k = 0; k < pml.length; k++)
				menu.getPopupMenu().removePopupMenuListener(pml[k]);
		}
		Component c;
		for (int i = 0, n = menu.getComponentCount(); i < n; i++) {
			c = menu.getComponent(i);
			if (c instanceof JMenu) {
				destroyMenu((JMenu) c);
			}
			else if (c instanceof JMenuItem) {
				JMenuItem menuItem = (JMenuItem) c;
				menuItem.setAction(null);
				ActionListener[] al = menuItem.getActionListeners();
				if (al != null) {
					for (int k = 0; k < al.length; k++)
						menuItem.removeActionListener(al[k]);
				}
				ItemListener[] il = menuItem.getItemListeners();
				if (il != null) {
					for (int k = 0; k < il.length; k++)
						menuItem.removeItemListener(il[k]);
				}
			}
		}
		menu.removeAll();
	}

	/** set the model this menu bar is associated with */
	public void setModel(Model model) {
		this.model = model;
	}

	/** get the model this menu bar is associated with */
	public Model getModel() {
		return model;
	}

	public void threadPreempt() {

		int n = getMenuCount();
		if (n == 0)
			return;

		PopupMenuListener listener = new PopupMenuListener() {

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				if (model == null)
					return;
				if (model.getJob() == null)
					return;
				if (model.getJob().isStopped())
					return;
				model.getJob().stop();
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				if (model == null)
					return;
				if (model.getJob() == null)
					return;
				if (model.getJob().isStopped() && !model.getJob().isTurnedOff())
					model.getJob().start();
			}

			public void popupMenuCanceled(PopupMenuEvent e) {
			}

		};

		for (int i = 0; i < n; i++)
			getMenu(i).getPopupMenu().addPopupMenuListener(listener);

	}

}
