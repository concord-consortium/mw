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

import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTML;

import org.concord.modeler.SnapshotGallery;

/**
 * @author Charles Xie
 * 
 */
class SnapshotGalleryFormatter extends ReportFormatter {

	SnapshotGalleryFormatter(Page page) {
		super(page);
	}

	void format() {
		page.createNewPage();
		String[] snapshot = SnapshotGallery.sharedInstance().getImageNames();
		if (snapshot == null || snapshot.length <= 0)
			return;
		StyledDocument doc = page.getStyledDocument();
		try {
			doc.insertString(doc.getLength(), "My snapshot images\n\n", titleStyle);
		}
		catch (BadLocationException e) {
			e.printStackTrace();
		}
		String parent = SnapshotGallery.sharedInstance().getAnnotatedImageFolder().getAbsolutePath()
				+ System.getProperty("file.separator");
		String s;
		Image image = null;
		for (int k = 0; k < snapshot.length; k++) {
			if (k >= MAX_IMAGE) {
				page.insertString("\n" + (k + 1) + ". Location: ");
				s = parent + snapshot[k];
				linkStyle.removeAttribute(HTML.Attribute.HREF);
				linkStyle.removeAttribute(HTML.Attribute.TARGET);
				linkStyle.addAttribute(HTML.Attribute.HREF, s);
				page.insertString(s, linkStyle);
				page.insertLineBreak();
				image = SnapshotGallery.sharedInstance().getThumbnail(k);
				if (image != null) {
					page.insertIcon(new ImageIcon(image, "tn_" + snapshot[k]));
					page.insertLineBreak();
				}
			}
			else {
				page.insertString((k + 1) + ".\n");
				page.insertIcon(SnapshotGallery.sharedInstance().loadAnnotatedImage(snapshot[k]));
				page.insertLineBreak();
			}
			Object o = SnapshotGallery.sharedInstance().getProperty("comment:" + snapshot[k]);
			if (o != null)
				page.insertString(o + "\n\n", answerStyle);
		}
		page.insertLineBreak();
		page.insertLineBreak();
		page.setTitle("My snapshot images");
		appendButtonsToReport(false);
	}

}