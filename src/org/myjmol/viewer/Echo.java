/* $RCSfile: Echo.java,v $
 * $Author: qxie $
 * $Date: 2007-03-28 01:54:32 $
 * $Revision: 1.13 $
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


import org.myjmol.g3d.*;
import org.myjmol.util.Logger;

import java.util.BitSet;
import java.util.Enumeration;

class Echo extends TextShape {

  
  /*
   * set echo Text.TOP    [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo MIDDLE [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo BOTTOM [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo name   [Text.LEFT|Text.CENTER|Text.RIGHT]
   * set echo name  x-position y-position
   * set echo none  to initiate setting default settings
   * 
   */
  

  private final static String FONTFACE = "Serif";
  private final static int FONTSIZE = 20;
  private final static short COLOR = Graphics3D.RED;
  
  void initShape() {
    myType = ECHO;
    setProperty("target", "top", null);
  }

  @SuppressWarnings("unchecked")
void setProperty(String propertyName, Object value, BitSet bsSelected) {

    Logger.debug("Echo.setProperty(" + propertyName + "," + value + ")");

    if ("target" == propertyName) {
      String target = ((String) value).intern().toLowerCase();
      if (target != "none" && target != "all") {
        Text text = (Text) texts.get(target);
        if (text == null) {
          int valign = Text.XY;
          int halign = Text.LEFT;
          if ("top" == target) {
            valign = Text.TOP;
            halign = Text.CENTER;
          } else if ("middle" == target) {
            valign = Text.MIDDLE;
            halign = Text.CENTER;
          } else if ("bottom" == target) {
            valign = Text.BOTTOM;
          }
          text = new Text(g3d, g3d.getFont3D(FONTFACE, FONTSIZE), target,
              COLOR, valign, halign);
          text.setAdjustForWindow(true); // when a box is around it
          texts.put(target, text);
          if (currentFont != null)
            text.setFont(currentFont);
          if (currentColor != null)
            text.setColix(currentColor);
          if (currentBgColor != null)
            text.setBgColix(currentBgColor);
        }
        currentText = text;
        //process super
      }
    }
    super.setProperty(propertyName, value, null);
  }
  
  String getShapeState() {
    StringBuffer s = new StringBuffer();
    String lastFormat = "";
    Enumeration e = texts.elements();
    while (e.hasMoreElements()) {
      Text t = (Text) e.nextElement();
      s.append(t.getState(true));
      String format = t.getState(false);
      if (format.equals(lastFormat))
        continue;
      lastFormat = format;
      s.append(format);
    }
    return s.toString();
  }
}

