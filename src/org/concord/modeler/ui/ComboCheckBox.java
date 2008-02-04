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

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.SystemColor;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

public class ComboCheckBox extends JComponent {

	private JCheckBox checkBox;
	private JLabel label;
	private boolean highlighted;

	public ComboCheckBox() {
		checkBox = new JCheckBox();
		label = new ColorLabel();
		setLayout(new BorderLayout(5, 5));
		add(checkBox, BorderLayout.WEST);
		add(label, BorderLayout.CENTER);
	}

	public void setHighlighted(boolean b) {
		label.setBackground(b ? SystemColor.textHighlight : getBackground());
		label.setForeground(b ? SystemColor.textHighlightText : SystemColor.textText);
		highlighted = b;
	}

	public boolean isHighlighted() {
		return highlighted;
	}

	public JCheckBox getCheckBox() {
		return checkBox;
	}

	public JLabel getLabel() {
		return label;
	}

	class ColorLabel extends JLabel {

		ColorLabel() {
			super();
		}

		public void paintComponent(Graphics g) {
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			super.paintComponent(g);
		}

	}

}
