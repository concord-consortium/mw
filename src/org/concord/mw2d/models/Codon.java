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

package org.concord.mw2d.models;

import java.io.Serializable;

import org.concord.molbio.engine.Aminoacid;

/** This is a DNA codon */

public class Codon implements Serializable {

	public static final char T = 'T';
	public static final char A = 'A';
	public static final char G = 'G';
	public static final char C = 'C';

	private char[] code;

	/** construct a GGG codon */
	public Codon() {
		code = new char[] { G, G, G };
	}

	public Codon(char[] c) {
		this(c[0], c[1], c[2]);
	}

	public Codon(char a, char b, char c) {
		if ((a != T && a != A && a != G && a != C && a != 't' && a != 'a' && a != 'g' && a != 'c')
				|| (b != T && b != A && b != G && b != C && b != 't' && b != 'a' && b != 'g' && b != 'c')
				|| (c != T && c != A && c != G && c != C && c != 't' && c != 'a' && c != 'g' && c != 'c'))
			throw new IllegalArgumentException("illegal codon");
		code = new char[] { a, b, c };
	}

	public String toString() {
		return new String(code);
	}

	public boolean equals(Object o) {
		if (!(o instanceof Codon))
			return false;
		return code[0] == ((Codon) o).code[0] && code[1] == ((Codon) o).code[1] && code[2] == ((Codon) o).code[2];
	}

	public int hashCode() {
		return code[0] ^ code[1] ^ code[2];
	}

	public char getCode(int n) {
		if (n < 0 || n >= 3)
			throw new IllegalArgumentException("Array index out of bound");
		return code[n];
	}

	/*
	 * public void setCode(char[] c){ code=c; }
	 */

	public char[] getCode() {
		return code;
	}

	public char[] getComplementaryCode() {
		if (code == null)
			throw new IllegalArgumentException("cannot get complementary for null codon");
		char[] c = new char[3];
		for (int i = 0; i < 3; i++) {
			switch (code[i]) {
			case T:
				c[i] = A;
				break;
			case A:
				c[i] = T;
				break;
			case C:
				c[i] = G;
				break;
			case G:
				c[i] = C;
				break;
			case 't':
				c[i] = 'a';
				break;
			case 'a':
				c[i] = 't';
				break;
			case 'c':
				c[i] = 'g';
				break;
			case 'g':
				c[i] = 'c';
				break;
			}
		}
		return c;
	}

	public static char[] getComplementaryCode(Codon c0) {
		if (c0 == null)
			throw new IllegalArgumentException("input codon error");
		return c0.getComplementaryCode();
	}

	public static char[] getComplementaryCode(char[] c0) {
		if (c0 == null || c0.length != 3)
			throw new IllegalArgumentException("input codon error");
		return new Codon(c0).getComplementaryCode();
	}

	public boolean isStopCodon() {
		char[] c = new String(code).toUpperCase().toCharArray();
		return (c[0] == T && c[1] == A && c[2] == G) || (c[0] == T && c[1] == A && c[2] == A)
				|| (c[0] == T && c[1] == G && c[2] == A);
	}

	public static boolean isStopCodon(char[] code) {
		if (code == null)
			throw new IllegalArgumentException("input codon error");
		return new Codon(code).isStopCodon();
	}

	/** express this DNA codon to an amino acid */
	public Aminoacid express() {
		return expressFromDNA(code);
	}

	/** express a DNA codon to an amino acid */
	public static Aminoacid expressFromDNA(char[] code) {
		if (code == null)
			throw new IllegalArgumentException("input codon error");
		return Aminoacid.express(code);
	}

}