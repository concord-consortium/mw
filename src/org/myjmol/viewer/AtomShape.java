/* $RCSfile: AtomShape.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:11 $
 * $Revision: 1.1 $

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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.myjmol.viewer;

import java.util.BitSet;
import java.util.Hashtable;

import org.myjmol.g3d.Graphics3D;
import org.myjmol.util.ArrayUtil;

class AtomShape extends Shape {

  // Balls, Dots, Halos, Labels, Polyhedra, Stars, Vectors
  
  short[] mads;
  short[] colixes;
  byte[] paletteIDs;
  BitSet bsSizeSet;
  BitSet bsColixSet;
  int atomCount;
  Atom[] atoms;
  boolean isActive;
  
  void initShape() {
    atomCount = frame.atomCount;
    atoms = frame.atoms;  
  }
  
  void setSize(int size, BitSet bsSelected) {
    //Halos Stars Vectors only
    isActive = true;
    if (bsSizeSet == null)
      bsSizeSet = new BitSet();
    boolean isVisible = (size != 0);
    for (int i = atomCount; --i >= 0;)
      if (bsSelected.get(i)) {
        if (mads == null)
          mads = new short[atomCount];
        Atom atom = atoms[i];
        mads[i] = atom.convertEncodedMad(size);
        bsSizeSet.set(i, isVisible);
        atom.setShapeVisibility(myVisibilityFlag, isVisible);
      }
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    if ("color" == propertyName) {
      isActive = true;
      short colix = Graphics3D.getColix(value);
      if (bsColixSet == null)
        bsColixSet = new BitSet();
      byte pid = JmolConstants.pidOf(value);
      for (int i = atomCount; --i >= 0; )
        if (bs.get(i))
          setColixAndPalette(colix, pid, i);
      return;
    }
    if ("translucency" == propertyName) {
      isActive = true;
      boolean isTranslucent = ("translucent" == value);
      for (int i = atomCount; --i >= 0; )
        if (bs.get(i)) {
          if (colixes == null) {
            colixes = new short[atomCount];
            paletteIDs = new byte[atomCount];
          }
          colixes[i] = Graphics3D.getColixTranslucent(colixes[i], isTranslucent);
          if (isTranslucent)
            bsColixSet.set(i);
        }
      return;
    }
  }

  void setColixAndPalette(short colix, byte paletteID, int atomIndex) {
    if (colixes == null || atomIndex >= colixes.length) {
      if (colix == Graphics3D.INHERIT)
        return;
      colixes = ArrayUtil.ensureLength(colixes, atomIndex + 1);
      paletteIDs = ArrayUtil.ensureLength(paletteIDs, atomIndex + 1);
    }
    if (bsColixSet == null)
      bsColixSet = new BitSet();
    colixes[atomIndex] = colix = setColix(colix, paletteID, atomIndex);
    bsColixSet.set(atomIndex, colix != Graphics3D.INHERIT);    
    paletteIDs[atomIndex] = paletteID;
  }
  
  void setModelClickability() {
    if (!isActive)
      return;
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.shapeVisibilityFlags & myVisibilityFlag) == 0
          || frame.bsHidden.get(i))
        continue;
      atom.clickabilityFlags |= myVisibilityFlag;
    }
  }

  String getShapeState() {
    if (!isActive)
      return "";
    Hashtable temp = new Hashtable();
    Hashtable temp2 = new Hashtable();
    String type = JmolConstants.shapeClassBases[shapeID];
    for (int i = atomCount; --i >= 0;) {
      if (bsSizeSet != null && bsSizeSet.get(i))
        setStateInfo(temp, i, type + " " + (mads[i] / 2000f));
      if (bsColixSet != null && bsColixSet.get(i))
        setStateInfo(temp2, i, getColorCommand(type, paletteIDs[i], colixes[i]));
    }
    return getShapeCommands(temp, temp2, atomCount);
  }
}
