/* $RCSfile: DotsRenderer.java,v $
 * $Author: qxie $
 * $Date: 2007-03-28 01:54:32 $
 * $Revision: 1.13 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import java.util.Hashtable;
import javax.vecmath.*;

import org.myjmol.g3d.Graphics3D;
import org.myjmol.util.Logger;

class DotsRenderer extends ShapeRenderer {

  boolean perspectiveDepth;
  int scalePixelsPerAngstrom;
  boolean bondSelectionModeOr;

  Geodesic geodesic, geodesicSolvent, g1, g2;

  final static int[] mapNull = Dots.mapNull;
  final Point3f[] vertexTest = new Point3f[12];
  final Vector3f[] vectorTest = new Vector3f[12];

  void initRenderer() {

    //level = 3 for both

    geodesic = new Geodesic(); // 12 vertices
    geodesic.quadruple(); // 12 * 4 - 6 = 42 vertices
    geodesic.quadruple(); // 42 * 4 - 6 = 162 vertices
    geodesic.quadruple(); // 162 * 4 - 6 = 642 vertices

    geodesicSolvent = new Geodesic(); // 12 vertices
    geodesicSolvent.quadruple(); // 12 * 4 - 6 = 42 vertices
    geodesicSolvent.quadruple(); // 42 * 4 - 6 = 162 vertices
    geodesicSolvent.quadruple(); // 162 * 4 - 6 = 642 vertices
    //geodesicSolvent.quadruple(); // 642 * 4 - 6 

    //these next two are for the geodesic fragment at a distance
    g1 = new Geodesic(); // 12 vertices
    g1.quadruple(); // 12 * 4 - 6 = 42 vertices

    g2 = new Geodesic(); // 12 vertices
    g2.quadruple(); // 12 * 4 - 6 = 42 vertices
    g2.quadruple(); // 42 * 4 - 6 = 162 vertices

    for(int i = 0; i < 12; i++)
      vertexTest[i] = new Point3f();
    for(int i = 0; i < 12; i++)
      vectorTest[i] = new Vector3f();
  }

  void render() {
    perspectiveDepth = viewer.getPerspectiveDepth();
    scalePixelsPerAngstrom = (int) viewer.getScalePixelsPerAngstrom();
    bondSelectionModeOr = viewer.getBondSelectionModeOr();

    geodesic.transform();
    Dots dots = (Dots) shape;
    if (dots == null)
      return;

    dots.timeBeginExecution = System.currentTimeMillis();

    Atom[] atoms = frame.atoms;
    int[][] dotsConvexMaps = dots.dotsConvexMaps;
    short[] colixes = dots.colixes;
    boolean isInMotion = (viewer.getInMotion() && dots.dotsConvexMax > 100);
    boolean iShowSolid = dots.isSurface;
    //boolean iShowSolid = (viewer.getTestFlag3()||dots.showSurface) && dots.useBobsAlgorithm;
    for (int i = dots.dotsConvexMax; --i >= 0;) {
      Atom atom = atoms[i];
      if (!atom.isShapeVisible(myVisibilityFlag) || frame.bsHidden.get(i)
          ||!g3d.isInDisplayRange(atom.screenX, atom.screenY))
        continue;
      renderConvex(dots, atom, colixes[i], dotsConvexMaps[i], iShowSolid,
          isInMotion);
    }
    dots.timeEndExecution = System.currentTimeMillis();
    //Logger.debug("dots rendering time = "+ dots.getExecutionWalltime());
  }

  int[] mapAtoms = null; 
  void renderConvex(Dots dots, Atom atom, short colix, int[] visibilityMap,
                    boolean iShowSolid, boolean isInMotion) {
    colix = Graphics3D.getColixInherited(colix, atom.colixAtom);
    if (mapAtoms == null)
      mapAtoms = new int[geodesic.vertices.length];
    boolean isSolid = (iShowSolid && !isInMotion);
    geodesic.calcScreenPoints(visibilityMap, dots.getAppropriateRadius(atom),
        atom.screenX, atom.screenY, atom.screenZ, mapAtoms,
        isSolid);
    if (geodesic.screenCoordinateCount == 0)
      return;
    if (isSolid)
      renderGeodesicFragment(dots.surfaceColix, geodesic, visibilityMap,
          mapAtoms, geodesic.screenDotCount);
    else
      g3d.drawPoints(colix, geodesic.screenCoordinateCount,
          geodesic.screenCoordinates);
  }

  Point3i facePt1 = new Point3i();
  Point3i facePt2 = new Point3i();
  Point3i facePt3 = new Point3i();
  
  void renderGeodesicFragment(short colix, Geodesic g, int[] points, int[] map,
                              int dotCount) {
    short[] faces = (g.screenLevel == 1 ? g1.faceIndices
        : g.screenLevel == 2 ? g2.faceIndices : g.faceIndices);
    int[] coords = g.screenCoordinates;
    short p1, p2, p3;
    int mapMax = (points.length << 5);
    //Logger.debug("geod frag "+mapMax+" "+dotCount);
    if (dotCount < mapMax)
      mapMax = dotCount;
    for (int f = 0; f < faces.length;) {
      p1 = faces[f++];
      p2 = faces[f++];
      p3 = faces[f++];
      if (p1 >= mapMax || p2 >= mapMax || p3 >= mapMax)
        continue;
      //Logger.debug("geod frag "+p1+" "+p2+" "+p3+" "+dotCount);
      if (!Dots.getBit(points, p1) || !Dots.getBit(points, p2)
          || !Dots.getBit(points, p3))
        continue;
      facePt1.set(coords[map[p1]], coords[map[p1] + 1], coords[map[p1] + 2]);
      facePt2.set(coords[map[p2]], coords[map[p2] + 1], coords[map[p2] + 2]);
      facePt3.set(coords[map[p3]], coords[map[p3] + 1], coords[map[p3] + 2]);
      g3d.fillTriangle(colix, facePt1, facePt2, facePt3);
    }
  }
  final static float halfRoot5 = (float)(0.5 * Math.sqrt(5));
  final static float oneFifth = 2 * (float)Math.PI / 5;
  final static float oneTenth = oneFifth / 2;
  

  final static int[] power4 = {1, 4, 16, 64, 256};
  
  final static short[] faceIndicesInitial = {
    0, 1, 2,
    0, 2, 3,
    0, 3, 4,
    0, 4, 5,
    0, 5, 1,

    1, 6, 2,
    2, 7, 3,
    3, 8, 4,
    4, 9, 5,
    5, 10, 1,


    6, 1, 10,
    7, 2, 6,
    8, 3, 7,
    9, 4, 8,
    10, 5, 9,

    11, 6, 10,
    11, 7, 6,
    11, 8, 7,
    11, 9, 8,
    11, 10, 9,
  };

  
  /****************************************************************
   * This code constructs a geodesic sphere which is used to
   * represent the vanderWaals and Connolly dot surfaces
   * One geodesic sphere is constructed. It is a unit sphere
   * with radius of 1.0 <p>
   * Many times a sphere is constructed with lines of latitude and
   * longitude. With this type of rendering, the atom has north and
   * south poles. And the faces are not regularly shaped ... at the
   * poles they are triangles but elsewhere they are quadrilaterals. <p>
   * I think that a geodesic sphere is more appropriate for this type
   * of application. The geodesic sphere does not have poles and 
   * looks the same in all orientations ... as a sphere should. All
   * faces are equilateral triangles. <p>
   * The geodesic sphere is constructed by starting with an icosohedron, 
   * a platonic solid with 12 vertices and 20 equilateral triangles
   * for faces. The call to the method <code>quadruple</code> will
   * split each triangular face into 4 faces by creating a new vertex
   * at the midpoint of each edge. These midpoints are still in the
   * plane, so they are then 'pushed out' to the surface of the
   * enclosing sphere by normalizing their length back to 1.0<p>
   * Individual atoms construct bitmaps to determine which dots are
   * visible and which are obscured. Each bit corresponds to a single
   * dot.<p>
   * The sequence of vertex counts is 12, 42, 162, 642. The vertices
   * are stored so that when atoms are small they can choose to display
   * only the first n bits where n is one of the above vertex counts.<p>
   * The vertices of the 'one true sphere' are rotated to the current
   * molecular rotation at the beginning of the repaint cycle. That way,
   * individual atoms only need to scale the unit vector to the vdw
   * radius for that atom. <p>
   * (If necessary, this on-the-fly scaling could be eliminated by
   * storing multiple geodesic spheres ... one per vdw radius. But
   * I suspect there are bigger performance problems with the saddle
   * and convex connolly surfaces.)<p>
   * I experimented with rendering the dots with light shading. However
   * I found that it was much harder to look at. The dots in the front
   * are lighter, but on a white background they are harder to see. The
   * end result is that I tended to focus on the back side of the sphere
   * of dots ... which made rotations very strange. So I turned off
   * shading of dot surfaces.
   ****************************************************************/

  class Geodesic {

    Vector3f[] vertices;
    Vector3f[] verticesTransformed;
    //    byte[] intensitiesTransformed;
    int screenCoordinateCount;
    int[] screenCoordinates;
    //    byte[] intensities;
    short[] faceIndices;
    int level;

    Geodesic() {
      level = 0;
      vertices = new Vector3f[12];
      vertices[0] = new Vector3f(0, 0, halfRoot5);
      for (int i = 0; i < 5; ++i) {
        vertices[i+1] = new Vector3f((float)Math.cos(i * oneFifth),
                                     (float)Math.sin(i * oneFifth),
                                     0.5f);
        vertices[i+6] = new Vector3f((float)Math.cos(i * oneFifth + oneTenth),
                                     (float)Math.sin(i * oneFifth + oneTenth),
                                     -0.5f);
      }
      vertices[11] = new Vector3f(0, 0, -halfRoot5);
      for (int i = 12; --i >= 0; )
        vertices[i].normalize();
      faceIndices = faceIndicesInitial;
      verticesTransformed = new Vector3f[12];
      for (int i = 12; --i >= 0; )
        verticesTransformed[i] = new Vector3f();
      screenCoordinates = new int[3 * 12];
      //      intensities = new byte[12];
      //      intensitiesTransformed = new byte[12];
    }

    void transform() {
      for (int i = vertices.length; --i >= 0; ) {
        Vector3f t = verticesTransformed[i];
        viewer.transformVector(vertices[i], t);
        //        intensitiesTransformed[i] =
        //          Shade3D.calcIntensity((float)t.x, (float)t.y, (float)t.z);
      }
    }

    float scaledRadius;
    int screenLevel;
    int screenDotCount;
    int screenCenterX, screenCenterY, screenCenterZ;
    
    void calcScreenPoints(int[] visibilityMap, float radius, int x, int y,
                          int z, int[] coordMap, boolean isSolid) {
      int dotCount;
      if (isSolid) {
        dotCount = 642;
        screenLevel = 3;
      } else if (scalePixelsPerAngstrom > 5) {
        dotCount = 42;
        screenLevel = 1;
        if (scalePixelsPerAngstrom > 10) {
          dotCount = 162;
          screenLevel = 2;
          if (scalePixelsPerAngstrom > 20) {
            dotCount = 642;
            screenLevel = 3;
            //		  if (scalePixelsPerAngstrom > 32) {
            //          screenLevel = 4; //untested
            //		      dotCount = 2562;
            //      }
          }
        }
      } else {
        dotCount = 12;
        screenLevel = 0;
      }
      screenDotCount = dotCount;
      screenCenterX = x;
      screenCenterY = y;
      screenCenterZ = z;

      //      if (coordMap != null)
      //      dotCount = 2562;
      scaledRadius = viewer.scaleToPerspective(z, radius);
      int icoordinates = 0;
      //      int iintensities = 0;
      int iDot = visibilityMap.length << 5;
      screenCoordinateCount = 0;
      if (iDot > dotCount)
        iDot = dotCount;
      while (--iDot >= 0) {
        if (!Dots.getBit(visibilityMap, iDot))
          continue;
        //        intensities[iintensities++] = intensitiesTransformed[iDot];
        Vector3f vertex = verticesTransformed[iDot];
        if (coordMap != null)
          coordMap[iDot] = icoordinates;
        screenCoordinates[icoordinates++] = x
            + (int) ((scaledRadius * vertex.x) + (vertex.x < 0 ? -0.5 : 0.5));
        screenCoordinates[icoordinates++] = y
            + (int) ((scaledRadius * vertex.y) + (vertex.y < 0 ? -0.5 : 0.5));
        screenCoordinates[icoordinates++] = z
            + (int) ((scaledRadius * vertex.z) + (vertex.z < 0 ? -0.5 : 0.5));
        ++screenCoordinateCount;
      }
    }

    void calcScreenPoint(short iDot, Point3i pt) {
      Vector3f vertex = verticesTransformed[iDot];
      pt.x = screenCenterX
          + (int) ((scaledRadius * vertex.x) + (vertex.x < 0 ? -0.5 : 0.5));
      pt.y = screenCenterY
          + (int) ((scaledRadius * vertex.y) + (vertex.y < 0 ? -0.5 : 0.5));
      pt.z = screenCenterZ
          + (int) ((scaledRadius * vertex.z) + (vertex.z < 0 ? -0.5 : 0.5));
    }
    
    short iVertexNew;
    Hashtable htVertex;
    
    void quadruple() {
      level++;
      htVertex = new Hashtable();
      int nVerticesOld = vertices.length;
      short[] faceIndicesOld = faceIndices;
      int nFaceIndicesOld = faceIndicesOld.length;
      int nEdgesOld = nVerticesOld + nFaceIndicesOld/3 - 2;
      int nVerticesNew = nVerticesOld + nEdgesOld;
      Vector3f[] verticesNew = new Vector3f[nVerticesNew];
      System.arraycopy(vertices, 0, verticesNew, 0, nVerticesOld);
      vertices = verticesNew;
      verticesTransformed = new Vector3f[nVerticesNew];
      for (int i = nVerticesNew; --i >= 0; )
        verticesTransformed[i] = new Vector3f();
      screenCoordinates = new int[3 * nVerticesNew];
      //      intensitiesTransformed = new byte[nVerticesNew];
      //      intensities

      short[] faceIndicesNew = new short[4 * nFaceIndicesOld];
      faceIndices = faceIndicesNew;
      iVertexNew = (short)nVerticesOld;
      
      int iFaceNew = 0;
      for (int i = 0; i < nFaceIndicesOld; ) {
        short iA = faceIndicesOld[i++];
        short iB = faceIndicesOld[i++];
        short iC = faceIndicesOld[i++];
        short iAB = getVertex(iA, iB);
        short iBC = getVertex(iB, iC);
        short iCA = getVertex(iC, iA);
        
        faceIndicesNew[iFaceNew++] = iA;
        faceIndicesNew[iFaceNew++] = iAB;
        faceIndicesNew[iFaceNew++] = iCA;

        faceIndicesNew[iFaceNew++] = iB;
        faceIndicesNew[iFaceNew++] = iBC;
        faceIndicesNew[iFaceNew++] = iAB;

        faceIndicesNew[iFaceNew++] = iC;
        faceIndicesNew[iFaceNew++] = iCA;
        faceIndicesNew[iFaceNew++] = iBC;

        faceIndicesNew[iFaceNew++] = iCA;
        faceIndicesNew[iFaceNew++] = iAB;
        faceIndicesNew[iFaceNew++] = iBC;
      }
      if (iFaceNew != faceIndicesNew.length) {
        Logger.debug("que?");
        throw new NullPointerException();
      }
      if (iVertexNew != nVerticesNew) {
        Logger.debug("huh? " + " iVertexNew=" + iVertexNew +
                           "nVerticesNew=" + nVerticesNew);
        throw new NullPointerException();
      }
      htVertex = null;
    }
    
    @SuppressWarnings("unchecked")
	private short getVertex(short i1, short i2) {
      if (i1 > i2) {
        short t = i1;
        i1 = i2;
        i2 = t;
      }
      Integer hashKey = new Integer((i1 << 16) + i2);
      Short iv = (Short)htVertex.get(hashKey);
      if (iv != null)
        return iv.shortValue();
      Vector3f vertexNew = new Vector3f(vertices[i1]);
      vertexNew.add(vertices[i2]);
      vertexNew.scale(0.5f);
      vertexNew.normalize();
      htVertex.put(hashKey, new Short(iVertexNew));
      vertices[iVertexNew] = vertexNew;
      return iVertexNew++;
    }
 /*   
    String showMap(int[] map) {
      String s = "showMap";
      int n = 0;
      int iDot = map.length << 5;
      while (--iDot >= 0)
        if (getBit(map, iDot)) {
          n++;
          s += " " + iDot;
        }
      s = n + " points:" + s;
      return s;
    }
 */ 
  }
}

