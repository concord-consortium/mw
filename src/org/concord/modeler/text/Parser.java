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

class Parser {

	private Parser() {
	}

	static Byte parseByte(String s) {
		Byte b = null;
		try {
			b = Byte.valueOf(s);
		}
		catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			b = null;
		}
		return b;
	}

	static Short parseShort(String s) {
		Short t = null;
		try {
			t = Short.valueOf(s);
		}
		catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			t = null;
		}
		return t;
	}

	static Integer parseInt(String s) {
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

	static Integer parseInt(String s, int radix) {
		Integer i = null;
		try {
			i = Integer.valueOf(s, radix);
		}
		catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			i = null;
		}
		return i;
	}

	static Long parseLong(String s) {
		Long i = null;
		try {
			i = Long.valueOf(s);
		}
		catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			i = null;
		}
		return i;
	}

	static Float parseFloat(String s) {
		Float f = null;
		try {
			f = Float.valueOf(s);
		}
		catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			f = null;
		}
		return f;
	}

	static Double parseDouble(String s) {
		Double d = null;
		try {
			d = Double.valueOf(s);
		}
		catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			d = null;
		}
		return d;
	}

}