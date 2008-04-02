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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTML;

import org.concord.modeler.ImageQuestion;
import org.concord.modeler.Modeler;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.QuestionAndAnswer;
import org.concord.modeler.SnapshotGallery;
import org.concord.modeler.util.FileUtilities;

/*
 * @author Charles Xie
 * 
 */
class MultiplePageReportFormatter extends ReportFormatter {

	MultiplePageReportFormatter(Page page) {
		super(page);
	}

	void format(Map<String, Object> map, PageNameGroup group) {

		page.createNewPage();
		createReportHeader(map);

		StyledDocument doc = page.getStyledDocument();
		try {
			doc.insertString(doc.getLength(), "\n\n", answerStyle);
		}
		catch (BadLocationException e) {
			e.printStackTrace();
		}
		String url = (String) map.remove("Page Address");
		String fn = FileUtilities.getFileName(url);
		int n = group.size();
		String name = null;
		QuestionAndAnswer qa = null;
		int imagePosition = -1;
		boolean hasImageQuestion;
		Object o;
		int k;
		List<String> used = new ArrayList<String>();

		for (int i = 0; i < n; i++) {

			name = group.getPageName(i);
			hasImageQuestion = false;

			linkStyle.removeAttribute(HTML.Attribute.HREF);
			linkStyle.removeAttribute(HTML.Attribute.TARGET);
			try {
				if (i > 0)
					doc.insertString(doc.getLength(), "\n\n\n", answerStyle);
				doc.insertString(doc.getLength(), "Page " + (i + 1) + " : ", subtitleStyle);
				if (url != null) {
					String u = url.replaceFirst(fn, name);
					linkStyle.addAttribute(HTML.Attribute.HREF, u);
					doc.insertString(doc.getLength(), u, linkStyle);
				}
				doc.insertString(doc.getLength(), "\n\n", answerStyle);
			}
			catch (BadLocationException ble) {
				ble.printStackTrace();
			}

			imagePosition = doc.getLength();
			used.clear();
			for (String key : map.keySet()) {
				if (key.startsWith(name)) {
					o = map.get(key);
					if (o instanceof SnapshotImageWrapper) {
						used.add(key);
					}
				}
			}

			k = 1;
			for (String key : map.keySet()) {
				if (key.startsWith(name)) {
					o = map.get(key);
					if (o instanceof QuestionAndAnswer) {
						qa = (QuestionAndAnswer) o;
						if (key.indexOf("ImageQuestion") != -1) {
							hasImageQuestion = true;
							key = ModelerUtilities.extractPlainText(qa.getQuestion()).replace('\n', ' ');
							try {
								doc.insertString(doc.getLength(), k + ". " + key + "\n\n", questionStyle);
							}
							catch (BadLocationException ble) {
								ble.printStackTrace();
							}
							Style style = page.addStyle(null, null);
							ImageIcon image = null;
							if (!QuestionAndAnswer.NO_ANSWER.equals(qa.getAnswer()))
								image = ImageQuestion.getImage(qa.getAnswer());
							try {
								if (image != null) {
									StyleConstants.setIcon(style, image);
									doc.insertString(doc.getLength(), " ", style);
									doc.insertString(doc.getLength(), "\n", null);
									Object comment = SnapshotGallery.sharedInstance().getProperty(
											"comment:" + image.getDescription());
									if (comment != null)
										doc.insertString(doc.getLength(), comment.toString() + "   ", null);
									doc.insertString(doc.getLength(), "("
											+ DateFormat.getTimeInstance().format(new Date(qa.getTimestamp())) + ")",
											tinyStyle);
									doc.insertString(doc.getLength(), "\n\n", null);
								}
								else {
									doc.insertString(doc.getLength(), QuestionAndAnswer.NO_ANSWER + "\n\n",
											highlightStyle);
								}
							}
							catch (BadLocationException ble) {
								ble.printStackTrace();
							}
						}
						else if (key.indexOf("PageMultipleChoice") != -1) {
							int ii = qa.getQuestion().indexOf("(a)");
							String s1 = qa.getQuestion().substring(0, ii - 1);
							String s2 = qa.getQuestion().substring(ii);
							key = ModelerUtilities.extractPlainText(s1).replace('\n', ' ');
							s1 = qa.getAnswer();
							if (s1 == null) {
								s1 = "";
							}
							else {
								StringTokenizer st = new StringTokenizer(s1);
								char[] val = new char[st.countTokens()];
								int m = 0;
								while (st.hasMoreTokens()) {
									val[m++] = (char) (Integer.valueOf(st.nextToken()).intValue() + 'a');
								}
								s1 = "";
								if (val.length == 0 || (val.length == 1 && val[0] == '`')) {
									// do nothing
								}
								else {
									for (int j = 0; j < val.length; j++)
										s1 += "(" + val[j] + ") ";
								}
							}
							try {
								doc.insertString(doc.getLength(), k + ". " + key + "\n\n", questionStyle);
								if (s1.equals("")) {
									doc.insertString(doc.getLength(), s2, answerStyle);
									doc.insertString(doc.getLength(), "NOT ANSWERED.\n\n", highlightStyle);
								}
								else {
									doc.insertString(doc.getLength(), s2 + s1 + "  ", answerStyle);
									doc.insertString(doc.getLength(), "("
											+ DateFormat.getTimeInstance().format(new Date(qa.getTimestamp())) + ")",
											tinyStyle);
									doc.insertString(doc.getLength(), "\n\n", null);
								}
							}
							catch (BadLocationException ble) {
								ble.printStackTrace();
							}
						}
						else {
							key = ModelerUtilities.extractPlainText(qa.getQuestion()).replace('\n', ' ');
							try {
								doc.insertString(doc.getLength(), k + ". " + key + "\n\n", questionStyle);
								String answer = qa.getAnswer();
								if (QuestionAndAnswer.NO_ANSWER.equals(answer)) {
									doc.insertString(doc.getLength(), answer + "\n\n", highlightStyle);
								}
								else {
									doc.insertString(doc.getLength(), answer, answerStyle);
									doc.insertString(doc.getLength(), "    ("
											+ DateFormat.getTimeInstance().format(new Date(qa.getTimestamp())) + ")",
											tinyStyle);
									doc.insertString(doc.getLength(), "\n\n", null);
								}
							}
							catch (BadLocationException ble) {
								ble.printStackTrace();
							}
						}
						k++;
						used.add(key);
					}
				}
			}
			if (!hasImageQuestion) {
				String parent = SnapshotGallery.sharedInstance().getAnnotatedImageFolder().getAbsolutePath()
						+ System.getProperty("file.separator");
				Image image = null;
				for (int j = used.size() - 1; j >= 0; j--) {
					o = map.get(used.get(j));
					if (o instanceof SnapshotImageWrapper) {
						if (j < MAX_IMAGE) {
							Style style = page.addStyle(null, null);
							StyleConstants.setIcon(style, ((SnapshotImageWrapper) o).getImage());
							try {
								Object comment = SnapshotGallery.sharedInstance().getProperty("comment:" + o);
								if (comment != null)
									doc.insertString(imagePosition, comment + "\n\n", null);
								doc.insertString(imagePosition, "\n\n", null);
								doc.insertString(imagePosition, " ", style);
								doc.insertString(imagePosition, "\n\n", null);
							}
							catch (BadLocationException ble) {
								ble.printStackTrace();
							}
						}
						else {
							Style style = page.addStyle(null, null);
							image = SnapshotGallery.sharedInstance().getThumbnail(o.toString());
							if (image != null) {
								StyleConstants.setIcon(style, new ImageIcon(image, "tn_" + o));
							}
							linkStyle.removeAttribute(HTML.Attribute.HREF);
							linkStyle.removeAttribute(HTML.Attribute.TARGET);
							String link = parent + o;
							linkStyle.addAttribute(HTML.Attribute.HREF, link);
							try {
								doc.insertString(imagePosition, "\n\n", null);
								doc.insertString(imagePosition, link, linkStyle);
								doc.insertString(imagePosition, "Location: ", answerStyle);
								Object comment = SnapshotGallery.sharedInstance().getProperty("comment:" + o);
								if (comment != null)
									doc.insertString(imagePosition, comment + "\n", null);
								if (image != null) {
									doc.insertString(imagePosition, "\n", null);
									doc.insertString(imagePosition, " ", style);
									doc.insertString(imagePosition, "\n", null);
								}
								if (j == MAX_IMAGE) {
									doc.insertString(imagePosition, "More snapshot images for this page:\n\n",
											highlightStyle);
								}
							}
							catch (BadLocationException ble) {
								ble.printStackTrace();
							}
						}
					}
				}
			}

			for (String key : used)
				map.remove(key);

		}

		appendButtonsToReport(!Modeler.user.isEmpty());
		page.saveReminder.setChanged(true);

	}

}