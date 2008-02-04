/* $RCSfile: Monomer.java,v $
 * $Author: qxie $
 * $Date: 2007-03-28 01:54:32 $
 * $Revision: 1.12 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

import org.jmol.util.Logger;

import java.util.Hashtable;
import java.util.BitSet;
import javax.vecmath.Point3f;

abstract class Monomer extends Group {

  Polymer polymer;

  final byte[] offsets;

  Monomer(Chain chain, String group3, int seqcode,
          int firstAtomIndex, int lastAtomIndex,
          byte[] interestingAtomOffsets) {
    super(chain, group3, seqcode, firstAtomIndex, lastAtomIndex);
    offsets = interestingAtomOffsets;
  }

  void setPolymer(Polymer polymer) {
    this.polymer = polymer;
  }

  int getPolymerLength() {
    return polymer == null ? 0 : polymer.monomerCount;
  }

  int getPolymerIndex() {
    return polymer == null ? -1 : polymer.getIndex(this);
  }

  ////////////////////////////////////////////////////////////////

  static byte[] scanForOffsets(int firstAtomIndex,
                               int[] specialAtomIndexes,
                               byte[] interestingAtomIDs) {
    /*
     * from validateAndAllocate in AminoMonomer or NucleicMonomer extensions
     * 
     * sets offsets for the FIRST conformation ONLY
     * (provided that the conformation is listed first in each atom case)
     *  
     *  specialAtomIndexes[] corrolates with JmolConstants.specialAtomNames[]
     *  and is set up back in the calling frame.distinguishAndPropagateGroups
     */
    int interestingCount = interestingAtomIDs.length;
    byte[] offsets = new byte[interestingCount];
    for (int i = interestingCount; --i >= 0; ) {
      int atomIndex;
      int atomID = interestingAtomIDs[i];
      // mth 2004 06 09
      // use ~ instead of - as the optional indicator
      // because I got hosed by a missing comma
      // in an interestingAtomIDs table
      if (atomID < 0) {
        atomIndex = specialAtomIndexes[~atomID]; // optional
      } else {
        atomIndex = specialAtomIndexes[atomID];  // required
        if (atomIndex < 0)
          return null;
      }
      int offset;
      if (atomIndex < 0)
        offset = 255;
      else {
        offset = atomIndex - firstAtomIndex;
        if (offset < 0 || offset > 254) {
          Logger.warn("Monomer.scanForOffsets i="+i+" atomID="+atomID+" atomIndex:"+atomIndex+" firstAtomIndex:"+firstAtomIndex+" offset out of 0-254 range. Groups aren't organized correctly. Is this really a protein?: "+offset);
          if (atomID < 0) {
            offset = 255; //it was optional anyway RMH
          } else {
            //throw new NullPointerException();
          }
        }
      }
      offsets[i] = (byte)offset;
    }
    return offsets;
  }

  ////////////////////////////////////////////////////////////////

  boolean isDna() { return false; }
  boolean isRna() { return false; }
  final boolean isProtein() {return this instanceof AlphaMonomer;}
  final boolean isNucleic() {return this instanceof PhosphorusMonomer;}

  ////////////////////////////////////////////////////////////////

  void setStructure(ProteinStructure proteinstructure) { }
  ProteinStructure getProteinStructure() { return null; }
  byte getProteinStructureType() { return 0; }
  boolean isHelix() { return false; }
  boolean isSheet() { return false; }

  ////////////////////////////////////////////////////////////////

  final Atom getAtomFromOffset(byte offset) {
    if (offset == -1)
      return null;
    return chain.frame.atoms[firstAtomIndex + (offset & 0xFF)];
  }

  final Point3f getAtomPointFromOffset(byte offset) {
    if (offset == -1)
      return null;
    return chain.frame.atoms[firstAtomIndex + (offset & 0xFF)];
  }

  ////////////////////////////////////////////////////////////////

  final Atom getAtomFromOffsetIndex(int offsetIndex) {
    if (offsetIndex > offsets.length)
      return null;
    int offset = offsets[offsetIndex] & 0xFF;
    if (offset == 255)
      return null;
    return chain.frame.atoms[firstAtomIndex + offset];
  }

  final Point3f getAtomPointFromOffsetIndex(int offsetIndex) {
    return getAtomFromOffsetIndex(offsetIndex);
  }

  final Atom getSpecialAtom(byte[] interestingIDs, byte specialAtomID) {
    for (int i = interestingIDs.length; --i >= 0; ) {
      int interestingID = interestingIDs[i];
      if (interestingID < 0)
        interestingID = -interestingID;
      if (specialAtomID == interestingID) {
        int offset = offsets[i] & 0xFF;
        if (offset == 255)
          return null;
        return chain.frame.atoms[firstAtomIndex + offset];
      }
    }
    return null;
  }

  final Point3f getSpecialAtomPoint(byte[] interestingIDs,
                                    byte specialAtomID) {
    for (int i = interestingIDs.length; --i >= 0; ) {
      int interestingID = interestingIDs[i];
      if (interestingID < 0)
        interestingID = -interestingID;
      if (specialAtomID == interestingID) {
        int offset = offsets[i] & 0xFF;
        if (offset == 255)
          return null;
        return chain.frame.atoms[firstAtomIndex + offset];
      }
    }
    return null;
  }

  Atom getAtom(byte specialAtomID) { return null; }

  Point3f getAtomPoint(byte specialAtomID) { return null; }

  final int getLeadAtomIndex() {
    return firstAtomIndex + (offsets[0] & 0xFF);
  }

  final Atom getLeadAtom() {
    return getAtomFromOffsetIndex(0);
  }

  final Point3f getLeadAtomPoint() {
    return getAtomPointFromOffsetIndex(0);
  }

  final Atom getWingAtom() {
    return getAtomFromOffsetIndex(1);
  }

  final Point3f getWingAtomPoint() {
    return getAtomPointFromOffsetIndex(1);
  }

  final Point3f getPointAtomPoint() {
    return getAtomPointFromOffsetIndex(3);
  }

  Atom getInitiatorAtom() {
    return getLeadAtom();
  }
  
  Atom getTerminatorAtom() {
    return getLeadAtom();
  }

  abstract boolean isConnectedAfter(Monomer possiblyPreviousMonomer);

  /**
   * Selects LeadAtom when this Monomer is clicked iff it is
   * closer to the user.
   * 
   * @param x
   * @param y
   * @param closest
   * @param madBegin
   * @param madEnd
   */
  void findNearestAtomIndex(int x, int y, Closest closest,
                            short madBegin, short madEnd) {
  }

  Hashtable getMyInfo() {
    Hashtable<String, Object> info = new Hashtable<String, Object>();
    info.put("chain", ""+chain.chainID);

    int seqNum = getSeqNumber();
    char insCode = getInsertionCode();
    if (seqNum > 0)      
      info.put("sequenceNumber",new Integer(seqNum));
    if (insCode != 0)      
      info.put("insertionCode","" + insCode);
    info.put("atomInfo1", chain.frame.atoms[firstAtomIndex].getInfo());
    info.put("atomInfo2", chain.frame.atoms[lastAtomIndex].getInfo());
    info.put("_apt1", new Integer(firstAtomIndex));
    info.put("_apt2", new Integer(lastAtomIndex));
    if (!Float.isNaN(phi))
      info.put("phi", new Float(phi));
    if (!Float.isNaN(psi))
      info.put("psi", new Float(psi));
    ProteinStructure structure = getProteinStructure();
    if(structure != null) {
      info.put("structureIndex", new Integer(structure.index));
      info.put("structureType", getStructureTypeName(structure.type));
    }
    info.put("shapeVisibilityFlags", new Integer(shapeVisibilityFlags));
    return info;
  }
  
  static String getStructureTypeName(byte type) {
    switch(type) {
    case JmolConstants.PROTEIN_STRUCTURE_HELIX:
      return "helix";
    case JmolConstants.PROTEIN_STRUCTURE_SHEET:
      return "sheet";
    case JmolConstants.PROTEIN_STRUCTURE_TURN:
      return "turn";
    case JmolConstants.PROTEIN_STRUCTURE_DNA:
      return "DNA";
    case JmolConstants.PROTEIN_STRUCTURE_RNA:
      return "RNA";
    default:
      return type+"?";
    }
  }

  final void updateOffsetsForAlternativeLocations(BitSet bsSelected,
                                                  int nAltLocInModel) {
    for (int offsetIndex = offsets.length; --offsetIndex >= 0;) {
      int offset = offsets[offsetIndex] & 0xFF;
      if (offset == 255)
        continue;
      int iThis = firstAtomIndex + offset;
      Atom atom = chain.frame.atoms[iThis];
      if (atom.alternateLocationID == 0)
        continue;
      // scan entire group list to ensure including all of
      // this atom's alternate conformation locations.
      // (PDB order may be AAAAABBBBB, not ABABABABAB)
      int nScan = lastAtomIndex - firstAtomIndex;
      for (int i = 1; i <= nScan; i++) {
        int iNew = iThis + i;
        if (iNew > lastAtomIndex)
          iNew -= nScan + 1;
        int offsetNew = iNew - firstAtomIndex;
        if (offsetNew < 0 || offsetNew > 255  || iNew == iThis
            || chain.frame.atomNames[iNew] != chain.frame.atomNames[iThis]
            || !bsSelected.get(iNew))
          continue;
        offsets[offsetIndex] = (byte) offsetNew;
        /*
        Logger.debug(iNew + " " + offsetNew + " old:" + offset + " "
            + chain.frame.atoms[iThis].getIdentity() + " " + " new:"
            + offsetNew + " " + chain.frame.atoms[iNew].getIdentity());
        */
        break;
      }
    }
  }
  
}
