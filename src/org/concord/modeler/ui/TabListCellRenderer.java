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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.StringTokenizer;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

public class TabListCellRenderer extends JLabel implements ListCellRenderer {

	private static Border noFocusBorder;
	private FontMetrics fm;
	private Insets insets;
	private int defaultTab = 100;
	private int[] tabs;

	public TabListCellRenderer() {
		super();
		noFocusBorder = new EmptyBorder(1, 1, 1, 1);
		insets = new Insets(0, 0, 0, 0);
		setOpaque(true);
		setBorder(noFocusBorder);
	}

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {

		setText(value.toString());

		setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
		setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());

		setFont(list.getFont());
		setBorder((cellHasFocus) ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);

		return this;

	}

	public void setDefaultTab(int defaultTab) {
		this.defaultTab = defaultTab;
	}

	public int getDefaultTab() {
		return defaultTab;
	}

	public void setTabs(int[] tabs) {
		this.tabs = tabs;
	}

	public int[] getTabs() {
		return tabs;
	}

	public int getTab(int index) {
		if (tabs == null) {
			return defaultTab * index;
		}

		int len = tabs.length;
		if (index >= 0 && index < len)
			return tabs[index];

		return tabs[len - 1] + defaultTab * (index - len + 1);
	}

	public void paint(Graphics g) {

		fm = g.getFontMetrics();

		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		getBorder().paintBorder(this, g, 0, 0, getWidth(), getHeight());

		g.setColor(getForeground());
		g.setFont(getFont());
		insets = getInsets();

		javax.swing.Icon icon = getIcon();
		if (icon != null)
			icon.paintIcon(this, g, insets.left, insets.top);

		int x = insets.left + (icon == null ? 0 : icon.getIconWidth()) + 5;
		int y = insets.top + fm.getAscent();

		StringTokenizer st = new StringTokenizer(getText(), "\t");
		while (st.hasMoreTokens()) {
			String sNext = st.nextToken();
			g.drawString(sNext, x, y);
			x += fm.stringWidth(sNext);

			if (!st.hasMoreTokens())
				break;
			int index = 0;
			while (x >= getTab(index))
				index++;
			x = getTab(index);
		}

	}

}
