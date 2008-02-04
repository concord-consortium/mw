/* $RCSfile: MepCalculation.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:18 $
 * $Revision: 1.1 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
package org.jmol.quantum;

import javax.vecmath.Point3f;

import org.jmol.viewer.Atom;

/*
 * a simple molecular electrostatic potential cube generator
 * just using q/r here
 * 
 * http://teacher.pas.rochester.edu/phy122/Lecture_Notes/Chapter25/Chapter25.html
 * 
 * applying some of the tricks in QuantumCalculation for speed
 * 
 */
public class MepCalculation {

  final static float bohr_per_angstrom = 1 / 0.52918f;

  public static int MAX_GRID = 40;

  // grid coordinates relative t  o charge center in Bohr 
  float[] X = new float[MAX_GRID];
  float[] Y = new float[MAX_GRID];
  float[] Z = new float[MAX_GRID];

  // grid coordinate squares relative to charge center in Bohr
  float[] X2 = new float[MAX_GRID];
  float[] Y2 = new float[MAX_GRID];
  float[] Z2 = new float[MAX_GRID];

  Atom[] atoms;
  float[] mepCharges;
  
  Point3f[] atomCoordBohr;
  // absolute grid coordinates in Bohr 
  float[][] xyzBohr = new float[MAX_GRID][3];

  float[][][] voxelData;
  int[] countsXYZ;
  float[] originBohr = new float[3];
  float[] stepBohr = new float[3];

  public MepCalculation() {
  }

  public MepCalculation(Atom[] atoms, float[] mepCharges) {
    this.atoms = atoms;
    this.mepCharges = mepCharges;
  }

  public void createMepCube(float[][][] voxelData, int[] countsXYZ,
                               float[] originXYZ, float[] stepsXYZ) {
    this.voxelData = voxelData;
    this.countsXYZ = countsXYZ;
    setupCoordinates(originXYZ, stepsXYZ);
    processMep();
  }

  private void setupCoordinates(float[] originXYZ, float[] stepsXYZ) {

    // all coordinates come in as angstroms, not bohr, and are converted here into bohr

    for (int i = 3; --i >= 0;) {
      originBohr[i] = originXYZ[i] * bohr_per_angstrom;
      stepBohr[i] = stepsXYZ[i] * bohr_per_angstrom;
    }
    for (int i = 3; --i >= 0;) {
      xyzBohr[0][i] = originBohr[i];
      int n = countsXYZ[i];
      float inc = stepBohr[i];
      for (int j = 0; ++j < n;)
        xyzBohr[j][i] = xyzBohr[j - 1][i] + inc;
    }
    /* 
     * allowing null atoms allows for selectively removing
     * atoms from the rendering. Maybe a first time this has ever been done?
     * 
     */
    atomCoordBohr = new Point3f[atoms.length];
    for (int i = 0; i < atoms.length; i++) {
      if (atoms[i] == null)
        continue;
      atomCoordBohr[i] = new Point3f(atoms[i]);
      atomCoordBohr[i].scale(bohr_per_angstrom);
    }
  }

  private void processMep() {
    setMinMax();
    int firstAtom = 0;
    int lastAtom = atomCoordBohr.length;
    for (int i = 0; i < lastAtom; i++)
      if (atomCoordBohr[i] != null) {
        firstAtom = i;
        break;
      }
    for (int i = lastAtom; --i >= firstAtom;)
      if (atomCoordBohr[i] != null) {
        lastAtom = i + 1;
        break;
    }

    for (int atomIndex = firstAtom; atomIndex < lastAtom; atomIndex++) {
      if (atomCoordBohr[atomIndex] == null)
        continue;
      float x = atomCoordBohr[atomIndex].x;
      float y = atomCoordBohr[atomIndex].y;
      float z = atomCoordBohr[atomIndex].z;
      float charge = mepCharges[atomIndex];
      for (int i = countsXYZ[0]; --i >= 0;) {
        X2[i] = X[i] = xyzBohr[i][0] - x;
        X2[i] *= X[i];
      }
      for (int i = countsXYZ[1]; --i >= 0;) {
        Y2[i] = Y[i] = xyzBohr[i][1] - y;
        Y2[i] *= Y[i];
      }
      for (int i = countsXYZ[2]; --i >= 0;) {
        Z2[i] = Z[i] = xyzBohr[i][2] - z;
        Z2[i] *= Z[i];
      }
      for (int ix = xMax; --ix >= xMin;) {
        for (int iy = yMax; --iy >= yMin;)
          for (int iz = zMax; --iz >= zMin;) {
            float d2 = X2[ix] + Y2[iy] + Z2[iz];
            voxelData[ix][iy][iz] += (d2 == 0 ? charge
                * Float.POSITIVE_INFINITY : charge / (float) Math.sqrt(d2));
          }
      }
    }
  }

  int xMin;
  int xMax;
  int yMin;
  int yMax;
  int zMin;
  int zMax;

  private void setMinMax() {
    // optimization will come later
    xMin = 0;
    yMin = 0;
    zMin = 0;
    xMax = countsXYZ[0];
    yMax = countsXYZ[1];
    zMax = countsXYZ[2];
  }

}
