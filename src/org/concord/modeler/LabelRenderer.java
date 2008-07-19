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

import java.awt.Color;
import java.awt.Component;
import java.awt.SystemColor;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

class LabelRenderer extends JLabel implements ListCellRenderer {

	public LabelRenderer() {
		setOpaque(true);
	}

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {

		setBackground(isSelected ? SystemColor.textHighlight : Color.white);
		setForeground(isSelected ? SystemColor.textHighlightText : Color.black);

		if (value instanceof PageMolecularViewer) {
			PageMolecularViewer mv = (PageMolecularViewer) value;
			setText("3D Molecular Viewer #" + mv.getIndex());
		}
		else if (value instanceof PageMd3d) {
			PageMd3d md = (PageMd3d) value;
			setText("3D Molecular Simulator #" + md.getIndex());
		}
		else if (value instanceof PageJContainer) {
			PageJContainer c = (PageJContainer) value;
			setText("Plugin #" + c.getIndex());
		}
		else if (value instanceof PageApplet) {
			PageApplet a = (PageApplet) value;
			setText("Applet #" + a.getIndex());
		}
		else if (value != null) {
			setText(value.toString());
		}

		return this;

	}

}