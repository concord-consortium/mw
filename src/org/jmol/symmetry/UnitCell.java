/* $RCSfile: UnitCell.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:13 $
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


package org.jmol.symmetry;

/*
 * Bob Hanson 9/2006
 * 
 */
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

public class UnitCell {
  
  final static float toRadians = (float) Math.PI * 2 / 360;
  final static Point3f[] unitCubePoints = { new Point3f(0, 0, 0),
      new Point3f(0, 0, 1), new Point3f(0, 1, 0), new Point3f(0, 1, 1),
      new Point3f(1, 0, 0), new Point3f(1, 0, 1), new Point3f(1, 1, 0),
      new Point3f(1, 1, 1), };

  public final static int INFO_A = 0;
  public final static int INFO_B = 1;
  public final static int INFO_C = 2;
  public final static int INFO_ALPHA = 3;
  public final static int INFO_BETA = 4;
  public final static int INFO_GAMMA = 5;
  
  float a, b, c, alpha, beta, gamma;
  float[] notionalUnitcell; //6 parameters + 16 matrix items
  Matrix4f matrixNotional;
  Matrix4f matrixCartesianToFractional;
  Matrix4f matrixFractionalToCartesian;
  Point3f[] vertices; // eight corners

  Point3f cartesianOffset = new Point3f();
  Point3f fractionalOffset = new Point3f();
  
  public UnitCell(float[] notionalUnitcell) {
    setUnitCell(notionalUnitcell);
  }

  public final void toCartesian(Point3f pt) {
    if (matrixFractionalToCartesian == null)
      return;
    matrixFractionalToCartesian.transform(pt);
  }
  
  public final void toFractional(Point3f pt) {
    if (matrixCartesianToFractional == null)
      return;
    matrixCartesianToFractional.transform(pt);
  }
  
  public void setOffset(Point3f pt) {
    // from "unitcell {i j k}" via uccage
    fractionalOffset.set(pt);
    cartesianOffset.set(pt);
    matrixFractionalToCartesian.transform(cartesianOffset);
  }

  public void setOffset(int nnn) {
    // from "unitcell ijk" via uccage
    Point3f cell = new Point3f();
    cell.x = nnn / 100 - 5;
    cell.y = (nnn % 100) / 10 - 5;
    cell.z = (nnn % 10) - 5;
    setOffset(cell);
  }

  public final String dumpInfo(boolean isFull) {
    return "a=" + a + ", b=" + b + ", c=" + c + ", alpha=" + alpha + ", beta=" + beta + ", gamma=" + gamma
       + (isFull ? "\nfractional to cartesian: " + matrixFractionalToCartesian 
       + "\ncartesian to fractional: " + matrixCartesianToFractional : "");
  }

  public Point3f[] getVertices() {
    return vertices;
  }
  
  public Point3f getCartesianOffset() {
    return cartesianOffset;
  }
  
  public Point3f getFractionalOffset() {
    return fractionalOffset;
  }
  
  public float[] getNotionalUnitCell() {
    return notionalUnitcell;
  }
  
  public float getInfo(int infoType) {
    switch (infoType) {
    case INFO_A:
      return a;
    case INFO_B:
      return b;
    case INFO_C:
      return c;
    case INFO_ALPHA:
      return alpha;
    case INFO_BETA:
      return beta;
    case INFO_GAMMA:
      return gamma;
    }
    return Float.NaN;
  }
  
  /// private methods
  
  private void setUnitCell(float[] notionalUnitcell) {
    if (notionalUnitcell == null || notionalUnitcell[0] == 0)
      return;
    this.notionalUnitcell = notionalUnitcell;

    a = notionalUnitcell[INFO_A];
    b = notionalUnitcell[INFO_B];
    c = notionalUnitcell[INFO_C];
    alpha = notionalUnitcell[INFO_ALPHA];
    beta = notionalUnitcell[INFO_BETA];
    gamma = notionalUnitcell[INFO_GAMMA];
    calcNotionalMatrix();
    constructFractionalMatrices();
    calcUnitcellVertices();
  }

  private final void calcNotionalMatrix() {
    // note that these are oriented as columns, not as row
    // this is because we will later use the transform method,
    // which operates M * P, where P is a column vector
    matrixNotional = new Matrix4f();

    float cosAlpha = (float) Math.cos(toRadians * alpha);
    float cosBeta = (float) Math.cos(toRadians * beta);
    float cosGamma = (float) Math.cos(toRadians * gamma);
    float sinGamma = (float) Math.sin(toRadians * gamma);

    // 1. align the a axis with x axis
    matrixNotional.setColumn(0, a, 0, 0, 0);
    // 2. place the b is in xy plane making a angle gamma with a
    matrixNotional.setColumn(1, b * cosGamma, b * sinGamma, 0, 0);
    // 3. now the c axis,
    // http://server.ccl.net/cca/documents/molecular-modeling/node4.html
    float V = a
        * b
        * c
        * (float) Math.sqrt(1.0 - cosAlpha * cosAlpha - cosBeta * cosBeta
            - cosGamma * cosGamma + 2.0 * cosAlpha * cosBeta * cosGamma);
    matrixNotional.setColumn(2, c * cosBeta, c
        * (cosAlpha - cosBeta * cosGamma) / sinGamma, V / (a * b * sinGamma), 0);
    matrixNotional.setColumn(3, 0, 0, 0, 1);
  }

  private final void constructFractionalMatrices() {
    if (notionalUnitcell.length > 6 && !Float.isNaN(notionalUnitcell[6])) {
        float[] scaleMatrix = new float[16];
      for (int i = 0; i < 16; i++)
        scaleMatrix[i] = notionalUnitcell[6 + i];
      matrixCartesianToFractional = new Matrix4f(scaleMatrix);
      matrixFractionalToCartesian = new Matrix4f();
      matrixFractionalToCartesian.invert(matrixCartesianToFractional);
    } else {
      //System.out.println("notional: "+matrixNotional);
      matrixFractionalToCartesian = matrixNotional;
      matrixCartesianToFractional = new Matrix4f();
      matrixCartesianToFractional.invert(matrixFractionalToCartesian);
    }
    
    /* 
    Point3f v = new Point3f(1,2,3);
    toFractional(v);
    System.out.println("fractionaltocart:" + matrixFractionalToCartesian);
    System.out.println("testing mat.transform [1 2 3]" + matrixCartesianToFractional+v);
    */
  }

  private final void calcUnitcellVertices() {
    vertices = new Point3f[8];
    for (int i = 8; --i >= 0;) {
      vertices[i] = new Point3f();
      matrixFractionalToCartesian.transform(unitCubePoints[i], vertices[i]);
    }
  }
}
