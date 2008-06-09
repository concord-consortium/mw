/* $RCSfile: NucleicMonomer.java,v $
 * $Author: qxie $
 * $Date: 2006-12-12 00:32:11 $
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
package org.myjmol.viewer;

import javax.vecmath.Point3f;

class NucleicMonomer extends PhosphorusMonomer {

  // negative values are optional
  final static byte[] interestingNucleicAtomIDs = {
    ~JmolConstants.ATOMID_NUCLEIC_PHOSPHORUS,    // 0 P  the lead, phosphorus
    JmolConstants.ATOMID_NUCLEIC_WING,           // 1 the wing man, c6

    ~JmolConstants.ATOMID_RNA_O2PRIME, // 2  O2' for RNA

    JmolConstants.ATOMID_C5,   //  3 C5
    JmolConstants.ATOMID_C6,   //  4 C6
    JmolConstants.ATOMID_N1,   //  5 N1
    JmolConstants.ATOMID_C2,   //  6 C2
    JmolConstants.ATOMID_N3,   //  7 N3
    JmolConstants.ATOMID_C4,   //  8 C4

    ~JmolConstants.ATOMID_O2,  //  9 O2

    ~JmolConstants.ATOMID_N7,  // 10 N7
    ~JmolConstants.ATOMID_C8,  // 11 C8
    ~JmolConstants.ATOMID_N9,  // 12 C9

    ~JmolConstants.ATOMID_O4,  // 13 O4   U (& ! C5M)
    ~JmolConstants.ATOMID_O6,  // 14 O6   I (& ! N2)
    ~JmolConstants.ATOMID_N4,  // 15 N4   C
    ~JmolConstants.ATOMID_C5M, // 16 C5M  T
    ~JmolConstants.ATOMID_N6,  // 17 N6   A
    ~JmolConstants.ATOMID_N2,  // 18 N2   G
    ~JmolConstants.ATOMID_S4,  // 19 S4   tU

    ~JmolConstants.ATOMID_H5T_TERMINUS, // 20 H5T terminus
    ~JmolConstants.ATOMID_O5T_TERMINUS, // 21 O5T terminus
    JmolConstants.ATOMID_O5_PRIME,      // 22 O5' terminus

    ~JmolConstants.ATOMID_H3T_TERMINUS, // 23 H3T terminus
    JmolConstants.ATOMID_O3_PRIME,      // 24 O3' terminus
    ~JmolConstants.ATOMID_NUCLEIC_PHOSPHORUS,    // 25 P phosphorus
    JmolConstants.ATOMID_C3_PRIME,              // 26 C3'
  };

  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstAtomIndex, int lastAtomIndex,
                        int[] specialAtomIndexes, Atom[] atoms) {

    byte[] offsets = scanForOffsets(firstAtomIndex,
                                    specialAtomIndexes,
                                    interestingNucleicAtomIDs);

    if (offsets == null)
      return null;
    NucleicMonomer nucleicMonomer =
      new NucleicMonomer(chain, group3, seqcode,
                         firstAtomIndex, lastAtomIndex, offsets);
    return nucleicMonomer;
  }

  ////////////////////////////////////////////////////////////////

  NucleicMonomer(Chain chain, String group3, int seqcode,
                 int firstAtomIndex, int lastAtomIndex,
                 byte[] offsets) {
    super(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
    if (offsets[0] == -1) {
      byte leadOffset = offsets[20];
      if (leadOffset == -1)
        leadOffset = offsets[21];
      if (leadOffset == -1)
        leadOffset = offsets[22];
      offsets[0] = leadOffset;
    }
    this.hasRnaO2Prime = offsets[2] != -1;
    this.isPyrimidine = offsets[9] != -1;
    this.isPurine =
      offsets[10] != -1 && offsets[11] != -1 && offsets[12] != -1;
  }

  boolean hasRnaO2Prime;

  boolean isNucleicMonomer() { return true; }

  boolean isDna() { return ! hasRnaO2Prime || chain.isDna(); }

  boolean isRna() { return hasRnaO2Prime || chain.isRna(); }

  boolean isPurine() { return isPurine; }

  boolean isPyrimidine() { return isPyrimidine; }

  boolean isGuanine() { return offsets[18] != -1; }

  byte getProteinStructureType() {
    return (hasRnaO2Prime
            ? JmolConstants.PROTEIN_STRUCTURE_RNA
            : JmolConstants.PROTEIN_STRUCTURE_DNA);
  }

  ////////////////////////////////////////////////////////////////

  Atom getN1() {
    return getAtomFromOffsetIndex(5);
  }

  Atom getN3() {
    return getAtomFromOffsetIndex(7);
  }

  Atom getN2() {
    return getAtomFromOffsetIndex(18);
  }

  Atom getO2() {
    return getAtomFromOffsetIndex(9);
  }

  Atom getO6() {
    return getAtomFromOffsetIndex(14);
  }

  Atom getN4() {
    return getAtomFromOffsetIndex(15);
  }

  Atom getN6() {
    return getAtomFromOffsetIndex(17);
  }

  Atom getO4() {
    return getAtomFromOffsetIndex(13);
  }

  Atom getAtom(byte specialAtomID) {
    return getSpecialAtom(interestingNucleicAtomIDs, specialAtomID);
  }

  Point3f getAtomPoint(byte specialAtomID) {
    return getSpecialAtomPoint(interestingNucleicAtomIDs, specialAtomID);
  }

  Atom getTerminatorAtom() {
    return getAtomFromOffsetIndex(offsets[23] != -1 ? 23 : 24);
  }

  Atom getO3PrimeAtom() {
    return getAtomFromOffsetIndex(24);
  }

  Atom getPhosphorusAtom() {
    return getAtomFromOffsetIndex(25);
  }

  Atom getO5PrimeAtom() {
    return getAtomFromOffsetIndex(22);
  }

  Atom getC3PrimeAtom() {
    return getAtomFromOffsetIndex(26);
  }

  void getBaseRing6Points(Point3f[] ring6Points) {
    for (int i = 6; --i >= 0; ) {
      Atom atom = getAtomFromOffsetIndex(i + 3);
      ring6Points[i] = atom;
    }
  }

  final static byte[] ring5OffsetIndexes = {3, 10, 11, 12, 8};

  boolean maybeGetBaseRing5Points(Point3f[] ring5Points) {
    if (isPurine)
      for (int i = 5; --i >= 0; ) {
        Atom atom = getAtomFromOffsetIndex(ring5OffsetIndexes[i]);
        ring5Points[i] = atom;
      }
    return isPurine;
  }

  ////////////////////////////////////////////////////////////////

  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    if (possiblyPreviousMonomer == null)
      return true;
    Atom myPhosphorusAtom = getPhosphorusAtom();
    if (myPhosphorusAtom == null)
      return false;
    if (! (possiblyPreviousMonomer instanceof NucleicMonomer))
      return false;
    NucleicMonomer other = (NucleicMonomer)possiblyPreviousMonomer;
    return other.getO3PrimeAtom().isBonded(myPhosphorusAtom);
  }

  ////////////////////////////////////////////////////////////////

  void findNearestAtomIndex(int x, int y, Closest closest,
                            short madBegin, short madEnd) {
    Frame frame = chain.frame;
    Atom competitor = closest.atom;
    Atom lead = getLeadAtom();
    Atom o5prime = getO5PrimeAtom();
    Atom c3prime = getC3PrimeAtom();
    short mar = (short)(madBegin / 2);
    if (mar < 1900)
      mar = 1900;
    int radius = frame.viewer.scaleToScreen(lead.screenZ, mar);
    if (radius < 4)
      radius = 4;
    if (frame.isCursorOnTopOf(lead, x, y, radius, competitor)
        || frame.isCursorOnTopOf(o5prime, x, y, radius, competitor)
        || frame.isCursorOnTopOf(c3prime, x, y, radius, competitor))
      closest.atom = lead;
  }
  
  void setModelClickability() {
    Atom atom;
    atom = getLeadAtom();
    if (chain.frame.bsHidden.get(atom.atomIndex))
      return;
    int cartoonflag = JmolConstants.getShapeVisibilityFlag(JmolConstants.SHAPE_CARTOON);
    
    for (int i = 6; --i >= 0; ) {
      atom = getAtomFromOffsetIndex(i + 3);
      atom.clickabilityFlags |= cartoonflag;
    }
    if (isPurine)
      for (int i = 5; --i >= 0; ) {
        atom = getAtomFromOffsetIndex(ring5OffsetIndexes[i]);
        atom.clickabilityFlags |= cartoonflag;
    }
  }
}
