/* $RCSfile: FrameExportJmolAdapter.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:11 $
 * $Revision: 1.11 $
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

import org.jmol.api.JmolAdapter;


final public class FrameExportJmolAdapter extends JmolAdapter {

  Viewer viewer;
  Frame frame;

  FrameExportJmolAdapter(Viewer viewer, Frame frame) {
    super("FrameExportJmolAdapter", null);
    this.viewer = viewer;
    this.frame = frame;
  }

  public String getAtomSetCollectionName(Object clientFile) {
    return viewer.getModelSetName();
  }

  public int getEstimatedAtomCount(Object clientFile) {
    return frame.atomCount;
  }

  public float[] getNotionalUnitcell(Object clientFile) {
    return frame.notionalUnitcell;
  }

  public JmolAdapter.AtomIterator
    getAtomIterator(Object clientFile) {
    return new AtomIterator();
  }

  public JmolAdapter.BondIterator
    getBondIterator(Object clientFile) {
    return new BondIterator();
  }

  class AtomIterator extends JmolAdapter.AtomIterator {
    int iatom;
    Atom atom;

    public boolean hasNext() {
      if (iatom == frame.atomCount)
        return false;
      atom = frame.atoms[iatom++];
      return true;
    }
    public Object getUniqueID() { return new Integer(iatom); }
    public int getElementNumber() { return atom.getElementNumber(); }//no isotope here
    public String getElementSymbol() { return atom.getElementSymbol(); }
    public int getFormalCharge() { return atom.getFormalCharge(); }
    public float getPartialCharge() { return atom.getPartialCharge(); }
    public float getX() { return atom.getAtomX(); }
    public float getY() { return atom.getAtomY(); }
    public float getZ() { return atom.getAtomZ(); }
  }

  class BondIterator extends JmolAdapter.BondIterator {
    int ibond;
    Bond bond;

    public boolean hasNext() {
      if (ibond >= frame.bondCount)
        return false;
      bond = frame.bonds[ibond++];
      return true;
    }
    public Object getAtomUniqueID1(){
      return new Integer(bond.atom1.atomIndex);
    }
    public Object getAtomUniqueID2() {
      return new Integer(bond.atom2.atomIndex);
    }
    public int getEncodedOrder() { return bond.order; }
  }
}
