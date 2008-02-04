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

public class MixedMutator extends Mutator{
MutatorRandomizer randomizer;
    protected MixedMutator(){
        mutatorType = MUTATOR_MIXED;
        randomizer = new MutatorRandomizer(1,1,1,1);
    }
    
    protected MixedMutator(int ident,int subst,int insert,int delet){
        mutatorType = MUTATOR_MIXED;
        randomizer = new MutatorRandomizer(ident,subst,insert,delet);
    }
    
    public void setMutationParam(Object []params){
        if(params == null || params.length != NUMB_POSSIBLE_MUTATORS) return;
        for(int i = 0; i < params.length; i++){
            if(!(params[i] instanceof Integer)) return;
        }
        randomizer = new MutatorRandomizer(((Integer)params[0]).intValue(),
                                           ((Integer)params[1]).intValue(),
                                           ((Integer)params[2]).intValue(),
                                           ((Integer)params[3]).intValue());
    }
    
    protected void mutate(DNA dna,int strandIndex,int nucleotideIndex){
        int needMutator = randomizer.getRandomMutatorType();
        Mutator mutator = getInstance(needMutator);
        //System.out.println("MixedMutator needMutator "+needMutator+" mutator "+mutator);
        if(mutator != null){
            Nucleotide oldNucleotide = dna.getStrand(strandIndex).getNucleotide(nucleotideIndex);
            mutator.doMutation(dna,strandIndex,nucleotideIndex,fragmentLength);
            Nucleotide newNucleotide = dna.getStrand(strandIndex).getNucleotide(nucleotideIndex);
            notifyMutationListeners(new MutationEvent(mutator,strandIndex,nucleotideIndex,oldNucleotide,newNucleotide));
        }
    }
    
    protected void mutate(Strand strand, int nucleotideIndex){
        int needMutator = randomizer.getRandomMutatorType();
        Mutator mutator = getInstance(needMutator);
        // System.out.println("MixedMutator needMutator "+needMutator+" mutator "+mutator);
        if(mutator != null){
            Nucleotide oldNucleotide = strand.getNucleotide(nucleotideIndex);
            mutator.doMutation(strand,nucleotideIndex,fragmentLength);
            Nucleotide newNucleotide = strand.getNucleotide(nucleotideIndex);
            notifyMutationListeners(new MutationEvent(mutator,nucleotideIndex,oldNucleotide,newNucleotide));
        }
    }
}