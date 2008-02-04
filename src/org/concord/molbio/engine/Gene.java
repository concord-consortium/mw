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
import java.util.Vector;

public class Gene{
DNA     dna;
int     []indexes;
int     strandIndex;

int     startIndex = -1;
int     endIndex = -1;


    public Gene(DNA dna){
        this(dna,DNA.DNA_STRAND_BASE);
    }    
    
    public Gene(DNA dna,int strandIndex){
        setDNA(dna,strandIndex);
        if(dna != null){
            if(strandIndex == DNA.DNA_STRAND_BASE){
                startIndex = 0;
                endIndex = 3 * (dna.getLength() / 3);
            }else{
                startIndex = dna.getLength() - 3 * (dna.getLength() / 3);
                endIndex = dna.getLength() - 1;
            }
        }
    }    
    
    protected void setDNA(DNA dna,int strandIndex){
        this.dna = dna;
        this.strandIndex = strandIndex;
    }
    
    protected void createIndexes(){
    }
    
    protected void setIndexes(int []indexes,int startIndex,int endIndex){
        this.indexes = indexes;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }
    
    public RNA transcript(){
        return transcript(false);
    }
    
    public RNA transcript(boolean needCheckStartIndex){
        if(needCheckStartIndex && !checkStartIndex()) return null;
        RNA rna = dna.transcript(indexes,strandIndex,startIndex,endIndex);
        return rna;
    }
    
    protected boolean checkStartIndex(){
        Strand strand = dna.getStrand(strandIndex);
        if(strand == null || strand.getLength() < 3) return false;
        Vector bases = strand.bases;
        if(bases == null) return false;
        int startCheckGene = startIndex;
        int endCheckGene = endIndex;
        if(indexes != null){
            try{
                startCheckGene = indexes[0];
                endCheckGene = indexes[1];
            }catch(ArrayIndexOutOfBoundsException e){
                return false;
            }
        }
        
        if(startCheckGene < 0 || startCheckGene >= strand.getLength()) return false;
        if(endCheckGene < 0 || endCheckGene >= strand.getLength()) return false;
        if(endCheckGene - startCheckGene < 3) return false;
        
        if(!Nucleotide.isThymine(bases.elementAt(startCheckGene))) return false;
        if(!Nucleotide.isAdenine(bases.elementAt(startCheckGene+1))) return false;
        if(!Nucleotide.isCytosine(bases.elementAt(startCheckGene+2))) return false;
        return true;
    }

    protected boolean checkEndIndex(){
        String endFragmentStr = DNA.endFragment.getFragmentAsString();
        int endFragmentStrLength = endFragmentStr.length();
        Strand strand = dna.getStrand(strandIndex);
        if(strand == null || strand.getLength() < endFragmentStrLength) return false;
        Vector bases = strand.bases;
        if(bases == null) return false;
        int startCheckGene = startIndex;
        int endCheckGene = endIndex;
        if(indexes != null){
            try{
                endCheckGene = indexes[indexes.length - 1];
                startCheckGene = endCheckGene - endFragmentStrLength;
                if(startCheckGene < indexes[indexes.length - 2]) return false;
                String testDNA = dna.getFragmentAsString(startCheckGene,endCheckGene);
                return testDNA.equals(endFragmentStr);
            }catch(ArrayIndexOutOfBoundsException e){
                return false;
            }catch(Throwable t){}
        }
        return false;
    }
    
    
}
