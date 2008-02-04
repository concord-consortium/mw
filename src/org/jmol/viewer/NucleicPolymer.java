/* $RCSfile: NucleicPolymer.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:07 $
 * $Revision: 1.11 $
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

import java.util.BitSet;


class NucleicPolymer extends Polymer {

  NucleicPolymer(Monomer[] monomers) {
    super(monomers);
  }

  Atom getNucleicPhosphorusAtom(int monomerIndex) {
    return monomers[monomerIndex].getLeadAtom();
  }

  boolean hasWingPoints() { return true; }

  void calcHydrogenBonds(BitSet bsA, BitSet bsB) {
    for (int i = model.getPolymerCount(); --i >= 0; ) {
      Polymer otherPolymer = model.getPolymer(i);
      if (otherPolymer == this) // don't look at self
        continue;
      if (otherPolymer == null || !(otherPolymer instanceof NucleicPolymer))
        continue;
      lookForHbonds((NucleicPolymer)otherPolymer, bsA, bsB);
    }
  }

  void lookForHbonds(NucleicPolymer other, BitSet bsA, BitSet bsB) {
    //Logger.debug("NucleicPolymer.lookForHbonds()");
    for (int i = monomerCount; --i >= 0; ) {
      NucleicMonomer myNucleotide = (NucleicMonomer)monomers[i];
      if (! myNucleotide.isPurine())
        continue;
      Atom myN1 = myNucleotide.getN1();
      Atom bestN3 = null;
      float minDist2 = 5*5;
      NucleicMonomer bestNucleotide = null;
      for (int j = other.monomerCount; --j >= 0; ) {
        NucleicMonomer otherNucleotide = (NucleicMonomer)other.monomers[j];
        if (! otherNucleotide.isPyrimidine())
          continue;
        Atom otherN3 = otherNucleotide.getN3();
        float dist2 = myN1.distanceSquared(otherN3);
        if (dist2 < minDist2) {
          bestNucleotide = otherNucleotide;
          bestN3 = otherN3;
          minDist2 = dist2;
        }
      }
      if (bestN3 != null) {
        createHydrogenBond(myN1, bestN3, bsA, bsB);
        if (myNucleotide.isGuanine()) {
          createHydrogenBond(myNucleotide.getN2(),
                             bestNucleotide.getO2(), bsA, bsB);
          createHydrogenBond(myNucleotide.getO6(),
                             bestNucleotide.getN4(), bsA, bsB);
        } else {
          createHydrogenBond(myNucleotide.getN6(),
                             bestNucleotide.getO4(), bsA, bsB);
        }
      }
    }
  }

  void createHydrogenBond(Atom atom1, Atom atom2, BitSet bsA, BitSet bsB) {
    //Logger.debug("createHydrogenBond:" +
    // atom1.getAtomNumber() + "<->" + atom2.getAtomNumber());
    if (atom1 != null && atom2 != null) {
      Frame frame = model.mmset.frame;
      frame.addHydrogenBond(atom1, atom2, JmolConstants.BOND_H_NUCLEOTIDE, bsA, bsB);
    }
  }
}
