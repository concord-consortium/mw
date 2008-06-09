/* $RCSfile: DipolesRenderer.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:06 $
 * $Revision: 1.1 $
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

package org.myjmol.viewer;

import org.myjmol.g3d.*;

import javax.vecmath.*;

class DipolesRenderer extends ShapeRenderer {

  float dipoleVectorScale;

  void render() {
    Dipoles dipoles = (Dipoles) shape;
    dipoleVectorScale = dipoles.dipoleVectorScale;
    for (int i = dipoles.dipoleCount; --i >= 0;) {
      Dipole dipole = dipoles.dipoles[i];
      if (dipole.visibilityFlags != 0 && transform(dipole))
        renderDipoleVector(dipole);
    }
  }

  final Vector3f offset = new Vector3f();
  final Point3i[] screens = new Point3i[6];
  final Point3f[] points = new Point3f[6];
  {
    for (int i = 0; i < 6; i++) {
      screens[i] = new Point3i();
      points[i] = new Point3f();
    }
  }
  Point3f cross0 = new Point3f();
  Point3f cross1 = new Point3f();
  
  final static int cylinderBase = 0;
  final static int cross = 1;
  final static int crossEnd = 2;
  final static int center = 3;
  final static int arrowHeadBase = 4;
  final static int arrowHeadTip = 5;

  int diameter;
  int headWidthPixels;
  int crossWidthPixels;

  final static float arrowHeadOffset = 0.8f;
  final static float arrowHeadWidthFactor = 4f;
  final static float crossOffset = 0.1f;
  final static float crossWidth = 0.04f;

  boolean transform(Dipole dipole) {
    Vector3f vector = dipole.vector;
    offset.set(vector);
    if (dipole.center == null) { 
      offset.scale(dipole.offsetAngstroms / dipole.dipoleValue);
      if (dipoleVectorScale < 0)
        offset.add(vector);
    points[cylinderBase].set(dipole.origin);
    points[cylinderBase].add(offset);
    } else {
      offset.scale(-0.5f * dipoleVectorScale);
      points[cylinderBase].set(dipole.center);
      points[cylinderBase].add(offset);    
      if (dipole.offsetAngstroms != 0) { 
        offset.set(vector);
        offset.scale(dipole.offsetAngstroms / dipole.dipoleValue);
        points[cylinderBase].add(offset);       
      }
    }
    
    points[cross].scaleAdd(dipoleVectorScale * crossOffset, vector,
        points[cylinderBase]);
    points[crossEnd].scaleAdd(dipoleVectorScale * (crossOffset + crossWidth),
        vector, points[cylinderBase]);
    points[center]
        .scaleAdd(dipoleVectorScale / 2, vector, points[cylinderBase]);
    points[arrowHeadBase].scaleAdd(dipoleVectorScale * arrowHeadOffset, vector,
        points[cylinderBase]);
    points[arrowHeadTip].scaleAdd(dipoleVectorScale, vector,
        points[cylinderBase]);

    if (dipole.atoms[0] != null) {
      if (frame.bsHidden.get(dipole.atoms[0].atomIndex))
        return false;
      offset.set(points[center]);
      offset.cross(offset, vector);
      if (offset.length() == 0) {
        offset.set(points[center].x + 0.2345f, points[center].y + 0.1234f,
            points[center].z + 0.4321f);
        offset.cross(offset, vector);
      }
      offset.scale(dipole.offsetSide / offset.length());
      for (int i = 0; i < 6; i++)
        points[i].add(offset);
    }
    for (int i = 0; i < 6; i++)
      viewer.transformPoint(points[i], screens[i]);
    viewer.transformPoint(points[cross], cross0);
    viewer.transformPoint(points[crossEnd], cross1);
    short mad = dipole.mad;
    diameter = viewer.scaleToScreen(screens[center].z, mad);
    headWidthPixels = (int) (diameter * arrowHeadWidthFactor);
    if (headWidthPixels < diameter + 10)
      headWidthPixels = diameter + 10;
    crossWidthPixels = headWidthPixels;
    return true;
  }

  void renderDipoleVector(Dipole dipole) {
    short colixA = (dipole.bond == null ? dipole.colix : Graphics3D
        .getColixInherited(dipole.colix, dipole.bond.colix));
    short colixB = colixA;
    if (dipole.atoms[0] != null) {
      colixA = Graphics3D.getColixInherited(colixA, dipole.atoms[0].colixAtom);
      colixB = Graphics3D.getColixInherited(colixB, dipole.atoms[1].colixAtom);
    }
    if (colixA == 0)
      colixA = Graphics3D.ORANGE;
    if (colixB == 0)
      colixB = Graphics3D.ORANGE;
    if (dipoleVectorScale < 0) {
      short c = colixA;
      colixA = colixB;
      colixB = c;
    }
    g3d.fillCylinder(colixA, Graphics3D.ENDCAPS_OPEN, diameter,
        screens[cylinderBase], screens[center]);
    g3d.fillCylinder(colixB, Graphics3D.ENDCAPS_OPEN, diameter,
        screens[center], screens[arrowHeadBase]);
    if (!dipole.noCross)
      g3d.fillCylinderBits(colixA, Graphics3D.ENDCAPS_FLAT, crossWidthPixels,
          cross0, cross1);
    g3d.fillCone(colixB, Graphics3D.ENDCAPS_FLAT, headWidthPixels,
        screens[arrowHeadBase], screens[arrowHeadTip]);
  }
}
