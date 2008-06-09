/* $RCSfile: Normix3D.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:14 $
 * $Revision: 1.10 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
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

package org.myjmol.g3d;

import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import java.util.Random;
import java.util.BitSet;

import org.myjmol.util.Logger;

/**
 * Provides quantization of normalized vectors so that shading for
 * lighting calculations can be handled by a simple index lookup
 *<p>
 * A 'normix' is a normal index, represented as a short
 *
 * @author Miguel, miguel@jmol.org
 */
class Normix3D {

  final static int NORMIX_GEODESIC_LEVEL = 3;

  final Graphics3D g3d;
  final Vector3f[] transformedVectors;
  final byte[] intensities;
  final byte[] intensities2Sided;
  final int normixCount;
  final static int[] vertexCounts = {12, 42, 162, 642, 2562};

  static short[][] faceNormixesArrays;
    // not "final" = new short[NORMIX_GEODESIC_LEVEL + 1][];

  private final static boolean TIMINGS = false;
  private final static boolean DEBUG_WITH_SEQUENTIAL_SEARCH = false;


  private final Matrix3f rotationMatrix = new Matrix3f();

  Normix3D(Graphics3D g3d) {
    // 12, 42, 162, 642, 2562
    this.g3d = g3d;
    normixCount = Geodesic3D.getVertexCount(NORMIX_GEODESIC_LEVEL);
    intensities = new byte[normixCount];
    intensities2Sided = new byte[normixCount];
    transformedVectors = new Vector3f[normixCount];
    for (int i = normixCount; --i >= 0; )
      transformedVectors[i] = new Vector3f();

    if (TIMINGS) {
      Logger.debug("begin timings!");
      for (int i = 0; i < normixCount; ++i) {
        short normix = getNormix(Geodesic3D.vertexVectors[i]);
        if (normix != i)
          Logger.debug("" + i + " -> " + normix);
      }
      short[] neighborVertexes =
        Geodesic3D.neighborVertexesArrays[NORMIX_GEODESIC_LEVEL];
      
      Random rand = new Random();
      Vector3f vFoo = new Vector3f();
      Vector3f vBar = new Vector3f();
      Vector3f vSum = new Vector3f();
      
      int runCount = 100000;
      long timeBegin, runTime;
      
      timeBegin = System.currentTimeMillis();
      for (int i = 0; i < runCount; ++i) {
        short foo = (short)(rand.nextDouble() * normixCount);
        int offsetNeighbor;
        short bar;
        do {
          offsetNeighbor = foo * 6 + (int)(rand.nextDouble() * 6);
          bar = neighborVertexes[offsetNeighbor];
        } while (bar == -1);
        vFoo.set(Geodesic3D.vertexVectors[foo]);
        vFoo.scale(rand.nextFloat());
        vBar.set(Geodesic3D.vertexVectors[bar]);
        vBar.scale(rand.nextFloat());
        vSum.add(vFoo, vBar);
        vSum.normalize();
      }
      runTime = System.currentTimeMillis() - timeBegin;
      Logger.debug("base runtime for " + runCount + " -> " +
                         runTime + " ms");

      timeBegin = System.currentTimeMillis();
      for (int i = 0; i < runCount; ++i) {
        short foo = (short)(rand.nextDouble() * normixCount);
        int offsetNeighbor;
        short bar;
        do {
          offsetNeighbor = foo * 6 + (int)(rand.nextDouble() * 6);
          bar = neighborVertexes[offsetNeighbor];
        } while (bar == -1);
        vFoo.set(Geodesic3D.vertexVectors[foo]);
        vFoo.scale(rand.nextFloat());
        vBar.set(Geodesic3D.vertexVectors[bar]);
        vBar.scale(rand.nextFloat());
        vSum.add(vFoo, vBar);
        short sum = getNormix(vSum);
        if (sum != foo && sum != bar) {
          Logger.debug("foo:" + foo + " -> " +
                             Geodesic3D.vertexVectors[foo] + "\n" +
                             "bar:" + bar + " -> " +
                             Geodesic3D.vertexVectors[bar] + "\n" +
                             "sum:" + sum + " -> " +
                             Geodesic3D.vertexVectors[sum] + "\n" +
                             "foo.dist="+dist2(vSum,
                                                  Geodesic3D.vertexVectors[foo])+"\n"+
                             "bar.dist="+dist2(vSum,
                                                  Geodesic3D.vertexVectors[bar])+"\n"+
                             "sum.dist="+dist2(vSum,
                                                  Geodesic3D.vertexVectors[sum])+"\n"+
                             "\nvSum:" + vSum + "\n");
          throw new NullPointerException();
        }
        short sum2 = getNormix(vSum);
        if (sum != sum2) {
          Logger.debug("normalized not the same answer?");
          throw new NullPointerException();
        }
      }
      runTime = System.currentTimeMillis() - timeBegin;
      Logger.debug("normix2 runtime for " + runCount + " -> " +
                         runTime + " ms");

    }
  }

  final BitSet bsNull = new BitSet();
  final BitSet bsConsidered = new BitSet();

  short getNormix(Vector3f v) {
    return getNormix(v.x, v.y, v.z, NORMIX_GEODESIC_LEVEL);
  }

  Vector3f getVector(short normix) {
    return Geodesic3D.vertexVectors[normix];
  }
  
  short getNormix(double x, double y, double z, int geodesicLevel) {
    short champion;
    double t;
    if (z >= 0) {
      champion = 0;
      t = z - 1;
    } else {
      champion = 11;
      t = z - (-1);
    }
    bsConsidered.and(bsNull);
    bsConsidered.set(champion);
    double championDist2 = x*x + y*y + t*t;
    for (int lvl = 0; lvl <= geodesicLevel; ++lvl) {
      short[] neighborVertexes = Geodesic3D.neighborVertexesArrays[lvl];
      for (int offsetNeighbors = 6 * champion,
             i = offsetNeighbors + (champion < 12 ? 5 : 6);
           --i >= offsetNeighbors; ) {
        short challenger = neighborVertexes[i];
        if (bsConsidered.get(challenger))
            continue;
        bsConsidered.set(challenger);
        //Logger.debug("challenger=" + challenger);
        Vector3f v = Geodesic3D.vertexVectors[challenger];
        double d;
        // d = dist2(v, x, y, z);
        //Logger.debug("challenger d2=" + (d*d));
        d = v.x - x;
        double d2 = d * d;
        if (d2 >= championDist2)
          continue;
        d = v.y - y;
        d2 += d * d;
        if (d2 >= championDist2)
          continue;
        d = v.z - z;
        d2 += d * d;
        if (d2 >= championDist2)
          continue;
        champion = challenger;
        championDist2 = d2;
      }
    }

    if (DEBUG_WITH_SEQUENTIAL_SEARCH) {
      int champSeq = 0;
      double champSeqD2 = dist2(Geodesic3D.vertexVectors[champSeq], x, y, z);
      for (int k = vertexCounts[geodesicLevel]; --k > 0; ) {
        double challengerD2 = dist2(Geodesic3D.vertexVectors[k], x, y, z);
        if (challengerD2 < champSeqD2) {
          champSeq = k;
          champSeqD2 = challengerD2;
        }
      }
      if (champion != champSeq) {
        if (champSeqD2 + .01 < championDist2) {
          Logger.debug("?que? getNormix is messed up?");
          boolean considered = bsConsidered.get(champSeq);
          Logger.debug("Was the sequential winner considered? " +
                             considered);
          Logger.debug("champion " + champion + " @ " +
                             championDist2 +
                             " sequential champ " + champSeq + " @ " +
                             champSeqD2 +
                             "\n");
          return (short)champSeq;
        }
      }
    }
    return champion;
  }

  short[] inverseNormixes;

  void calculateInverseNormixes() {
    inverseNormixes = new short[normixCount];
    for (int n = normixCount; --n >= 0; ) {
      Vector3f v = Geodesic3D.vertexVectors[n];
      inverseNormixes[n] = getNormix(-v.x, -v.y, -v.z, NORMIX_GEODESIC_LEVEL);
      }
    // validate that everyone's inverse is themselves
    for (int n = normixCount; --n >= 0; )
      if (inverseNormixes[inverseNormixes[n]] != n)
        throw new NullPointerException();
  }

  byte getIntensity(short normix) {
    if (normix < 0)
      return intensities2Sided[~normix];
    return intensities[normix];
  }

  void setRotationMatrix(Matrix3f rotationMatrix) {
    this.rotationMatrix.set(rotationMatrix);
    for (int i = normixCount; --i >= 0; ) {
      Vector3f tv = transformedVectors[i];
      rotationMatrix.transform(Geodesic3D.vertexVectors[i], tv);
      float x = tv.x;
      float y = -tv.y;
      float z = tv.z;
      /*
        enable this code in order to allow
        lighting of the inside of surfaces.
        but they probably should not be specular
        and light source should be from another position ... like a headlamp
        
      if (z < 0) {
        x = -x;
        y = -y;
        z = -z;
      }
      */
      byte intensity = Shade3D.calcIntensityNormalized(x, y, z);
      intensities[i] = intensity;
      if (z >= 0)
        intensities2Sided[i] = intensity;
      else
        intensities2Sided[i] = Shade3D.calcIntensityNormalized(-x, -y, -z);
    }
  }

  Vector3f[] getTransformedVectors() {
    return transformedVectors;
  }

  short[] getFaceNormixes(int level) {
    if (faceNormixesArrays == null)
      faceNormixesArrays = new short[NORMIX_GEODESIC_LEVEL + 1][];
    short[] faceNormixes = faceNormixesArrays[level];
    if (faceNormixes != null)
      return faceNormixes;
    return calcFaceNormixes(level);
  }

  static double dist2(Vector3f v1, Vector3f v2) {
    double dx = v1.x - v2.x;
    double dy = v1.y - v2.y;
    double dz = v1.z - v2.z;
    return dx*dx + dy*dy + dz*dz;
  }
    
  static double dist2(Vector3f v1, double x, double y, double z) {
    double dx = v1.x - x;
    double dy = v1.y - y;
    double dz = v1.z - z;
    return dx*dx + dy*dy + dz*dz;
  }
    
  private final static boolean DEBUG_FACE_VECTORS = false;

  private synchronized short[] calcFaceNormixes(int level) {
    //Logger.debug("calcFaceNormixes(" + level + ")");
    short[] faceNormixes = faceNormixesArrays[level];
    if (faceNormixes != null)
      return faceNormixes;
    Vector3f t = new Vector3f();
    short[] faceVertexes = Geodesic3D.faceVertexesArrays[level];
    int j = faceVertexes.length;
    int faceCount = j / 3;
    faceNormixes = new short[faceCount];
    for (int i = faceCount; --i >= 0; ) {
      Vector3f vA = Geodesic3D.vertexVectors[faceVertexes[--j]];
      Vector3f vB = Geodesic3D.vertexVectors[faceVertexes[--j]];
      Vector3f vC = Geodesic3D.vertexVectors[faceVertexes[--j]];
      t.add(vA, vB);
      t.add(vC);
      short normix = getNormix(t);
      faceNormixes[i] = normix;
      if (DEBUG_FACE_VECTORS) {
        Vector3f vN = Geodesic3D.vertexVectors[normix];
        
        double d2At = dist2(t, vA);
        double d2Bt = dist2(t, vB);
        double d2Ct = dist2(t, vC);
        double d2Nt = dist2(t, vN);
        if (d2At < d2Nt ||
            d2Bt < d2Nt ||
            d2Ct < d2Nt) {
          Logger.debug(" d2At =" + d2At +
                             " d2Bt =" + d2Bt +
                             " d2Ct =" + d2Ct +
                             " d2Nt =" + d2Nt);
        }
      }
 
      /*
      double champD = dist2(Geodesic3D.vertexVectors[normix], t);
      int champ = normix;
      for (int k = normixCount; --k >= 0; ) {
        double d = dist2(Geodesic3D.vertexVectors[k], t);
        if (d < champD) {
          champ = k;
          champD = d;
        }
      }
      if (champ != normix) {
        Logger.debug("normix " + normix + " @ " +
                           dist2(Geodesic3D.vertexVectors[normix], t) +
                           "\n" +
                           "champ " + champ + " @ " +
                           dist2(Geodesic3D.vertexVectors[champ], t) +
                           "\n");
      }
      */
    }
    faceNormixesArrays[level] = faceNormixes;
    return faceNormixes;
  }

  boolean isDirectedTowardsCamera(short normix) {
    // normix < 0 means a double sided normix, so always visible
    return (normix < 0) || (transformedVectors[normix].z > 0);
  }

  /* only reference was Graphics3D.getClosestVisibleGeodesicVertexIndex
   * removed - Bob Hanson 7/17/06
   * 
  short getVisibleNormix(double x, double y, double z,
                         int[] visibilityBitmap, int level) {
    int minMapped = Bmp.getMinMappedBit(visibilityBitmap);
    //Logger.debug("minMapped =" + minMapped);
    if (minMapped < 0)
      return -1;
    int maxMapped = Bmp.getMaxMappedBit(visibilityBitmap);
    int maxVisible = Geodesic3D.vertexCounts[level];
    int max = maxMapped < maxVisible ? maxMapped : maxVisible;
    Vector3f v;
    double d;
    double championDist2;
    int champion = minMapped;
    v = Geodesic3D.vertexVectors[champion];
    d = x - v.x;
    championDist2 = d * d;
    d = y - v.y;
    championDist2 += d * d;
    d = z - v.z;
    championDist2 += d * d;

    for (int challenger = minMapped + 1; challenger < max; ++challenger) {
      if (! Bmp.getBit(visibilityBitmap, challenger))
        continue;
      double challengerDist2 = dist2(Geodesic3D.vertexVectors[challenger],
                                     x, y, z);
      if (challengerDist2 < championDist2) {
        champion = challenger;
        championDist2 = challengerDist2;
      }
    }
    //Logger.debug("visible champion=" + champion);
    if (! Bmp.getBit(visibilityBitmap, champion))
      throw new IndexOutOfBoundsException();
    return (short)champion;
  }
  */
}
