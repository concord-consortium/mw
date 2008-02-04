/* $RCSfile: AminoMonomer.java,v $
 * $Author: qxie $
 * $Date: 2006-12-12 00:32:11 $
 * $Revision: 1.13 $
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


import javax.vecmath.Point3f;
import org.jmol.util.Logger;

class AminoMonomer extends AlphaMonomer {

  // negative values are optional
  final static byte[] interestingAminoAtomIDs = {
    JmolConstants.ATOMID_ALPHA_CARBON,      // 0 CA alpha carbon
    ~JmolConstants.ATOMID_CARBONYL_OXYGEN,   // 1 O wing man
    JmolConstants.ATOMID_AMINO_NITROGEN,    // 2 N
    JmolConstants.ATOMID_CARBONYL_CARBON,   // 3 C  point man
    ~JmolConstants.ATOMID_TERMINATING_OXT,  // 4 OXT
    ~JmolConstants.ATOMID_O1,               // 5 O1
  };

  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstAtomIndex, int lastAtomIndex,
                        int[] specialAtomIndexes, Atom[] atoms) {
    byte[] offsets = scanForOffsets(firstAtomIndex, specialAtomIndexes,
                                    interestingAminoAtomIDs);
    if (offsets == null)
      return null;
    if (specialAtomIndexes[JmolConstants.ATOMID_CARBONYL_OXYGEN] < 0) {
      int carbonylOxygenIndex = specialAtomIndexes[JmolConstants.ATOMID_O1];
      Logger.debug("I see someone who does not have a carbonyl oxygen");
      if (carbonylOxygenIndex < 0)
        return null;
      offsets[1] = (byte)(carbonylOxygenIndex - firstAtomIndex);
    }
    if (atoms[firstAtomIndex].isHetero() && !isBondedCorrectly(firstAtomIndex, offsets, atoms)) 
      return null;
    AminoMonomer aminoMonomer =
      new AminoMonomer(chain, group3, seqcode,
                       firstAtomIndex, lastAtomIndex, offsets);
    return aminoMonomer;
  }

  static boolean isBondedCorrectly(int offset1, int offset2,
                                   int firstAtomIndex,
                                   byte[] offsets, Atom[] atoms) {
    int atomIndex1 = firstAtomIndex + (offsets[offset1] & 0xFF);
    int atomIndex2 = firstAtomIndex + (offsets[offset2] & 0xFF);
    if (atomIndex1 >= atomIndex2)
      return false;
    return atoms[atomIndex1].isBonded(atoms[atomIndex2]);
  }

  static boolean isBondedCorrectly(int firstAtomIndex, byte[] offsets,
                                 Atom[] atoms) {
    return (isBondedCorrectly(2, 0, firstAtomIndex, offsets, atoms) &&
            isBondedCorrectly(0, 3, firstAtomIndex, offsets, atoms) &&
            isBondedCorrectly(3, 1, firstAtomIndex, offsets, atoms));
  }
  
  ////////////////////////////////////////////////////////////////

  AminoMonomer(Chain chain, String group3, int seqcode,
               int firstAtomIndex, int lastAtomIndex,
               byte[] offsets) {
    super(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
  }

  boolean isAminoMonomer() { return true; }

  Atom getNitrogenAtom() {
    return getAtomFromOffsetIndex(2);
  }

  Point3f getNitrogenAtomPoint() {
    return getAtomPointFromOffsetIndex(2);
  }

  Atom getCarbonylCarbonAtom() {
    return getAtomFromOffsetIndex(3);
  }

  Point3f getCarbonylCarbonAtomPoint() {
    return getAtomPointFromOffsetIndex(3);
  }

  Atom getCarbonylOxygenAtom() {
    return getWingAtom();
  }

  Point3f getCarbonylOxygenAtomPoint() {
    return getWingAtomPoint();
  }

  Atom getInitiatorAtom() {
    return getNitrogenAtom();
  }

  Atom getTerminatorAtom() {
    return getAtomFromOffsetIndex(offsets[4] != -1 ? 4 : 3);
  }

  ////////////////////////////////////////////////////////////////

  Atom getAtom(byte specialAtomID) {
    return getSpecialAtom(interestingAminoAtomIDs, specialAtomID);
  }

  Point3f getAtomPoint(byte specialAtomID) {
    return getSpecialAtomPoint(interestingAminoAtomIDs, specialAtomID);
  }

  ////////////////////////////////////////////////////////////////

  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    if (possiblyPreviousMonomer == null)
      return true;
    if (! (possiblyPreviousMonomer instanceof AminoMonomer))
      return false;
    AminoMonomer other = (AminoMonomer)possiblyPreviousMonomer;
    return other.getCarbonylCarbonAtom().isBonded(getNitrogenAtom());
  }

  ////////////////////////////////////////////////////////////////

  void findNearestAtomIndex(int x, int y, Closest closest,
                            short madBegin, short madEnd) {
    Frame frame = chain.frame;
    Atom competitor = closest.atom;
    Atom nitrogen = getNitrogenAtom();
    short marBegin = (short) (madBegin / 2);
    if (marBegin < 1200)
      marBegin = 1200;
    if (nitrogen.screenZ == 0)
      return;
    int radiusBegin = frame.viewer.scaleToScreen(nitrogen.screenZ, marBegin);
    if (radiusBegin < 4)
      radiusBegin = 4;
    Atom ccarbon = getCarbonylCarbonAtom();
    short marEnd = (short) (madEnd / 2);
    if (marEnd < 1200)
      marEnd = 1200;
    int radiusEnd = frame.viewer.scaleToScreen(nitrogen.screenZ, marEnd);
    if (radiusEnd < 4)
      radiusEnd = 4;
    Atom alpha = getLeadAtom();
    if (frame.isCursorOnTopOf(alpha, x, y, (radiusBegin + radiusEnd) / 2,
        competitor)
        || frame.isCursorOnTopOf(nitrogen, x, y, radiusBegin, competitor)
        || frame.isCursorOnTopOf(ccarbon, x, y, radiusEnd, competitor))
      closest.atom = alpha;
  }
}
