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

import org.concord.molbio.event.MutationEvent;

public class InsertionMutator extends Mutator {

	private String insertionString;

	protected InsertionMutator() {
		mutatorType = MUTATOR_INSERTION;
	}

	public void setMutationParam(Object[] params) {
		insertionString = null;
		if (params == null || params.length < 1)
			return;
		if (params[0] instanceof String) {
			insertionString = (String) params[0];
		}
	}

	protected void mutate(DNA dna, int strandIndex, int nucleotideIndex) {
		if (dna == null)
			return;
		if (!dna.checkStrandComplementarity())
			return;
		Nucleotide oldNucleotide = dna.getStrand(strandIndex).getNucleotide(nucleotideIndex);
		Strand firstStrand = dna.getStrand(strandIndex);
		Strand secondaryStrand = dna.getComplimentaryStrand(strandIndex);
		if (nucleotideIndex < 0)
			nucleotideIndex = 0;
		if (nucleotideIndex > firstStrand.getLength())
			nucleotideIndex = firstStrand.getLength();
		Strand strandToInsert = generateStrandForInsertion();
		if (strandToInsert == null || strandToInsert.getLength() < 1)
			return;
		for (int i = 0; i < strandToInsert.getLength(); i++) {
			Nucleotide n = strandToInsert.getNucleotide(i);
			firstStrand.addNucleotide(nucleotideIndex, n);
			secondaryStrand.addNucleotide(nucleotideIndex, n.getComplimentaryNucleotide());
			if (getMutationDirection() >= 0) {
				nucleotideIndex++;
			}
		}
		if (getMutationDirection() >= 0 && nucleotideIndex > 0)
			nucleotideIndex--;
		Nucleotide newNucleotide = dna.getStrand(strandIndex).getNucleotide(nucleotideIndex);
		notifyMutationListeners(new MutationEvent(this, strandIndex, nucleotideIndex, oldNucleotide, newNucleotide));
	}

	private Strand generateStrandForInsertion() {
		if (insertionString == null)
			return Strand.generateRandomStrand(getFragmentLength());
		String strindToInsert = insertionString;
		if (getMutationDirection() < 0) {
			StringBuffer sb = new StringBuffer();
			for (int i = insertionString.length() - 1; i >= 0; i--) {
				sb.append(insertionString.charAt(i));
			}
			strindToInsert = sb.toString();
		}
		return new Strand(strindToInsert);
	}

	protected void mutate(Strand strand, int nucleotideIndex) {
		if (strand == null)
			return;
		if (nucleotideIndex < 0)
			nucleotideIndex = 0;
		if (nucleotideIndex > strand.getLength())
			nucleotideIndex = strand.getLength();
		Nucleotide oldNucleotide = strand.getNucleotide(nucleotideIndex);
		Strand strandToInsert = null;
		if (strand instanceof RNA) {
			strandToInsert = RNA.generateRandomRNA(getFragmentLength());
		}
		else {
			strandToInsert = Strand.generateRandomStrand(getFragmentLength());
		}
		if (strandToInsert == null || strandToInsert.getLength() < 1)
			return;
		for (int i = 0; i < strandToInsert.getLength(); i++) {
			Nucleotide n = strandToInsert.getNucleotide(i);
			strand.addNucleotide(nucleotideIndex++, n);
		}
		Nucleotide newNucleotide = strand.getNucleotide(nucleotideIndex);
		notifyMutationListeners(new MutationEvent(this, nucleotideIndex, oldNucleotide, newNucleotide));
	}
}