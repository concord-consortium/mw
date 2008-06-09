/* $RCSfile: ProteinStructure.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:09 $
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

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.myjmol.util.Logger;

abstract class ProteinStructure {

  AlphaPolymer apolymer;
  byte type;
  int monomerIndex;
  int monomerCount;
  Point3f axisA, axisB;
  Vector3f axisUnitVector;
  Point3f[] segments;
  int index;
  Point3f center;

  ProteinStructure(AlphaPolymer apolymer, byte type,
                   int monomerIndex, int monomerCount) {
    this.apolymer = apolymer;
    this.type = type;
    
    if(Logger.isActiveLevel(Logger.LEVEL_DEBUG))
      Logger.debug("Creating ProteinStructure" + type + " from " + monomerIndex + " through "+(monomerIndex+monomerCount -1));
    
    this.monomerIndex = monomerIndex;
    this.monomerCount = monomerCount;
  }

  void calcAxis() {
  }

  void calcSegments() {
    if (segments != null)
      return;
    calcAxis();
    segments = new Point3f[monomerCount + 1];
    segments[monomerCount] = axisB;
    segments[0] = axisA;
    Vector3f axis = new Vector3f(axisUnitVector);
    axis.scale(axisB.distance(axisA) / monomerCount);
    for (int i = 1; i < monomerCount; i++) {
      Point3f point = segments[i] = new Point3f();
      point.set(segments[i - 1]);
      point.add(axis);
      //now it's just a constant-distance segmentation. 
      //there isn't anything significant about seeing the
      //amino colors in different-sized slices, and (IMHO)
      //it looks better this way anyway. RMH 11/2006
      
      //apolymer.getLeadMidPoint(monomerIndex + i, point);
      //projectOntoAxis(point);
    }
  }

  boolean lowerNeighborIsHelixOrSheet() {
    if (monomerIndex == 0)
      return false;
    return apolymer.monomers[monomerIndex - 1].isHelix()
        || apolymer.monomers[monomerIndex - 1].isSheet();
  }

  boolean upperNeighborIsHelixOrSheet() {
    int upperNeighborIndex = monomerIndex + monomerCount;
    if (upperNeighborIndex == apolymer.monomerCount)
      return false;
    return apolymer.monomers[upperNeighborIndex].isHelix()
        || apolymer.monomers[upperNeighborIndex].isSheet();
  }

  final Vector3f vectorProjection = new Vector3f();
  void projectOntoAxis(Point3f point) {
    // assumes axisA, axisB, and axisUnitVector are set;
    vectorProjection.sub(point, axisA);
    float projectedLength = vectorProjection.dot(axisUnitVector);
    point.set(axisUnitVector);
    point.scaleAdd(projectedLength, axisA);
    vectorProjection.sub(point, axisA);
  }

  int getMonomerCount() {
    return monomerCount;
  }

  int getMonomerIndex() {
    return monomerIndex;
  }

  int getIndex(Monomer monomer) {
    Monomer[] monomers = apolymer.monomers;
    int i;
    for (i = monomerCount; --i >= 0; )
      if (monomers[monomerIndex + i] == monomer)
        break;
    return i;
  }

  Point3f[] getSegments() {
    if (segments == null)
      calcSegments();
    return segments;
  }

  Point3f getAxisStartPoint() {
    calcAxis();
    return axisA;
  }

  Point3f getAxisEndPoint() {
    calcAxis();
    return axisB;
  }

  Point3f getStructureMidPoint(int index) {
    if (segments == null)
      calcSegments();
    return segments[index];
  }
}
