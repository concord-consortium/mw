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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.SnapshotGallery;
import org.concord.modeler.util.FileUtilities;

class ImagePopupMenu extends JPopupMenu {

	private String url;
	private JMenuItem miCut, miCopy;
	private Icon image;
	private Page page;
	private ImagePropertiesDialog imagePropertiesDialog;

	ImagePopupMenu(Page p) {

		super("Image");
		page = p;

		String s = Modeler.getInternationalText("Copy");
		miCopy = new JMenuItem(s != null ? s : "Copy");
		miCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (image == null)
					return;
				page.copyImage(image);
			}
		});
		add(miCopy);

		s = Modeler.getInternationalText("Cut");
		miCut = new JMenuItem(s != null ? s : "Cut");
		miCut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (image == null)
					return;
				page.removeImage(image);
			}
		});
		add(miCut);

		s = Modeler.getInternationalText("SaveButton");
		JMenuItem mi = new JMenuItem((s != null ? s : "Save Image As") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (imagePropertiesDialog.getImage() != null) {
					ModelerUtilities.saveTargetAs(page, imagePropertiesDialog.getImage().toString());
				}
				else if (url != null) {
					url = ModelerUtilities.convertURLToFilePath(url);
					ModelerUtilities.saveTargetAs(page, url);
				}
			}
		});
		add(mi);

		s = Modeler.getInternationalText("Annotate");
		mi = new JMenuItem((s != null ? s : "Annotate") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (image != null) {
					SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), image);
				}
				else if (url != null) {
					url = ModelerUtilities.convertURLToFilePath(url);
					ImageIcon i2 = new ImageIcon(url);
					i2.setDescription(FileUtilities.getFileName(url));
					SnapshotGallery.sharedInstance().takeSnapshot(page.getAddress(), i2);
				}
				image = null;
				url = null;
			}
		});
		add(mi);
		addSeparator();

		s = Modeler.getInternationalText("Properties");
		mi = new JMenuItem((s != null ? s : "Properties") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				imagePropertiesDialog.setCurrentValues();
				imagePropertiesDialog.pack();
				imagePropertiesDialog.setVisible(true);
			}
		});
		add(mi);

	}

	public void show(Component invoker, int x, int y) {
		boolean b = page.isEditable();
		miCut.setEnabled(b);
		Object o = getClientProperty("copy disabled");
		miCopy.setEnabled(o != Boolean.TRUE);
		if (invoker instanceof IconWrapper) {
			Icon icon = ((IconWrapper) invoker).getIcon();
			if (icon instanceof ImageIcon)
				setImage((ImageIcon) icon);
		}
		super.show(invoker, x, y);
	}

	void setImage(ImageIcon image) {
		if (imagePropertiesDialog == null)
			imagePropertiesDialog = new ImagePropertiesDialog(page);
		imagePropertiesDialog.setImage(image);
		setName(image.toString());
		this.image = image;
	}

	public void setName(String s) {
		if (FileUtilities.isRelative(s)) {
			String s1 = FileUtilities.getCodeBase(page.getAddress());
			if (s1 != null)
				s = s1 + s;
		}
		url = s;
		if (imagePropertiesDialog == null)
			imagePropertiesDialog = new ImagePropertiesDialog(page);
		imagePropertiesDialog.setLocation(url);
	}

}