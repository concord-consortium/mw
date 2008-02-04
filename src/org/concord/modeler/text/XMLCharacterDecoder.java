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

/**
 * restores entity references to characters. Right now it does only the five entity references used by the XML syntax
 * itself. No Unicode support is provided.
 * 
 * <pre>
 *  &lt; &lt; less than
 *  &gt; &gt; greater than
 *  &amp; &amp; ampersand 
 *  &amp;apos ' apostrophe 
 *  &quot; &quot; quotation mark
 * </pre>
 */

public final class XMLCharacterDecoder {

	private final static String LESS_THAN = "<";
	private final static String GREATER_THAN = ">";
	private final static String AMPERSAND = "&";
	private final static String APOSTROPHE = "\'";
	private final static String QUOTATION = "\"";
	private final static String LESS_THAN_ER = "&lt;";
	private final static String GREATER_THAN_ER = "&gt;";
	private final static String AMPERSAND_ER = "&amp;";
	private final static String APOSTROPHE_ER = "&apos;";
	private final static String QUOTATION_ER = "&quot;";

	private XMLCharacterDecoder() {
	}

	public static String decode(String text) {

		if (text == null)
			return null;

		return text.replaceAll(LESS_THAN_ER, LESS_THAN).replaceAll(GREATER_THAN_ER, GREATER_THAN).replaceAll(
				AMPERSAND_ER, AMPERSAND).replaceAll(APOSTROPHE_ER, APOSTROPHE).replaceAll(QUOTATION_ER, QUOTATION);

	}

}