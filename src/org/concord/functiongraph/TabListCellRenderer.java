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

package org.concord.functiongraph;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
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

class TabListCellRenderer extends JLabel implements ListCellRenderer {

	protected static Border noFocusBorder;
	protected FontMetrics fm = null;
	protected Insets insets = new Insets(0, 0, 0, 0);
	protected int defaultTab = 130;
	protected int[] tabs = null;

	private static Font plainFont = new Font("Arial", Font.PLAIN, 12);
	private static Font italicFont = new Font("Courier New", Font.PLAIN, 12);

	public TabListCellRenderer() {
		super();
		noFocusBorder = new EmptyBorder(1, 1, 1, 1);
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

		if (tabs == null)
			return defaultTab * index;

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
		g.setFont(plainFont);
		insets = getInsets();
		int x = insets.left;
		int y = insets.top + fm.getAscent();

		StringTokenizer st = new StringTokenizer(getText(), "\t");
		int i = 0;

		while (st.hasMoreTokens()) {

			String sNext = st.nextToken();
			if (i == 0) {
				g.setColor(Color.blue);
				g.setFont(italicFont);
				g.drawString(sNext, x + 5, y);
				g.setFont(plainFont);
				g.setColor(Color.black);
			}
			else {
				g.drawString(sNext, x + 5, y);
			}
			x += fm.stringWidth(sNext);

			if (!st.hasMoreTokens())
				break;
			int index = 0;
			while (x >= getTab(index))
				index++;
			x = getTab(index);

			i++;

		}

	}

}
