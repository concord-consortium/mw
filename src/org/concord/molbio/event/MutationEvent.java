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

package org.concord.molbio.event;

import java.util.EventObject;
import org.concord.molbio.engine.Nucleotide;

public class MutationEvent extends EventObject{
int strandIndex;
int nucleotideIndex;
Nucleotide oldNucleotide;
Nucleotide newNucleotide;

    public MutationEvent(Object source,int strandIndex,int nucleotideIndex,Nucleotide oldNucleotide,Nucleotide newNucleotide){
        super(source);
        this.strandIndex = strandIndex;
        this.nucleotideIndex = nucleotideIndex;
        this.oldNucleotide = oldNucleotide;
        this.newNucleotide = newNucleotide;
    }   
     
    public MutationEvent(Object source,int nucleotideIndex,Nucleotide oldNucleotide,Nucleotide newNucleotide){
        this(source,0,nucleotideIndex,oldNucleotide,newNucleotide);
    }   
     
    public int getNucleotideIndex(){
        return nucleotideIndex;
    }
    
    public int getStrandIndex(){
        return strandIndex;
    }
    
    public Nucleotide getOldNucleotide(){
        return oldNucleotide;
    }
    
    public Nucleotide getNewNucleotide(){
        return newNucleotide;
    }
}

