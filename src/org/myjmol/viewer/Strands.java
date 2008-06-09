/* $RCSfile: Strands.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:10 $
 * $Revision: 1.11 $
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
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

package org.myjmol.viewer;
import java.util.BitSet;

class Strands extends Mps {

  /*==============================================================*
   * M. Carson and C.E. Bugg (1986)
   * Algorithm for Ribbon Models of Proteins. J.Mol.Graphics 4:121-122.
   * http://sgce.cbse.uab.edu/carson/papers/ribbons86/ribbons86.html
   *==============================================================*/

  int strandCount = 5;

  Mps.MpsShape allocateMpspolymer(Polymer polymer) {
    return new Schain(polymer);
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    initialize();
    if ("strandCount" == propertyName) {
      strandCount = Math.min(20, Math.max(0, ((Integer) value).intValue()));
      return;
    }
    super.setProperty(propertyName, value, bs);
  }

  class Schain extends Mps.MpsShape {
    Schain(Polymer polymer) {
      super(polymer, -2, 3000, 800, 5000);
    }
  }
  
  String getShapeState() {
    return "set strandCount " + strandCount + ";\n"
      + super.getShapeState();
  }
}
