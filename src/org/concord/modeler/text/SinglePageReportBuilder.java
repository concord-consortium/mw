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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.ImageIcon;
import javax.swing.text.JTextComponent;

import org.concord.modeler.ImageQuestion;
import org.concord.modeler.ModelerUtilities;
import org.concord.modeler.PageMultipleChoice;
import org.concord.modeler.PageTextArea;
import org.concord.modeler.PageTextField;
import org.concord.modeler.SnapshotGallery;
import org.concord.modeler.util.FileUtilities;

/**
 * @author Charles Xie
 * 
 */
class SinglePageReportBuilder {

	static ImageIcon questionmarkImage;

	private Page page;
	private Map<String, Object> map;

	SinglePageReportBuilder(Page page) {
		this.page = page;
		map = new LinkedHashMap<String, Object>();
		if (questionmarkImage == null)
			questionmarkImage = new ImageIcon(SinglePageReportBuilder.class.getResource("images/Questionmark.gif"));
	}

	Map<String, Object> prepare() {

		map.clear();
		map.put("Page Title", page.getTitle());
		map.put("Page Address", page.getAddress());

		String[] snapshot = SnapshotGallery.sharedInstance().getImageNames();
		if (snapshot != null && snapshot.length > 0) {
			String name = FileUtilities.removeSuffix(FileUtilities.getFileName(page.getAddress()));
			for (int k = 0; k < snapshot.length; k++) {
				if (snapshot[k].startsWith(name + "_"))
					map.put("image" + k, new SnapshotImageWrapper(snapshot[k]));
			}
		}

		List list = page.getComponentsOfGroup("Question");
		if (list != null && !list.isEmpty()) {
			int k = 1;
			for (Object o : list) {
				if (o instanceof PageTextField) {
					String t = ((PageTextField) o).getTitle();
					if (t.toLowerCase().indexOf("<html>") != -1) {
						map.put(k++ + ". " + ModelerUtilities.extractPlainText(t), ((PageTextField) o).getText());
					}
					else {
						map.put(k++ + ". " + t, ((PageTextField) o).getText());
					}
				}
				else if (o instanceof PageTextArea) {
					String t = ((PageTextArea) o).getTitle();
					if (t.toLowerCase().indexOf("<html>") != -1) {
						map.put(k++ + ". " + ModelerUtilities.extractPlainText(t), ((PageTextArea) o).getText());
					}
					else {
						map.put(k++ + ". " + t, ((PageTextArea) o).getText());
					}
				}
				else if (o instanceof PageMultipleChoice) {
					String s1 = ((PageMultipleChoice) o).getQuestion();
					if (s1.toLowerCase().indexOf("<html>") != -1) {
						s1 = ModelerUtilities.extractPlainText(s1);
					}
					String s2 = ((PageMultipleChoice) o).getUserSelection();
					if (s2 == null) {
						s2 = "";
					}
					else {
						StringTokenizer st = new StringTokenizer(s2);
						char[] val = new char[st.countTokens()];
						int m = 0;
						while (st.hasMoreTokens()) {
							val[m++] = (char) (Integer.valueOf(st.nextToken()).intValue() + 'a');
						}
						s2 = "";
						if (val.length == 0 || (val.length == 1 && val[0] == '`')) {
							// do nothing
						}
						else {
							for (int i = 0; i < val.length; i++)
								s2 += "(" + val[i] + ") ";
						}
					}
					map.put(k++ + ". " + s1, ((PageMultipleChoice) o).formatChoices()
							+ (s2.equals("") ? "\nNOT ANSWERED." : "\nMy answer is " + s2));
				}
				else if (o instanceof ImageQuestion) {
					String t = ((ImageQuestion) o).getQuestion();
					ImageIcon image = ((ImageQuestion) o).getImage();
					if (image == null)
						image = questionmarkImage;
					if (t.toLowerCase().indexOf("<html>") != -1) {
						map.put(k++ + ". " + ModelerUtilities.extractPlainText(t), image);
					}
					else {
						map.put(k++ + ". " + t, image);
					}
				}
				else if (o instanceof JTextComponent) {
					String t = (String) ((JTextComponent) o).getDocument().getProperty("question");
					map.put(k++ + ". " + t, ((JTextComponent) o).getText());
				}
			}
		}

		return map;

	}

}