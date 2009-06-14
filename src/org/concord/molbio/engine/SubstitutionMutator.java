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

public class SubstitutionMutator extends Mutator {

	private String substitutionString;

	protected SubstitutionMutator() {
		mutatorType = MUTATOR_SUBSTITUTION;
	}

	public void setMutationParam(Object[] params) {
		substitutionString = null;
		if (params == null || params.length < 1)
			return;
		if (params[0] instanceof String) {
			substitutionString = (String) params[0];
		}
	}

	protected void mutate(DNA dna, int strandIndex, int nucleotideIndex) {
		if (!dna.checkStrandComplementarity())
			return;
		Nucleotide oldNucleotide = dna.getStrand(strandIndex).getNucleotide(nucleotideIndex);
		int[] intervals = new int[2];
		Strand firstStrand = dna.getStrand(strandIndex);
		Strand secondStrand = dna.getComplimentaryStrand(strandIndex);
		defineRightMutationInterval(firstStrand.getLength(), nucleotideIndex, intervals);
		int substitutionIndex = 0;
		for (int i = intervals[0]; i < intervals[1]; i++) {
			Nucleotide n = substituteNucleotide(firstStrand, i, substitutionIndex++);
			if (n == null)
				continue;
			secondStrand.removeNucleotide(i);
			secondStrand.addNucleotide(i, n.getComplementaryNucleotide());
		}
		Nucleotide newNucleotide = dna.getStrand(strandIndex).getNucleotide(nucleotideIndex);
		notifyMutationListeners(new MutationEvent(this, strandIndex, nucleotideIndex, oldNucleotide, newNucleotide));
	}

	private Nucleotide substituteNucleotide(Strand strand, int index, int substitutionIndex) {
		if (substitutionString == null || substitutionIndex < 0 || substitutionIndex >= substitutionString.length())
			return strand.substituteNucleotideRandomly(index);
		Nucleotide nucleo = Nucleotide.getNucleotide(substitutionString.charAt(substitutionIndex));
		return strand.substituteNucleotide(index, nucleo);
	}

	protected void mutate(Strand strand, int nucleotideIndex) {
		if (strand == null || getFragmentLength() == 0)
			return;
		Nucleotide oldNucleotide = strand.getNucleotide(nucleotideIndex);
		int[] intervals = new int[2];
		defineRightMutationInterval(strand.getLength(), nucleotideIndex, intervals);
		for (int i = intervals[0]; i < intervals[1]; i++) {
			strand.substituteNucleotideRandomly(i);
		}
		Nucleotide newNucleotide = strand.getNucleotide(nucleotideIndex);
		notifyMutationListeners(new MutationEvent(this, nucleotideIndex, oldNucleotide, newNucleotide));
	}

}