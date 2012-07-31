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

public class Codon {

	Nucleotide[] bases = new Nucleotide[3];
	private String nameStr;
	private char[] code;// Charles compatibility
	public static final int CODON_STATE_NONE = 0;
	public static final int CODON_STATE_START = 1;
	public static final int CODON_STATE_STOP = 2;

	static final String MITOCHONDRIA_SUFFIX = "_M";

	private int codonState = CODON_STATE_NONE;
	private boolean inMitochondria = false;

	protected Codon(char b1, char b2, char b3, boolean inMitochondria) throws IllegalArgumentException {
		init(b1, b2, b3, inMitochondria);
	}

	protected Codon(String str, boolean inMitochondria) throws IllegalArgumentException {
		if (str == null || str.length() != 3)
			throw new IllegalArgumentException("parameter of the codon constructor should be 3 letter string");
		init(str.charAt(0), str.charAt(1), str.charAt(2), inMitochondria);
	}

	protected Codon(Nucleotide b1, Nucleotide b2, Nucleotide b3, boolean inMitochondria) {
		bases[0] = (b1 == Nucleotide.THYMINE) ? Nucleotide.URACIL : b1;
		bases[1] = (b2 == Nucleotide.THYMINE) ? Nucleotide.URACIL : b2;
		bases[2] = (b3 == Nucleotide.THYMINE) ? Nucleotide.URACIL : b3;
		nameStr = "" + bases[0].getName() + bases[1].getName() + bases[2].getName();
		this.inMitochondria = inMitochondria;
		setCodonState(bases[0].getName(), bases[1].getName(), bases[2].getName());
	}

	public static Codon getCodon(char b1, char b2, char b3) {
		return new Codon(b1, b2, b3);
	}

	public static Codon getCodon(String str) {
		return new Codon(str);
	}

	protected Codon(char b1, char b2, char b3) throws IllegalArgumentException {
		this(b1, b2, b3, false);
	}

	public Codon(String str) throws IllegalArgumentException {
		this(str, false);
	}

	protected Codon(Nucleotide b1, Nucleotide b2, Nucleotide b3) {
		this(b1, b2, b3, false);
	}

	protected void init(char b1, char b2, char b3, boolean inMitochondria) throws IllegalArgumentException {
		if (b1 == Nucleotide.THYMINE_NAME)
			b1 = Nucleotide.URACIL_NAME;
		if (b2 == Nucleotide.THYMINE_NAME)
			b2 = Nucleotide.URACIL_NAME;
		if (b3 == Nucleotide.THYMINE_NAME)
			b3 = Nucleotide.URACIL_NAME;
		bases[0] = Nucleotide.getNucleotide(b1);
		bases[1] = Nucleotide.getNucleotide(b2);
		bases[2] = Nucleotide.getNucleotide(b3);
		nameStr = "" + b1 + b2 + b3;
		this.inMitochondria = inMitochondria;
		setCodonState(bases[0].getName(), bases[1].getName(), bases[2].getName());
		code = new char[3];
		for (int i = 0; i < 3; i++)
			code[i] = bases[i].getName();
	}

	public char[] getCode() {
		return code;
	}

	protected void setCodonState(char b1, char b2, char b3) {
		if (b1 == 'A' && b2 == 'U' && b3 == 'G') {
			codonState = CODON_STATE_START;
		}
		else if (b1 == 'U' && b2 == 'A' && b3 == 'A') {
			codonState = CODON_STATE_STOP;
		}
		else if (b1 == 'U' && b2 == 'A' && b3 == 'G') {
			codonState = CODON_STATE_STOP;
		}
		else if (!inMitochondria && b1 == 'U' && b2 == 'G' && b3 == 'A') {
			codonState = CODON_STATE_STOP;
		}
		else if (inMitochondria && b1 == 'A' && b2 == 'G' && b3 == 'A') {
			codonState = CODON_STATE_STOP;
		}
		else if (inMitochondria && b1 == 'A' && b2 == 'G' && b3 == 'G') {
			codonState = CODON_STATE_STOP;
		}
	}

	public boolean isCodonStart() {
		return codonState == CODON_STATE_START;
	}

	public boolean isCodonStop() {
		return codonState == CODON_STATE_STOP;
	}

	protected void setInMitochondria(boolean inMitochondria) {
		this.inMitochondria = inMitochondria;
	}

	public boolean isInMitochondria() {
		return inMitochondria;
	}

	public String toString() {
		return nameStr;
	}

	public Codon getTranscripted() {
		return getTranscripted(false);
	}

	public Codon getTranscripted(boolean reversed) {
		return new Codon(bases[(reversed) ? 2 : 0].getComplementaryNucleotide(true), bases[1].getComplementaryNucleotide(true), bases[(reversed) ? 0 : 2].getComplementaryNucleotide(true));
	}

	public Aminoacid createAminoacidWithTranscription() {
		return getTranscripted().createAminoacid();
	}

	public Aminoacid createAminoacid() {
		if (isCodonStop())
			return null;
		if (!inMitochondria) {
			return (Aminoacid.aminoCreation.get(nameStr)).amino;
		}
		Aminoacid amino = (Aminoacid.aminoCreation.get(nameStr + MITOCHONDRIA_SUFFIX)).amino;
		if (amino != null)
			return amino;
		return (Aminoacid.aminoCreation.get(nameStr)).amino;
	}

}