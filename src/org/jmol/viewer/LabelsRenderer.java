/* $RCSfile: LabelsRenderer.java,v $
 * $Author: qxie $
 * $Date: 2007-03-28 01:54:32 $
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
import java.awt.FontMetrics;

class LabelsRenderer extends ShapeRenderer {

  // offsets are from the font baseline
  byte fidPrevious;
  Font3D font3d;
  FontMetrics fontMetrics;
  int ascent;
  int descent;
  int msgWidth;

  @SuppressWarnings("unchecked")
void render() {
    fidPrevious = 0;

    Labels labels = (Labels) shape;

    String[] labelStrings = labels.strings;
    short[] colixes = labels.colixes;
    short[] bgcolixes = labels.bgcolixes;
    byte[] fids = labels.fids;
    int[] offsets = labels.offsets;
    if (labelStrings == null)
      return;
    Atom[] atoms = frame.atoms;
    for (int i = labelStrings.length; --i >= 0;) {
      Atom atom = atoms[i];
      if (!atom.isShapeVisible(myVisibilityFlag) || frame.bsHidden.get(i))
        continue;
      String label = labelStrings[i];
      if (label == null || label.length() == 0)
        continue;
      short colix = (colixes == null || i >= colixes.length) ? 0 : colixes[i];
      colix = Graphics3D.getColixInherited(colix, atom.colixAtom);
      short bgcolix = (bgcolixes == null || i >= bgcolixes.length) ? 0
          : bgcolixes[i];
      byte fid = ((fids == null || i >= fids.length || fids[i] == 0) ? labels.zeroFontId
          : fids[i]);
      int offsetFull = (offsets == null || i >= offsets.length ? 0 : offsets[i]);
      boolean labelsFront = ((offsetFull & Labels.FRONT_FLAG) != 0);
      boolean labelsGroup = ((offsetFull & Labels.GROUP_FLAG) != 0);
      int offset = offsetFull >> 6;
      int textAlign = (offsetFull >> 2) & 3;
      int pointer = offsetFull & 3;
      int zSlab = atom.screenZ - atom.screenDiameter / 2 - 2;
      if (zSlab < 1)
        zSlab = 1;
      int zBox = (labelsFront ? 1
          : labelsGroup && atom.group != null ? atom.group.minZ : zSlab);
      if (zBox < 1)
        zBox = 1;

      boolean isSimple = (textAlign == 0 && label.indexOf("|") < 0);

      Text text = (isSimple ? null : (Text) labels.atomLabels.get(atom));
      if (text != null) {
        text.setColix(colix);
        text.setXYZs(atom.screenX, atom.screenY, zBox, zSlab);
        text.render();
        continue;
      }
      if (fid != fidPrevious) {
        g3d.setFont(fid);
        fidPrevious = fid;
        font3d = g3d.getFont3DCurrent();
        if (textAlign == 0) {
          fontMetrics = font3d.fontMetrics;
          ascent = fontMetrics.getAscent();
          descent = fontMetrics.getDescent();
        }
      }
      if (isSimple) {
        boolean doPointer = ((pointer & Text.POINTER_ON) != 0);
        //(viewer.getLabelPointerBox() && bgcolix != 0 || bgcolix == 0 && viewer.getLabelPointerNoBox());
        //short pointercolix = (viewer.getLabelPointerBackground() && bgcolix != 0 ? bgcolix : colix);
        short pointercolix = ((pointer & Text.POINTER_BACKGROUND) != 0 
            && bgcolix != 0 ? bgcolix : colix);
        Text.renderSimple(g3d, font3d, label, colix, bgcolix, atom.screenX,
            atom.screenY, zBox, zSlab, Text.getXOffset(offset), Text
                .getYOffset(offset), ascent, descent, doPointer, pointercolix);
      } else {
        text = new Text(g3d, font3d, label, colix, bgcolix, atom.screenX,
            atom.screenY, zBox, zSlab, textAlign);
        labels.atomLabels.put(atom, text);
        text.setOffset(offset);
        text.render();
      }
    }
  }
}
