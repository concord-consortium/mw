/* $RCSfile: MeasuresRenderer.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:09 $
 * $Revision: 1.12 $
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

package org.jmol.viewer;

import org.jmol.g3d.*;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;

class MeasuresRenderer extends ShapeRenderer {

  boolean showMeasurementNumbers;
  short measurementMad;
  Font3D font3d;
  Measurement measurement;
  boolean doJustify;
  
  void render() {
    if (!viewer.getShowMeasurements())
      return;

    Measures measures = (Measures) shape;
    doJustify = viewer.getJustifyMeasurements();
    measurementMad = measures.mad;
    font3d = measures.font3d;
    showMeasurementNumbers = measures.showMeasurementNumbers;
    measures.setVisibilityInfo();
    short colix;
    for (int i = measures.measurementCount; --i >= 0;) {
      if (!measures.measurements[i].isVisible)
        continue;
      colix = measures.measurements[i].colix;
      if (colix == 0)
        colix = measures.colix;
      if (colix == 0)
        colix = viewer.getColixBackgroundContrast();
      renderMeasurement(measures.measurements[i], colix);
    }
    renderPendingMeasurement(measures.pendingMeasurement);
  }

  void renderMeasurement(Measurement measurement, short colix) {
    renderMeasurement(measurement.count, measurement, colix, true); 
  }

  void renderMeasurement(int count, Measurement measurement,
                         short colix, boolean renderArcs) {
    this.measurement = measurement;
    switch(count) {
    case 2:
      renderDistance(colix);
      break;
    case 3:
      renderAngle(colix, renderArcs);
      break;
    case 4:
      renderTorsion(colix, renderArcs);
      break;
    default:
      throw new NullPointerException();
    }
  }

  int drawSegment(int x1, int y1, int z1, int x2, int y2, int z2,
                  short colix) {
    if (measurementMad < 0) {
      g3d.drawDashedLine(colix, 4, 2, x1, y1, z1, x2, y2, z2);
      return 1;
    }
    int widthPixels = measurementMad;
    if (measurementMad >= 20)
      widthPixels = viewer.scaleToScreen((z1 + z2) / 2, measurementMad);
    g3d.fillCylinder(colix, Graphics3D.ENDCAPS_FLAT,
                     widthPixels, x1, y1, z1, x2, y2, z2);

    return (widthPixels + 1) / 2;
  }

  void renderDistance(short colix) {
    
    renderDistance(frame.getAtomAt(measurement.countPlusIndices[1]),
                   frame.getAtomAt(measurement.countPlusIndices[2]), colix);
  }

  void renderDistance(Atom atomA, Atom atomB, short colix) {
    int zA = atomA.screenZ - atomA.screenDiameter - 10;
    int zB = atomB.screenZ - atomB.screenDiameter - 10;
    int radius = drawSegment(atomA.screenX, atomA.screenY, zA, atomB
        .screenX, atomB.screenY, zB, colix);
    int z = (zA + zB) / 2;
    if (z < 1)
      z = 1;
    int x = (atomA.screenX + atomB.screenX) / 2;
    int y = (atomA.screenY + atomB.screenY) / 2;
    paintMeasurementString(x, y, z, radius, colix, (x - atomA.screenX)*(y-atomA.screenY) > 0, 0);
  }
                           

  AxisAngle4f aaT = new AxisAngle4f();
  Matrix3f matrixT = new Matrix3f();
  Point3f pointT = new Point3f();

  void renderAngle(short colix, boolean renderArcs) {
    renderAngle(frame.getAtomAt(measurement.countPlusIndices[1]),
                frame.getAtomAt(measurement.countPlusIndices[2]),
                frame.getAtomAt(measurement.countPlusIndices[3]),
                colix, renderArcs);
  }

  void renderAngle(Atom atomA, Atom atomB, Atom atomC,
                   short colix, boolean renderArcs) {
    g3d.setColix(colix);
    int zA = atomA.screenZ - atomA.screenDiameter - 10;
    int zB = atomB.screenZ - atomB.screenDiameter - 10;
    int zC = atomC.screenZ - atomC.screenDiameter - 10;
    int zOffset = (zA + zB + zC) / 3;
    int radius = drawSegment(atomA.screenX, atomA.screenY, zA,
                             atomB.screenX, atomB.screenY, zB,
                             colix);
    radius += drawSegment(atomB.screenX, atomB.screenY, zB,
                          atomC.screenX, atomC.screenY, zC, colix);
    radius = (radius + 1) / 2;

    if (! renderArcs)
      return;


    // FIXME mth -- this really should be a function of pixelsPerAngstrom
    // in addition, the location of the arc is not correct
    // should probably be some percentage of the smaller distance
    AxisAngle4f aa = measurement.aa;
    if (aa == null) { // 180 degrees
      paintMeasurementString(atomB.screenX + 5, atomB.screenY - 5,
                             zB, radius, colix, false, 0);
      return;
    }
    int dotCount = (int)((aa.angle / (2 * Math.PI)) * 64);
    float stepAngle = aa.angle / dotCount;
    aaT.set(aa);
    int iMid = dotCount / 2;
    for (int i = dotCount; --i >= 0; ) {
      aaT.angle = i * stepAngle;
      matrixT.set(aaT);
      pointT.set(measurement.pointArc);
      matrixT.transform(pointT);
      pointT.add(atomB);
      //CAUTION! screenArc and screenLabel are the SAME OBJECT, TransformManager.point3iScreenTemp
      Point3i screenArc = viewer.transformPoint(pointT);
      int zArc = screenArc.z - zOffset;
      if (zArc < 0) zArc = 0;
      g3d.drawPixel(screenArc.x, screenArc.y, zArc);
      if (i == iMid) {
        pointT.set(measurement.pointArc);
        pointT.scale(1.1f);
        matrixT.transform(pointT);
        pointT.add(atomB);
        Point3i screenLabel = viewer.transformPoint(pointT);
        int zLabel = screenLabel.z - zOffset;
        paintMeasurementString(screenLabel.x, screenLabel.y, zLabel,
                               radius, colix, screenLabel.x < atomB.screenX, atomB.screenY);
      }
    }
  }

  void renderTorsion(short colix, boolean renderArcs) {
    int[] countPlusIndices = measurement.countPlusIndices;
    renderTorsion(frame.getAtomAt(countPlusIndices[1]),
                  frame.getAtomAt(countPlusIndices[2]),
                  frame.getAtomAt(countPlusIndices[3]),
                  frame.getAtomAt(countPlusIndices[4]),
                  colix, renderArcs);
  }

  void renderTorsion(Atom atomA, Atom atomB, Atom atomC, Atom atomD,
                     short colix, boolean renderArcs) {
    int zA = atomA.screenZ - atomA.screenDiameter - 10;
    int zB = atomB.screenZ - atomB.screenDiameter - 10;
    int zC = atomC.screenZ - atomC.screenDiameter - 10;
    int zD = atomD.screenZ - atomD.screenDiameter - 10;
    int radius = drawSegment(atomA.screenX, atomA.screenY, zA, atomB.screenX, atomB.screenY, zB,
                             colix);
    radius += drawSegment(atomB.screenX, atomB.screenY, zB, atomC.screenX, atomC.screenY, zC, colix);
    radius += drawSegment(atomC.screenX, atomC.screenY, zC, atomD.screenX, atomD.screenY, zD, colix);
    radius /= 3;
    paintMeasurementString((atomA.screenX + atomB.screenX + atomC.screenX + atomD.screenX) / 4,
                           (atomA.screenY + atomB.screenY + atomC.screenY + atomD.screenY) / 4,
                           (zA + zB + zC + zD) / 4, radius, colix, false, 0);
  }

  void paintMeasurementString(int x, int y, int z, int radius, short colix,
                              boolean rightJustify, int yRef) {
    if (!showMeasurementNumbers)
      return;
    // XIE
    if(viewer instanceof ExtendedViewer) {
    	measurement.formatMeasurement();
    }
    if (!doJustify) {
      rightJustify = false;
      yRef = y;
    }
    String strMeasurement = measurement.strMeasurement;
    if (strMeasurement == null)
      return;
    int xT = x;
    if (rightJustify)
      xT -= radius / 2 + 2 + font3d.fontMetrics.stringWidth(strMeasurement);
    else
      xT += radius / 2 + 2;
    int yT = y
        + (yRef == 0 || yRef < y ? font3d.fontMetrics.getAscent() : -radius / 2);
    int zT = z - radius - 2;
    if (zT < 1)
      zT = 1;
    g3d.drawString(strMeasurement, font3d, colix, xT, yT, zT, zT);
  }

  void renderPendingMeasurement(PendingMeasurement pendingMeasurement) {
    int count = pendingMeasurement.count;
    int[] countPlusIndices = pendingMeasurement.countPlusIndices;
    if (! pendingMeasurement.isActive || count < 2)
      return;
    short colixRubberband = viewer.getColixRubberband();
    if (countPlusIndices[count] == -1)
      renderPendingWithCursor(pendingMeasurement, colixRubberband);
    else
      renderMeasurement(pendingMeasurement, colixRubberband);
  }
  
  void renderPendingWithCursor(PendingMeasurement pendingMeasurement,
                               short colixRubberband) {
    int count = pendingMeasurement.count;
    if (count < 2)
      return;
    if (count > 2)
      renderMeasurement(count - 1, pendingMeasurement, colixRubberband, false);
    Atom atomLast = frame.getAtomAt(pendingMeasurement.
                                    countPlusIndices[count - 1]);
    int lastZ = atomLast.screenZ - atomLast.screenDiameter - 10;
    drawSegment(atomLast.screenX, atomLast.screenY, lastZ,
                viewer.getCursorX(), viewer.getCursorY(), 0, colixRubberband);
  }
}
