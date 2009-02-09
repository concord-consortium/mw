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

public class DeletionMutator extends Mutator {

	protected DeletionMutator() {
		mutatorType = MUTATOR_DELETION;
	}

	public void setMutationParam(Object[] params) {
	}

	protected void mutate(DNA dna, int strandIndex, int nucleotideIndex) {
		if (!dna.checkStrandComplementarity())
			return;
		int[] intervals = new int[2];
		Nucleotide oldNucleotide = dna.getStrand(strandIndex).getNucleotide(nucleotideIndex);
		Strand firstStrand = dna.getStrand(strandIndex);
		Strand secondStrand = dna.getComplimentaryStrand(strandIndex);
		defineRightMutationInterval(firstStrand.getLength(), nucleotideIndex, intervals);
		for (int i = 0; i < intervals[1] - intervals[0]; i++) {
			firstStrand.removeNucleotide(intervals[0]);
			secondStrand.removeNucleotide(intervals[0]);
		}
		Nucleotide newNucleotide = dna.getStrand(strandIndex).getNucleotide(nucleotideIndex);
		notifyMutationListeners(new MutationEvent(this, strandIndex, nucleotideIndex, oldNucleotide, newNucleotide));
	}

	protected void mutate(Strand strand, int nucleotideIndex) {
		if (strand == null || getFragmentLength() == 0)
			return;
		int[] intervals = new int[2];
		Nucleotide oldNucleotide = strand.getNucleotide(nucleotideIndex);
		defineRightMutationInterval(strand.getLength(), nucleotideIndex, intervals);
		for (int i = 0; i < intervals[1] - intervals[0]; i++) {
			strand.removeNucleotide(intervals[0]);
		}
		Nucleotide newNucleotide = strand.getNucleotide(nucleotideIndex);
		notifyMutationListeners(new MutationEvent(this, nucleotideIndex, oldNucleotide, newNucleotide));
	}

}