/* $RCSfile: MolecularOrbital.java,v $
 * $Author: qxie $
 * $Date: 2007-03-28 01:54:32 $
 * $Revision: 1.2 $
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
import java.util.Vector;

import javax.vecmath.Point4f;

import org.jmol.util.Logger;
import java.util.Hashtable;

class MolecularOrbital extends Isosurface {

  void initShape() {
    super.initShape();
    myType = "molecularOrbital";
    super.setProperty("thisID", "mo", null);
  }

  // these are globals, stored here and only passed on when the they are needed. 

  String moTranslucency = null;
  Point4f moPlane = null;
  Float moCutoff = new Float(Isosurface.defaultQMOrbitalCutoff);
  Float moResolution = null;
  Float moScale = null;
  Integer moColorPos = null;
  Integer moColorNeg = null;
  boolean moIsPositiveOnly = false;
  String moTitleFormat = null;
  boolean moDebug;
  int myColorPt;
  String strID;
  int moNumber;
  Hashtable htModels;
  Hashtable thisModel;
  Mesh thisMesh;
  
  @SuppressWarnings("unchecked")
void setProperty(String propertyName, Object value, BitSet bs) {

    Logger.debug("MolecularOrbital.setProperty " + propertyName + " " + value);

    // in the case of molecular orbitals, we just cache the information and
    // then send it all at once. 

    if ("init" == propertyName) {
      myColorPt = 0;
      moDebug = false;
      strID = getId(((Integer) value).intValue());
      // overide bitset selection
      super.setProperty("init", null, null);
      if (htModels == null)
        htModels = new Hashtable();
      if (!htModels.containsKey(strID))
        htModels.put(strID, new Hashtable());
      thisModel = (Hashtable) htModels.get(strID);
      moNumber = (thisModel == null || !thisModel.containsKey("moNumber")? 0 : ((Integer) thisModel.get("moNumber"))
          .intValue());
      return;
    }

    if ("cutoff" == propertyName) {
      thisModel.put("moCutoff", value);
      thisModel.put("moIsPositiveOnly", Boolean.FALSE);
      return;
    }

    if ("scale" == propertyName) {
      thisModel.put("moScale", moScale);
      return;
    }

    if ("cutoffPositive" == propertyName) {
      thisModel.put("moCutoff", value);
      thisModel.put("moIsPositiveOnly", Boolean.TRUE);
      return;
    }

    if ("resolution" == propertyName) {
      thisModel.put("moResolution", value);
      return;
    }

    if ("titleFormat" == propertyName) {
      moTitleFormat = (String) value;
      return;
    }

    if ("colorRGB" == propertyName) {
      moColorPos = (Integer) value;
      if (myColorPt++ == 0)
        moColorNeg = moColorPos;
      thisModel.put("moColorNeg", moColorNeg);
      thisModel.put("moColorPos", moColorPos);
      return;
    }

    if ("plane" == propertyName) {
      if (value == null)
        thisModel.remove("moPlane");
      else
        thisModel.put("moPlane", value);
      return;
    }

    if ("molecularOrbital" == propertyName) {
      moNumber = ((Integer) value).intValue();
      thisModel.put("moNumber", value);
      setOrbital(moNumber);
      return;
    }

    if ("translucency" == propertyName) {
      thisModel.put("moTranslucency", value);
      //pass through
    }
    if ("delete" == propertyName) {
      htModels.remove(strID);
      //pass through
    }

    super.setProperty(propertyName, value, bs);

  }

  String getId(int modelIndex) {
    return "mo_model" + viewer.getModelNumber(modelIndex);
  }
  
  Object getProperty(String propertyName, int param) {
    if (propertyName == "moNumber") {
      return (moNumber == 0 ? null : new Integer(moNumber));
    }
    if (propertyName == "showMO") {
      StringBuffer str = new StringBuffer();
      String infoType = "jvxlFileData";
      Vector mos = (Vector) (moData.get("mos"));
      int nOrb = (mos == null ? 0 : mos.size());
      if (nOrb == 0)
        return "";
      int thisMO = param;
      int currentMO = moNumber;
      if (currentMO == 0)
        thisMO = 0;
      int nTotal = (thisMO > 0 ? 1 : nOrb);
      for (int i = 1; i <= nOrb; i++)
        if (thisMO == 0 || thisMO == i || thisMO == Integer.MAX_VALUE
            && i == currentMO) {
          super.setProperty("init", "mo_show", null);
          setOrbital(i);
          str.append(super.getProperty(infoType, nTotal));
          infoType = "jvxlSurfaceData";
          super.setProperty("delete", "mo_show", null);
        }
      return "" + str;
    }
    return null;
  }
  
  boolean getSettings(String strID) {
    thisModel = (Hashtable)htModels.get(strID);
    if (thisModel == null)
      return false;
    moTranslucency = (String)thisModel.get("moTranslucency");
    moPlane = (Point4f)thisModel.get("moPlane");
    moCutoff = (Float )thisModel.get("moCutoff");
    if (moCutoff == null)
      moCutoff = new Float(Isosurface.defaultQMOrbitalCutoff);
    moResolution = (Float)thisModel.get("moResolution");
    moScale = (Float)thisModel.get("moScale");
    moColorPos = (Integer)thisModel.get("moColorPos");
    moColorNeg = (Integer)thisModel.get("moColorNeg");
    moNumber = ((Integer)thisModel.get("moNumber")).intValue();
    Object b = thisModel.get("moIsPositiveOnly");
    moIsPositiveOnly = (b != null  && ((Boolean)(b)).booleanValue());
    thisMesh = (Mesh)thisModel.get("mesh");
    return true;
  }
 
  @SuppressWarnings("unchecked")
void setOrbital(int moNumber) {
    super.setProperty("reset", strID, null);
    if (moDebug)
      super.setProperty("debug", Boolean.TRUE, null);
    getSettings(strID);
    if (moScale != null)
      super.setProperty("scale", moScale, null);
    if (moResolution != null)
      super.setProperty("resolution", moResolution, null);
    if (moPlane != null) {
      super.setProperty("plane", moPlane, null);
      if (moCutoff != null) {
        super.setProperty("red", new Float(-moCutoff.floatValue()), null);
        super.setProperty("blue", moCutoff, null);
      }
    } else {
      if (moCutoff != null)
        super.setProperty((moIsPositiveOnly ? "cutoffPositive" : "cutoff"),
            moCutoff, null);
      if (moColorNeg != null)
        super.setProperty("colorRGB", moColorNeg, null);
      if (moColorPos != null)
        super.setProperty("colorRGB", moColorPos, null);
    }
    super.setProperty("title", moTitleFormat, null);
    super.setProperty("molecularOrbital", new Integer(moNumber), null);
    if (moTranslucency != null)
      super.setProperty("translucency", moTranslucency, null);
    thisModel.put("mesh", currentMesh);
    return;
  }
 
  String getShapeState() {
    if (htModels == null)
      return "";
    StringBuffer s = new StringBuffer();
    for (int i = 0; i < modelCount; i++)
      s.append(getMoState(i));
    return s.toString();
  }
  
  String getMoState(int modelIndex) {
    strID = getId(modelIndex);
    if (!getSettings(strID))
      return "";
    StringBuffer s = new StringBuffer();
    if (modelCount > 1)
      appendCmd(s, "frame " + viewer.getModelNumber(modelIndex));
    getSettings(strID);
    if (moCutoff != null)
      appendCmd(s, "mo cutoff " + (isPositiveOnly ? "+" : "") + moCutoff);
    if (moScale != null)
      appendCmd(s, "mo scale " + moScale);
    if (moResolution != null)
      appendCmd(s, "mo resolution " + moResolution);
    if (moPlane != null)
      appendCmd(s, "mo plane {" + moPlane.x + " " + moPlane.y + " " + moPlane.z + " " + moPlane.w + "}");
    if (moTitleFormat != null)
      appendCmd(s, "mo titleFormat " + StateManager.escape(moTitleFormat));
    if (moColorNeg != null)
      appendCmd(s, "mo color " + StateManager.escapeColor(moColorNeg.intValue())
          + (moColorNeg == moColorPos ? "" : " " + StateManager.escapeColor(moColorPos.intValue())));
    appendCmd(s, "mo " + moNumber);
    appendCmd(s, getMeshState(currentMesh, "mo"));
    return s.toString();
  }
}