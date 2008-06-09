/* $RCSfile: Measures.java,v $
 * $Author: qxie $
 * $Date: 2007-03-27 21:10:05 $
 * $Revision: 1.13 $
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

import java.util.BitSet;
import java.util.Vector;
import java.util.Hashtable;

class Measures extends Shape {

  BitSet bsSizeSet;
  BitSet bsColixSet;
  BitSet bsSelected;

  final static int measurementGrowthIncrement = 16;
  int measurementCount = 0;
  Measurement[] measurements = new Measurement[measurementGrowthIncrement];
  PendingMeasurement pendingMeasurement;
  String[] formats = new String[5];
  short mad = (short)-1;
  short colix; // default to none in order to contrast with background
  String strFormat;
  boolean showMeasurementNumbers = true;
  boolean isAllConnected = false;
  
  Font3D font3d;
  float[] rangeMinMax = {Float.MAX_VALUE, Float.MAX_VALUE};
  
  void initShape() {
    pendingMeasurement = new PendingMeasurement(frame);
    font3d = g3d.getFont3D(JmolConstants.MEASURE_DEFAULT_FONTSIZE);
  }

  void setSize(int size, BitSet bsSelected) {
    mad = (short)size;
  }

  void setProperty(String propertyName, Object value, BitSet bsIgnored) {
    //Logger.debug("Measures " + propertyName  + " " + value);
    
    // the following can be used with "select measures ({bitset})"
    if ("select".equals(propertyName)) {
      BitSet bs = (BitSet) value;
      if (bs == null || viewer.cardinalityOf(bs) == 0) {
        bsSelected = null;
      } else {
        bsSelected = new BitSet();
        bsSelected.or(bs);
      }
      return;
    } else if ("color".equals(propertyName)) {
      if (bsColixSet == null)
        bsColixSet = new BitSet();
        short colix = (value == null ? Graphics3D.INHERIT : Graphics3D.getColix(value));
        if (bsSelected == null)
          this.colix = colix;
      for (int i = 0; i < measurements.length; i++)
        if (measurements[i] != null
            && (bsSelected != null && bsSelected.get(i) || bsSelected == null
                && (colix == Graphics3D.INHERIT || measurements[i].colix == Graphics3D.INHERIT))) {
          measurements[i].colix = colix;
          bsColixSet.set(i);
        }
      return;
    } else if ("delete".equals(propertyName)) {
      delete(value);
      setIndices();
      return;
    } else if ("hideAll".equals(propertyName)) {
      showHide(((Boolean) value).booleanValue());
      return;
    } else if ("setFormats".equals(propertyName)) {
      setFormats((String) value);
      return;
    }

    //any one of the following clears the "select measures" business
    
    bsSelected = null;
    if ("hide".equals(propertyName)) {
      showHide((int[]) value, true);
    } else if ("show".equals(propertyName)) {
      showHide((int[]) value, false);
    } else if ("setRange".equals(propertyName)) {
      setRange((float[]) value);
    } else if ("setFormat".equals(propertyName)) {
      strFormat = (String) value;
    } else if ("setConnected".equals(propertyName)) {
      setConnected(((Boolean) value).booleanValue());
    } else if ("defineVector".equals(propertyName)) {
      define((Vector) value, false, false, false);
    } else if ("deleteVector".equals(propertyName)) {
      define((Vector) value, true, false, false);
      setIndices();
    } else if ("hideVector".equals(propertyName)) {
      define((Vector) value, false, false, true);
    } else if ("showVector".equals(propertyName)) {
      define((Vector) value, false, true, false);
    } else if ("toggle".equals(propertyName)) {
      toggle((int[]) value);
    } else if ("pending".equals(propertyName)) {
      pending((int[]) value);
    } else if ("font".equals(propertyName)) {
      font3d = (Font3D) value;
    } else if ("clear".equals(propertyName)) {
      clear();
    } else if ("showMeasurementNumbers".equals(propertyName)) {
      showMeasurementNumbers = ((Boolean) value).booleanValue();
    } else if ("reformatDistances".equals(propertyName)) {
      reformatDistances();
    }
  }

  Object getProperty(String property, int index) {
    //Logger.debug("Measures.getProperty(" +property + "," + index +")");
    if ("count".equals(property))
      { return new Integer(measurementCount); }
    if ("countPlusIndices".equals(property)) {
      return index < measurementCount
        ? measurements[index].countPlusIndices : null;
    }
    if ("stringValue".equals(property)) {
      return index < measurementCount
        ? measurements[index].strMeasurement : null;
    }
    if ("info".equals(property)) {
      return getAllInfo();
    }
    if ("infostring".equals(property)) {
      return getAllInfoAsString();
    }
    return null;
  }

  private void clear() {
    int countT = measurementCount;
    measurementCount = 0;
    for (int i = countT; --i >= 0; )
      measurements[i] = null;
  }

  private int defined(int[] atomCountPlusIndices) {
    for (int i = measurementCount; --i >= 0; ) {
      if (measurements[i].sameAs(atomCountPlusIndices))
        return i;
    }
    return -1;
  }

  private void toggle(int[] atomCountPlusIndices) {
    rangeMinMax[0] = Float.MAX_VALUE;
    //toggling one that is hidden should be interpreted as DEFINE
    int i = defined(atomCountPlusIndices);
    if (i >= 0 && !measurements[i].isHidden) // delete it
      define(atomCountPlusIndices, true, false);
    else // define OR turn on if measureAllModels
      define(atomCountPlusIndices, false, true);
    setIndices();
  }

  private void delete(Object value) {
    if (value instanceof int[])
      define((int[])value, true, false);
    else if (value instanceof Integer)
      define(measurements[((Integer)value).intValue()].countPlusIndices, true, false);
  }
 
  private void define(Vector monitorExpressions, boolean isDelete, boolean isShow, boolean isHide) {
    /*
     * sets up measures based on an array of atom selection expressions -RMH 3/06
     * 
     *(1) run through first expression, choosing model
     *(2) for each item of next bs, iterate over next bitset, etc.
     *(3) for each last bitset, trigger toggle(int[])
     *
     *simple!
     *
     */
    int nPoints = monitorExpressions.size();
    if (nPoints < 2)
      return;
    boolean isOneToOne = true;
    for (int i = 0; i < nPoints && isOneToOne; i++)
      if (viewer.cardinalityOf((BitSet) monitorExpressions.get(i)) > 1)
        isOneToOne = false;
    int[] atomCountPlusIndices = new int[5];
    atomCountPlusIndices[0] = nPoints;
    nextMeasure(0, nPoints, monitorExpressions, atomCountPlusIndices,
        isOneToOne ? -1 : 0, isDelete, isShow, isHide);
  }

  private void setIndices() {
    for (int i = 0; i < measurementCount; i++)
      measurements[i].setIndex(i);
  }
  
  @SuppressWarnings("unchecked")
private void define(int[] atomCountPlusIndices, boolean isDelete, boolean isShow) {
    if (viewer.getMeasureAllModelsFlag()) {
      if (isShow) { // make sure all like this are deleted, not just hidden
        define(atomCountPlusIndices, true, false); // self-reference
        if (isDelete)
          return;
      }
      Vector measureList = new Vector();
      int nPoints = atomCountPlusIndices[0];
      for (int i = 1; i <= nPoints; i++) {
        Atom atom = frame.atoms[atomCountPlusIndices[i]];
        measureList.add(viewer.getAtomBits("atomno", atom.getAtomNumber()));
      }
      define(measureList, isDelete, false, false);
      return;
    }    
    define(atomCountPlusIndices, isDelete);
  }

  private void define(int[] atomCountPlusIndices, boolean isDelete) {
    int i = defined(atomCountPlusIndices);
    //Logger.debug("define " + isDelete + " " + i + " [" + atomCountPlusIndices[0] + " " + atomCountPlusIndices[1] + " " + atomCountPlusIndices[2] + " " + atomCountPlusIndices[3] + " " + atomCountPlusIndices[4] + "]");
    // nothing to delete and no A-A, A-B-A, A-B-C-B
    int count = atomCountPlusIndices[0];
    if (i < 0 && isDelete
        || atomCountPlusIndices[1] == atomCountPlusIndices[2]
        || count > 2 && atomCountPlusIndices[1] == atomCountPlusIndices[3]
        || count == 4 && atomCountPlusIndices[2] == atomCountPlusIndices[4])
      return;
    float value;
    value = frame.getMeasurement(atomCountPlusIndices);
    if (rangeMinMax[0] != Float.MAX_VALUE
        && (value < rangeMinMax[0] || value > rangeMinMax[1]))
      return;
    if (i >= 0) {
      if (isDelete) {
        viewer.setStatusNewDefaultModeMeasurement("measureDeleted", i, "");
        System.arraycopy(measurements, i + 1, measurements, i, measurementCount
            - i - 1);
        --measurementCount;
        measurements[measurementCount] = null;
      } else {
        measurements[i].isHidden = false;
      }
      return;
    }
    Measurement measureNew = new Measurement(frame, atomCountPlusIndices,
        value, colix, strFormat, measurementCount);
    if (measurementCount == measurements.length) {
      measurements = (Measurement[]) ArrayUtil.setLength(measurements,
          measurementCount + measurementGrowthIncrement);
    }
    viewer.setStatusNewDefaultModeMeasurement("measureCompleted",
        measurementCount, measureNew.toVector().toString());
    measurements[measurementCount++] = measureNew;
  }

  private void showHide(int[] atomCountPlusIndices, boolean isHide) {
    int i = defined(atomCountPlusIndices);
    if (i < 0)
      return;
    measurements[i].isHidden = isHide;
  }
  
  private void showHide(boolean isHide) {
    for (int i = measurementCount; --i >= 0; )
      if (bsSelected == null || bsSelected.get(i))
        measurements[i].isHidden = isHide;
  }

  private void nextMeasure(int thispt, int nPoints, Vector monitorExpressions,
                   int[] atomCountPlusIndices, int thisModel, boolean isDelete,
                   boolean isShow, boolean isHide) {
    BitSet bs = (BitSet) monitorExpressions.get(thispt);
    int iMax = frame.atomCount;
    for (int i = 0; i < iMax; i++) {
      if (bs.get(i)) {
        if (thispt > 0 && i == atomCountPlusIndices[thispt])
          continue;
        int modelIndex = frame.atoms[i].getModelIndex();
        if (thisModel >= 0) {
          if (thispt == 0) {
            thisModel = modelIndex;
          } else if (thisModel != modelIndex) {
            continue;
          }
        }
        atomCountPlusIndices[thispt + 1] = i;
        int iThis;
        if (thispt == nPoints - 1) {
          if (isAllConnected && !isConnected(atomCountPlusIndices))
            continue;
          if ((iThis = defined(atomCountPlusIndices)) >= 0) {
            if (isDelete)
              define(atomCountPlusIndices, true);
            else if (strFormat != null)
              measurements[iThis].formatMeasurement(strFormat);
            else
              showHide(atomCountPlusIndices, isHide);
            continue;
          }
          if (!isDelete && !isHide && !isShow)
            define(atomCountPlusIndices, false);
          continue;
        }
        nextMeasure(thispt + 1, nPoints, monitorExpressions,
            atomCountPlusIndices, thisModel, isDelete, isShow, isHide);
      }
    }
  }
    
  private boolean isConnected(int[] atomCountPlusIndices) {
    Atom[] atoms = frame.atoms;
    for (int i = atomCountPlusIndices[0]; i > 1; --i)
      if (!atoms[atomCountPlusIndices[i]].isBonded(atoms[atomCountPlusIndices[i-1]]))
        return false;
    return true;
  }
  
  private void setRange(float[] rangeMinMax) {
    //Logger.debug("setRange"+rangeMinMax[0]+rangeMinMax[1]);
    this.rangeMinMax[0] = rangeMinMax[0];
    this.rangeMinMax[1] = rangeMinMax[1];
  }
  
  private void setConnected(boolean isAllConnected) {
    this.isAllConnected = isAllConnected;  
  }
  
  private void pending(int[] countPlusIndices) {
    pendingMeasurement.setCountPlusIndices(countPlusIndices);
    if (pendingMeasurement.count > 1)
      viewer.setStatusNewDefaultModeMeasurement("measurePending" , pendingMeasurement.count, pendingMeasurement.strMeasurement);
  }

  private void reformatDistances() {
    for (int i = measurementCount; --i >= 0; )
      measurements[i].reformatDistanceIfSelected();    
  }
  
  private void setFormats(String format) {
    if (format != null && format.length() == 0)
      format = null;
    for (int i = measurementCount; --i >= 0;)
      if (bsSelected == null || bsSelected.get(i))
        measurements[i].formatMeasurement(format);
  }
  
  @SuppressWarnings("unchecked")
private Vector getAllInfo() {
    Vector info = new Vector();
    for (int i = 0; i< measurementCount; i++) {
      info.add(getInfo(i));
    }
    return info;
  }
  
  private String getAllInfoAsString() {
    String info = "Measurement Information";
    for (int i = 0; i< measurementCount; i++) {
      info += "\n" + getInfoAsString(i);
    }
    return info;
  }
  
  @SuppressWarnings("unchecked")
private Hashtable getInfo(int index) {
    int count = measurements[index].count;
    Hashtable info = new Hashtable();
    info.put("index", new Integer(index));
    info.put("type", (count == 2 ? "distance" : count == 3 ? "angle"
        : "dihedral"));
    info.put("strMeasurement", measurements[index].strMeasurement);
    info.put("count", new Integer(count));
    info.put("value", new Float(measurements[index].value));
    Vector atomsInfo = new Vector();
    for (int i = 0; i < count; i++) {
      Hashtable atomInfo = new Hashtable();
      Atom atom = frame.atoms[measurements[index].countPlusIndices[i + 1]];
      atomInfo.put("_ipt", new Integer(atom.atomIndex));
      atomInfo.put("atomno", new Integer(atom.getAtomNumber()));
      atomInfo.put("info", atom.getInfo());
      atomsInfo.add(atomInfo);
    }
    info.put("atoms", atomsInfo);
    return info;
  }

  private String getInfoAsString(int index) {
    int count = measurements[index].count;
    String info = (count == 2 ? "distance" : count == 3 ? "angle" : "dihedral")
        + " \t" + measurements[index].value + " \t"
        + measurements[index].strMeasurement;
    for (int i = 0; i < count; i++) {
      Atom atom = frame.atoms[measurements[index].countPlusIndices[i + 1]];
      info += " \t" + atom.getInfo();
    }
    return info;
  }
  
  void setVisibilityInfo() {
    out:
    for (int i = measurementCount; --i >= 0; ) {
      measurements[i].isVisible = false;
      if(mad == 0 || measurements[i].isHidden)
        continue;
      for (int iAtom = measurements[i].count; iAtom > 0; iAtom--) { 
        Atom atom = frame.getAtomAt(measurements[i].countPlusIndices[iAtom]);
        if (!atom.isClickable())
          continue out;
      }
      measurements[i].isVisible = true;
    }
  }
  
  String getShapeState() {
    StringBuffer commands = new StringBuffer();
    for (int i = 0; i < measurementCount; i++)
      commands.append(getState(i));
    if (!showMeasurementNumbers)
      commands.append("set measures off; # numbers off\n");
    appendCmd(commands, "set measures " + viewer.getMeasureDistanceUnits());
    appendCmd(commands, getFontCommand("measures", font3d));
    int n = 0;
    Hashtable temp = new Hashtable();
    BitSet bs = new BitSet(measurementCount);
    for (int i = 0; i < measurementCount; i++) {
      if (measurements[i].isHidden) {
        n++;
        bs.set(i);
      }
      if (bsColixSet != null && bsColixSet.get(i))
        setStateInfo(temp, i, getColorCommand("measure", measurements[i].colix));
      if (measurements[i].strFormat != null)
        setStateInfo(temp, i, "measure "
            + StateManager.escape(measurements[i].strFormat));
    }
    if (n > 0)
      if (n == measurementCount)
        commands.append("measures off; # lines and numbers off\n");
      else
        for (int i = 0; i < measurementCount; i++)
          if (measurements[i].isHidden)
            setStateInfo(temp, i, "measure off");
    String s = getShapeCommands(temp, null, -1, "select measures");
    if (s != null) {
      commands.append(s);
      appendCmd(commands, "select measures ({null})");
    }
    return commands.toString();
  }
  
  private String getState(int index) {
    int count = measurements[index].count;
    String info = "measure";
    for (int i = 0; i < count; i++)
      info += "({" + measurements[index].countPlusIndices[i + 1] + "})";
    info += "; # " + getInfoAsString(index) + "\n";
    return info;
  }
}
