/* $RCSfile: Sticks.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:10 $
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

import org.jmol.util.Logger;


import java.util.BitSet;
import java.util.Hashtable;

import org.jmol.g3d.Graphics3D;

class Sticks extends Shape {
  
  short myMask;  
  boolean reportAll;
  BitSet bsOrderSet;
  BitSet bsSizeSet;
  BitSet bsColixSet;
  
  void initShape() {
    myMask = JmolConstants.BOND_COVALENT_MASK;
    reportAll = false;
  }

  void setSize(int size, BitSet bsSelected) {
    if (bsSizeSet == null)
      bsSizeSet = new BitSet();
    boolean isBonds = viewer.isBondSelection();
    BondIterator iter = (isBonds ? frame.getBondIterator(bsSelected) : frame
        .getBondIterator(myMask, bsSelected));
    if(iter == null) return; // XIE
    short mad = (short) size;
    while (iter.hasNext()) {
      bsSizeSet.set(iter.nextIndex());
      iter.next().setMad(mad);
    }
  }
  
  void setProperty(String propertyName, Object value, BitSet bsSelected) {
    Logger.debug(propertyName + " " + value + " " + bsSelected);
    boolean isBonds = viewer.isBondSelection();

    if ("reportAll" == propertyName) {
      // when connections are restored, all we can do is report them all
      reportAll = true;
      return;
    }

    if ("reset" == propertyName) {
      // all bonds have been deleted -- start over
      bsOrderSet = null;
      bsSizeSet = null;
      bsColixSet = null;
      return;
    }

    if ("bondOrder" == propertyName) {
      if (bsOrderSet == null)
        bsOrderSet = new BitSet();
      short order = ((Short) value).shortValue();
      BondIterator iter = (isBonds ? frame.getBondIterator(bsSelected) : frame
          .getBondIterator(myMask, bsSelected));
      while (iter.hasNext()) {
        bsOrderSet.set(iter.nextIndex());
        iter.next().setOrder(order);
      }
      return;
    }
    if ("color" == propertyName) {
      if (bsColixSet == null)
        bsColixSet = new BitSet();
      short colix = Graphics3D.getColix(value);
      byte pid = JmolConstants.pidOf(value);
      if (pid == JmolConstants.PALETTE_TYPE) {
        //only for hydrogen bonds
        BondIterator iter = (isBonds ? frame.getBondIterator(bsSelected)
            : frame.getBondIterator(myMask, bsSelected));
        while (iter.hasNext()) {
          bsColixSet.set(iter.nextIndex());
          Bond bond = iter.next();
          bond.setColix(viewer.getColixHbondType(bond.order));
        }
        return;
      }
      if (colix == Graphics3D.USE_PALETTE)
        return; //palettes not implemented for bonds
      BondIterator iter = (isBonds ? frame.getBondIterator(bsSelected) : frame
          .getBondIterator(myMask, bsSelected));
      while (iter.hasNext()) {
        int iBond = iter.nextIndex();
        Bond bond = iter.next();
        bond.setColix(colix);
        bsColixSet.set(iBond, colix != Graphics3D.INHERIT);
      }
      return;
    }
    if ("translucency" == propertyName) {
      if (bsColixSet == null)
        bsColixSet = new BitSet();
      boolean isTranslucent = (((String) value).equals("translucent"));
      BondIterator iter = (isBonds ? frame.getBondIterator(bsSelected) : frame
          .getBondIterator(myMask, bsSelected));
      while (iter.hasNext()) {
        bsColixSet.set(iter.nextIndex());
        iter.next().setTranslucent(isTranslucent);
      }
      return;
    }
    //better not be here
    super.setProperty(propertyName, value, bsSelected);
  }

  void setModelClickability() {
    Bond[] bonds = frame.bonds;
    for (int i = frame.bondCount; --i >= 0;) {
      Bond bond = bonds[i];
      if ((bond.shapeVisibilityFlags & myVisibilityFlag) == 0
          || frame.bsHidden.get(bond.atom1.atomIndex)
          || frame.bsHidden.get(bond.atom2.atomIndex))
        continue;
      bond.atom1.clickabilityFlags |= myVisibilityFlag;
      bond.atom2.clickabilityFlags |= myVisibilityFlag;
    }
  }
  
  String getShapeState() {
    Hashtable temp = new Hashtable();
    Bond[] bonds = frame.bonds;
    for (int i = frame.bondCount; --i >= 0;) {
      Bond bond = bonds[i];
      if (reportAll || bsSizeSet != null && bsSizeSet.get(i))
        setStateInfo(temp, i, "wireframe "
            + (bond.mad == 1 ? "on" : "" + (bond.mad / 2000f)));
      if (reportAll || bsOrderSet != null && bsOrderSet.get(i))
        setStateInfo(temp, i, "bondOrder "
            + JmolConstants.getBondOrderNameFromOrder(bond.order));
      if (bsColixSet != null && bsColixSet.get(i))
        setStateInfo(temp, i, getColorCommand("bonds", bond.colix));
    }
    return getShapeCommands(temp, null, -1, "select BONDS") + "\n";
  }  
}
