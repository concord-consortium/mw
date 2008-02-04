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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.SystemColor;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.ListCellRenderer;

import org.concord.modeler.draw.ArrowRectangle;
import org.concord.modeler.draw.LineStyle;
import org.concord.modeler.draw.LineSymbols;
import org.concord.modeler.draw.LineWidth;

/** Render images in a combo box */

public abstract class ComboBoxRenderer {

	public static class IconRenderer extends JRadioButton implements ListCellRenderer {

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			setBackground(isSelected ? SystemColor.textHighlight : Color.white);
			setForeground(isSelected ? SystemColor.textHighlightText : Color.black);
			setHorizontalAlignment(CENTER);
			if (value instanceof Icon) {
				setIcon((Icon) value);
				if (value instanceof ImageIcon) {
					setToolTipText(((ImageIcon) value).getDescription());
				}
			}
			else {
				setText(value == null ? "Unknown" : value.toString());
			}
			return this;
		}
	}

	public static class Symbols extends LineSymbols implements ListCellRenderer {

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			setBackground(isSelected ? SystemColor.textHighlight : Color.white);
			setForeground(isSelected ? SystemColor.textHighlightText : Color.black);
			setSymbolNumber(((Integer) value).intValue());
			return this;
		}

	}

	public static class LineStyles extends LineStyle implements ListCellRenderer {

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			setBackground(isSelected ? SystemColor.textHighlight : Color.white);
			setForeground(isSelected ? SystemColor.textHighlightText : Color.black);
			setStrokeNumber(((Integer) value).intValue());
			return this;
		}

	}

	public static class LineThickness extends LineWidth implements ListCellRenderer {

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			setBackground(isSelected ? SystemColor.textHighlight : Color.white);
			setForeground(isSelected ? SystemColor.textHighlightText : Color.black);
			setStrokeNumber(((Float) value).floatValue());
			return this;

		}

	}

	public static class ColorCell extends ColorRectangle implements ListCellRenderer {

		public ColorCell() {
			super();
		}

		public ColorCell(Color moreColor) {
			this();
			if (!isDefaultColor(moreColor))
				setMoreColor(moreColor);
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			setBackground(isSelected ? SystemColor.textHighlight : Color.white);
			setForeground(isSelected ? SystemColor.textHighlightText : Color.black);
			setColorID((Integer) value);
			return this;
		}
	}

	public static class BorderCell extends BorderRectangle implements ListCellRenderer {

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			setBackground(isSelected ? SystemColor.textHighlight : SystemColor.menu);
			setForeground(isSelected ? SystemColor.textHighlightText : SystemColor.menuText);
			setBorderType(value.toString());
			return this;
		}

	}

	public static class ArrowCell extends ArrowRectangle implements ListCellRenderer {

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			setBackground(isSelected ? SystemColor.textHighlight : SystemColor.menu);
			setForeground(isSelected ? SystemColor.textHighlightText : SystemColor.menuText);
			setArrowType(((Byte) value).byteValue());
			return this;
		}

	}

	public static class FontLabel extends JLabel implements ListCellRenderer {

		public FontLabel() {
			setOpaque(true);
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			}
			else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			String s = (String) value;
			setText(s);
			setFont(new Font(s, Font.PLAIN, 12));
			return this;
		}

	}

}