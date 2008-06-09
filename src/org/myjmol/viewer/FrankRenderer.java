/* $RCSfile: FrankRenderer.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:13 $
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
package org.myjmol.viewer;

class FrankRenderer extends ShapeRenderer {

  void render() {
    Frank frank = (Frank) shape;
    if (frank.mad == 0)
      return;
    frank.calcMetrics();
    g3d.drawStringNoSlab(Frank.frankString, frank.font3d, frank.colix,
        frank.bgcolix, g3d.getRenderWidth() - frank.frankWidth
            - Frank.frankMargin, g3d.getRenderHeight() - frank.frankDescent
            - Frank.frankMargin, 0);
  }
}
