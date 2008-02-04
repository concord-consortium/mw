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

package org.concord.modeler.util;

/**
 * Search for a string in a text (represented by a longer string). This class does the search only. It is your
 * responsibility to mark the findings in the searched context, usually rendered. by a <code>JTextComponent</code>.
 * 
 * @author Charles Xie
 */

public class StringFinder {

	private static final char[] WORD_SEPARATORS = { ' ', '\t', '\n', '\r', '\f', '.', ',', ':', '-', '(', ')', '[',
			']', '{', '}', '<', '>', '/', '|', '\\', '\'', '\"' };

	public final static int STRING_ERROR = 0;
	public final static int STRING_NOT_FOUND = 1;
	public final static int STRING_FOUND = 2;
	public final static int SEARCH_ENDED = 3;
	public final static int SEARCH_ERROR = 4;

	private String text;
	private boolean searchUp;
	private boolean matchWholeWord;
	private boolean matchCase;
	private String subText;
	private String key;
	private int position;
	private int selectionStart;
	private int selectionEnd;

	private static boolean isSeparator(char ch) {
		for (char k : WORD_SEPARATORS)
			if (ch == k)
				return true;
		return false;
	}

	public StringFinder(String text) {
		this.text = text;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public void setSelectionStart(int i) {
		selectionStart = i;
	}

	public int getSelectionStart() {
		return selectionStart;
	}

	public void setSelectionEnd(int i) {
		selectionEnd = i;
	}

	public int getSelectionEnd() {
		return selectionEnd;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getPosition() {
		return position;
	}

	/** select the interval specified by i and j, and set the position to be j */
	public void select(int i, int j) {
		setSelectionStart(i);
		setSelectionEnd(j);
		setPosition(j);
	}

	public void setSearchUp(boolean b) {
		searchUp = b;
	}

	public boolean isSearchUp() {
		return searchUp;
	}

	public void setMatchCase(boolean b) {
		matchCase = b;
	}

	public boolean getMatchCase() {
		return matchCase;
	}

	public void setMatchWholeWord(boolean b) {
		matchWholeWord = b;
	}

	public boolean getMatchWholeWord() {
		return matchWholeWord;
	}

	protected String getSelectedText() {
		if (text == null)
			return null;
		if (selectionStart == selectionEnd)
			return null;
		return text.substring(selectionStart, selectionEnd);
	}

	/** find the next result */
	public int findNext() {

		if (key == null || text == null)
			return STRING_ERROR;

		/*
		 * if there is any text selected, check if it is the same to key. If not, clear the current selection and move
		 * the position to the starting position of the initally selected text.
		 */
		String selectedText = getSelectedText();
		if (selectedText != null) {
			if (matchCase ? !selectedText.equals(key) : !selectedText.equalsIgnoreCase(key)) {
				select(selectionStart, selectionStart);
			}
		}

		int begin;

		try {
			if (searchUp) {
				if (position < 0)
					position = 0;
				subText = text.substring(0, position);
			}
			else {
				if (position > text.length())
					position = text.length();
				subText = text.substring(position, text.length());
			}
		}
		catch (IndexOutOfBoundsException e) {
			e.printStackTrace(System.err);
			return SEARCH_ERROR;
		}

		if (!matchCase) {
			subText = subText.toLowerCase();
			key = key.toLowerCase();
		}

		if (searchUp) {
			begin = subText.lastIndexOf(key, position);
		}
		else {
			begin = subText.indexOf(key);
		}
		if (begin < 0)
			return SEARCH_ENDED;

		if (matchWholeWord) {

			int end = begin + key.length();
			boolean s1 = begin > 0;
			boolean b1 = s1 && !isSeparator(subText.charAt(begin - 1));
			boolean s2 = end < subText.length();
			boolean b2 = s2 && !isSeparator(subText.charAt(end));

			if (b1 || b2) { // Not a whole word
				if (searchUp && s1) { // Can continue up
					setPosition(begin);
					findNext();
					return STRING_NOT_FOUND;
				}
				if (!searchUp && s2) { // Can continue down
					setPosition(position + end);
					findNext();
					return STRING_NOT_FOUND;
				}
			}

		}

		if (searchUp) {
			select(begin, begin + key.length());
		}
		else {
			begin += position;
			select(begin, begin + key.length());
		}

		return STRING_FOUND;

	}

}