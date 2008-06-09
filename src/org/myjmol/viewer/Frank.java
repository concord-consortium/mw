/* $RCSfile: Frank.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:12 $
 * $Revision: 1.11 $
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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.myjmol.viewer;
import org.myjmol.g3d.Font3D;
import org.myjmol.g3d.Graphics3D;

import java.awt.FontMetrics;

class Frank extends FontLineShape {
  
  // Axes, Bbcage, Frank, Uccage

  final static String frankString = "Jmol";
  final static String defaultFontName = "SansSerif";
  final static String defaultFontStyle = "Bold";
  final static short defaultFontColix = Graphics3D.GRAY;
  final static int defaultFontSize = 16;
  final static int frankMargin = 4;

  Font3D currentMetricsFont3d;
  int frankWidth;
  int frankAscent;
  int frankDescent;


  void initShape() {
    mad = 1;
    myType = "frank";
    colix = defaultFontColix;
    font3d = g3d.getFont3D(defaultFontName, defaultFontStyle, defaultFontSize);
  }

  boolean wasClicked(int x, int y) {
    int width = g3d.getRenderWidth();
    int height = g3d.getRenderHeight();
    if (g3d.fullSceneAntialiasRendering()) {
      x *= 2;
      y *= 2;
    }
    return (width > 0 &&
            height > 0 &&
            x > width - frankWidth - frankMargin &&
            y > height - frankAscent - frankMargin);
  }

  void calcMetrics() {
    if (font3d != currentMetricsFont3d) {
      currentMetricsFont3d = font3d;
      FontMetrics fm = font3d.fontMetrics;
      frankWidth = fm.stringWidth(frankString);
      frankDescent = fm.getDescent();
      frankAscent = fm.getAscent();
    }
  }
}
