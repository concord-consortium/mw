/* $RCSfile: Bspf.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:17 $
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
package org.myjmol.bspt;

/**
 * A Binary Space Partitioning Forest
 *<p>
 * This is simply an array of Binary Space Partitioning Trees identified
 * by indexes
 *
 * @author Miguel, miguel@jmol.org
*/

public final class Bspf {

  int dimMax;
  Bspt bspts[];
  SphereIterator[] sphereIterators;
  
  public Bspf(int dimMax) {
    this.dimMax = dimMax;
    bspts = new Bspt[0];
    sphereIterators = new SphereIterator[0];
  }

  public int getBsptCount() {
    return bspts.length;
  }
  
  public void addTuple(int bsptIndex, Tuple tuple) {
    if (bsptIndex >= bspts.length) {
      Bspt[] t = new Bspt[bsptIndex + 1];
      System.arraycopy(bspts, 0, t, 0, bspts.length);
      bspts = t;
    }
    Bspt bspt = bspts[bsptIndex];
    if (bspt == null)
      bspt = bspts[bsptIndex] = new Bspt(dimMax);
    bspt.addTuple(tuple);
  }

  public void stats() {
    for (int i = 0; i < bspts.length; ++i)
      bspts[i].stats();
  }

  /*
  public void dump() {
    for (int i = 0; i < bspts.length; ++i) {
      Logger.debug(">>>>\nDumping bspt #" + i + "\n>>>>");
      bspts[i].dump();
    }
    Logger.debug("<<<<");
  }
  */

  public SphereIterator getSphereIterator(int bsptIndex) {
    if (bsptIndex >= sphereIterators.length) {
      SphereIterator[] t = new SphereIterator[bsptIndex + 1];
      System.arraycopy(sphereIterators, 0, t, 0, sphereIterators.length);
      sphereIterators = t;
    }
    if (sphereIterators[bsptIndex] == null &&
        bspts[bsptIndex] != null)
      sphereIterators[bsptIndex] = bspts[bsptIndex].allocateSphereIterator();
    return sphereIterators[bsptIndex];
  }
}
