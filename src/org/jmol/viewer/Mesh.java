/* $RCSfile: Mesh.java,v $
 * $Author: qxie $
 * $Date: 2006-12-12 00:32:11 $
 * $Revision: 1.12 $
 *
 * Copyright (C) 2005  Miguel, The Jmol Development Team
 *
 * Contact: miguel@jmol.org, jmol-developers@lists.sf.net
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

import org.jmol.util.Logger;
import org.jmol.util.ArrayUtil;

import java.util.Hashtable;

import org.jmol.g3d.*;

import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;
import java.util.BitSet;

class Mesh {
  Viewer viewer;
  String[] title = null;
  String thisID;
  boolean isValid = true;
  String scriptCommand;
  String jvxlFileHeader;
  String jvxlExtraLine;
  int jvxlCompressionRatio;
  String jvxlSurfaceData;
  String jvxlEdgeData;
  String jvxlColorData;
  boolean isJvxlPrecisionColor;
  Point4f jvxlPlane;
  String jvxlDefinitionLine;
  boolean isContoured;
  boolean isBicolorMap;
  float mappedDataMin;
  float mappedDataMax;
  float valueMappedToRed;
  float valueMappedToBlue;
  float cutoff;
  int nBytes;
  int nContours;
  boolean hasGridPoints;
  boolean hideBackground;
  BitSet[] surfaceSet;
  int firstViewableVertex;
  int lastViewableVertex;

  boolean visible = true;
  short colix;
  short[] vertexColixes;
  Graphics3D g3d;
    
  int vertexCount;
  Point3f[] vertices;
  float[] vertexValues;
  short[] normixes;
  int polygonCount;  // negative number indicates hermite curve
  int[][] polygonIndexes = null;
  
  int drawVertexCount;
  int[] drawVertexCounts;
  float scale = 1;
  int diameter;
  Point3f ptCenter = new Point3f(0,0,0);
  Point3f ptCenters[];
  Vector3f axis = new Vector3f(1,0,0);
  Vector3f axes[];
  String meshType = null;
  
  final static int DRAW_MULTIPLE = -1;
  final static int DRAW_NONE = 0;
  final static int DRAW_ARROW = 1;
  final static int DRAW_CIRCLE = 2;
  final static int DRAW_CURVE = 3;
  final static int DRAW_LINE = 4;
  final static int DRAW_PLANE = 5;
  final static int DRAW_POINT = 6;
  final static int DRAW_TRIANGLE = 7;
  int drawType = DRAW_TRIANGLE;
  int[] drawTypes;
  

  String getDrawType() {
    switch (drawType) {
    case Mesh.DRAW_MULTIPLE:
      return "multiple";
    case Mesh.DRAW_ARROW:
      return "arrow";
    case Mesh.DRAW_CIRCLE:
      return "circle";
    case Mesh.DRAW_CURVE:
      return "curve";
    case Mesh.DRAW_POINT:
      return "point";
    case Mesh.DRAW_LINE:
      return "line";
    case Mesh.DRAW_TRIANGLE:
      return "triangle";
    case Mesh.DRAW_PLANE:
      return "plane";
    }
    return "type is not identified in mesh.getDrawType()";
  }

  int atomIndex = -1;
  int modelIndex = -1;  // for Isosurface and Draw
  int visibilityFlags;
  int[] modelFlags = null; //one per POLYGON for DRAW
  
  boolean showPoints = false;
  boolean drawTriangles = false;
  boolean fillTriangles = true;

  static int SEED_COUNT = 25; //optimized for cartoon mesh hermites
  
  Mesh(Viewer viewer, String thisID, Graphics3D g3d, short colix) {
    this.viewer = viewer;
    this.thisID = thisID;
    this.g3d = g3d;
    this.colix = colix;
  }

  void clear(String meshType) {
    vertexCount = polygonCount = 0;
    scale = 1;
    vertices = null;
    vertexColixes = null;
    polygonIndexes = null;
    this.meshType = meshType;
  }

  void initialize() {
    initialize(true);
  }
  
  void initialize(boolean use2Sided) {
    Vector3f[] vectorSums = new Vector3f[vertexCount];
    for (int i = vertexCount; --i >= 0;)
      vectorSums[i] = new Vector3f();
    sumVertexNormals(vectorSums);
    normixes = new short[vertexCount];
    if (use2Sided)
      for (int i = vertexCount; --i >= 0;)
        normixes[i] = g3d.get2SidedNormix(vectorSums[i]);
    else
      for (int i = vertexCount; --i >= 0;)
        normixes[i] = g3d.getNormix(vectorSums[i]);
  }

  void offset(Vector3f offset) {
    for (int i = vertexCount; --i >= 0;)
      vertices[i].add(offset);
  }

  void allocVertexColixes() {
    if (vertexColixes == null) {
      vertexColixes = new short[vertexCount];
      for (int i = vertexCount; --i >= 0; )
        vertexColixes[i] = colix;
    }
  }

  void setTranslucent(boolean isTranslucent) {
    colix = Graphics3D.getColixTranslucent(colix, isTranslucent);
    if (vertexColixes != null)
      for (int i = vertexCount; --i >= 0; )
        vertexColixes[i] =
          Graphics3D.getColixTranslucent(vertexColixes[i], isTranslucent);
  }

  final Vector3f vAB = new Vector3f();
  final Vector3f vAC = new Vector3f();

  void sumVertexNormals(Vector3f[] vectorSums) {
    final Vector3f vNormalizedNormal = new Vector3f();

    for (int i = polygonCount; --i >= 0;) {
      int[] pi = polygonIndexes[i];
      try {
        if (pi != null) {
          Graphics3D.calcNormalizedNormal(vertices[pi[0]], vertices[pi[1]],
              vertices[pi[2]], vNormalizedNormal, vAB, vAC);
          // general 10.? error here was not watching out for 
          // occurrances of intersection AT a corner, leading to
          // two points of triangle being identical
          float l = vNormalizedNormal.length();
          if( l > 0.9 && l < 1.1) //test for not infinity or -infinity or isNaN
          for (int j = pi.length; --j >= 0;) {
            int k = pi[j];
            vectorSums[k].add(vNormalizedNormal);
          }
        }
      } catch (Exception e) {
      }
    }
  }

  void setVertexCount(int vertexCount) {
    this.vertexCount = vertexCount;
    vertices = new Point3f[vertexCount];
    vertexValues = new float[vertexCount];
  }

  void setPolygonCount(int polygonCount) {
    //Logger.debug("Mesh setPolygonCount" + polygonCount);
    this.polygonCount = polygonCount;
    if (polygonCount < 0)
      return;
    if (polygonIndexes == null || polygonCount > polygonIndexes.length)
      polygonIndexes = new int[polygonCount][];
  }

  int addVertexCopy(Point3f vertex, float value) {
    if (vertexCount == 0)
      vertexValues = new float[SEED_COUNT];
    else if (vertexCount >= vertexValues.length)
      vertexValues = (float[]) ArrayUtil.doubleLength(vertexValues);
    vertexValues[vertexCount] = value;
    return addVertexCopy(vertex);
  }

  int addVertexCopy(Point3f vertex) {
    if (vertexCount == 0)
      vertices = new Point3f[SEED_COUNT];
    else if (vertexCount == vertices.length)
      vertices = (Point3f[]) ArrayUtil.doubleLength(vertices);
    vertices[vertexCount] = new Point3f(vertex);
    //Logger.debug("mesh.addVertexCopy " + vertexCount + vertex +vertices[vertexCount]);
    return vertexCount++;
  }

  void invalidateVertex(int vertex) {
    //vertexValues[vertex] = Float.NaN;  
  }
  
  void addTriangle(int vertexA, int vertexB, int vertexC) {
    if (vertexValues != null && (Float.isNaN(vertexValues[vertexA])||Float.isNaN(vertexValues[vertexB])||Float.isNaN(vertexValues[vertexC])))
      return;
    if (Float.isNaN(vertices[vertexA].x)||Float.isNaN(vertices[vertexB].x)||Float.isNaN(vertices[vertexC].x))
      return;
    if (polygonCount == 0)
      polygonIndexes = new int[SEED_COUNT][];
    else if (polygonCount == polygonIndexes.length)
      polygonIndexes = (int[][]) ArrayUtil.doubleLength(polygonIndexes);
    polygonIndexes[polygonCount++] = new int[] {vertexA, vertexB, vertexC};
  }

  void addQuad(int vertexA, int vertexB, int vertexC, int vertexD) {
    if (polygonCount == 0)
      polygonIndexes = new int[SEED_COUNT][];
    else if (polygonCount == polygonIndexes.length)
      polygonIndexes = (int[][]) ArrayUtil.doubleLength(polygonIndexes);
    polygonIndexes[polygonCount++] = new int[] {vertexA, vertexB, vertexC, vertexD};
  }

  void setColix(short colix) {
    this.colix = colix;
  }

  void checkForDuplicatePoints(float cutoff) {
    //not implemented
    float cutoff2 = cutoff * cutoff;
    for (int i = vertexCount; --i >= 0; )
      for (int j = i; --j >= 0; ) {
        float dist2 = vertices[i].distanceSquared(vertices[j]);
        if (dist2 < cutoff2) {
          Logger.debug("Mesh.checkForDuplicates " +
                             vertices[i] + "<->" + vertices[j] +
                             " : " + Math.sqrt(dist2));
        }
      }
  }
  
  Hashtable getShapeDetail() {
    return null;
  }
  
 final  boolean isPolygonDisplayable(int index) {
	 if(polygonIndexes == null || polygonIndexes[index] == null) return false; // XIE
    return (polygonIndexes[index].length > 0 
        && (modelIndex == index || modelFlags == null 
            || modelFlags[index] != 0)); 
  }
  
  final void setCenter(int iModel) {
    Point3f center = new Point3f(0, 0, 0);
    int iptlast = -1;
    int ipt = 0;
    int n = 0;
    for (int i = polygonCount; --i >= 0;) {
      if (iModel >=0 && i != iModel)
        continue;
      iptlast = -1;
      for (int iV = polygonIndexes[i].length; --iV >= 0;) {
        ipt = polygonIndexes[i][iV];
        if (ipt == iptlast)
          continue;
        iptlast = ipt;
        center.add(vertices[ipt]);
        n++;
      }
      if (n > 0 && (i == iModel || i == 0)) {
        center.scale(1.0f / n);
        break;
      }
    }
    if (iModel < 0){
      ptCenter = center;
    } else {
      ptCenters[iModel] = center;
    }
  }
}
