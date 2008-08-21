/*
 *   Copyright (C) 2008  The Concord Consortium, Inc.,
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

import java.awt.FlowLayout;

import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * @author Charles Xie
 * 
 */
public class CheckBoxLinkPanel extends JPanel {

	private JCheckBox checkBox;
	private HyperlinkLabel label;

	public CheckBoxLinkPanel() {
		super(new FlowLayout(FlowLayout.LEFT, 0, 0));
		checkBox = new JCheckBox();
		label = new HyperlinkLabel();
		label.setHorizontalAlignment(SwingConstants.LEFT);
		add(checkBox);
		add(label);
	}

	public JCheckBox getCheckBox() {
		return checkBox;
	}

	public void setCheckBoxAction(Action a) {
		checkBox.setAction(a);
		checkBox.setText(null);
	}

	public void setLinkText(String text) {
		label.setText("<html><u><font color=\"#0000ff\">" + text + "</font></u></html>");
	}

	public void setLinkAction(Runnable r) {
		label.setAction(r);
	}

	public void setLinkToolTip(String s) {
		label.setToolTipText(s);
	}

}
