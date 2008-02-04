/* $RCSfile: Hover.java,v $
 * $Author: qxie $
 * $Date: 2006-12-12 00:35:54 $
 * $Revision: 1.12 $
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

import org.jmol.g3d.*;
import org.jmol.util.Logger;

import java.util.BitSet;
import java.util.Hashtable;

import javax.vecmath.Point3i;

class Hover extends TextShape {

  private final static String FONTFACE = "SansSerif";
  private final static String FONTSTYLE = "Plain";
  private final static int FONTSIZE = 12;

  Text hoverText;
  int atomIndex = -1;
  Point3i xy;
  String text;
  String labelFormat = "%U";
  String[] atomFormats;

  void initShape() {
    myType = HOVER;
    Font3D font3d = g3d.getFont3D(FONTFACE, FONTSTYLE, FONTSIZE);
    short bgcolix = Graphics3D.getColix("#FFFFC3"); // 255, 255, 195
    short colix = Graphics3D.BLACK;
    currentText = hoverText = new Text(g3d, font3d, null, colix, bgcolix,
        0, 0, 1, Integer.MIN_VALUE, Text.LEFT);
    hoverText.setAdjustForWindow(true);
  }

  void setProperty(String propertyName, Object value, BitSet bsSelected) {

    Logger.debug("Hover.setProperty(" + propertyName + "," + value + ")");

    if ("target" == propertyName) {
      if (value == null)
        atomIndex = -1;
      else {
        atomIndex = ((Integer) value).intValue();
        viewer
            .setStatusAtomHovered(atomIndex, viewer.getAtomInfoXYZ(atomIndex));
      }
      return;
    }
    
    if ("text" == propertyName) {
      text = (String) value;
      if (text != null && text.length() == 0)
        text = null;
      return;
    }
    
    if ("atomLabel" == propertyName) {
      String text = (String) value;
      if (text != null && text.length() == 0)
        text = null;
      int count = viewer.getAtomCount();
      if (atomFormats == null)
        atomFormats = new String[count];
      for (int i = count; --i >= 0; ) 
      if (bsSelected.get(i))
        atomFormats[i] = text;
      return;
    }
    
    if ("xy" == propertyName) {
      xy = (Point3i) value;
    }
    
    if ("label" == propertyName) {
      labelFormat = (String) value;
      if (labelFormat != null && labelFormat.length() == 0)
        labelFormat = null;
      return;
    }

    super.setProperty(propertyName, value, null);
    
  }

  String getShapeState() {
    Hashtable temp = new Hashtable();
    int atomCount = viewer.getAtomCount();
    if (atomFormats != null)
      for (int i = atomCount; --i >= 0;)
        if (atomFormats[i] != null)
          setStateInfo(temp, i, "set hover "
              + StateManager.escape(atomFormats[i]));
    return getShapeCommands(temp, null, atomCount);
  }  
}
