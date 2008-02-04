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

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;

/**
 * replace illegal characters in the text by entity references. Right now it legalizes only the five entity references
 * used by the XML syntax itself. No Unicode support is provided.
 * 
 * <pre>
 *  &lt; &lt; less than
 *  &gt; &gt; greater than
 *  &amp; &amp; ampersand 
 *  &amp;apos ' apostrophe 
 *  &quot; &quot; quotation mark
 * </pre>
 */

public final class XMLCharacterEncoder {

	private final static char LESS_THAN = '<';
	private final static char GREATER_THAN = '>';
	private final static char AMPERSAND = '&';
	private final static char APOSTROPHE = '\'';
	private final static char QUOTATION = '"';
	private final static String LESS_THAN_ER = "&lt;";
	private final static String GREATER_THAN_ER = "&gt;";
	private final static String AMPERSAND_ER = "&amp;";
	private final static String APOSTROPHE_ER = "&apos;";
	private final static String QUOTATION_ER = "&quot;";

	private static LinkedHashMap<Integer, Character> store;
	private static String characterEncoding;

	private XMLCharacterEncoder() {
	}

	static void setCharacterEncoding(String s) {
		characterEncoding = s;
	}

	public static String encode(String text) {

		if (text == null)
			return null;

		if (store == null) {
			store = new LinkedHashMap<Integer, Character>();
		}
		else {
			store.clear();
		}

		for (int i = 0; i < text.length(); i++) {
			switch (text.charAt(i)) {
			case LESS_THAN:
				store.put(i, LESS_THAN);
				break;
			case GREATER_THAN:
				store.put(i, GREATER_THAN);
				break;
			case AMPERSAND:
				store.put(i, AMPERSAND);
				break;
			case APOSTROPHE:
				store.put(i, APOSTROPHE);
				break;
			case QUOTATION:
				store.put(i, QUOTATION);
				break;
			}
		}

		if (!store.isEmpty()) {
			StringBuffer sb = new StringBuffer(text);
			int cumu = 0, del = 0;
			for (Integer index : store.keySet()) {
				Character character = (Character) store.get(index);
				switch (character.charValue()) {
				case LESS_THAN:
					del = index.intValue() + cumu;
					sb.deleteCharAt(del);
					sb.insert(del, LESS_THAN_ER);
					cumu += 3;
					break;
				case GREATER_THAN:
					del = index.intValue() + cumu;
					sb.deleteCharAt(del);
					sb.insert(del, GREATER_THAN_ER);
					cumu += 3;
					break;
				case AMPERSAND:
					del = index.intValue() + cumu;
					sb.deleteCharAt(del);
					sb.insert(del, AMPERSAND_ER);
					cumu += 4;
					break;
				case APOSTROPHE:
					del = index.intValue() + cumu;
					sb.deleteCharAt(del);
					sb.insert(del, APOSTROPHE_ER);
					cumu += 5;
					break;
				case QUOTATION:
					del = index.intValue() + cumu;
					sb.deleteCharAt(del);
					sb.insert(del, QUOTATION_ER);
					cumu += 5;
					break;
				}
			}
			text = sb.toString();
		}

		if (characterEncoding != null) {
			if ("UTF-8".equals(characterEncoding)) { // encoded in unicode
				char[] c = text.toCharArray();
				StringBuffer buffer = new StringBuffer();
				int n;
				for (int i = 0; i < c.length; i++) {
					n = c[i];
					if (n > 0x007f) {
						buffer.append("&#");
						buffer.append((int) c[i]);
						buffer.append(";");
					}
					else {
						buffer.append(c[i]);
					}
				}
				text = buffer.toString();
			}
			else if (!"ISO-8859-1".equals(characterEncoding)) { // encoded in a non-western character set
				try {
					text = new String(text.getBytes(characterEncoding));
				}
				catch (UnsupportedEncodingException e) {
					e.printStackTrace(System.err);
				}
			}
		}

		return text;

	}

}