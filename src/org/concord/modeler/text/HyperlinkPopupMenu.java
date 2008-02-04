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

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.event.PageEvent;
import org.concord.modeler.util.FileUtilities;

class HyperlinkPopupMenu extends JPopupMenu {

	private String hotLink;
	private JMenuItem miSave, miJNLP, miEdit;
	private Page page;
	private LinkPropertiesDialog linkPropertiesDialog;

	HyperlinkPopupMenu(Page p) {

		super("Hyperlink");
		page = p;

		String s = Modeler.getInternationalText("OpenButton");
		JMenuItem mi = new JMenuItem(s != null ? s : "Open");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// targetIsBlank=false;
				page.openHyperlink(hotLink);
			}
		});
		add(mi);

		s = Modeler.getInternationalText("OpenInNewWindow");
		mi = new JMenuItem(s != null ? s : "Open in a New Window");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.notifyPageListeners(new PageEvent(page, PageEvent.OPEN_NEW_WINDOW, hotLink));
			}
		});
		add(mi);

		s = Modeler.getInternationalText("Edit");
		miEdit = new JMenuItem(s != null ? s : "Edit");
		miEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				page.getAction("Hyperlink").actionPerformed(e);
			}
		});
		add(miEdit);

		s = Modeler.getInternationalText("SaveTargetAs");
		miSave = new JMenuItem(s != null ? s : "Save Target As");
		miSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ModelerUtilities.saveTargetAs(page, hotLink);
			}
		});
		add(miSave);
		addSeparator();

		s = Modeler.getInternationalText("CopyShortcut");
		mi = new JMenuItem(s != null ? s : "Copy Shortcut");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (hotLink == null)
					return;
				StringSelection clip = new StringSelection(hotLink);
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(clip, clip);
			}
		});
		add(mi);

		s = Modeler.getInternationalText("CreateJNLPLauncherForLink");
		miJNLP = new JMenuItem((s != null ? s : "Create a JNLP Launcher for This Link") + "...");
		miJNLP.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (hotLink == null)
					return;
				if (!FileUtilities.isRemote(hotLink))
					return;
				StringSelection ss = new StringSelection(Modeler.getContextRoot() + "tmp.jnlp?address=" + hotLink);
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
				JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(page),
						"An URL for opening the linked page from outside MW\nis now available for pasting.");
			}
		});
		add(miJNLP);
		addSeparator();

		s = Modeler.getInternationalText("Properties");
		mi = new JMenuItem((s != null ? s : "Properties") + "...");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (linkPropertiesDialog == null)
					linkPropertiesDialog = new LinkPropertiesDialog(page);
				linkPropertiesDialog.setCurrentValues();
				linkPropertiesDialog.pack();
				linkPropertiesDialog.setVisible(true);
			}
		});
		add(mi);

	}

	public void setName(String s) {
		if (FileUtilities.isRelative(s))
			s = FileUtilities.getCodeBase(page.getAddress()) + s;
		hotLink = s;
		miJNLP.setEnabled(FileUtilities.isRemote(hotLink));
		if (linkPropertiesDialog == null)
			linkPropertiesDialog = new LinkPropertiesDialog(page);
		linkPropertiesDialog.setLink(s);
		String s2 = s.toLowerCase();
		if (s2.endsWith(".jnlp") || s2.endsWith(".swf") || s2.endsWith(".jpg") || s2.endsWith(".jpeg")
				|| s2.endsWith(".png") || s2.endsWith(".gif")) {
			miSave.setEnabled(true);
		}
		else {
			miSave.setEnabled(false);
		}
		miEdit.setEnabled(page.isEditable());
	}

}