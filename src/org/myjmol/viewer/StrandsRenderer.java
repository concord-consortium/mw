/* $RCSfile: StrandsRenderer.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:07 $
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

import javax.vecmath.Point3i;

class StrandsRenderer extends MpsRenderer {

  int strandCount;
  float strandSeparation;
  float baseOffset;

  void renderMpspolymer(Mps.MpsShape mpspolymer) {
    if (wingVectors == null)
      return;
    setStrandCount(((Strands) shape).strandCount);
    render1();
  }
  
  void setStrandCount(int strandCount) {
    this.strandCount = strandCount;
    strandSeparation = (strandCount <= 1) ? 0 : 1f / (strandCount - 1);
    baseOffset = (strandCount % 2 == 0 ? strandSeparation / 2
        : strandSeparation);
  }

  void render1() {
    Point3i[] screens;
    for (int i = strandCount >> 1; --i >= 0;) {
      float f = (i * strandSeparation) + baseOffset;
      screens = calcScreens(f);
      render1Strand(screens);
      viewer.freeTempScreens(screens);
      screens = calcScreens(-f);
      render1Strand(screens);
      viewer.freeTempScreens(screens);
    }
    if (strandCount % 2 == 1) {
      screens = calcScreens(0f);
      render1Strand(screens);
      viewer.freeTempScreens(screens);
    }
  }

  void render1Strand(Point3i[] screens) {
    for (int i = monomerCount; --i >= 0;)
      if (bsVisible.get(i))
        renderHermiteCylinder(screens, i);
  }
}
