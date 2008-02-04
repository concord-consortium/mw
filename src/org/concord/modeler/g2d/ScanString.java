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

package org.concord.modeler.g2d;

import java.util.Hashtable;

/**
 * This class is similar to the ScanWord class, except it scans a string for keywords rather than an input stream.
 * 
 * @author Leigh Brookshaw, modified by Qian Xie
 */

public class ScanString extends Object {

	/** flag an unknown token */
	public final static int UNKNOWN = -256;

	/** Flag an error */
	public final static int ERROR = -257;

	/** Flag a Number */
	public final static int NUMBER = -258;

	/** flag the End of String */
	public final static int EOS = -259;

	private String string;

	private int position;
	private int kwlength;

	public String sval;
	public double nval;

	/* The hash table containing the keyword/value pairs. */
	private Hashtable<String, Integer> kwords = new Hashtable<String, Integer>();

	public ScanString() {
		string = null;
		position = 0;
		kwlength = 0;
		sval = null;
		nval = 0.0;
	}

	/**
	 * @param s
	 *            String to scan for tokens
	 */
	public ScanString(String s) {
		setString(s);
	}

	/**
	 * Set the string to be scanned
	 * 
	 * @param s
	 *            String
	 */
	public void setString(String s) {
		if (s == null)
			return;
		string = new String(s);
	}

	/**
	 * Add a keyword/token pair to the table of keywords to scan for.
	 * 
	 * @param s
	 *            keyword string to scan for
	 * @param i
	 *            token to return when the keyword is found
	 */
	public void addKeyWord(String s, int i) {
		if (s == null)
			return;
		if (kwlength < s.length())
			kwlength = s.length();
		kwords.put(s.toLowerCase(), i);
	}

	/**
	 * @param s
	 *            keyword string
	 * @return the token corresponding to the keyword
	 */
	public int getKeyValue(String s) {

		if (s == null)
			return UNKNOWN;
		if (!kwords.containsKey(s.toLowerCase()))
			return UNKNOWN;

		Integer i = kwords.get(s.toLowerCase());

		if (i == null)
			return UNKNOWN;
		return i.intValue();

	}

	/** clear the table containing the keyword/token pairs */
	public void resetKeyWords() {
		kwords.clear();
		kwlength = 0;
	}

	/**
	 * process the string and return the next token found.
	 * 
	 * @return token found
	 */
	public int nextWord() {
		int i;
		char c;
		int word;
		int count = 0;
		char buffer[] = new char[string.length()];
		boolean exponent = false;
		boolean point = false;

		if (position >= string.length())
			return EOS;

		c = string.charAt(position);

		/*
		 * * Remove white space
		 */

		while (c == 32 || c == 9 || c == 10 || c == 11 || c == 13) {
			position++;
			if (position >= string.length())
				return EOS;
			c = string.charAt(position);
		}

		/*
		 * * Is this the start of a number ?
		 */

		if ((c >= '0' && c <= '9') || c == '.') {

			for (i = position; i < string.length(); i++) {
				c = string.charAt(i);

				if (exponent && (c < '0' || c > '9'))
					break;

				if (c == 'E' || c == 'e' || c == 'D' || c == 'd') {
					exponent = true;
					buffer[count++] = 'e';

					c = string.charAt(i + 1);
					if (c == '-' || c == '+') {
						buffer[count++] = c;
						i++;
					}
				}
				else {
					if (point && c == '.') {
						break;
					}
					if (c == '.') {
						point = true;
						buffer[count++] = c;
					}
					else {
						if (c < '0' || c > '9') {
							break;
						}
						buffer[count++] = c;
					}
				}
			}

			try {
				sval = new String(buffer, 0, count);
				nval = Double.valueOf(sval).doubleValue();
				position += count;
				return NUMBER;
			}
			catch (Exception e) {
				return ERROR;
			}

		}

		/*
		 * * Scan for a keyword
		 */

		int last = UNKNOWN;
		int nchar = 0;
		int pos = position;

		while (pos < string.length()) {

			buffer[count++] = string.charAt(pos++);

			word = getKeyValue(new String(buffer, 0, count));

			if (word != UNKNOWN) {
				last = word;
				nchar = count;
			}
			else {
				if (nchar == 0 && count >= kwlength) {
					return ERROR;
				}
				if (count >= kwlength) {
					sval = new String(buffer, 0, nchar);
					position += nchar;
					return last;
				}
			}
		}

		if (nchar != 0) {
			sval = new String(buffer, 0, nchar);
			position += nchar;
			return last;
		}

		return ERROR;

	}

}
