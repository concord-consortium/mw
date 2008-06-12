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
package org.concord.jmol;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * @author Charles Xie
 * 
 */
class MyListCellRenderer extends JLabel implements ListCellRenderer {

	private final static Icon SCENE_ICON = new ImageIcon(JmolContainer.class.getResource("resources/Scene.gif"));
	private final static Icon INDENT_ICON = new Icon() {
		public int getIconHeight() {
			return SCENE_ICON.getIconHeight();
		}

		public int getIconWidth() {
			return SCENE_ICON.getIconWidth();
		}

		public void paintIcon(Component c, Graphics g, int w, int h) {
		}
	};

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		setText(value.toString());
		if (isSelected) {
			setIcon(SCENE_ICON);
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		}
		else {
			setIcon(INDENT_ICON);
			setBackground(Color.white);
			setForeground(list.getForeground());
		}
		setOpaque(true);
		setEnabled(list.isEnabled());
		setFont(list.getFont());
		return this;
	}
}