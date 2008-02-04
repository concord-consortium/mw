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

package org.concord.modeler.text;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.concord.modeler.Modeler;

class ColorBarPopupMenu extends JPopupMenu {

	private JMenuItem miCut, miCopy, miEdit;
	private LineIcon colorBar;
	private Page page;

	ColorBarPopupMenu(Page p) {

		super("Color Bar");

		page = p;

		String s = Modeler.getInternationalText("Copy");
		miCopy = new JMenuItem(s != null ? s : "Copy");
		miCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (colorBar == null)
					return;
				page.copyImage(colorBar);
			}
		});
		add(miCopy);

		s = Modeler.getInternationalText("Cut");
		miCut = new JMenuItem(s != null ? s : "Cut");
		miCut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (colorBar == null)
					return;
				// This need to be invoked later, or it won't be undoable if the image is newly inserted and gets cut
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						page.removeImage(colorBar);
					}
				});
			}
		});
		add(miCut);

		addSeparator();

		s = Modeler.getInternationalText("Properties");
		miEdit = new JMenuItem((s != null ? s : "Properties") + "...");
		miEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (page.colorBarDialog == null)
					page.colorBarDialog = new ColorBarDialog(page);
				page.colorBarDialog.setColorBar(colorBar);
				page.colorBarDialog.setVisible(true);
				page.repaint();
			}
		});
		add(miEdit);

	}

	public void show(Component invoker, int x, int y) {
		boolean b = page.isEditable();
		miCut.setEnabled(b);
		miEdit.setEnabled(b);
		if (invoker instanceof IconWrapper) {
			Icon icon = ((IconWrapper) invoker).getIcon();
			if (icon instanceof LineIcon)
				setColorBar((LineIcon) icon);
		}
		super.show(invoker, x, y);
	}

	void setColorBar(LineIcon colorBar) {
		this.colorBar = colorBar;
	}

}