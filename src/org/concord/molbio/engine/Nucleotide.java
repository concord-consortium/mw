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

package org.concord.molbio.engine;

import java.awt.Color;

public final class Nucleotide {

	private static final char UNDEFINE_NAME = ' ';

	public static final Color A_COLOR = new Color(0x7da7d9);
	public static final Color T_COLOR = new Color(0xfff699);
	public static final Color U_COLOR = new Color(0xfff699);
	public static final Color G_COLOR = new Color(0xfdc588);
	public static final Color C_COLOR = new Color(0xc4df9a);

	public static final char ADENINE_NAME = 'A';
	public static final char GUANINE_NAME = 'G';
	public static final char THYMINE_NAME = 'T';
	public static final char CYTOSINE_NAME = 'C';
	public static final char URACIL_NAME = 'U';

	public static final String ADENINE_FULL_NAME = "Adenine";
	public static final String THYMINE_FULL_NAME = "Thymine";
	public static final String GUANINE_FULL_NAME = "Guanine";
	public static final String CYTOSINE_FULL_NAME = "Cytosine";
	public static final String URACIL_FULL_NAME = "Uracile";

	private char name = UNDEFINE_NAME;
	private String fullName;
	private String nameStr;

	static final Nucleotide ADENINE = new Nucleotide(ADENINE_NAME, ADENINE_FULL_NAME);
	static final Nucleotide THYMINE = new Nucleotide(THYMINE_NAME, THYMINE_FULL_NAME);
	static final Nucleotide GUANINE = new Nucleotide(GUANINE_NAME, GUANINE_FULL_NAME);
	static final Nucleotide CYTOSINE = new Nucleotide(CYTOSINE_NAME, CYTOSINE_FULL_NAME);
	static final Nucleotide URACIL = new Nucleotide(URACIL_NAME, URACIL_FULL_NAME);

	private Nucleotide(char name, String fullName) {
		this.fullName = fullName;
		this.name = name;
		nameStr = "" + this.name;
	}

	public Nucleotide getComplimentaryNucleotide(boolean inRNA) {
		try {
			Nucleotide n = getNucleotide(getComplimentaryNucleotideName(inRNA));
			return n;
		}
		catch (IllegalArgumentException e) {
		}
		return null;
	}

	public Nucleotide getComplimentaryNucleotide() {
		return getComplimentaryNucleotide(false);
	}

	public boolean isUndefine() {
		return name == UNDEFINE_NAME;
	}

	protected static boolean checkCorrectness(char n) {
		if (n == ADENINE_NAME)
			return true;
		if (n == GUANINE_NAME)
			return true;
		if (n == THYMINE_NAME)
			return true;
		if (n == CYTOSINE_NAME)
			return true;
		if (n == URACIL_NAME)
			return true;
		return false;
	}

	public char getComplimentaryNucleotideName(boolean inRNA) {
		if (name == ADENINE_NAME)
			return (inRNA) ? URACIL_NAME : THYMINE_NAME;
		if (name == THYMINE_NAME)
			return ADENINE_NAME;
		if (name == GUANINE_NAME)
			return CYTOSINE_NAME;
		if (name == CYTOSINE_NAME)
			return GUANINE_NAME;
		if (name == URACIL_NAME)
			return ADENINE_NAME;
		return UNDEFINE_NAME;
	}

	public char getComplimentaryNucleotideName() {
		return getComplimentaryNucleotideName(false);
	}

	public char getName() {
		return name;
	}

	public String getFullName() {
		return fullName;
	}

	public String toString() {
		return nameStr;
	}

	public static Nucleotide getByFullName(String n) throws IllegalArgumentException {
		if (n != null) {
			if (n.equals(ADENINE_FULL_NAME))
				return ADENINE;
			if (n.equals(THYMINE_FULL_NAME))
				return THYMINE;
			if (n.equals(GUANINE_FULL_NAME))
				return GUANINE;
			if (n.equals(CYTOSINE_FULL_NAME))
				return CYTOSINE;
			if (n.equals(URACIL_FULL_NAME))
				return URACIL;
		}
		throw new IllegalArgumentException("it's impossible to create nucleotide with the name " + n);
	}

	public static char convert53DNAStrandToRNA(char c) throws IllegalArgumentException {
		if (c == URACIL_NAME)
			throw new IllegalArgumentException("DNA strand can't contain uracil");

		if (c == THYMINE_NAME)
			return URACIL_NAME;
		return c;
	}

	public static Nucleotide getAdenine() {
		return ADENINE;
	}

	public static Nucleotide getThymine() {
		return THYMINE;
	}

	public static Nucleotide getGuanine() {
		return GUANINE;
	}

	public static Nucleotide getCytosine() {
		return CYTOSINE;
	}

	public static Nucleotide getUracil() {
		return URACIL;
	}

	public static Nucleotide getNucleotide(char n) throws IllegalArgumentException {
		n = Character.toUpperCase(n);
		if (n == ADENINE_NAME)
			return ADENINE;
		if (n == THYMINE_NAME)
			return THYMINE;
		if (n == GUANINE_NAME)
			return GUANINE;
		if (n == CYTOSINE_NAME)
			return CYTOSINE;
		if (n == URACIL_NAME)
			return URACIL;
		throw new IllegalArgumentException("it's impossible to create nucleotide with the name " + n);
	}

	public static Nucleotide getRandomNucleotide() {
		int rN = (int) Math.round(3 * Math.random());
		switch (rN) {
		case 0:
			return ADENINE;
		case 1:
			return THYMINE;
		case 2:
			return GUANINE;
		case 3:
			return CYTOSINE;
		}
		return ADENINE;
	}

	public static boolean isAdenine(Object obj) {
		return obj == ADENINE;
	}

	public static boolean isThymine(Object obj) {
		return obj == THYMINE;
	}

	public static boolean isGuanine(Object obj) {
		return obj == GUANINE;
	}

	public static boolean isCytosine(Object obj) {
		return obj == CYTOSINE;
	}

	public static boolean isUracil(Object obj) {
		return obj == URACIL;
	}

}
