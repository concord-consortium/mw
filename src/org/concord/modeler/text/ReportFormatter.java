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

import java.awt.Color;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.concord.modeler.Modeler;
import org.concord.modeler.ui.IconPool;

/**
 * @author Charles Xie
 * 
 */
abstract class ReportFormatter {

	static Style questionStyle, answerStyle, titleStyle, subtitleStyle, linkStyle, highlightStyle, tinyStyle;
	final static int MAX_IMAGE = 5;

	Page page;

	ReportFormatter(Page page) {
		this.page = page;
		init();
	}

	void appendButtonsToReport(boolean submit) {

		Style style1 = null;
		String s = null;
		if (submit && !"true".equalsIgnoreCase(System.getProperty("mw.cd.mode"))) {
			style1 = page.addStyle(null, null);
			JButton button = new JButton(page.uploadReportAction);
			s = Modeler.getInternationalText("Submit");
			button.setText(s != null ? s : "Submit");
			StyleConstants.setComponent(style1, button);
		}

		Style style2 = page.addStyle(null, null);
		JButton button = new JButton(page.printAction);
		button.setIcon(IconPool.getIcon("printer"));
		s = Modeler.getInternationalText("Print");
		if (s != null)
			button.setText(s);
		StyleConstants.setComponent(style2, button);

		Style style3 = page.addStyle(null, null);
		button = new JButton(page.saveAsAction);
		button.setIcon(IconPool.getIcon("save"));
		s = Modeler.getInternationalText("SaveButton");
		button.setText(s != null ? s : "Save");
		StyleConstants.setComponent(style3, button);

		StyledDocument doc = page.getStyledDocument();

		try {
			doc.insertString(doc.getLength(), "\n\n", null);
			if (submit && !"true".equalsIgnoreCase(System.getProperty("mw.cd.mode"))) {
				doc.insertString(doc.getLength(), " ", style1);
				doc.insertString(doc.getLength(), " ", null);
			}
			doc.insertString(doc.getLength(), " ", style2);
			doc.insertString(doc.getLength(), " ", null);
			doc.insertString(doc.getLength(), " ", style3);
			doc.insertString(doc.getLength(), "\n\n", null);
		}
		catch (BadLocationException ble) {
			ble.printStackTrace();
		}

		SimpleAttributeSet sas = new SimpleAttributeSet();
		StyleConstants.setLeftIndent(sas, 8);
		StyleConstants.setRightIndent(sas, 8);
		doc.setParagraphAttributes(0, doc.getLength(), sas, false);

	}

	void createReportHeader(Map<String, Object> map) {
		StyledDocument doc = page.getStyledDocument();
		try {

			Object o = map.get("Page Title");
			String s = Modeler.getInternationalText("MyReport");
			if (s == null)
				s = "My report on";
			if (o != null) {
				doc.insertString(doc.getLength(), s + " \"" + o + "\"\n\n", titleStyle);
				page.setTitle(s + " \"" + o.toString() + "\"");
			}
			else {
				doc.insertString(doc.getLength(), s + "\n\n", titleStyle);
				page.setTitle(s);
			}
			map.remove("Page Title");

			s = Modeler.getInternationalText("StudentName");
			doc.insertString(doc.getLength(), (s != null ? s : "Student name") + ": ", answerStyle);
			String fullName = null;
			String school = null;
			if (Modeler.user != null && !Modeler.user.isEmpty()) {
				fullName = Modeler.user.getFullName();
				school = Modeler.user.getInstitution();
			}
			if (fullName == null)
				fullName = System.getProperty("student name");
			doc.insertString(doc.getLength(), (fullName == null || fullName.trim().equals("") ? "(Your name)"
					: fullName)
					+ "\n", highlightStyle);

			fullName = null;
			s = Modeler.getInternationalText("TeacherName");
			doc.insertString(doc.getLength(), (s != null ? s : "Teacher name") + ": ", answerStyle);
			if (Modeler.user.getTeacher() != null)
				fullName = Modeler.user.getTeacher();
			if (fullName == null)
				fullName = System.getProperty("teacher name");
			doc.insertString(doc.getLength(),
					fullName == null || fullName.trim().equals("") ? "(Your teacher's name)\n" : fullName + "\n",
					highlightStyle);

			if (school == null)
				school = System.getProperty("school name");
			s = Modeler.getInternationalText("School");
			doc.insertString(doc.getLength(), (s != null ? s : "School") + ": ", answerStyle);
			doc.insertString(doc.getLength(), school == null || school.trim().equals("") ? "(Your school)\n" : school
					+ "\n", highlightStyle);

			s = Modeler.getInternationalText("Date");
			doc.insertString(doc.getLength(), (s != null ? s : "Submission Time") + ": "
					+ DateFormat.getInstance().format(new Date()) + "\n", answerStyle);

		}
		catch (BadLocationException ble) {
			ble.printStackTrace();
		}
		appendButtonsToReport(!Modeler.user.isEmpty());

	}

	// initialize the text styles for the report page
	private void init() {
		if (titleStyle == null) {
			titleStyle = page.addStyle(null, null);
			StyleConstants.setBold(titleStyle, true);
			StyleConstants.setFontSize(titleStyle, Page.getDefaultFontSize() + 10);
			StyleConstants.setFontFamily(titleStyle, Page.getDefaultFontFamily());
		}
		if (subtitleStyle == null) {
			subtitleStyle = page.addStyle(null, null);
			StyleConstants.setBold(subtitleStyle, true);
			StyleConstants.setFontSize(subtitleStyle, Page.getDefaultFontSize() + 5);
			StyleConstants.setFontFamily(subtitleStyle, Page.getDefaultFontFamily());
		}
		if (questionStyle == null) {
			questionStyle = page.addStyle(null, null);
			StyleConstants.setItalic(questionStyle, true);
			StyleConstants.setFontSize(questionStyle, Page.getDefaultFontSize() + 1);
			StyleConstants.setFontFamily(questionStyle, Page.getDefaultFontFamily());
			StyleConstants.setBold(questionStyle, true);
		}
		if (answerStyle == null) {
			answerStyle = page.addStyle(null, null);
			StyleConstants.setFontSize(answerStyle, Page.getDefaultFontSize());
			StyleConstants.setFontFamily(answerStyle, Page.getDefaultFontFamily());
		}
		if (highlightStyle == null) {
			highlightStyle = page.addStyle(null, null);
			StyleConstants.setFontSize(highlightStyle, Page.getDefaultFontSize());
			StyleConstants.setForeground(highlightStyle, Color.red);
			StyleConstants.setBold(highlightStyle, true);
			StyleConstants.setFontFamily(highlightStyle, Page.getDefaultFontFamily());
		}
		if (linkStyle == null) {
			linkStyle = page.addStyle(null, answerStyle);
			StyleConstants.setForeground(linkStyle, Page.getLinkColor());
			StyleConstants.setUnderline(linkStyle, true);
		}
		if (tinyStyle == null) {
			tinyStyle = page.addStyle(null, null);
			StyleConstants.setFontSize(tinyStyle, Page.getDefaultFontSize() - 4);
			StyleConstants.setFontFamily(tinyStyle, Page.getDefaultFontFamily());
			StyleConstants.setForeground(tinyStyle, Color.gray);
		}
	}

}