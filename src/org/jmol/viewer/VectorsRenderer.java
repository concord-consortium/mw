/* $RCSfile: VectorsRenderer.java,v $
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import org.jmol.g3d.*;
import javax.vecmath.*;

class VectorsRenderer extends ShapeRenderer {

  void render() {
    Vectors vectors = (Vectors) shape;
    if (!vectors.isActive)
      return;
    short[] mads = vectors.mads;
    if (mads == null)
      return;
    Atom[] atoms = vectors.atoms;
    short[] colixes = vectors.colixes;
    for (int i = frame.atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.shapeVisibilityFlags & JmolConstants.ATOM_IN_MODEL) == 0
          || (atom.shapeVisibilityFlags & vectors.myVisibilityFlag) == 0
          || frame.bsHidden.get(i))
        continue;
      Vector3f vibrationVector = atom.getVibrationVector();
      if (vibrationVector == null)
        continue;
      vectorScale = vectors.scale;
      if (Float.isNaN(vectorScale)) {
        vectorScale = vectors.scale = viewer.getDefaultVectorScale();
      }
      if (transform(mads[i], atom, vibrationVector))
        renderVector((colixes == null ? Graphics3D.INHERIT : colixes[i]), atom);
    }
  }

  final Point3f pointVectorEnd = new Point3f();
  final Point3f pointArrowHead = new Point3f();
  final Point3i screenVectorEnd = new Point3i();
  final Point3i screenArrowHead = new Point3i();
  final Vector3f headOffsetVector = new Vector3f();
  int diameter;
  float headWidthAngstroms;
  int headWidthPixels;
  float vectorScale;
  float headScale;
  boolean doShaft;
  final static float arrowHeadOffset = -0.2f;


  boolean transform(short mad, Atom atom, Vector3f vibrationVector) {
    if (atom.madAtom == JmolConstants.MAR_DELETED)
      return false;

    float len = vibrationVector.length();
    // to have the vectors move when vibration is turned on
    if (Math.abs(len * vectorScale) < 0.01)
      return false;
    headScale = arrowHeadOffset;
    if (vectorScale < 0)
      headScale = -headScale;
    doShaft = (0.1 + Math.abs(headScale/len) < Math.abs(vectorScale));
    headOffsetVector.set(vibrationVector);
    headOffsetVector.scale(headScale / len);
    pointVectorEnd.scaleAdd(vectorScale, vibrationVector, atom);
    pointArrowHead.set(pointVectorEnd);
    pointArrowHead.add(headOffsetVector);
    viewer.transformPoint(pointArrowHead, vibrationVector, screenArrowHead);
    viewer.transformPoint(pointVectorEnd, vibrationVector, screenVectorEnd);
    diameter = (mad < 5 ? 5 : mad <= 20 ? mad : viewer.scaleToScreen(screenVectorEnd.z, mad));
    headWidthPixels = (int)(diameter * 1.5f);
    if (headWidthPixels < diameter + 2)
      headWidthPixels = diameter + 2;
    return true;
  }
  
  void renderVector(short colix, Atom atom) {
    colix = Graphics3D.getColixInherited(colix, atom.colixAtom);
    if (doShaft)
    g3d.fillCylinder(colix, Graphics3D.ENDCAPS_OPEN, diameter,
                 atom.screenX, atom.screenY, atom.screenZ,
                 screenArrowHead.x, screenArrowHead.y, screenArrowHead.z);
    g3d.fillCone(colix, Graphics3D.ENDCAPS_FLAT, headWidthPixels,
                 screenArrowHead, screenVectorEnd);
  }
}
