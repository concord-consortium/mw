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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

class HTMLConverter {

	private final static String LINE_BREAK = "" + '\n';
	// private final static String PARAGRAPH_BREAK = LINE_BREAK + '\n';

	private static HTMLConverter converter = new HTMLConverter();

	private HTMLEditorKit editorKit;
	private int caretPos;
	private Color globalTextColor = Color.black;
	private boolean isGlobalBold;
	private boolean isGlobalItalic;
	private boolean isGlobalUnderline;

	public static void insertHTMLFile(StyledDocument doc, int caretPosition, File file) {
		converter.caretPos = caretPosition;
		converter.insert(doc, file);
	}

	private HTMLConverter() {
	}

	private void resetStyles(Style style) {
		StyleConstants.setForeground(style, globalTextColor);
		StyleConstants.setBold(style, isGlobalBold);
		StyleConstants.setItalic(style, isGlobalItalic);
		StyleConstants.setFontSize(style, 12);
		StyleConstants.setUnderline(style, isGlobalUnderline);
		style.removeAttribute(HTML.Attribute.HREF);
	}

	private int insertLineBreak(Document doc, int pos) {
		try {
			doc.insertString(pos, LINE_BREAK, null);
			pos += LINE_BREAK.length();
		}
		catch (BadLocationException ble) {
			ble.printStackTrace(System.err);
		}
		return pos;
	}

	/*
	 * private int insertParagraphBreak(Document doc, int pos) { try { doc.insertString(pos, PARAGRAPH_BREAK, null); pos +=
	 * PARAGRAPH_BREAK.length(); } catch (BadLocationException ble) { ble.printStackTrace(System.err); } return pos; }
	 */

	private void insert(final StyledDocument doc, final File file) {

		if (editorKit == null)
			editorKit = new HTMLEditorKit();
		HTMLDocument htmlDoc = (HTMLDocument) editorKit.createDefaultDocument();
		StyleSheet styleSheet = htmlDoc.getStyleSheet();
		try {
			editorKit.read(new FileReader(file), htmlDoc, 0);
		}
		catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace(System.err);
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.err);
		}
		catch (BadLocationException ble) {
			ble.printStackTrace(System.err);
		}

		Style style = doc.addStyle("cml", null);
		AttributeSet as = null;
		Element elem = null;
		String elemName = null;
		String text = null;
		int startOffset = 0, endOffset = 0;
		Enumeration enum1 = null;
		Object name = null, attr = null;

		ElementIterator it = new ElementIterator(htmlDoc.getDefaultRootElement());
		while (it.next() != null) {

			elem = it.current();

			if (elem instanceof HTMLDocument.BlockElement) {

				as = elem.getAttributes();
				elemName = elem.getName();

				if (elemName.equalsIgnoreCase(HTML.Tag.BODY.toString())) {
					enum1 = as.getAttributeNames();
					while (enum1.hasMoreElements()) {
						name = enum1.nextElement();
						attr = as.getAttribute(name);
						if (name == HTML.Attribute.TEXT) {
							globalTextColor = styleSheet.stringToColor(attr.toString());
							StyleConstants.setForeground(style, globalTextColor);
						}
					}
				}
				else if (elemName.equalsIgnoreCase(HTML.Tag.P.toString())) {
					caretPos = insertLineBreak(doc, caretPos);
				}

				else if (elemName.equalsIgnoreCase(HTML.Tag.H1.toString())) {
					StyleConstants.setFontSize(style, 22);
					StyleConstants.setBold(style, true);
				}

				else if (elemName.equalsIgnoreCase(HTML.Tag.H2.toString())) {
					StyleConstants.setFontSize(style, 20);
					StyleConstants.setBold(style, true);
				}

				else if (elemName.equalsIgnoreCase(HTML.Tag.H3.toString())) {
					StyleConstants.setFontSize(style, 18);
					StyleConstants.setBold(style, true);
				}

				else if (elemName.equalsIgnoreCase(HTML.Tag.H4.toString())) {
					StyleConstants.setFontSize(style, 16);
					StyleConstants.setBold(style, true);
				}

				else if (elemName.equalsIgnoreCase(HTML.Tag.H5.toString())) {
					StyleConstants.setFontSize(style, 14);
					StyleConstants.setBold(style, true);
				}

				else if (elemName.equalsIgnoreCase(HTML.Tag.H6.toString())) {
					StyleConstants.setFontSize(style, 12);
					StyleConstants.setBold(style, true);
				}

				else if (elemName.equalsIgnoreCase(HTML.Tag.IMPLIED.toString())) {
					// insertLineBreak(doc, caretPos);
				}

			}

			else if (elem instanceof HTMLDocument.RunElement) {

				elemName = elem.getName();

				if (elemName.equalsIgnoreCase(HTML.Tag.BR.toString())) {
					caretPos = insertLineBreak(doc, caretPos);
				}

				else if (elemName.equalsIgnoreCase(HTML.Tag.CONTENT.toString())) {

					try {
						startOffset = elem.getStartOffset();
						endOffset = elem.getEndOffset();
						text = htmlDoc.getText(startOffset, endOffset - startOffset);
					}
					catch (BadLocationException ble) {
						ble.printStackTrace(System.err);
					}

					if (endOffset - startOffset == 1 && text != null && text.charAt(0) == '\n') {
						/*
						 * this is a line break spontaneously added to the end of a H tag, or before a P tag. There is
						 * usually no need to apply a style on it.
						 */
					}
					else {
						as = elem.getAttributes();
						enum1 = as.getAttributeNames();
						while (enum1.hasMoreElements()) {
							name = enum1.nextElement();
							attr = as.getAttribute(name);
							if (name == CSS.Attribute.COLOR) {
								StyleConstants.setForeground(style, styleSheet.stringToColor(attr.toString()));
							}
							else if (name == CSS.Attribute.FONT_WEIGHT) {
								if (attr.toString().equalsIgnoreCase("bold")) {
									StyleConstants.setBold(style, true);
								}
							}
							else if (name == CSS.Attribute.FONT_STYLE) {
								if (attr.toString().equalsIgnoreCase("italic")) {
									StyleConstants.setItalic(style, true);
								}
							}
							else if (name == CSS.Attribute.FONT_FAMILY) {
								StyleConstants.setFontFamily(style, attr.toString());
							}
							else if (name == CSS.Attribute.FONT_SIZE) {
								float n = 2.0f;
								try {
									n += styleSheet.getPointSize(Integer.parseInt(attr.toString()));
								}
								catch (NumberFormatException nfe) {
									if (attr.toString().startsWith("+") || attr.toString().startsWith("-")) {
										n += styleSheet.getPointSize(attr.toString());
									}
									else {
										n = 12;
									}
								}
								StyleConstants.setFontSize(style, (int) n);
							}
							else if (name == CSS.Attribute.TEXT_DECORATION) {
								if (attr.toString().equalsIgnoreCase("underline")) {
									StyleConstants.setUnderline(style, true);
								}
							}
							else if (name == HTML.Tag.A) {
								AttributeSet href = (AttributeSet) attr;
								Enumeration e = href.getAttributeNames();
								Object a = null, b = null;
								while (e.hasMoreElements()) {
									a = e.nextElement();
									if (a == HTML.Attribute.HREF) {
										b = href.getAttribute(a);
										style.addAttribute(HTML.Attribute.HREF, b.toString());
										StyleConstants.setUnderline(style, true);
										StyleConstants.setForeground(style, Color.blue);
									}
									else if (a == HTML.Attribute.NAME) {
										System.out.println("HTML anchor not supported yet");
									}
								}
							}
						}

					}

					try {
						doc.insertString(caretPos, text, style);
						caretPos += endOffset - startOffset;
					}
					catch (BadLocationException ble) {
						ble.printStackTrace(System.err);
					}
					resetStyles(style);

				}

			}

		}

	}

}