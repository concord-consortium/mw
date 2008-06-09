/* $RCSfile: FontLineShape.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:06 $
 * $Revision: 1.1 $
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

import org.myjmol.g3d.Font3D;
import org.myjmol.g3d.Graphics3D;

import java.util.BitSet;

abstract class FontLineShape extends Shape {
  
  // Axes, Bbcage, Frank, Uccage

  short mad;
  short colix;
  short bgcolix;
  Font3D font3d;
  String myType;

  void initShape() {
  }
  
  void setSize(int size, BitSet bsSelected) {
    this.mad = (short)size;
  }
  
  void setProperty(String propertyName, Object value, BitSet bs) {
    if ("color" == propertyName) {
      colix = Graphics3D.getColix(value);
      return;
    }

    if ("font" == propertyName) {
      font3d = (Font3D)value;
      return;
    }

    if ("bgcolor" == propertyName) {
      bgcolix = Graphics3D.getColix(value);
      return;
    }
  }
  
  String getShapeState() {
    StringBuffer s = new StringBuffer();
    appendCmd(s, myType
        + (mad == 0 ? " off" : mad == 1 ? " on" : mad == -1 ? " dotted" 
            : mad < 20 ? " " + mad : " " + (mad / 2000f)));
    appendCmd(s, getColorCommand(myType, colix));
    appendCmd(s, getFontCommand(myType, font3d));
    return s.toString();
  }
}
