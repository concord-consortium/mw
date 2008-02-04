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

public class RNA extends Strand {

	Codon[] codons;
	int[] indexes;
	int startIndex;
	int endIndex;

	public RNA() {
	}

	protected RNA(String str) throws IllegalArgumentException {
		if (str == null || str.length() < 1)
			return;
		for (int i = 0; i < str.length(); i++) {
			addNucleotide(str.charAt(i));
		}
	}

	protected void addRandomNucleotide() {
		double dr = 4 * Math.random();
		Nucleotide nn = null;
		if (dr >= 0 && dr < 1)
			nn = Nucleotide.ADENINE;
		else if (dr >= 1 && dr < 2)
			nn = Nucleotide.GUANINE;
		else if (dr >= 2 && dr < 3)
			nn = Nucleotide.CYTOSINE;
		else if (dr >= 3 && dr < 4)
			nn = Nucleotide.URACIL;
		addNucleotide(nn);
	}

	public static RNA generateRandomRNA(int len) {
		if (len < 1)
			return null;
		RNA rna = new RNA();
		for (int i = 0; i < len; i++) {
			rna.addRandomNucleotide();
		}
		return rna;
	}

	public Nucleotide substituteNucleotide(int index, Nucleotide nucleo) {
		if (nucleo == null)
			return null;
		if (nucleo == Nucleotide.THYMINE)
			return null;
		bases.removeElementAt(index);
		bases.insertElementAt(nucleo, index);
		return nucleo;
	}

	public Nucleotide substituteNucleotideRandomly(int index) {
		Nucleotide n = getNucleotide(index);
		if (n == null)
			return null;
		if (n == Nucleotide.URACIL)
			n = Nucleotide.THYMINE;
		Nucleotide nn = getRandomNucleotide(n);
		if (nn == null)
			return null;
		if (nn == Nucleotide.THYMINE)
			nn = Nucleotide.URACIL;
		bases.removeElementAt(index);
		bases.insertElementAt(nn, index);
		return nn;
	}

	protected void addNucleotide(Nucleotide b) throws IllegalArgumentException {
		if (b.getName() == Nucleotide.THYMINE_NAME)
			throw new IllegalArgumentException("Thymine could not be added to the RNA strand");
		addNucleotide0(b);
	}

	protected void addNucleotide(int index, Nucleotide b) throws IllegalArgumentException {
		if (b.getName() == Nucleotide.THYMINE_NAME)
			throw new IllegalArgumentException("Thymine could not be added to the RNA strand");
		addNucleotide0(index, b);
	}

	protected void setIndexes(int[] indexes, int startIndex, int endIndex) {
		this.indexes = indexes;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}

	public RNA createMRNA() {
		RNA rna = null;
		if (indexes == null) {
			rna = this;
		}
		else {
			String str = "";
			int i = 0;
			while (i < indexes.length) {
				int startIndex = indexes[i++];
				int endIndex = (i < indexes.length) ? indexes[i++] : getLength();
				String s = null;
				try {
					s = getFragmentAsString(startIndex, endIndex);
				}
				catch (Exception e) {
				}
				if (s == null)
					break;
				str += s;
			}
			rna = new RNA(str);
		}
		rna.createCodons();
		return rna;
	}

	protected Strand getComplimentaryStrand() {
		Strand strand = new Strand();
		if (bases == null)
			return strand;
		String str = toString();
		for (int i = 0; i < str.length(); i++) {
			char n = str.charAt(i);
			Nucleotide nuc = Nucleotide.getNucleotide(n);
			Nucleotide complNuc = nuc.getComplimentaryNucleotide(true);
			strand.addNucleotide(complNuc);
		}
		return strand;
	}

	protected void createCodons() {
		codons = null;
		if (bases == null)
			return;
		int nCodons = bases.size() / 3;
		if (nCodons < 1)
			return;
		codons = new Codon[nCodons];
		for (int i = 0; i < nCodons; i++) {
			int index = 3 * i;
			Nucleotide b1 = (Nucleotide) bases.elementAt(index);
			Nucleotide b2 = (Nucleotide) bases.elementAt(index + 1);
			Nucleotide b3 = (Nucleotide) bases.elementAt(index + 2);
			codons[i] = new Codon(b1, b2, b3);
		}
	}

	public Protein translate() {
		RNA rna = createMRNA();
		Codon[] newcodons = rna.codons;
		if (newcodons == null || newcodons.length < 1)
			return null;
		Protein p = new Protein();
		for (int i = 0; i < newcodons.length; i++) {
			if (newcodons[i].isCodonStop())
				break;
			p.addAminoacid(newcodons[i].createAminoacid());
		}
		return p;
	}

}