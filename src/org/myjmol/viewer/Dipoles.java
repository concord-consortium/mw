/* $RCSfile: Dipoles.java,v $
 * $Author: qxie $
 * $Date: 2007-03-27 21:10:05 $
 * $Revision: 1.2 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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


import org.myjmol.g3d.*;
import org.myjmol.util.ArrayUtil;
import org.myjmol.util.Logger;

import java.util.BitSet;
import java.util.Vector;
import java.util.Hashtable;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

class Dipoles extends Shape {

  final static short DEFAULT_MAD = 5;
  final static float DEFAULT_OFFSETSIDE = 0.40f;

  float dipoleVectorScale = 1.0f;
  int dipoleCount = 0;
  Dipole[] dipoles = new Dipole[4];
  Dipole currentDipole;
  Dipole tempDipole;
  Point3f startCoord = new Point3f();
  Point3f endCoord = new Point3f();
  float dipoleValue;
  boolean isUserValue;
  boolean isBond;
  boolean iHaveTwoEnds;
  boolean iHaveTwoAtoms;
  boolean isValid;
  int atomIndex1;
  int atomIndex2;
  short colix;
  
  void setProperty(String propertyName, Object value, BitSet bs) {

    Logger.debug("dipoles setProperty " + propertyName + " " + value);

    if ("init" == propertyName) {
      tempDipole = new Dipole();
      tempDipole.dipoleValue = 1;
      tempDipole.mad = DEFAULT_MAD;
      atomIndex1 = -1;
      tempDipole.modelIndex = -1;
      dipoleValue = 0;
      isValid = isUserValue = isBond = iHaveTwoEnds = iHaveTwoAtoms = false;
      if (currentDipole != null)
        Logger.debug("current dipole: " + currentDipole.thisID);
      return;
    }

    if ("thisID" == propertyName) {
      if (value == null) {
        currentDipole = null;
        return;
      }
      String thisID = "" + (String) value;
      currentDipole = findDipole(thisID);
      if (currentDipole == null)
        currentDipole = allocDipole(thisID, "");
      Logger.debug("current dipole now " + currentDipole.thisID);
      tempDipole = currentDipole;
      if (thisID.equals("molecular")) {
        Vector3f v = viewer.getModelDipole();
        Logger.info("file molecular dipole = " + v);
        if (v == null) {
          Logger.warn("No molecular dipole found in file; setting to {0 0 0}");
          v = new Vector3f();
        }
        tempDipole.set(new Point3f(0, 0, 0), new Vector3f(-v.x, -v.y, -v.z));
        tempDipole.type = Dipole.DIPOLE_TYPE_MOLECULAR;
        tempDipole.thisID = "molecular";
        setDipole();
      }
      return;
    }

    if ("dipoleVectorScale" == propertyName) {
      dipoleVectorScale = ((Float) value).floatValue();
      return;
    }

    if ("bonds" == propertyName) {
      isBond = true;
      currentDipole = null;
      for (int i = dipoleCount; --i >= 0;)
        if (isBondDipole(i))
          return;
      // only once if any bond dipoles are defined
      viewer.getBondDipoles();
      return;
    }

    if ("on" == propertyName) {
      if (currentDipole != null)
        currentDipole.visible = true;
      else {
        for (int i = dipoleCount; --i >= 0;)
          if (!isBond || isBondDipole(i))
            dipoles[i].visible = true;
      }
      return;
    }

    if ("off" == propertyName) {
      if (currentDipole != null)
        currentDipole.visible = false;
      else {
        for (int i = dipoleCount; --i >= 0;)
          if (!isBond || isBondDipole(i))
            dipoles[i].visible = false;
      }
      return;
    }

    if ("delete" == propertyName) {
      if (currentDipole != null)
        deleteDipole(currentDipole);
      else
        clear(false);
      return;
    }

    if ("clear" == propertyName) {
      currentDipole = null;
      clear(false);
    }

    if ("dipoleWidth" == propertyName) {
      short mad = tempDipole.mad = (short) (((Float) value).floatValue() * 1000);
      if (currentDipole == null)
        for (int i = dipoleCount; --i >= 0;)
          dipoles[i].mad = mad;
      return;
    }

    if ("dipoleOffset" == propertyName) {
      tempDipole.offsetAngstroms = ((Float) value).floatValue();
      if (currentDipole == null)
        for (int i = dipoleCount; --i >= 0;)
          if (!isBond || isBondDipole(i))
            dipoles[i].offsetAngstroms = tempDipole.offsetAngstroms;
      return;
    }

    if ("dipoleOffsetPercent" == propertyName) {
      tempDipole.offsetPercent = ((Integer) value).intValue();
      if (tempDipole.dipoleValue != 0)
        tempDipole.offsetAngstroms = tempDipole.offsetPercent / 100f
            * tempDipole.dipoleValue;
      if (currentDipole == null)
        for (int i = dipoleCount; --i >= 0;)
          if (!isBond || isBondDipole(i))
            dipoles[i].offsetAngstroms = tempDipole.offsetPercent / 100f
                * dipoles[i].dipoleValue;
      return;
    }

    if ("offsetSide" == propertyName) {
      float offsetSide = ((Float) value).floatValue();
      if (currentDipole != null)
        currentDipole.offsetSide = offsetSide;
      else
        for (int i = dipoleCount; --i >= 0;)
          if (!isBond || isBondDipole(i))
            dipoles[i].offsetSide = offsetSide;
      return;
    }

    if ("cross" == propertyName) {
      boolean isOFF = !((Boolean) value).booleanValue();
      if (currentDipole != null)
        currentDipole.noCross = isOFF;
      else
        for (int i = dipoleCount; --i >= 0;)
          if (!isBond || isBondDipole(i))
            dipoles[i].noCross = isOFF;
      return;
    }

    if ("color" == propertyName) {
      colix = Graphics3D.getColix(value);
      if (isBond) {
        setColixDipole(colix, JmolConstants.BOND_COVALENT_MASK, bs);
      } else if (value != null) {
        if (currentDipole != null)
          currentDipole.colix = colix;
        else
          for (int i = dipoleCount; --i >= 0;)
            dipoles[i].colix = colix;
      }
      return;
    }

    if ("translucency" == propertyName) {
      boolean isTranslucent = ("translucent" == value);
      if (currentDipole != null)
        currentDipole.setTranslucent(isTranslucent);
      else
        for (int i = dipoleCount; --i >= 0;)
          if (!isBond || isBondDipole(i))
            dipoles[i].setTranslucent(isTranslucent);
      return;
    }

    if ("startSet" == propertyName) {
      BitSet atomset = (BitSet) value;
      startCoord = viewer.getAtomSetCenter(atomset);
      tempDipole.set(startCoord, new Point3f(0, 0, 0), dipoleValue);
      if (viewer.cardinalityOf(atomset) == 1)
        atomIndex1 = viewer.firstAtomOf(atomset);
      return;
    }

    if ("atomBitSet" == propertyName) {
      BitSet atomset = (BitSet) value;
      atomIndex1 = viewer.firstAtomOf(atomset);
      startCoord = frame.atoms[atomIndex1];
      atomset.clear(atomIndex1);
      value = atomset;
      propertyName = "endSet";
      //passes to endSet
    }
    
    if ("endSet" == propertyName) {
      iHaveTwoEnds = true;
      iHaveTwoAtoms = true;
      BitSet atomset = (BitSet) value;
      if (atomIndex1 >= 0 && viewer.cardinalityOf(atomset) == 1) {
        atomIndex2 = viewer.firstAtomOf(atomset);
        tempDipole.set(frame.atoms[atomIndex1], frame.atoms[atomIndex2], 1);
        currentDipole = findDipole(tempDipole.thisID, tempDipole.dipoleInfo);
        tempDipole.thisID = currentDipole.thisID;
        if (isSameAtoms(currentDipole, tempDipole.dipoleInfo)) {
          tempDipole = currentDipole;
          if (dipoleValue > 0)
            tempDipole.dipoleValue = dipoleValue;
        }
      } else {
        tempDipole.set(startCoord, viewer.getAtomSetCenter(atomset),
            dipoleValue);
      }
      //NOTTTTTT!!!! currentDipole = tempDipole;
      return;
    }

    if ("startCoord" == propertyName) {
      startCoord.set((Point3f) value);
      tempDipole.set(startCoord, new Point3f(0, 0, 0), dipoleValue);
      return;
    }

    if ("endCoord" == propertyName) {
      iHaveTwoEnds = true;
      endCoord.set((Point3f) value);
      tempDipole.set(startCoord, endCoord, dipoleValue);
      dumpDipoles("endCoord");
      return;
    }

    if ("dipoleValue" == propertyName) {
      dipoleValue = ((Float) value).floatValue();
      isUserValue = true;
      tempDipole.set(dipoleValue);
      if (tempDipole.offsetPercent != 0)
        tempDipole.offsetAngstroms = tempDipole.offsetPercent / 100f
            * tempDipole.dipoleValue;
      return;
    }

    if ("set" == propertyName) {
      if (isBond || !iHaveTwoEnds)
        return;
      isValid = true;
      setDipole();
      setModelIndex();
      return;
    }
  }

  private boolean isBondDipole(int i) {
    if (i >= dipoles.length || dipoles[i] == null)
      return false;
    return (dipoles[i].isBondType());
  }

  private void setColixDipole(short colix, short bondTypeMask, BitSet bs) {
    if (colix == Graphics3D.USE_PALETTE)
      return; // not implemented
    BondIterator iter = frame.getBondIterator(bondTypeMask, bs);
    while (iter.hasNext()) {
      Dipole d = findBondDipole(iter.next());
      if (d != null)
        d.colix = colix;
    }
  }

  private void setDipole() {
    if (currentDipole == null)
      currentDipole = allocDipole("", "");
    currentDipole.set(tempDipole.thisID, tempDipole.dipoleInfo,
        tempDipole.atoms, tempDipole.dipoleValue, tempDipole.mad,
        tempDipole.offsetAngstroms, tempDipole.offsetPercent, tempDipole.offsetSide, tempDipole.origin,
        tempDipole.vector);
    currentDipole.isUserValue = isUserValue;
  }

  private int getDipoleIndex(String dipoleInfo, String thisID) {
    if (dipoleInfo != null && dipoleInfo.length() > 0)
      for (int i = dipoleCount; --i >= 0;)
        if (isSameAtoms(dipoles[i], dipoleInfo))
          return i;
    return getIndexFromName(thisID);
  }

  private boolean isSameAtoms(Dipole dipole, String dipoleInfo) {
    // order-independent search for two atoms:
    // looking for (xyz)(x'y'z') in (xyz)(x'y'z')(xyz)(x'y'z')
    return (dipole != null && dipole.isBondType() && (dipole.dipoleInfo + dipole.dipoleInfo)
        .indexOf(dipoleInfo) >= 0);
  }

  private int getDipoleIndex(int atomIndex1, int atomIndex2) {
    for (int i = dipoleCount; --i >= 0;) {
      if (dipoles[i] != null
          && dipoles[i].atoms[0] != null
          && dipoles[i].atoms[1] != null
          && (dipoles[i].atoms[0].atomIndex == atomIndex1
              && dipoles[i].atoms[1].atomIndex == atomIndex2 || dipoles[i].atoms[1].atomIndex == atomIndex1
              && dipoles[i].atoms[0].atomIndex == atomIndex2))
        return i;
    }
    return -1;
  }

  private void deleteDipole(Dipole dipole) {
    if (dipole == null)
      return;
    if (currentDipole == dipole)
      currentDipole = null;
    int i;
    for (i = dipoleCount; dipoles[--i] != dipole;) {
    }
    if (i < 0)
      return;
    for (int j = i + 1; j < dipoleCount; ++j)
      dipoles[j - 1] = dipoles[j];
    dipoles[--dipoleCount] = null;
  }

  private Dipole findDipole(String thisID) {
    int dipoleIndex = getIndexFromName(thisID);
    if (dipoleIndex >= 0) {
      return dipoles[dipoleIndex];
    }
    return null;
  }

  Dipole findDipole(Atom atom1, Atom atom2, boolean doAllocate) {
    int dipoleIndex = getDipoleIndex(atom1.atomIndex, atom2.atomIndex);
    if (dipoleIndex >= 0) {
      return dipoles[dipoleIndex];
    }
    return (doAllocate ? allocDipole("", "") : null);
  }

  private Dipole findBondDipole(Bond bond) {
    Dipole d = findDipole(bond.atom1, bond.atom2, false);
    return (d == null || d.atoms[0] == null ? null : d);
  }

  private Dipole findDipole(String thisID, String dipoleInfo) {
    // must be able to identify a dipole from its ID only SECONDARILY,
    // as we want one dipole per bond. So we look for coord ID.
    int dipoleIndex = getDipoleIndex(dipoleInfo, thisID);
    if (dipoleIndex >= 0) {
      if (thisID.length() > 0)
        dipoles[dipoleIndex].thisID = thisID;
      return dipoles[dipoleIndex];
    }
    return allocDipole(thisID, dipoleInfo);
  }

  private Dipole allocDipole(String thisID, String dipoleInfo) {
    dipoles = (Dipole[]) ArrayUtil.ensureLength(dipoles, dipoleCount + 1);
    if (thisID == null || thisID.length() == 0)
      thisID = "dipole" + (dipoleCount + 1);
    Dipole d = dipoles[dipoleCount++] = new Dipole(viewer, thisID, dipoleInfo,
        g3d, colix, DEFAULT_MAD, true);
    return d;
  }

  private void dumpDipoles(String msg) {
    for (int i = dipoleCount; --i >= 0;) {
      Dipole dipole = dipoles[i];
      Logger.info("\n\n" + msg + " dump dipole " + i + " " + dipole
          + " " + dipole.thisID + " " + dipole.dipoleInfo + " "
          + dipole.visibilityFlags + " mad=" + dipole.mad + " vis="
          + dipole.visible + "\n orig" + dipole.origin + " " + " vect"
          + dipole.vector + " val=" + dipole.dipoleValue);
    }
    if (currentDipole != null)
      Logger.info(" current = " + currentDipole + currentDipole.origin);
    if (tempDipole != null)
      Logger.info(" temp = " + tempDipole + " " + tempDipole.origin);
  }
 
  void clear(boolean clearBondDipolesOnly) {
    if (clearBondDipolesOnly) {
      for (int i = dipoleCount; --i >= 0;)
        if (isBondDipole(i))
          deleteDipole(dipoles[i]);
      return;
    }
    for (int i = dipoleCount; --i >= 0;)
      if (!isBond || isBondDipole(i))
        deleteDipole(dipoles[i]);
  }

  int getIndexFromName(String thisID) {
    if (thisID == null)
      return -1;
    for (int i = dipoleCount; --i >= 0;) {
      if (dipoles[i] != null && thisID.equals(dipoles[i].thisID))
        return i;
    }
    return -1;
  }

  @SuppressWarnings("unchecked")
Vector getShapeDetail() {
    Vector V = new Vector();
    Hashtable atomInfo;
    for (int i = 0; i < dipoleCount; i++) {
      Hashtable info = new Hashtable();
      Dipole dipole = dipoles[i];
      info.put("ID", dipole.thisID);
      info.put("vector", dipole.vector);
      info.put("origin", dipole.origin);
      if (dipole.atoms[0] != null) {
        atomInfo = new Hashtable();
        viewer.getAtomIdentityInfo(dipole.atoms[0].atomIndex, atomInfo);
        Vector atoms = new Vector();
        atoms.add(atomInfo);
        atomInfo = new Hashtable();
        viewer.getAtomIdentityInfo(dipole.atoms[1].atomIndex, atomInfo);
        atoms.add(atomInfo);
        info.put("atoms", atoms);
        info.put("magnitude", new Float(dipole.vector.length()));
      }
      V.add(info);
    }
    return V;
  }

  void setModelIndex() {
    if (currentDipole == null)
      return;
    currentDipole.visible = true;
    currentDipole.modelIndex = viewer.getDisplayModelIndex();
  }

  void setVisibilityFlags(BitSet bs) {
    /*
     * set all fixed objects visible; others based on model being displayed
     * 
     */
    for (int i = dipoleCount; --i >= 0;) {
      Dipole dipole = dipoles[i];
      dipole.visibilityFlags = ((dipole.modelIndex < 0 || bs
          .get(dipole.modelIndex))
          && dipole.mad != 0
          && dipole.visible
          && dipole.origin != null
          && dipole.vector != null
          && dipole.vector.length() != 0
          && dipole.dipoleValue != 0 ? myVisibilityFlag : 0);
    }
    //dumpDipoles("setVis");
  }
  
  String getShapeState() {
    if (dipoleCount == 0)
      return "";
    StringBuffer s = new StringBuffer();
    int thisModel = -1;
    int modelCount = frame.modelCount;
    for (int i = 0; i < dipoleCount; i++) {
      Dipole dipole = dipoles[i];
      if (dipole.isValid) {
        if (modelCount > 1 && dipole.modelIndex != thisModel)
          appendCmd(s, "frame "
              + viewer.getModelName(thisModel = dipole.modelIndex));
        s.append(dipole.getShapeState());
        appendCmd(s, getColorCommand("dipole", dipole.colix));
      }
    }
    appendCmd(s, "set dipoleScale " + dipoleVectorScale);
    return s.toString();
  }
}
