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

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.swing.AbstractButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.swing.event.PopupMenuListener;

/**
 * @author Charles Xie
 * 
 */
public final class Destroyer {

	public static void destroyToolBar(JToolBar toolBar) {
		if (toolBar == null)
			return;
		int n = toolBar.getComponentCount();
		if (n <= 0)
			return;
		Component c;
		AbstractButton b;
		for (int i = 0; i < n; i++) {
			c = toolBar.getComponentAtIndex(i);
			if (c instanceof AbstractButton) {
				b = (AbstractButton) c;
				ActionListener[] al = b.getActionListeners();
				if (al != null && al.length > 0) {
					for (ActionListener k : al)
						b.removeActionListener(k);
				}
				b.setAction(null);
				ItemListener[] il = b.getItemListeners();
				if (il != null && il.length > 0) {
					for (ItemListener k : il)
						b.removeItemListener(k);
				}
			}
		}
		toolBar.removeAll();
	}

	public static void destroyMenu(JMenu menu) {
		if (menu == null)
			return;
		PopupMenuListener[] pml = menu.getPopupMenu().getPopupMenuListeners();
		if (pml != null) {
			for (PopupMenuListener k : pml)
				menu.getPopupMenu().removePopupMenuListener(k);
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
					for (ActionListener k : al)
						menuItem.removeActionListener(k);
				}
				ItemListener[] il = menuItem.getItemListeners();
				if (il != null) {
					for (ItemListener k : il)
						menuItem.removeItemListener(k);
				}
			}
		}
		menu.removeAll();
	}

}