/* $RCSfile: AlphaMonomer.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:12 $
 * $Revision: 1.12 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

class AlphaMonomer extends Monomer {

  final static byte[] alphaOffsets = { 0 };

  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstIndex, int lastIndex,
                        int[] specialAtomIndexes, Atom[] atoms) {
    if (firstIndex != lastIndex ||
        specialAtomIndexes[JmolConstants.ATOMID_ALPHA_CARBON] != firstIndex)
      return null;
    return new AlphaMonomer(chain, group3, seqcode,
                            firstIndex, lastIndex, alphaOffsets);
  }
  
  ////////////////////////////////////////////////////////////////

  AlphaMonomer(Chain chain, String group3, int seqcode,
               int firstAtomIndex, int lastAtomIndex,
               byte[] offsets) {
    super(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
  }

  boolean isAlphaMonomer() { return true; }

  ProteinStructure proteinStructure;
  void setStructure(ProteinStructure proteinStructure) {
    this.proteinStructure = proteinStructure;
  }

  ProteinStructure getProteinStructure() { return proteinStructure; }

  byte getProteinStructureType() {
    return proteinStructure == null ? 0 : proteinStructure.type;
  }

  boolean isHelix() {
    return proteinStructure != null &&
      proteinStructure.type == JmolConstants.PROTEIN_STRUCTURE_HELIX;
  }

  boolean isSheet() {
    return proteinStructure != null &&
      proteinStructure.type == JmolConstants.PROTEIN_STRUCTURE_SHEET;
  }

  Atom getAtom(byte specialAtomID) {
    return (specialAtomID == JmolConstants.ATOMID_ALPHA_CARBON
            ? getLeadAtom()
            : null);
  }

  Point3f getAtomPoint(byte specialAtomID) {
    return (specialAtomID == JmolConstants.ATOMID_ALPHA_CARBON
            ? getLeadAtomPoint()
            : null);
  }

  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    if (possiblyPreviousMonomer == null)
      return true;
    if (! (possiblyPreviousMonomer instanceof AlphaMonomer))
      return false;
    float distance =
      getLeadAtomPoint().distance(possiblyPreviousMonomer.getLeadAtomPoint());
    // jan reichert in email to miguel on 10 May 2004 said 4.2 looked good
    return distance <= 4.2f;
  }

}
