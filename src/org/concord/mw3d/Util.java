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

package org.concord.mw3d;

import java.awt.Color;
import java.awt.Component;
import java.awt.SystemColor;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.event.PopupMenuListener;

class Util {

	private Util() {
	}

	static void showMessageWithPopupMenu(Component parent, String message) {
		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.setBackground(SystemColor.info);
		popupMenu.setBorder(BorderFactory.createLineBorder(Color.black));
		JLabel descriptionLabel = new JLabel(message);
		descriptionLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		popupMenu.add(descriptionLabel);
		popupMenu.pack();
		popupMenu.show(parent, 10, 10);
	}

	static void setWithoutNotifyingListeners(AbstractButton button, boolean selected) {

		if (button == null)
			return;

		Action a = button.getAction();
		String text = button.getText();

		ItemListener[] il = button.getItemListeners();
		if (il != null) {
			for (int i = 0; i < il.length; i++)
				button.removeItemListener(il[i]);
		}
		ActionListener[] al = button.getActionListeners();
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				button.removeActionListener(al[i]);
		}

		button.setSelected(selected);

		if (il != null) {
			for (int i = 0; i < il.length; i++)
				button.addItemListener(il[i]);
		}
		if (al != null) {
			for (int i = 0; i < al.length; i++)
				button.addActionListener(al[i]);
		}
		if (a != null)
			button.setAction(a);
		button.setText(text);

	}

	static void destroyToolBar(JToolBar toolBar) {
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
					for (int k = 0; k < al.length; k++)
						b.removeActionListener(al[k]);
				}
				b.setAction(null);
				ItemListener[] il = b.getItemListeners();
				if (il != null && il.length > 0) {
					for (int k = 0; k < il.length; k++)
						b.removeItemListener(il[k]);
				}
			}
		}
		toolBar.removeAll();
	}

	static void destroyMenu(JMenu menu) {
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

}
