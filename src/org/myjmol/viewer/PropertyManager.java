/* $RCSfile: PropertyManager.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:11 $
 * $Revision: 1.1 $
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
package org.myjmol.viewer;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.myjmol.util.Logger;

/**
 * 
 * The PropertyManager handles all operations relating to delivery of
 * properties with the getProperty() method, or its specifically cast 
 * forms getPropertyString() or getPropertyJSON().
 *
 */

class PropertyManager {

  Viewer viewer;

  PropertyManager(Viewer viewer) {
    this.viewer = viewer;
  }


  final static String[] propertyTypes = {
    "appletInfo"      , "", "",
    "fileName"        , "", "",
    "fileHeader"      , "", "",
    "fileContents"    , "", "",
    "fileContents"    , "<pathname>", "",
  
    "animationInfo"   , "", "",
    "modelInfo"       , "", "",
    "X -vibrationInfo", "", "",  //not implemented -- see auxiliaryInfo
    "shapeInfo"       , "", "",
    "measurementInfo" , "", "",
    
    "centerInfo"      , "", "",
    "orientationInfo" , "", "",
    "transformInfo"   , "", "",
    "atomList"        , "<atom selection>", "visible",
    "atomInfo"        , "<atom selection>", "visible",
    
    "bondInfo"        , "<atom selection>", "visible",
    "chainInfo"       , "<atom selection>", "visible",
    "polymerInfo"     , "<atom selection>", "visible",
    "moleculeInfo"    , "<atom selection>", "visible",
    "stateInfo"       , "", "",
    
    "extractModel"    , "<atom selection>", "visible",
    "jmolStatus"      , "statusNameList", "",
    "jmolViewer"      , "", "",
    "messageQueue"    , "", "",
    "auxiliaryInfo"   , "", "",
    
    "boundBoxInfo"    , "", "",  
    "dataInfo"        , "<data type>", "types",
    "image"           , "", "",
  };

  final static int PROP_APPLET_INFO = 0;
  final static int PROP_FILENAME = 1;
  final static int PROP_FILEHEADER = 2;
  final static int PROP_FILECONTENTS = 3;
  final static int PROP_FILECONTENTS_PATH = 4;
  
  final static int PROP_ANIMATION_INFO = 5;
  final static int PROP_MODEL_INFO = 6;
  final static int PROP_VIBRATION_INFO = 7; //not implemented -- see auxiliaryInfo
  final static int PROP_SHAPE_INFO = 8;
  final static int PROP_MEASUREMENT_INFO = 9;
  
  final static int PROP_CENTER_INFO = 10;
  final static int PROP_ORIENTATION_INFO = 11;
  final static int PROP_TRANSFORM_INFO = 12;
  final static int PROP_ATOM_LIST = 13;
  final static int PROP_ATOM_INFO = 14;
  
  final static int PROP_BOND_INFO = 15;
  final static int PROP_CHAIN_INFO = 16;
  final static int PROP_POLYMER_INFO = 17;
  final static int PROP_MOLECULE_INFO = 18;
  final static int PROP_STATE_INFO = 19;
  
  final static int PROP_EXTRACT_MODEL = 20;
  final static int PROP_JMOL_STATUS = 21;
  final static int PROP_JMOL_VIEWER = 22;
  final static int PROP_MESSAGE_QUEUE = 23;
  final static int PROP_AUXILIARY_INFO = 24;
  
  final static int PROP_BOUNDBOX_INFO = 25;
  final static int PROP_DATA_INFO = 26;
  final static int PROP_IMAGE = 27;
  final static int PROP_COUNT = 28;

  int getPropertyNumber(String infoType) {
    if (infoType == null)
      return -1;
    for(int i = 0; i < PROP_COUNT; i++)
      if(infoType.equalsIgnoreCase(getPropertyName(i)))
        return i;
    return -1;
  }
  
  String getPropertyName(int propID) {
    if (propID < 0)
      return "";
    return propertyTypes[propID * 3];
  }
  
  String getParamType(int propID) {
    if (propID < 0)
      return "";
    return propertyTypes[propID * 3 + 1];
  }
  
  String getDefaultParam(int propID) {
    if (propID < 0)
      return "";
    return propertyTypes[propID * 3 + 2];
  }
  
  final static String[] readableTypes = {
    "stateinfo", "extractmodel", "filecontents", "fileheader", "image"};
  
  boolean isReadableAsString(String infoType) {
    for (int i = readableTypes.length; --i >= 0; )
      if (infoType.equalsIgnoreCase(readableTypes[i]))
          return true;
    return false;
  }

  boolean requestedReadable = false;
  
  synchronized Object getProperty(String returnType, String infoType, String paramInfo) {
    if (propertyTypes.length != PROP_COUNT * 3)
      Logger.warn("propertyTypes is not the right length: " + propertyTypes.length + " != " + PROP_COUNT * 3);
    
    Object info = getPropertyAsObject(infoType, paramInfo);
    if (returnType == null)
      return info;
    requestedReadable = returnType.equalsIgnoreCase("readable");
    if (requestedReadable)
      returnType = (isReadableAsString(infoType) ? "String" : "JSON");
    if (returnType.equalsIgnoreCase("String")) return info.toString();
    if (requestedReadable)
      return toReadable(infoType, info);
    else if (returnType.equalsIgnoreCase("JSON"))
      return "{" + toJSON(infoType, info) + "}";
    return info;
  }
  
  synchronized private Object getPropertyAsObject(String infoType,
                                                  String paramInfo) {
    //Logger.debug("getPropertyAsObject(\"" + infoType+"\", \"" + paramInfo + "\")");
    int id = getPropertyNumber(infoType);
    boolean iHaveParameter = (paramInfo != null && paramInfo.length() > 0);
    String myParam = (iHaveParameter ? paramInfo : getDefaultParam(id));
    switch (id) {
    case PROP_APPLET_INFO:
      return viewer.getAppletInfo();
    case PROP_ANIMATION_INFO:
      return viewer.getAnimationInfo();
    case PROP_ATOM_LIST:
      return viewer.getAtomBitSetVector(myParam);
    case PROP_ATOM_INFO:
      return viewer.getAllAtomInfo(myParam);
    case PROP_AUXILIARY_INFO:
      return viewer.getAuxiliaryInfo();
    case PROP_BOND_INFO:
      return viewer.getAllBondInfo(myParam);
    case PROP_BOUNDBOX_INFO:
      return viewer.getBoundBoxInfo();
    case PROP_CENTER_INFO:
      return viewer.getRotationCenter();
    case PROP_CHAIN_INFO:
      return viewer.getAllChainInfo(myParam);
    case PROP_EXTRACT_MODEL:
      return viewer.getModelExtract(myParam);
    case PROP_FILENAME:
      return viewer.getFullPathName();
    case PROP_FILEHEADER:
      return viewer.getFileHeader();
    case PROP_FILECONTENTS:
    case PROP_FILECONTENTS_PATH:
      if (iHaveParameter)
        return viewer.getFileAsString(myParam);
      return viewer.getCurrentFileAsString();
    case PROP_JMOL_STATUS:
      return viewer.getStatusChanged(myParam);
    case PROP_JMOL_VIEWER:
      return viewer.getViewer();
    case PROP_MEASUREMENT_INFO:
      return viewer.getMeasurementInfo();
    case PROP_MESSAGE_QUEUE:
      return viewer.getMessageQueue();
    case PROP_MODEL_INFO:
      return viewer.getModelInfo();
    case PROP_MOLECULE_INFO:
      return viewer.getMoleculeInfo(myParam);
    case PROP_ORIENTATION_INFO:
      return viewer.getOrientationInfo();
    case PROP_POLYMER_INFO:
      return viewer.getAllPolymerInfo(myParam);
    case PROP_SHAPE_INFO:
      return viewer.getShapeInfo();
    case PROP_STATE_INFO:
      return viewer.getStateInfo();
    case PROP_TRANSFORM_INFO:
      return viewer.getMatrixRotate();
    case PROP_DATA_INFO:
      return viewer.getData(myParam);
    case PROP_IMAGE:
      return viewer.getJpegBase64(100);
    }
    String info = "getProperty ERROR\n" + infoType + "?\nOptions include:\n";
    for (int i = 0; i < PROP_COUNT; i++) {
      String paramType = getParamType(i);
      String paramDefault = getDefaultParam(i);
      String name = getPropertyName(i);
      if (name.charAt(0) != 'X')
        info += "\n getProperty(\""
            + name
            + "\""
            + (paramType != "" ? ",\"" + paramType
                + (paramDefault != "" ? "[" + paramDefault + "]" : "") + "\""
                : "") + ")";
    }
    return info;
  }
   
  String packageJSON (String infoType, String info) {
    if (infoType == null) return info;
    return "\"" + infoType + "\": " + info;
  }
  
  String packageReadable (String infoType, String info) {
    if (infoType == null) return info;
    return "\n" + infoType + "\t" + info;
  }
 
  String fixString(String s) {
    if (s == null || s.indexOf("{\"") == 0) //don't doubly fix JSON strings when retrieving status
      return s;
    s = viewer.simpleReplace(s,"\"","''");
    s = viewer.simpleReplace(s,"\n"," | ");
   return "\"" + s + "\"";  
  }
  
  String toJSON(String infoType, Object info) {

    //Logger.debug(infoType+" -- "+info);

    String str = "";
    String sep = "";
    if (info == null)
      return packageJSON(infoType, null);
    if (info instanceof String)
      return packageJSON(infoType, fixString((String) info));
    if (info instanceof String[]) {
      str = "[";
      int imax = ((String[]) info).length;  
      for (int i = 0; i < imax; i++) {
        str += sep + fixString(((String[]) info)[i]);
        sep = ",";
      }
      str += "]";
      return packageJSON(infoType, str);
    }
    if (info instanceof int[]) {
      str = "[";
      int imax = ((int[]) info).length;
      for (int i = 0; i < imax; i++) {
        str += sep + ((int[]) info)[i];
        sep = ",";
      }
      str += "]";
      return packageJSON(infoType, str);
    }
    if (info instanceof float[]) {
      str = "[";
      int imax = ((float[]) info).length;
      for (int i = 0; i < imax; i++) {
        str += sep + ((float[]) info)[i];
        sep = ",";
      }
      str += "]";
      return packageJSON(infoType, str);
    }
    if (info instanceof int[][]) {
      str = "[";
      int imax = ((int[][]) info).length;
      for (int i = 0; i < imax; i++) {
        str += sep + toJSON(null, ((int[][]) info)[i]);
        sep = ",";
      }
      str += "]";
      return packageJSON(infoType, str);
    }
    if (info instanceof float[][]) {
      str = "[";
      int imax = ((float[][]) info).length;
      for (int i = 0; i < imax; i++) {
        str += sep + toJSON(null, ((float[][]) info)[i]);
        sep = ",";
      }
      str += "]";
      return packageJSON(infoType, str);
    }
    if (info instanceof Vector) {
      str = "[";
      int imax = ((Vector) info).size();
      for (int i = 0; i < imax; i++) {
        str += sep + toJSON(null, ((Vector) info).get(i));
        sep = ",";
      }
      str += "]";
      return packageJSON(infoType, str);
    }
    if (info instanceof Matrix3f) {
      str = "[";
      str += "[" + ((Matrix3f) info).m00 + "," + ((Matrix3f) info).m01
          + "," + ((Matrix3f) info).m02 + "]";
      str += ",[" + ((Matrix3f) info).m10 + "," + ((Matrix3f) info).m11
          + "," + ((Matrix3f) info).m12 + "]";
      str += ",[" + ((Matrix3f) info).m20 + "," + ((Matrix3f) info).m21
          + "," + ((Matrix3f) info).m22 + "]";
      str += "]";
      return packageJSON(infoType, str);
    }
    if (info instanceof Point3f) {
      str += "[" + ((Point3f) info).x + "," + ((Point3f) info).y + ","
          + ((Point3f) info).z + "]";
      return packageJSON(infoType, str);
    }
    if (info instanceof Vector3f) {
      str += "[" + ((Vector3f) info).x + "," + ((Vector3f) info).y + ","
          + ((Vector3f) info).z + "]";
      return packageJSON(infoType, str);
    }
    if (info instanceof Hashtable) {
      str = "{";
      Enumeration e = ((Hashtable) info).keys();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        str += sep
            + packageJSON(key, toJSON(null, ((Hashtable) info).get(key)));
        sep = ",";
      }
      str += "}";
      return packageJSON(infoType, str);
    }
    return packageJSON(infoType, info.toString());
  }

  String toReadable(String infoType, Object info) {

    //Logger.debug(infoType+" -- "+info);

    String str = "";
    String sep = "";
    if (info == null)
      return "null";
    if (info instanceof String)
      return packageReadable(infoType, fixString((String) info));
    if (info instanceof String[]) {
      str = "[";
      int imax = ((String[]) info).length;  
      for (int i = 0; i < imax; i++) {
        str += sep + fixString(((String[]) info)[i]);
        sep = ",";
      }
      str += "]";
      return packageReadable(infoType, str);
    }
    if (info instanceof int[]) {
      str = "[";
      int imax = ((int[]) info).length;
      for (int i = 0; i < imax; i++) {
        str += sep + ((int[]) info)[i];
        sep = ",";
      }
      str += "]";
      return packageReadable(infoType, str);
    }
    if (info instanceof float[]) {
      str = "";
      int imax = ((float[]) info).length;
      for (int i = 0; i < imax; i++) {
        str += sep + ((float[]) info)[i];
        sep = ",";
      }
      str += "";
      return packageReadable(infoType, str);
    }
    if (info instanceof int[][]) {
      str = "[";
      int imax = ((int[][]) info).length;
      for (int i = 0; i < imax; i++) {
        str += sep + toReadable(null, ((int[][]) info)[i]);
        sep = ",";
      }
      str += "]";
      return packageReadable(infoType, str);
    }
    if (info instanceof float[][]) {
      str = "[";
      int imax = ((float[][]) info).length;
      for (int i = 0; i < imax; i++) {
        str += sep + toReadable(null, ((float[][]) info)[i]);
        sep = ",";
      }
      str += "]";
      return packageReadable(infoType, str);
    }
    if (info instanceof Vector) {
      str = "";
      int imax = ((Vector) info).size();
      for (int i = 0; i < imax; i++) {
        str += sep + toReadable(null, ((Vector) info).get(i));
        sep = ",";
      }
//      str += "\n";
      return packageReadable(infoType, str);
    }
    if (info instanceof Matrix3f) {
      str = "[";
      str += "[" + ((Matrix3f) info).m00 + "," + ((Matrix3f) info).m01
          + "," + ((Matrix3f) info).m02 + "]";
      str += ",[" + ((Matrix3f) info).m10 + "," + ((Matrix3f) info).m11
          + "," + ((Matrix3f) info).m12 + "]";
      str += ",[" + ((Matrix3f) info).m20 + "," + ((Matrix3f) info).m21
          + "," + ((Matrix3f) info).m22 + "]";
      str += "]";
      return packageReadable(infoType, str);
    }
    if (info instanceof Point3f) {
      str += "[" + ((Point3f) info).x + "," + ((Point3f) info).y + ","
          + ((Point3f) info).z + "]";
      return packageReadable(infoType, str);
    }
    if (info instanceof Vector3f) {
      str += "[" + ((Vector3f) info).x + "," + ((Vector3f) info).y + ","
          + ((Vector3f) info).z + "]";
      return packageReadable(infoType, str);
    }
    if (info instanceof Hashtable) {
      str = "";
      Enumeration e = ((Hashtable) info).keys();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        str += sep
            + packageReadable(key, toReadable(null, ((Hashtable) info).get(key)));
        sep = "";
      }
      str += "\n";
      return packageReadable(infoType, str);
    }
    return packageReadable(infoType, info.toString());
  }
}
