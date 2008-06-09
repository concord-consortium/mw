/* $RCSfile: Line3D.java,v $
 * $Author: qxie $
 * $Date: 2007-03-28 15:24:21 $
 * $Revision: 1.11 $
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
package org.myjmol.g3d;

/**
 *<p>
 * Implements 3D line drawing routines.
 *</p>
 *<p>
 * A number of line drawing routines, most of which are used to
 * implement higher-level shapes. Triangles and cylinders are drawn
 * as a series of lines
 *</p>
 *
 * @author Miguel, miguel@jmol.org and Bob Hanson, hansonr@stolaf.edu
 */

 /* rewritten by Bob 7/2006 to fully implement the capabilities of the
  * Cohen-Sutherland algorithm.
  * 
  * added line bitset option for rockets. Rendering times for bonds done this way are a bit slower.
  */

import java.util.BitSet;
import java.util.Hashtable;

final class Line3D {

  Graphics3D g3d;

  Line3D(Graphics3D g3d) {
    this.g3d = g3d;
  }

  BitSet lineBits;
  float slope;
  boolean lineTypeX;
  int lineDirection;
  int nBits;
  int nCached = 0;
  int nFound = 0;
  //int test = 5;
  
  @SuppressWarnings("unchecked")
void setLineBits(float dx, float dy) {    
    slope = (dx != 0 ?  dy / dx : dy >= 0 ? Float.MAX_VALUE  : -Float.MAX_VALUE);
    lineTypeX = (slope <=1 && slope >= -1);
    lineDirection = (slope < 0 ? -1 : 1);
    if (getCachedLine())
      return;
    nBits = (lineTypeX ? g3d.width : g3d.height);
    lineBits = new BitSet(nBits);
    dy = Math.abs(dy);
    dx = Math.abs(dx);
    if (dy > dx) {
      float t = dx;
      dx = dy;
      dy = t;
    }
    int twoDError = 0;
    float twoDx = dx + dx, twoDy = dy + dy;
    for (int i = 0; i < nBits; i++) {
      twoDError += twoDy;
      if (twoDError > dx) {
        lineBits.set(i);
        twoDError -= twoDx;
      }
    }    
    lineCache.put(slopeKey, lineBits);
    nCached++;
    //if (--test > 0 || ((100-test) % 100 == 0)) System.out.println(test+" "+dx + " " + dy + " " + lineBits);
  }
  
  Hashtable lineCache = new Hashtable();
  Float slopeKey;
  boolean getCachedLine() {
    slopeKey = new Float(slope);
    if (!lineCache.containsKey(slopeKey))
      return false;
    lineBits = (BitSet) lineCache.get(slopeKey);
    nFound++;
    if (nFound == 1000000)
      System.out.println("nCached/nFound lines: " + nCached + " " + nFound);
    return true;
  }
  
  void drawHLine(int argb, boolean tScreened, int x, int y, int z, int w) {
    // hover, labels only
    int width = g3d.width;
    if (w < 0) {
      x += w;
      w = -w;
    }
    int[] pbuf = g3d.pbuf;
    int[] zbuf = g3d.zbuf;
    if (x < 0) {
      w += x;
      x = 0;
    }
    if (x + w >= width) {
      w = width - 1 - x;
    }
    int offset = x + width * y;
    if (!tScreened) {
      for (int i = 0; i <= w; i++) {
        if (z < zbuf[offset]) {
          zbuf[offset] = z;
          pbuf[offset] = argb;
        }
        offset++;
      }
      return;
    }
    boolean flipflop = ((x ^ y) & 1) != 0;
    for (int i = 0; i <= w; i++) {
      if ((flipflop = !flipflop) && z < zbuf[offset]) {
        zbuf[offset] = z;
        pbuf[offset] = argb;
      }
      offset++;
    }
  }

  final static int VISIBILITY_UNCLIPPED = 0;
  final static int VISIBILITY_CLIPPED = 1;
  final static int VISIBILITY_OFFSCREEN = 2;

  void drawVLine(int argb, boolean tScreened, int x, int y, int z, int h) {
    // hover, labels only
    int width = g3d.width;
    int height = g3d.height;
    if (h < 0) {
      y += h;
      h = -h;
    }
    int[] pbuf = g3d.pbuf;
    int[] zbuf = g3d.zbuf;
    if (y < 0) {
      h += y;
      y = 0;
    }
    if (y + h >= height) {
      h = height - 1 - y;
    }
    int offset = x + width * y;
    if (!tScreened) {
      for (int i = 0; i <= h; i++) {
        if (z < zbuf[offset]) {
          zbuf[offset] = z;
          pbuf[offset] = argb;
        }
        offset += width;
      }
      return;
    }
    boolean flipflop = ((x ^ y) & 1) != 0;
    for (int i = 0; i <= h; i++) {
      if ((flipflop = !flipflop) && z < zbuf[offset]) {
        zbuf[offset] = z;
        pbuf[offset] = argb;
      }
      offset += width;
    }
  }

  int x1t, y1t, z1t, x2t, y2t, z2t, cc1, cc2; // trimmed

  /**
   *<p>
   * Cohen-Sutherland line clipping used to check visibility.
   *</p>
   *<p>
   * Note that this routine is only used for visibility checking. To avoid
   * integer rounding errors which cause cracking to occur in 'solid'
   * surfaces, the lines are actually drawn from their original end-points.
   * 
   * The nuance is that this algorithm doesn't just deliver a boolean. It
   * delivers the trimmed line. Although we need to start the raster loop
   * at the origin for good surfaces, we can save lots of time by saving the
   * known endpoints as globals variables. -- Bob Hanson 7/06
   *</p>
   *
   * @return Visibility (see VISIBILITY_... constants);
   */

  int getTrimmedLine() {   // formerly "visibilityCheck()"

    cc1 = clipCode(x1t, y1t, z1t);
    cc2 = clipCode(x2t, y2t, z2t);
    if ((cc1 | cc2) == 0)
      return VISIBILITY_UNCLIPPED;

    int xLast = g3d.xLast;
    int yLast = g3d.yLast;
    int slab = g3d.slab;
    int depth = g3d.depth;
    do {
      if ((cc1 & cc2) != 0)
        return VISIBILITY_OFFSCREEN;

      float dx = x2t - x1t;
      float dy = y2t - y1t;
      float dz = z2t - z1t;
      if (cc1 != 0) { //cohen-sutherland line clipping
        if ((cc1 & xLT) != 0) {
          y1t += (-x1t * dy) / dx;
          z1t += (-x1t * dz) / dx;
          x1t = 0;
        } else if ((cc1 & xGT) != 0) {
          y1t += ((xLast - x1t) * dy) / dx;
          z1t += ((xLast - x1t) * dz) / dx;
          x1t = xLast;
        } else if ((cc1 & yLT) != 0) {
          x1t += (-y1t * dx) / dy;
          z1t += (-y1t * dz) / dy;
          y1t = 0;
        } else if ((cc1 & yGT) != 0) {
          x1t += ((yLast - y1t) * dx) / dy;
          z1t += ((yLast - y1t) * dz) / dy;
          y1t = yLast;
        } else if ((cc1 & zLT) != 0) {
          x1t += ((slab - z1t) * dx) / dz;
          y1t += ((slab - z1t) * dy) / dz;
          z1t = slab;
        } else // must be zGT
        {
          x1t += ((depth - z1t) * dx) / dz;
          y1t += ((depth - z1t) * dy) / dz;
          z1t = depth;
        }

        cc1 = clipCode(x1t, y1t, z1t);
      } else {
        if ((cc2 & xLT) != 0) {
          y2t += (-x2t * dy) / dx;
          z2t += (-x2t * dz) / dx;
          x2t = 0;
        } else if ((cc2 & xGT) != 0) {
          y2t += ((xLast - x2t) * dy) / dx;
          z2t += ((xLast - x2t) * dz) / dx;
          x2t = xLast;
        } else if ((cc2 & yLT) != 0) {
          x2t += (-y2t * dx) / dy;
          z2t += (-y2t * dz) / dy;
          y2t = 0;
        } else if ((cc2 & yGT) != 0) {
          x2t += ((yLast - y2t) * dx) / dy;
          z2t += ((yLast - y2t) * dz) / dy;
          y2t = yLast;
        } else if ((cc2 & zLT) != 0) {
          x2t += ((slab - z2t) * dx) / dz;
          y2t += ((slab - z2t) * dy) / dz;
          z2t = slab;
        } else // must be zGT
        {
          x2t += ((depth - z2t) * dx) / dz;
          y2t += ((depth - z2t) * dy) / dz;
          z2t = depth;
        }
        cc2 = clipCode(x2t, y2t, z2t);
      }
    } while ((cc1 | cc2) != 0);
    //System.out.println("trimmed line " + x1t + " " + y1t + " " + z1t + " " + x2t + " " + y2t + " " + z2t + " " + cc1 + "/" + cc2);
    return VISIBILITY_CLIPPED;
  }

  final static int zLT = 32;
  final static int zGT = 16;
  final static int xLT = 8;
  final static int xGT = 4;
  final static int yLT = 2;
  final static int yGT = 1;

  final int clipCode(int x, int y, int z) {
    int code = 0;
    if (x < 0)
      code |= xLT;
    else if (x >= g3d.width)
      code |= xGT;

    if (y < 0)
      code |= yLT;
    else if (y >= g3d.height)
      code |= yGT;

    if (z < g3d.slab)
      code |= zLT;
    else if (z > g3d.depth) // note that this is .GT., not .GE.
      code |= zGT;

    return code;
  }

  void plotLine(int argbA, boolean tScreenedA, int argbB, boolean tScreenedB,
                int xA, int yA, int zA, int xB, int yB, int zB,
                boolean notClipped) {
    // primary method for mesh triangle, quadrilateral, hermite, backbone,
    // sticks, and stars
    x1t = xA;
    x2t = xB;
    y1t = yA;
    y2t = yB;
    z1t = zA;
    z2t = zB;
    switch (notClipped ? VISIBILITY_UNCLIPPED : getTrimmedLine()) {
    case VISIBILITY_UNCLIPPED:
    case VISIBILITY_CLIPPED:
      plotLineClipped(argbA, tScreenedA, argbB, tScreenedB, xA, yA, zA,
          xB - xA, yB - yA, zB - zA, notClipped, 0, 0);
    }
  }

  void plotLineDelta(int argbA, boolean tScreenedA, int argbB,
                     boolean tScreenedB, int xA, int yA, int zA, int dxBA,
                     int dyBA, int dzBA, boolean notClipped) {
    // from cylinder -- endcaps open or flat, diameter 1, cone
    x1t = xA;
    x2t = xA + dxBA;
    y1t = yA;
    y2t = yA + dyBA;
    z1t = zA;
    z2t = zA + dzBA;
    switch (notClipped ? VISIBILITY_UNCLIPPED : getTrimmedLine()) {
    case VISIBILITY_UNCLIPPED:
    case VISIBILITY_CLIPPED:
      plotLineClipped(argbA, tScreenedA, argbB, tScreenedB, xA, yA, zA,
          dxBA, dyBA, dzBA, notClipped, 0, 0);
    }
  }

  void plotLineDelta(int[] shades1, boolean tScreened1, int[] shades2,
                     boolean tScreened2, int intensity, int xA, int yA, int zA,
                     int dxBA, int dyBA, int dzBA, boolean notClipped) {
    // from cylinder -- standard bond with two colors and translucencies
    x1t = xA;
    x2t = xA + dxBA;
    y1t = yA;
    y2t = yA + dyBA;
    z1t = zA;
    z2t = zA + dzBA;
    switch (notClipped ? VISIBILITY_UNCLIPPED : getTrimmedLine()) {
    case VISIBILITY_UNCLIPPED:
    case VISIBILITY_CLIPPED:
      //      System.out.println("plotlinedelta " + notClipped + " " + xA + " " + yA
      //        + " " + zA + " " + dxBA + " " + dyBA + " " + dzBA);

      plotLineClipped(shades1, tScreened1, shades2, tScreened2, intensity, xA,
          yA, zA, dxBA, dyBA, dzBA, notClipped, 0, 0);
    }
  }

  void plotLineDeltaBits(int[] shades1, boolean tScreened1, int[] shades2,
                     boolean tScreened2, int intensity, int xA, int yA, int zA,
                     int dxBA, int dyBA, int dzBA, boolean notClipped) {
    // from cylinder -- cartoonRockets
    x1t = xA;
    x2t = xA + dxBA;
    y1t = yA;
    y2t = yA + dyBA;
    z1t = zA;
    z2t = zA + dzBA;
    switch (notClipped ? VISIBILITY_UNCLIPPED : getTrimmedLine()) {
    case VISIBILITY_UNCLIPPED:
    case VISIBILITY_CLIPPED:
      //      System.out.println("plotlinedelta " + notClipped + " " + xA + " " + yA
      //        + " " + zA + " " + dxBA + " " + dyBA + " " + dzBA);

      plotLineClippedBits(shades1, tScreened1, shades2, tScreened2, intensity, xA,
          yA, zA, dxBA, dyBA, dzBA, notClipped, 0, 0);
    }
  }

  void plotDashedLine(int argbA, boolean tScreenedA, int argbB,
                      boolean tScreenedB, int run, int rise, int xA, int yA,
                      int zA, int xB, int yB, int zB, boolean notClipped) {
    // measures, axes, bbcage only    
    x1t = xA;
    x2t = xB;
    y1t = yA;
    y2t = yB;
    z1t = zA;
    z2t = zB;
    switch (notClipped ? VISIBILITY_UNCLIPPED : getTrimmedLine()) {
    case VISIBILITY_UNCLIPPED:
    case VISIBILITY_CLIPPED:
      plotLineClipped(argbA, tScreenedA, argbB, tScreenedB, xA, yA, zA,
          xB - xA, yB - yA, zB - zA, notClipped, run, rise);
    }
  }

  private void plotLineClipped(int argb1, boolean tScreened1, int argb2,
                               boolean tScreened2, int x, int y, int z, int dx,
                               int dy, int dz, boolean notClipped, int run,
                               int rise) {
    // standard, dashed or not dashed
    int[] pbuf = g3d.pbuf;
    int[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int runIndex = 0;
    if (run == 0) {
      rise = Integer.MAX_VALUE;
      run = 1;
    }
    int offset = y * width + x;
    int offsetMax = pbuf.length;
    boolean flipflop = (((x ^ y) & 1) != 0);
    boolean tScreened = tScreened1;
    int argb = argb1;
    if ((!tScreened || (flipflop = !flipflop)) && notClipped && offset >= 0
        && offset < offsetMax && z < zbuf[offset]) {
      zbuf[offset] = z;
      pbuf[offset] = argb1;
    }
    if (dx == 0 && dy == 0)
      return;
    int xIncrement = 1;
    int yOffsetIncrement = width;

    int x2 = x + dx;
    int y2 = y + dy;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0)
        roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      int n1 = Math.abs(x2 - x2t) + 1;
      int n2 = Math.abs(x2 - x1t) - 1;
      for (int n = dx - 1, nMid = n / 2; --n >= n1;) {
        if (n == nMid) {
          tScreened = tScreened2;
          argb = argb2;
        }
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
          flipflop = !flipflop;
        }
        if ((!tScreened || (flipflop = !flipflop)) && n < n2 && offset >= 0
            && offset < offsetMax && runIndex < rise) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent < zbuf[offset]) {
            zbuf[offset] = zCurrent;
            pbuf[offset] = argb;
          }
        }
        runIndex = (runIndex + 1) % run;
      }
    } else {
      int roundingFactor = dy - 1;
      if (dz < 0)
        roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
      int twoDyAccumulatedXError = 0;
      int n1 = Math.abs(y2 - y2t) + 1;
      int n2 = Math.abs(y2 - y1t) - 1;
      for (int n = dy - 1, nMid = n / 2; --n >= n1;) {
        if (n == nMid) {
          tScreened = tScreened2;
          argb = argb2;
        }
        offset += yOffsetIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDyAccumulatedXError += twoDx;
        if (twoDyAccumulatedXError > dy) {
          offset += xIncrement;
          twoDyAccumulatedXError -= twoDy;
          flipflop = !flipflop;
        }
        if ((!tScreened || (flipflop = !flipflop)) && n < n2 && offset >= 0
            && offset < offsetMax && runIndex < rise) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent < zbuf[offset]) {
            zbuf[offset] = zCurrent;
            pbuf[offset] = argb;
          }
        }
        runIndex = (runIndex + 1) % run;
      }
    }
  }

  private void plotLineClipped(int[] shades1, boolean tScreened1,
                               int[] shades2, boolean tScreened2,
                               int intensity, int x, int y, int z, int dx,
                               int dy, int dz, boolean notClipped, int run,
                               int rise) {
    // special shading for bonds
    int[] pbuf = g3d.pbuf;
    int[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int runIndex = 0;
    if (run == 0) {
      rise = Integer.MAX_VALUE;
      run = 1;
    }
    int offset = y * width + x;
    int offsetMax = pbuf.length;
    int intensityUp = (intensity < Shade3D.shadeLast ? intensity + 1
        : intensity);
    int intensityDn = (intensity > 0 ? intensity - 1 : intensity);
    int argb1 = shades1[intensity];
    int argb1Up = shades1[intensityUp];
    int argb1Dn = shades1[intensityDn];
    int argb2 = shades2[intensity];
    int argb2Up = shades2[intensityUp];
    int argb2Dn = shades2[intensityDn];
    boolean tScreened = tScreened1;
    boolean flipflop = (((x ^ y) & 1) != 0);
    if ((!tScreened || (flipflop = !flipflop)) && notClipped && offset >= 0
        && offset < offsetMax && z < zbuf[offset]) {
      zbuf[offset] = z;
      pbuf[offset] = argb1;
    }
    if (dx == 0 && dy == 0) {
      return;
    }
    int xIncrement = 1;
    int yOffsetIncrement = width;
    int x2 = x + dx;
    int y2 = y + dy;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z << 10;
    int argb = argb1;
    int argbUp = argb1Up;
    int argbDn = argb1Dn;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0)
        roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      int n1 = Math.abs(x2 - x2t);// + 1;
      int n2 = Math.abs(x2 - x1t);// - 1;
      //     System.out.println("shade dx-mode n1n2" + " " + n1 + " " + n2 + " xyz  " + x
      //       + " " + y + " " + z + " x2 " + (x2) + " y2" + (y2) + " z2 "
      //     + (z + "=z dz= " + dz) + " x2y2t=" + x2t + " " + y2t);
      for (int n = dx - 1, nMid = n / 2; --n >= n1;) {
        if (n == nMid) {
          argb = argb2;
          argbUp = argb2Up;
          argbDn = argb2Dn;
          tScreened = tScreened2;
          if (tScreened && !tScreened1) {
            int yT = offset / width;
            int xT = offset % width;
            flipflop = ((xT ^ yT) & 1) == 0;
          }
        }
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          offset += yOffsetIncrement;
          //sy=" n,ny="+n+"  accumulated error "+dx+"/"+twoDxAccumulatedYError+" yincr="+yOffsetIncrement+" offset " + offset;
          twoDxAccumulatedYError -= twoDx;
          flipflop = !flipflop;
        }
        //System.out.println("shade n offset" + n + " " + offset);
        if ((!tScreened || (flipflop = !flipflop)) && n < n2 && offset >= 0
            && offset < offsetMax && runIndex < rise) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent < zbuf[offset]) {
            zbuf[offset] = zCurrent;
            int rand8 = Shade3D.nextRandom8Bit();
            pbuf[offset] = rand8 < 85 ? argbDn : (rand8 > 170 ? argbUp : argb);
          }
        }
        runIndex = (runIndex + 1) % run;
      }
    } else {
      int roundingFactor = dy - 1;
      if (dz < 0)
        roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
      int twoDyAccumulatedXError = 0;
      int n1 = Math.abs(y2 - y2t);// + 1;
      int n2 = Math.abs(y2 - y1t);// - 1;
      //      System.out.println("shade dy-mode " + " n1=" + n1 + " n2=" + n2 + " xyz  " + x
      //        + " " + y + " " + z + " x2 " + (x2) + " y2" + (y2) + " z2 "
      //      + (z + "=z dz= " + dz) + " x2t=" + x2t + " y2t=" + y2t  );
      for (int n = dy - 1, nMid = n / 2; --n >= n1;) {
        if (n == nMid) {
          argb = argb2;
          argbUp = argb2Up;
          argbDn = argb2Dn;
          tScreened = tScreened2;
          if (tScreened && !tScreened1) {
            int yT = offset / width;
            int xT = offset % width;
            flipflop = ((xT ^ yT) & 1) == 0;
          }
        }
        offset += yOffsetIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDyAccumulatedXError += twoDx;
        if (twoDyAccumulatedXError > dy) {
          offset += xIncrement;
          twoDyAccumulatedXError -= twoDy;
          flipflop = !flipflop;
        }
        if ((!tScreened || (flipflop = !flipflop)) && n < n2 && offset >= 0
            && offset < offsetMax && runIndex < rise) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent < zbuf[offset]) {
            zbuf[offset] = zCurrent;
            int rand8 = Shade3D.nextRandom8Bit();
            pbuf[offset] = rand8 < 85 ? argbDn : (rand8 > 170 ? argbUp : argb);
          }
        }
        runIndex = (runIndex + 1) % run;
      }
    }
  }
  
  private void plotLineClippedBits(int[] shades1, boolean tScreened1,
                                   int[] shades2, boolean tScreened2,
                                   int intensity, int x, int y, int z, int dx,
                                   int dy, int dz, boolean notClipped, int run,
                                   int rise) {
    // special shading for rockets; somewhat slower than above;
    // System.out.println("line3d plotLineClippedBits "+x+" "+y+" "+z+" "+dx+" "+dy+" "+dz+" "+shades1);
    int[] pbuf = g3d.pbuf;
    int[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int runIndex = 0;
    if (run == 0) {
      rise = Integer.MAX_VALUE;
      run = 1;
    }
    int intensityUp = (intensity < Shade3D.shadeLast ? intensity + 1
        : intensity);
    int intensityDn = (intensity > 0 ? intensity - 1 : intensity);
    int argb1 = shades1[intensity];
    int argb1Up = shades1[intensityUp];
    int argb1Dn = shades1[intensityDn];
    int argb2 = shades2[intensity];
    int argb2Up = shades2[intensityUp];
    int argb2Dn = shades2[intensityDn];
    boolean tScreened = tScreened1;
    boolean flipflop = (((x ^ y) & 1) != 0);
    int offset = y * width + x;
    int offsetMax = pbuf.length;
    int i0, iMid, i1, i2, iIncrement, xIncrement, yIncrement;
    float zIncrement;
    if (lineTypeX) {
      i0 = x;
      i1 = x1t;
      i2 = x2t;
      iMid = x + dx / 2;
      iIncrement = (dx >= 0 ? 1 : -1);
      xIncrement = iIncrement;
      yIncrement = (dy >= 0 ? width : -width);
      zIncrement = (float)dz/(float)Math.abs(dx);
    } else {
      i0 = y;
      i1 = y1t;
      i2 = y2t;
      iMid = y + dy / 2;
      iIncrement = (dy >= 0 ? 1 : -1);
      xIncrement = (dy >= 0 ? width : -width);
      yIncrement = (dx >= 0 ? 1 : -1);
      zIncrement = (float)dz/(float)Math.abs(dy);
    }
    //System.out.println(lineTypeX+" dx dy dz " + dx + " " + dy + " " + dz);
    float zFloat = z;
    int argb = argb1;
    int argbUp = argb1Up;
    int argbDn = argb1Dn;
    boolean isInWindow = false;

    // "x" is not necessarily the x-axis.
    
    //  x----x1t-----------x2t---x2
    //  ------------xMid-----------
    //0-|------------------>-----------w

    // or
    
    //  x2---x2t-----------x1t----x
    //  ------------xMid-----------    
    //0-------<-------------------|----w
    
    for (int i = i0, iBits = i0;; i += iIncrement, iBits += iIncrement) {
      if (i == i1)
        isInWindow = true;
      if (i == iMid) {
        argb = argb2;
        argbUp = argb2Up;
        argbDn = argb2Dn;
        tScreened = tScreened2;
        if (tScreened && !tScreened1) {
          int yT = offset / width;
          int xT = offset % width;
          flipflop = ((xT ^ yT) & 1) == 0;
        }
      }
      //if(test > 0)System.out.println(isInWindow + " i1="+ i1 + " i0=" + i0 + " i=" + i + " offset="+offset );
      if (isInWindow && (!tScreened || (flipflop = !flipflop)) && offset >= 0
          && offset < offsetMax && runIndex < rise) {
        if (zFloat < zbuf[offset]) {
          //if (test > 0)System.out.println("ok");
          zbuf[offset] = (int) zFloat;
          int rand8 = Shade3D.nextRandom8Bit();
          pbuf[offset] = rand8 < 85 ? argbDn : (rand8 > 170 ? argbUp : argb);
        }
      }
      if (i == i2)
        break;
      runIndex = (runIndex + 1) % run;
      offset += xIncrement;
      while (iBits < 0)
        iBits += nBits;
      if (lineBits.get(iBits % nBits))
        offset += yIncrement;
      zFloat += zIncrement;
      //System.out.println("x y z "+offset+" "+zFloat+ " "+xIncrement+" "+yIncrement+" "+zIncrement);
    }
  }

}
