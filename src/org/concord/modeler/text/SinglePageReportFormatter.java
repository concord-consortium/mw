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
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTML;

import org.concord.modeler.Modeler;
import org.concord.modeler.SnapshotGallery;

/**
 * @author Charles Xie
 * 
 */
class SinglePageReportFormatter extends ReportFormatter {

	SinglePageReportFormatter(Page page) {
		super(page);
	}

	void format(Map<String, Object> map) {

		page.createNewPage();
		createReportHeader(map);

		StyledDocument doc = page.getStyledDocument();
		String s = Modeler.getInternationalText("PageLocation");
		Object o;
		try {
			doc.insertString(doc.getLength(), (s != null ? s : "Page location") + ": ", answerStyle);
			o = map.get("Page Address");
			if (o != null) {
				linkStyle.removeAttribute(HTML.Attribute.HREF);
				linkStyle.removeAttribute(HTML.Attribute.TARGET);
				linkStyle.addAttribute(HTML.Attribute.HREF, "" + o);
				doc.insertString(doc.getLength(), "" + o, linkStyle);
			}
			doc.insertString(doc.getLength(), "\n\n", answerStyle);
			map.remove("Page Address");

		}
		catch (BadLocationException ble) {
			ble.printStackTrace();
		}

		final int position0 = doc.getLength();
		boolean hasImageQuestion = false;
		for (String key : map.keySet()) {
			o = map.get(key);
			if (o instanceof ImageIcon) {
				hasImageQuestion = true;
				break;
			}
		}

		boolean firstImage = true;
		int imageCount = 0;
		String parent = SnapshotGallery.sharedInstance().getAnnotatedImageFolder().getAbsolutePath()
				+ System.getProperty("file.separator");
		Image image = null;
		for (String key : map.keySet()) {
			o = map.get(key);
			if (o instanceof String) {
				key = key.replace('\n', ' ');
				try {
					doc.insertString(doc.getLength(), key + "\n\n", questionStyle);
					doc.insertString(doc.getLength(), o + "\n\n", answerStyle);
				}
				catch (BadLocationException ble) {
					ble.printStackTrace();
				}
			}
			else if (o instanceof SnapshotImageWrapper) {
				if (hasImageQuestion)
					continue;
				imageCount++;
				if (imageCount <= MAX_IMAGE) {
					Style style = page.addStyle(null, null);
					StyleConstants.setIcon(style, ((SnapshotImageWrapper) o).getImage());
					try {
						if (firstImage) {
							doc.insertString(position0, "Snapshots you have taken for this page:\n\n", answerStyle);
							firstImage = false;
						}
						doc.insertString(doc.getLength(), "\n\n", null);
						doc.insertString(doc.getLength(), " ", style);
						doc.insertString(doc.getLength(), "\n\n", null);
						Object comment = SnapshotGallery.sharedInstance().getProperty("comment:" + o);
						if (comment != null) {
							doc.insertString(doc.getLength(), "" + comment, null);
							doc.insertString(doc.getLength(), "\n\n", null);
						}
					}
					catch (BadLocationException ble) {
						ble.printStackTrace();
					}
				}
				else {
					if (imageCount == MAX_IMAGE + 1) {
						page.insertString("More snapshot images for this page:", highlightStyle);
						page.insertLineBreak();
					}
					page.insertLineBreak();
					image = SnapshotGallery.sharedInstance().getThumbnail(o.toString());
					if (image != null) {
						page.insertIcon(new ImageIcon(image, "tn_" + o));
						page.insertLineBreak();
					}
					page.insertString("Location: ", answerStyle);
					linkStyle.removeAttribute(HTML.Attribute.HREF);
					linkStyle.removeAttribute(HTML.Attribute.TARGET);
					String link = parent + o;
					linkStyle.addAttribute(HTML.Attribute.HREF, link);
					page.insertString(link, linkStyle);
					page.insertLineBreak();
					Object comment = SnapshotGallery.sharedInstance().getProperty("comment:" + o);
					if (comment != null) {
						page.insertString("" + comment + "\n\n", answerStyle);
					}
					else {
						page.insertLineBreak();
					}
				}
			}
			else if (o instanceof ImageIcon) {
				key = key.replace('\n', ' ');
				Style style = page.addStyle(null, null);
				StyleConstants.setIcon(style, (ImageIcon) o);
				try {
					doc.insertString(doc.getLength(), key + "\n\n", questionStyle);
					doc.insertString(doc.getLength(), " ", style);
					doc.insertString(doc.getLength(), "\n\n", null);
					Object comment = SnapshotGallery.sharedInstance().getProperty(
							"comment:" + ((ImageIcon) o).getDescription());
					if (comment != null)
						doc.insertString(doc.getLength(), "" + comment, null);
					doc.insertString(doc.getLength(), "\n\n", null);
				}
				catch (BadLocationException ble) {
					ble.printStackTrace();
				}
			}
		}

		appendButtonsToReport(!Modeler.user.isEmpty());
		page.saveReminder.setChanged(true);

	}

}