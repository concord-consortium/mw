/* $RCSfile: Vectors.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:11 $
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

import java.util.BitSet;

class Vectors extends AtomShape {

  float scale = Float.NaN;
  
  void initShape() {
    super.initShape();
    if (!(isActive = frame.hasVibrationVectors))
      return;
  }

  void setProperty(String propertyName, Object value, BitSet bsSelected) {
    if (!isActive)
      return;
    if (propertyName == "scale") {
      scale = ((Float)value).floatValue();
      return;
    }
    super.setProperty(propertyName, value, bsSelected);
  }
  
  String getShapeState() {
    if (!isActive)
      return "";
    return super.getShapeState() 
    + (Float.isNaN(scale) ? "" : "vector scale " + scale +";\n");
  }
}
