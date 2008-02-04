/* $RCSfile: UccageRenderer.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:12 $
 * $Revision: 1.11 $
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.viewer;

import javax.vecmath.Point3i;
import javax.vecmath.Point3f;
import java.text.NumberFormat;

import org.jmol.g3d.Graphics3D;
import org.jmol.symmetry.UnitCell;

class UccageRenderer extends ShapeRenderer {

  NumberFormat nf;
  byte fid;
  void initRenderer() {
  }

  final Point3i[] screens = new Point3i[8];
  final Point3f[] verticesT = new Point3f[8];  
  {
    for (int i = 8; --i >= 0; ) {
      screens[i] = new Point3i();
      verticesT[i] = new Point3f();
    }
  }

  Uccage uccage;

  void render() {
    uccage = (Uccage) shape;
    short colix = uccage.colix;
    short mad = uccage.mad;
    if (mad == 0)
      return;
    render1(mad, colix);
  }
  
  void render1(short mad, short colix) {
    if (frame.cellInfos == null)
      return;
    UnitCell unitCell = viewer.getCurrentUnitCell();
    if (unitCell == null)
      return;
    Frame.CellInfo cellInfo = frame.cellInfos[viewer.getDisplayModelIndex()];
    Point3f[] vertices = unitCell.getVertices();
    Point3f offset = unitCell.getCartesianOffset(); 
    if (colix == 0)
      colix = Graphics3D.OLIVE;
    for (int i = 8; --i >= 0;)
      verticesT[i].add(vertices[i], offset);
    BbcageRenderer.render(viewer, g3d, mad, colix, verticesT, screens);
    if (!viewer.getDisplayCellParameters())
      return;
    if (nf == null) {
      nf = NumberFormat.getInstance();
      fid = g3d.getFontFid("Monospaced", 14);
    }
    if (nf != null) {
      nf.setMaximumFractionDigits(3);
      nf.setMinimumFractionDigits(3);
    }
    g3d.setFont(fid);
    int line = 15;
    int lineheight = 15;
    if (cellInfo.spaceGroup != null) {
      line += lineheight;
      g3d.drawStringNoSlab(cellInfo.spaceGroup, null, colix, (short) 0, 5,
          line, 0);
    }
    line += lineheight;
    g3d.drawStringNoSlab("a=" + nfformat(unitCell.getInfo(UnitCell.INFO_A)) + "\u00C5", null, colix,
        (short) 0, 5, line, 0);
    line += lineheight;
    g3d.drawStringNoSlab("b=" + nfformat(unitCell.getInfo(UnitCell.INFO_B)) + "\u00C5", null, colix,
        (short) 0, 5, line, 0);
    line += lineheight;
    g3d.drawStringNoSlab("c=" + nfformat(unitCell.getInfo(UnitCell.INFO_C)) + "\u00C5", null, colix,
        (short) 0, 5, line, 0);
    if (nf != null)
      nf.setMaximumFractionDigits(1);
    line += lineheight;
    g3d.drawStringNoSlab("\u03B1=" + nfformat(unitCell.getInfo(UnitCell.INFO_ALPHA)) + "\u00B0", null,
        colix, (short) 0, 5, line, 0);
    line += lineheight;
    g3d.drawStringNoSlab("\u03B2=" + nfformat(unitCell.getInfo(UnitCell.INFO_BETA)) + "\u00B0", null,
        colix, (short) 0, 5, line, 0);
    line += lineheight;
    g3d.drawStringNoSlab("\u03B3=" + nfformat(unitCell.getInfo(UnitCell.INFO_GAMMA)) + "\u00B0", null,
        colix, (short) 0, 5, line, 0);
  }
  
  String nfformat(float x) {
    if (nf == null)
      return "" + x;
    return nf.format(x);
  }
}

