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

package org.concord.mw3d.models;

public abstract class ColumnDataParser {

	static float[] decimalScale = { 0.1f, 0.01f, 0.001f, 0.0001f, 0.00001f, 0.000001f, 0.0000001f, 0.00000001f };
	static float[] tensScale = { 10, 100, 1000, 10000, 100000, 1000000 };

	protected int ichNextParse;

	protected float parseFloat(String str) {
		return parseFloatChecked(str, 0, str.length());
	}

	protected float parseFloat(String str, int ich) {
		int cch = str.length();
		if (ich >= cch)
			return 0;
		return parseFloatChecked(str, ich, cch);
	}

	float parseFloat(String str, int ichStart, int ichMax) {
		int cch = str.length();
		if (ichMax > cch)
			ichMax = cch;
		if (ichStart >= ichMax)
			return 0;
		return parseFloatChecked(str, ichStart, ichMax);
	}

	float parseFloatChecked(String str, int ichStart, int ichMax) {
		boolean digitSeen = false;
		float value = 0;
		int ich = ichStart;
		char ch;
		while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
			++ich;
		boolean negative = false;
		if (ich < ichMax && str.charAt(ich) == '-') {
			++ich;
			negative = true;
		}
		ch = 0;
		while (ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
			value = value * 10 + (ch - '0');
			++ich;
			digitSeen = true;
		}
		if (ch == '.') {
			int iscale = 0;
			while (++ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
				if (iscale < decimalScale.length)
					value += (ch - '0') * decimalScale[iscale];
				++iscale;
				digitSeen = true;
			}
		}
		if (!digitSeen)
			value = Float.NaN;
		else if (negative)
			value = -value;
		if (ich < ichMax && (ch == 'E' || ch == 'e')) {
			if (++ich >= ichMax)
				return Float.NaN;
			ch = str.charAt(ich);
			if ((ch == '+') && (++ich >= ichMax))
				return Float.NaN;
			int exponent = parseIntChecked(str, ich, ichMax);
			if (exponent == Integer.MIN_VALUE)
				return Float.NaN;
			if (exponent > 0)
				value *= ((exponent < tensScale.length) ? tensScale[exponent - 1] : Math.pow(10, exponent));
			else if (exponent < 0)
				value *= ((-exponent < decimalScale.length) ? decimalScale[-exponent - 1] : Math.pow(10, exponent));
		}
		else {
			ichNextParse = ich; // the exponent code finds its own ichNextParse
		}
		return value;
	}

	protected int parseInt(String str) {
		return parseIntChecked(str, 0, str.length());
	}

	protected int parseInt(String str, int ich) {
		int cch = str.length();
		if (ich >= cch)
			return Integer.MIN_VALUE;
		return parseIntChecked(str, ich, cch);
	}

	int parseInt(String str, int ichStart, int ichMax) {
		int cch = str.length();
		if (ichMax > cch)
			ichMax = cch;
		if (ichStart >= ichMax)
			return Integer.MIN_VALUE;
		return parseIntChecked(str, ichStart, ichMax);
	}

	int parseIntChecked(String str, int ichStart, int ichMax) {
		boolean digitSeen = false;
		int value = 0;
		int ich = ichStart;
		char ch;
		while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
			++ich;
		boolean negative = false;
		if (ich < ichMax && str.charAt(ich) == '-') {
			negative = true;
			++ich;
		}
		while (ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
			value = value * 10 + (ch - '0');
			digitSeen = true;
			++ich;
		}
		if (!digitSeen)
			value = Integer.MIN_VALUE;
		else if (negative)
			value = -value;
		ichNextParse = ich;
		return value;
	}

	protected String parseToken(String str) {
		return parseTokenChecked(str, 0, str.length());
	}

	protected String parseToken(String str, int ich) {
		int cch = str.length();
		if (ich >= cch)
			return null;
		return parseTokenChecked(str, ich, cch);
	}

	String parseTokenChecked(String str, int ichStart, int ichMax) {
		int ich = ichStart;
		char ch;
		while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
			++ich;
		int ichNonWhite = ich;
		while (ich < ichMax && ((ch = str.charAt(ich)) != ' ' && ch != '\t'))
			++ich;
		ichNextParse = ich;
		if (ichNonWhite == ich)
			return null;
		return str.substring(ichNonWhite, ich);
	}

}