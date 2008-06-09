/* $RCSfile: Backbone.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:11 $
 * $Revision: 1.12 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.myjmol.viewer;

import java.util.BitSet;

class Backbone extends Mps {

  Mps.MpsShape allocateMpspolymer(Polymer polymer) {
    return new Bbpolymer(polymer);
  }

  class Bbpolymer extends Mps.MpsShape {

    Bbpolymer(Polymer polymer) {
      super(polymer, 1, 1500, 500, 2000);
    }

    void setMad(short mad, BitSet bsSelected) {
      boolean bondSelectionModeOr = viewer.getBondSelectionModeOr();
      int[] atomIndices = polymer.getLeadAtomIndices();
      // note that i is initialized to monomerCount - 1
      // in order to skip the last atom
      // but it is picked up within the loop by looking at i+1
      boolean isVisible = (mad != 0);
      for (int i = monomerCount - 1; --i >= 0;) {
        int index1 = atomIndices[i];
        int index2 = atomIndices[i + 1];
        boolean isAtom1 = bsSelected.get(index1);
        boolean isAtom2 = bsSelected.get(index2);
        if (isAtom1 && isAtom2 || bondSelectionModeOr && (isAtom1 || isAtom2)) {
          monomers[i].setShapeVisibility(myVisibilityFlag, isVisible);
          Atom atomA = frame.getAtomAt(index1);
          Atom atomB = frame.getAtomAt(index2);
          boolean wasVisible = (mads[i] != 0); 
          if (wasVisible != isVisible) {
            atomA.addDisplayedBackbone(myVisibilityFlag, isVisible);
            atomB.addDisplayedBackbone(myVisibilityFlag, isVisible);
          }
          mads[i] = mad;
        }
      }
    }

    void setModelClickability() {
      int[] atomIndices = polymer.getLeadAtomIndices();
      for (int i = monomerCount; --i >= 0; ) {
        Atom atom = frame.getAtomAt(atomIndices[i]);
        if (atom.nBackbonesDisplayed > 0 && !frame.bsHidden.get(i))
          atom.clickabilityFlags |= myVisibilityFlag;
      }
    }
  }
  
}
