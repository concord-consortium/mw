/*
 *   Copyright (C) 2007  The Concord Consortium, Inc.,
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
package org.concord.modeler;

/**
 * @author Charles Xie
 * 
 */

import java.util.BitSet;

import org.concord.modeler.text.XMLCharacterDecoder;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Charles Xie
 * 
 */
class RecordHandler extends DefaultHandler {

	private String attribName;
	private String attribValue;
	private String str;
	private StringBuffer buffer;

	private String url;
	private int id;
	private String question;
	private String answer;
	private String referenceAnswer;

	// for multiple choices
	private BitSet correctBitSet;
	private BitSet selectedBitSet;
	private String[] choices;
	private int iChoice;

	// for image questions
	private String fileName;
	private String description;

	RecordHandler() {
		buffer = new StringBuffer();
		correctBitSet = new BitSet(10);
		selectedBitSet = new BitSet(10);
		choices = new String[10];
	}

	public void startDocument() {
	}

	public void endDocument() {
	}

	public void startElement(String uri, String localName, String qName, Attributes attrib) {

		buffer.setLength(0);

		if (qName.equals("choice")) {
			if (attrib != null) {
				for (int i = 0, n = attrib.getLength(); i < n; i++) {
					attribName = attrib.getQName(i);
					attribValue = attrib.getValue(i);
					if ("correct".equals(attribName)) {
						if ("true".equals(attribValue)) {
							correctBitSet.set(iChoice);
						}
					}
					else if ("selected".equals(attribName)) {
						if ("true".equals(attribValue)) {
							selectedBitSet.set(iChoice);
						}
					}
				}
			}
		}

	}

	public void endElement(String uri, String localName, String qName) {

		str = buffer.toString();

		if (qName.equals("url")) {
			url = str;
		}

		else if (qName.equals("id")) {
			if (str != null && !str.trim().equals("")) {
				Integer i = parseInt(str);
				if (i != null)
					id = i.intValue();
			}
		}

		else if (qName.equals("question")) {
			question = XMLCharacterDecoder.decode(str);
		}

		else if (qName.equals("choice")) {
			choices[iChoice] = str;
			iChoice++;
		}

		else if (qName.equals("answer")) {
			answer = str;
		}

		else if (qName.equals("referenceanswer")) {
			referenceAnswer = str;
		}

		else if (qName.equals("filename")) {
			fileName = str;
		}

		else if (qName.equals("description")) {
			description = str;
		}

		else if (qName.equals("multiplechoice")) {
			if (question != null) {
				String key = url + "#" + ModelerUtilities.getSortableString(id, 3) + "%"
						+ PageMultipleChoice.class.getName();
				String selection = formatSelection();
				QuestionAndAnswer q = new QuestionAndAnswer(question + '\n' + formatChoices() + "\nMy answer is ",
						selection == null ? "-1" : selection);
				q.setReferenceAnswer(formatCorrectAnswers());
				UserData.sharedInstance().putData(key, q);
			}
			question = null;
			iChoice = 0;
			correctBitSet.clear();
			selectedBitSet.clear();
		}

		// how do we know this is a text area or text field? since we don't know, we do both
		else if (qName.equals("qanda")) {
			if (question != null) {
				QuestionAndAnswer q = new QuestionAndAnswer(question, answer);
				q.setReferenceAnswer(referenceAnswer);
				String key = url + "#" + ModelerUtilities.getSortableString(id, 3) + "%" + PageTextArea.class.getName();
				UserData.sharedInstance().putData(key, q);
				key = url + "#" + ModelerUtilities.getSortableString(id, 3) + "%" + PageTextField.class.getName();
				UserData.sharedInstance().putData(key, q);
			}
			question = null;
			answer = null;
		}

		else if (qName.equals("imagequestion")) {
			if (question != null) {
				String key = url + "#" + ModelerUtilities.getSortableString(id, 3) + "%"
						+ ImageQuestion.class.getName();
				QuestionAndAnswer q = new QuestionAndAnswer(question, fileName);
				q.setReferenceAnswer(description);
				UserData.sharedInstance().putData(key, q);
			}
			question = null;
			fileName = null;
			description = null;
		}

	}

	public void characters(char[] ch, int start, int length) {
		str = new String(ch, start, length);
		buffer.append(str); // used to harvest enclosed HTML data
	}

	public void warning(SAXParseException e) {
		e.printStackTrace();
	}

	public void error(SAXParseException e) {
		e.printStackTrace();
	}

	public void fatalError(SAXParseException e) {
		e.printStackTrace();
	}

	private static Integer parseInt(String s) {
		Integer i = null;
		try {
			i = Integer.valueOf(s);
		}
		catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			i = null;
		}
		return i;
	}

	private String formatChoices() {
		String s = "";
		char c = 'a';
		for (int i = 0; i < iChoice; i++) {
			s += "(" + c + ") " + choices[i] + '\n';
			c++;
		}
		return s;
	}

	private String formatCorrectAnswers() {
		String s = "";
		for (int i = 0; i < iChoice; i++) {
			if (correctBitSet.get(i)) {
				s += i + " ";
			}
		}
		return s.trim();
	}

	private String formatSelection() {
		String s = null;
		for (int i = 0; i < iChoice; i++) {
			if (selectedBitSet.get(i)) {
				if (s == null) {
					s = i + "";
				}
				else {
					s += " " + i;
				}
			}
		}
		return s;
	}

}