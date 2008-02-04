/* $RCSfile: Halos.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:07 $
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

package org.jmol.viewer;

import java.util.BitSet;

import org.jmol.g3d.Graphics3D;

class Halos extends AtomShape {

  short colixSelection = Graphics3D.USE_PALETTE;

  void setProperty(String propertyName, Object value, BitSet bs) {
    if ("translucency" == propertyName)
      return;
    if ("argbSelection" == propertyName) {
      colixSelection = Graphics3D.getColix(((Integer)value).intValue());
      return;
    }
    super.setProperty(propertyName, value, bs);
  }

  void setVisibilityFlags() {
    BitSet bsSelected = (viewer.getSelectionHaloEnabled() ? viewer
        .getSelectionSet() : null);
    for (int i = atomCount; --i >= 0;) {
      boolean isVisible = bsSelected != null && bsSelected.get(i)
          || (mads != null && mads[i] != 0);
      atoms[i].setShapeVisibility(myVisibilityFlag, isVisible);
    }
  }
  
  String getShapeState() {
    return super.getShapeState()
        + (colixSelection == Graphics3D.USE_PALETTE ? "" 
            : getColorCommand("selectionHalos", colixSelection) + ";\n");
  }
}
