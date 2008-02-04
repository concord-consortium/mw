/* $RCSfile: HoverRenderer.java,v $
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

class HoverRenderer extends ShapeRenderer {
  void render() {
    Hover hover = (Hover) shape;
    if (hover.atomIndex >= 0) {
      Atom atom = frame.getAtomAt(hover.atomIndex);
      String label = (hover.atomFormats != null
          && hover.atomFormats[hover.atomIndex] != null ? 
              atom.formatLabel(hover.atomFormats[hover.atomIndex])
          : hover.labelFormat != null ? atom.formatLabel(hover.labelFormat)
              : null);
      if (label == null)
        return;
      Text text = hover.hoverText;
      text.setText(label);
      text.setXY(atom.screenX, atom.screenY);
      text.render();
    } else if (hover.text != null) {
      Text text = hover.hoverText;
      text.setText(hover.text);
      text.setXY(hover.xy.x, hover.xy.y);
      text.render();
    }
  }
}
