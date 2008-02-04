/* $RCSfile: Model.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:09 $
 * $Revision: 1.11 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;
import java.util.BitSet;

import org.jmol.util.ArrayUtil;

final class Model {

  /*
   * In Jmol all atoms and bonds are kept as a pair of arrays in 
   * the overall Frame object. Thus, "Model" is not atoms and bonds. 
   * It is a description of all the:
   * 
   * chains (as defined in the file)
   *   and their associated file-associated groups,  
   * polymers (same, I think, but in terms of secondary structure)
   *   and their associated monomers
   * molecules (as defined by connectivity)
   *  
   * Note that "monomer" extends group. A group only becomes a 
   * monomer if it can be identified as one of the following 
   * PDB/mmCIF types:
   * 
   *   amino  -- has an N, a C, and a CA
   *   alpha  -- has just a CA
   *   nucleic -- has C1',C2',C3',C4',C5',O3', and O5'
   *   phosphorus -- has P
   *   
   * The term "conformation" is a bit loose. It means "what you get
   * when you go with one or another set of alternative locations.
   * 
   * Also held here is the "modelTag" and information
   * about how many atoms there were before symmetry was applied
   * as well as a bit about insertions and alternative locations.
   * 
   * 
   * one model = one animation "frame", but we don't use the "f" word
   * here because that would confuse the issue with the overall "Frame"
   * frame of which there is only one ever in Jmol.
   * 
   * If multiple files are loaded, then they will appear here in 
   * at least as many Model objects. Each vibration will be a complete
   * set of atoms as well. 
   *  
   */
  
  Mmset mmset;
  int modelIndex;   // our 0-based reference
  int modelNumber;  // what the user works with
  String modelTag;
  int preSymmetryAtomIndex = -1;
  int preSymmetryAtomCount;
  int firstMolecule;
  int moleculeCount;
  int nAltLocs;
  int nInsertions;
  boolean isPDB = false;
  private int chainCount = 0;
  private Chain[] chains = new Chain[8];
  private int polymerCount = 0;
  private Polymer[] polymers = new Polymer[8];


  Model(Mmset mmset, int modelIndex, int modelNumber, String modelTag) {
    this.mmset = mmset;
    this.modelIndex = modelIndex;
    this.modelNumber = modelNumber;
    this.modelTag = modelTag;
  }

  void setSymmetryAtomInfo(int atomIndex, int atomCount) {
    preSymmetryAtomIndex = atomIndex;
    preSymmetryAtomCount = atomCount;
  }
  
  void setNAltLocs(int nAltLocs) {
    this.nAltLocs = nAltLocs;  
  }
  
  void setNInsertions(int nInsertions) {
    this.nInsertions = nInsertions;  
  }
  
  void freeze() {
    //Logger.debug("Mmset.freeze() chainCount=" + chainCount);
    chains = (Chain[])ArrayUtil.setLength(chains, chainCount);
    for (int i = 0; i < chainCount; ++i)
      chains[i].freeze();
    polymers = (Polymer[])ArrayUtil.setLength(polymers, polymerCount);
  }

  void clearStructures() {
    chainCount = 0;
    chains = new Chain[8];
    polymerCount = 0;
    polymers = new Polymer[8];
  }
  
  void addSecondaryStructure(byte type,
                             char startChainID, int startSeqcode,
                             char endChainID, int endSeqcode) {
    for (int i = polymerCount; --i >= 0; ) {
      Polymer polymer = polymers[i];
      polymer.addSecondaryStructure(type, startChainID, startSeqcode,
                                    endChainID, endSeqcode);
    }
  }

  void calculateStructures() {
    //Logger.debug("Model.calculateStructures");
    for (int i = polymerCount; --i >= 0; )
      polymers[i].calculateStructures();
  }

  void setConformation(BitSet bsConformation) {
    //Logger.debug("Model.calculateStructures");
    for (int i = polymerCount; --i >= 0; )
      polymers[i].setConformation(bsConformation, nAltLocs);
  }

  int getChainCount() {
    return chainCount;
  }

  int getPolymerCount() {
    return polymerCount;
  }

  void calcSelectedGroupsCount(BitSet bsSelected) {
    for (int i = chainCount; --i >= 0; )
      chains[i].calcSelectedGroupsCount(bsSelected);
  }

  void calcSelectedMonomersCount(BitSet bsSelected) {
    for (int i = polymerCount; --i >= 0; )
      polymers[i].calcSelectedMonomersCount(bsSelected);
  }

  void selectSeqcodeRange(int seqcodeA, int seqcodeB, BitSet bs) {
    for (int i = chainCount; --i >= 0; )
      chains[i].selectSeqcodeRange(seqcodeA, seqcodeB, bs);
  }

  int getGroupCount() {
    int groupCount = 0;
    for (int i = chainCount; --i >= 0; )
      groupCount += chains[i].getGroupCount();
    return groupCount;
  }

  Chain getChain(char chainID) {
    for (int i = chainCount; --i >= 0; ) {
      Chain chain = chains[i];
      if (chain.chainID == chainID)
        return chain;
    }
    return null;
  }

  Chain getChain(int i) {
    return (i < chainCount ? chains[i] : null);
  }

  Chain getOrAllocateChain(char chainID) {
    //Logger.debug("chainID=" + chainID + " -> " + (chainID + 0));
    Chain chain = getChain(chainID);
    if (chain != null)
      return chain;
    if (chainCount == chains.length)
      chains = (Chain[])ArrayUtil.doubleLength(chains);
    return chains[chainCount++] = new Chain(mmset.frame, this, chainID);
  }

  void addPolymer(Polymer polymer) {
    if (polymerCount == polymers.length)
      polymers = (Polymer[])ArrayUtil.doubleLength(polymers);
    polymers[polymerCount++] = polymer;
  }

  Polymer getPolymer(int polymerIndex) {
    return polymers[polymerIndex];
  }

  void calcHydrogenBonds(BitSet bsA, BitSet bsB) {
    for (int i = polymerCount; --i >= 0; )
      polymers[i].calcHydrogenBonds(bsA, bsB);
  }
  
}
