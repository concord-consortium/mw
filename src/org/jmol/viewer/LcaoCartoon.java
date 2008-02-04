/* $RCSfile: LcaoCartoon.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:11 $
 * $Revision: 1.1 $
 *
 * Copyright (C) 2005 Miguel, Jmol Development
 *
 * Contact: miguel@jmol.org,jmol-developers@lists.sourceforge.net
 * Contact: hansonr@stolaf.edu
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

import java.util.BitSet;

import javax.vecmath.Vector3f;

import org.jmol.util.Logger;

class LcaoCartoon extends Isosurface {

  // these are globals, stored here and only passed on when the they are needed. 

  void initShape() {
    super.initShape();
    myType = "lcaoCartoon";
  }
  
  Integer lcaoColorPos = null;
  Integer lcaoColorNeg = null;
  String thisType;
  Float lcaoScale = null; //actually, we need to reset this one.
  int myColorPt;
  String lcaoID;
  BitSet thisSet = null;
  boolean isMolecular;

  void setProperty(String propertyName, Object value, BitSet bs) {

    Logger.debug("\nLcaoCartoon.setProperty " + propertyName + " " + value);

    // in the case of molecular orbitals, we just cache the information and
    // then send it all at once. 

    if ("init" == propertyName) {
      myColorPt = 0;
      lcaoID = null;
      thisSet = bs;
      isMolecular = false;
      thisType = null;
      lcaoScale = null;
      // overide bitset selection
      super.setProperty("init", null, null);
      return;
    }

    if ("molecular" == propertyName) {
      isMolecular = (thisType != null && (thisType.indexOf("px") >= 0
          || thisType.indexOf("py") >= 0 || thisType.indexOf("pz") >= 0));
      return;
    }

    if ("on" == propertyName) {
      setLcaoOn(true);
      return;
    }

    if ("off" == propertyName) {
      setLcaoOn(false);
      return;
    }

    if ("delete" == propertyName) {
      deleteLcaoCartoon();
      return;
    }

    if ("lcaoID" == propertyName) {
      lcaoID = (String) value;
      return;
    }

    if ("selectType" == propertyName) {
      thisType = (String) value;
      return;
    }

    if ("create" == propertyName) {
      thisType = (String) value;
      createLcaoCartoon();
      return;
    }

    if ("scale" == propertyName) {
      lcaoScale = (Float) value;
      //pass through
    }

    if ("colorRGB" == propertyName) {
      lcaoColorPos = (Integer) value;
      if (myColorPt++ == 0)
        lcaoColorNeg = lcaoColorPos;
      //pass through
    }

    if ("select" == propertyName) {
      thisSet = (BitSet)value;
      //pass through
    }

    super.setProperty(propertyName,value,bs);
  }

  void setLcaoOn(boolean TF) {
    int atomCount = viewer.getAtomCount();
    for (int i = atomCount; --i >= 0;)
      if (thisSet.get(i))
        setLcaoOn(i, TF);
  }

  void setLcaoOn(int iAtom, boolean TF) {
    String id = getID(lcaoID, iAtom);
    for (int i = meshCount; --i >=0;)
      if (meshes[i].thisID.indexOf(id) == 0)
        meshes[i].visible = TF;
  }
    
  void deleteLcaoCartoon() {
    int atomCount = viewer.getAtomCount();
    for (int i = atomCount; --i >= 0;)
      if (thisSet.get(i))
        deleteLcaoCartoon(i);
  }

  void deleteLcaoCartoon(int iAtom) {
    String id = getID(lcaoID, iAtom);
    for (int i = meshCount; --i >=0;)
      if (meshes[i].thisID.indexOf(id) == 0)
        deleteMesh(i);
  }
    
  void createLcaoCartoon() {
    int atomCount = viewer.getAtomCount();
    for (int i = atomCount; --i >= 0;)
      if (thisSet.get(i))
        createLcaoCartoon(i);
  }

  void createLcaoCartoon(int iAtom) {
    String id = getID(null, iAtom);
    for (int i = meshCount; --i >= 0;)
      if (meshes[i].thisID.indexOf(id) == 0)
        deleteMesh(i);
    super.setProperty("init", null, null);
    super.setProperty("thisID", id, null);
    if (lcaoScale != null)
      super.setProperty("scale", lcaoScale, null);
    if (lcaoColorNeg != null) {
      super.setProperty("colorRGB", lcaoColorNeg, null);
      super.setProperty("colorRGB", lcaoColorPos, null);
    }
    super.setProperty("lcaoType", thisType, null);
    super.setProperty("atomIndex", new Integer(iAtom), null);

    Vector3f[] axes = { new Vector3f(), new Vector3f(),
        new Vector3f(frame.atoms[iAtom]) };
    if (isMolecular) {
      if (thisType.indexOf("px") >= 0) {
        axes[0].set(0, -1, 0);
        axes[1].set(1, 0, 0);
      } else if (thisType.indexOf("py") >= 0) {
        axes[0].set(-1, 0, 0);
        axes[1].set(0, 0, 1);
      } else if (thisType.indexOf("pz") >= 0) {
        axes[0].set(0, 0, 1);
        axes[1].set(1, 0, 0);
      }
      if (isMolecular && thisType.indexOf("-") == 0) {
        axes[0].scale(-1);
      }
    }
    if (isMolecular || thisType.equalsIgnoreCase("s")
        || viewer.getPrincipalAxes(iAtom, axes[0], axes[1], thisType, true))
      super.setProperty("lcaoCartoon", axes, null);
  }
    
  String getID(String id, int i) {
    // remove "-" from "-px" "-py" "-pz" because we never want to have
    // both "pz" and "-pz" on the same atom
    // but we can have "-sp3a" and "sp3a"
    return (id != null ? id : "lcao_" + (i + 1)
        + (thisType == null ? "" : viewer.simpleReplace(thisType, "-",
            (thisType.indexOf("-p") == 0 ? "" : "_"))));
  }

}
