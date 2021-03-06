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

public class IdentityMutator extends Mutator {

	protected IdentityMutator() {
		mutatorType = MUTATOR_IDENTITY;
	}

	public void setMutationParam(Object[] params) {
	}

	protected void mutate(DNA dna, int strandIndex, int nucleotideIndex) {
		org.concord.molbio.engine.Nucleotide n = dna.getStrand(strandIndex).getNucleotide(nucleotideIndex);
		notifyMutationListeners(new MutationEvent(this, strandIndex, nucleotideIndex, n, n));
	}

	protected void mutate(Strand strand, int nucleotideIndex) {
		org.concord.molbio.engine.Nucleotide n = strand.getNucleotide(nucleotideIndex);
		notifyMutationListeners(new MutationEvent(this, nucleotideIndex, n, n));
	}
}