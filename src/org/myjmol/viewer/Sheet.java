/* $RCSfile: Sheet.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:07 $
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

class Sheet extends ProteinStructure {

  AminoPolymer aminoPolymer;
  Sheet(AminoPolymer aminoPolymer, int monomerIndex, int monomerCount) {
    super(aminoPolymer, JmolConstants.PROTEIN_STRUCTURE_SHEET,
          monomerIndex, monomerCount);
    this.aminoPolymer = aminoPolymer;
  }

  void calcAxis() {
    if (axisA != null)
      return;
    if (monomerCount == 2) {
      axisA = aminoPolymer.getLeadPoint(monomerIndex);
      axisB = aminoPolymer.getLeadPoint(monomerIndex + 1);
    } else {
      axisA = new Point3f();
      aminoPolymer.getLeadMidPoint(monomerIndex + 1, axisA);
      axisB = new Point3f();
      aminoPolymer.getLeadMidPoint(monomerIndex + monomerCount - 1, axisB);
    }

    axisUnitVector = new Vector3f();
    axisUnitVector.sub(axisB, axisA);
    axisUnitVector.normalize();

    Point3f tempA = new Point3f();
    aminoPolymer.getLeadMidPoint(monomerIndex, tempA);
    if (lowerNeighborIsHelixOrSheet()) {
      System.out.println("ok"); 
    } else {
      projectOntoAxis(tempA);
    }
    Point3f tempB = new Point3f();
    aminoPolymer.getLeadMidPoint(monomerIndex + monomerCount, tempB);
    if (upperNeighborIsHelixOrSheet()) {
      System.out.println("ok");       
    } else {
      projectOntoAxis(tempB);
    }
    axisA = tempA;
    axisB = tempB;
  }

  Vector3f widthUnitVector;
  Vector3f heightUnitVector;
  
  void calcSheetUnitVectors() {
    if (widthUnitVector == null) {
      Vector3f vectorCO = new Vector3f();
      Vector3f vectorCOSum = new Vector3f();
      AminoMonomer amino = (AminoMonomer)aminoPolymer.monomers[monomerIndex];
      vectorCOSum.sub(amino.getCarbonylOxygenAtomPoint(),
                      amino.getCarbonylCarbonAtomPoint());
      for (int i = monomerCount; --i > 0; ) {
        amino = (AminoMonomer)aminoPolymer.monomers[i];
        vectorCO.sub(amino.getCarbonylOxygenAtomPoint(),
                     amino.getCarbonylCarbonAtomPoint());
        if (vectorCOSum.angle(vectorCO) < (float)Math.PI/2)
          vectorCOSum.add(vectorCO);
        else
          vectorCOSum.sub(vectorCO);
      }
      heightUnitVector = vectorCO; // just reuse the same temp vector;
      heightUnitVector.cross(axisUnitVector, vectorCOSum);
      heightUnitVector.normalize();
      widthUnitVector = vectorCOSum;
      widthUnitVector.cross(axisUnitVector, heightUnitVector);
    }
  }

  Vector3f getWidthUnitVector() {
    if (widthUnitVector == null)
      calcSheetUnitVectors();
    return widthUnitVector;
  }

  Vector3f getHeightUnitVector() {
    if (heightUnitVector == null)
      calcSheetUnitVectors();
    return heightUnitVector;
  }
}
