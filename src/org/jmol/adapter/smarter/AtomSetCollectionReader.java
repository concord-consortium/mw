/* $RCSfile: AtomSetCollectionReader.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:16 $
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

package org.jmol.adapter.smarter;


import org.jmol.api.JmolAdapter;
import org.jmol.symmetry.SpaceGroup;
import org.jmol.symmetry.UnitCell;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.viewer.JmolConstants;

import java.io.BufferedReader;

/*
 * Notes 9/2006 Bob Hanson
 * 
 * all parsing functions now moved to org.jmol.util.Parser
 * 
 * to add symmetry capability to any reader, some or all of the following 
 * methods need to be there:
 * 
 *  setFractionalCoordinates()
 *  setSpaceGroupName()
 *  setUnitCell()
 *  setUnitCellItem()
 *  setAtomCoord()
 *  applySymmetry()
 * 
 * At the very minimum, you need:
 * 
 *  setAtomCoord()
 *  applySymmetry()
 * 
 * so that:
 *  (a) atom coordinates can be turned fractional by load parameters
 *  (b) symmetry can be applied once per model in the file
 *  
 *  If you know where the end of the atom+bond data are, then you can
 *  use applySymmetry() once, just before exiting. Otherwise, use it
 *  twice -- it has a check to make sure it doesn't RUN twice -- once
 *  at the beginning and once at the end of the model.
 *  
 *  LOAD PARAMETERS:
 *  
 *  load parameters are in the form of an integer array of varying length.
 *  Some of these must be implemented in individual readers
 *  
 *  0:       desired model number
 *  1 - 3:   {i j k} lattice parameters for applying symmetry
 *  4:       desired space group number
 *  5 - 10:  unit cell parameters a b c alpha beta gamma
 * 
 */

abstract class AtomSetCollectionReader extends Parser {
  AtomSetCollection atomSetCollection;
  JmolAdapter.Logger logger;
  BufferedReader reader;
  String line;

  final static float ANGSTROMS_PER_BOHR = 0.5291772f;

  void setLogger(JmolAdapter.Logger logger) {
    this.logger = logger;
  }

  int desiredModelNumber;
  int modelNumber;
  boolean iHaveDesiredModel;

  String spaceGroup;
  UnitCell unitcell;
  float[] notionalUnitCell; //0-5 a b c alpha beta gamma; 6-21 matrix c->f
  int[] latticeCells = new int[3];
  int desiredSpaceGroupIndex;

  // load options:

  boolean doApplySymmetry;
  boolean doConvertToFractional;
  boolean fileCoordinatesAreFractional;
  boolean ignoreFileUnitCell;
  boolean ignoreFileSymmetryOperators;
  boolean ignoreFileSpaceGroupName;

  // state variables
  boolean iHaveUnitCell;
  boolean iHaveCartesianToFractionalMatrix;
  boolean iHaveFractionalCoordinates;
  boolean iHaveSymmetryOperators;
  boolean needToApplySymmetry;

  abstract AtomSetCollection readAtomSetCollection(BufferedReader reader)
      throws Exception;

  AtomSetCollection readAtomSetCollectionFromDOM(Object DOMNode)
      throws Exception {
    return null;
  }

  void initialize() {
    // called by the resolver
    modelNumber = 0;
    desiredModelNumber = -1;
    iHaveDesiredModel = false;

    latticeCells[0] = latticeCells[1] = latticeCells[2] = 0;

    desiredSpaceGroupIndex = -1;

    ignoreFileUnitCell = false;
    ignoreFileSpaceGroupName = false;
    ignoreFileSymmetryOperators = false;
    doConvertToFractional = false;
    doApplySymmetry = false;

    fileCoordinatesAreFractional = false;

    iHaveUnitCell = false;
    iHaveCartesianToFractionalMatrix = false;
    iHaveFractionalCoordinates = false;
    iHaveSymmetryOperators = false;

    initializeSymmetry();
  }

  void initialize(int[] params) {
    initialize();
    if (params == null)
      return;

    // params is of variable length: 4, 5, or 11
    // [desiredModelNumber, i, j, k, 
    //  desiredSpaceGroupIndex,
    //  a*10000, b*10000, c*10000, alpha*10000, beta*10000, gamma*10000]

    desiredModelNumber = params[0];
    latticeCells[0] = params[1];
    latticeCells[1] = params[2];
    latticeCells[2] = params[3];
    doApplySymmetry = (latticeCells[0] > 0 && latticeCells[1] > 0
        && latticeCells[2] > 0 || latticeCells[0] > 9 && latticeCells[1] > 9);
    //allows for {1 1 1} or {555 555 0|1}
    if (!doApplySymmetry) {
      latticeCells[0] = 0;
      latticeCells[1] = 0;
      latticeCells[2] = 0;
    }

    //this flag FORCES symmetry -- generally if coordinates are not fractional,
    //we may note the unit cell, but we do not apply symmetry
    //with this flag, we convert any nonfractional coordinates to fractional
    //if a unit cell is available.

    if (params.length >= 5) {
      // three options include:
      // = -1: normal -- use operators if present or name if not
      // >=0: spacegroup fully determined
      // = -999: ignore just the operators

      desiredSpaceGroupIndex = params[4];
      ignoreFileSpaceGroupName = (desiredSpaceGroupIndex >= 0);
      ignoreFileSymmetryOperators = (desiredSpaceGroupIndex != -1);
    }
    if (params.length >= 11) {
      setUnitCell(params[5] / 10000f, params[6] / 10000f, params[7] / 10000f,
          params[8] / 10000f, params[9] / 10000f, params[10] / 10000f);
      ignoreFileUnitCell = iHaveUnitCell;
    }
  }

  private void initializeSymmetry() {
    iHaveUnitCell = ignoreFileUnitCell;
    if (!ignoreFileUnitCell) {
      notionalUnitCell = new float[22];
      //0-5 a b c alpha beta gamma; 6-21 m00 m01... m33 cartesian-->fractional
      for (int i = 22; --i >= 0;)
        notionalUnitCell[i] = Float.NaN;
      unitcell = null;
    }
    if (!ignoreFileSpaceGroupName)
      spaceGroup = "unspecified";

    needToApplySymmetry = false;
  }

  void initializeCartesianToFractional() {
    for (int i = 0; i < 16; i++)
      notionalUnitCell[6 + i] = ((i % 5 == 0 ? 1 : 0));
  }

  void newAtomSet(String name) {
    if (atomSetCollection.currentAtomSetIndex >= 0) {
      atomSetCollection.newAtomSet();
      atomSetCollection.setCollectionName("<collection of "
          + (atomSetCollection.currentAtomSetIndex + 1) + " models>");
    } else {
      atomSetCollection.setCollectionName(name);
    }
    logger.log(name);
  }

  void setSpaceGroupName(String name) {
    if (ignoreFileSpaceGroupName)
      return;
    spaceGroup = name.trim();
  }

  void setSymmetryOperator(String jonesFaithful) {
    if (ignoreFileSymmetryOperators)
      return;
    atomSetCollection.setLatticeCells(latticeCells);
    if (!atomSetCollection.addSymmetry(jonesFaithful))
      Logger.warn("Skipping symmetry operation " + jonesFaithful);
    iHaveSymmetryOperators = true;
  }

  void setUnitCellItem(int i, float x) {
    if (ignoreFileUnitCell)
      return;
    if (i >= 6 && Float.isNaN(notionalUnitCell[6]))
      initializeCartesianToFractional();
    notionalUnitCell[i] = x;
    if (logger != null) logger.log("setunitcellitem " + i + " " + x);
    if (i < 6)
      iHaveUnitCell = checkUnitCell(6);
    else
      iHaveCartesianToFractionalMatrix = checkUnitCell(22);
  }

  void setUnitCell(float a, float b, float c, float alpha, float beta,
                   float gamma) {
    if (ignoreFileUnitCell)
      return;
    notionalUnitCell[UnitCell.INFO_A] = a;
    notionalUnitCell[UnitCell.INFO_B] = b;
    notionalUnitCell[UnitCell.INFO_C] = c;
    notionalUnitCell[UnitCell.INFO_ALPHA] = alpha;
    notionalUnitCell[UnitCell.INFO_BETA] = beta;
    notionalUnitCell[UnitCell.INFO_GAMMA] = gamma;
    iHaveUnitCell = checkUnitCell(6);
  }

  private boolean checkUnitCell(int n) {
    for (int i = 0; i < n; i++)
      if (Float.isNaN(notionalUnitCell[i]))
        return false;
    unitcell = new UnitCell(notionalUnitCell);
    if (doApplySymmetry)
      doConvertToFractional = !fileCoordinatesAreFractional;
    //if (but not only if) applying symmetry do we force conversion
    return true;
  }

  void setFractionalCoordinates(boolean TF) {
    iHaveFractionalCoordinates = fileCoordinatesAreFractional = TF;
  }

  void setAtomCoord(Atom atom, float x, float y, float z) {
    atom.x = x;
    atom.y = y;
    atom.z = z;
    setAtomCoord(atom);
  }

  void setAtomCoord(Atom atom) {
    if (doConvertToFractional && !fileCoordinatesAreFractional
        && unitcell != null) {
      unitcell.toFractional(atom);
      iHaveFractionalCoordinates = true;
    }
    //if (Logger.isActiveLevel(Logger.LEVEL_DEBUG))
    //Logger.debug(" atom "+atom.atomName + " " + atom.x + " " + atom.y+" "+atom.z);
    needToApplySymmetry = true;
  }

  void applySymmetry() {
    if (!needToApplySymmetry || !iHaveUnitCell) {
      initializeSymmetry();
      return;
    }
    atomSetCollection.setCoordinatesAreFractional(iHaveFractionalCoordinates);
    atomSetCollection.setNotionalUnitCell(notionalUnitCell);
    atomSetCollection.setAtomSetSpaceGroupName(spaceGroup);
    if (doConvertToFractional || fileCoordinatesAreFractional) {
      atomSetCollection.setLatticeCells(latticeCells);
      if (ignoreFileSpaceGroupName || !iHaveSymmetryOperators) {
        SpaceGroup sg = SpaceGroup.createSpaceGroup(desiredSpaceGroupIndex,
            spaceGroup, notionalUnitCell);
        if (sg != null) {
          if (Logger.isActiveLevel(Logger.LEVEL_DEBUG))
            Logger.debug("using generated space group " + sg.dumpInfo());
          atomSetCollection.setAtomSetSpaceGroupName(sg.getName());
          atomSetCollection.applySymmetry(sg);
        }
      } else {
        atomSetCollection.applySymmetry();
      }
    }
    initializeSymmetry();
  }

  static String getElementSymbol(int elementNumber) {
    return JmolConstants.elementSymbolFromNumber(elementNumber);
  }

  void fillDataBlock(String[][] data) throws Exception {
    int nLines = data.length;
    for (int i = 0; i < nLines; i++)
      data[i] = getTokens(discardLinesUntilNonBlank());
  }

  void discardLines(int nLines) throws Exception {
    for (int i = nLines; --i >= 0;)
      readLine();
  }

  String discardLinesUntilStartsWith(String startsWith) throws Exception {
    while (readLine() != null && !line.startsWith(startsWith)) {
    }
    return line;
  }

  String discardLinesUntilContains(String containsMatch) throws Exception {
    while (readLine() != null && line.indexOf(containsMatch) < 0) {
    }
    return line;
  }

  void discardLinesUntilBlank() throws Exception {
    while (readLine() != null && line.trim().length() != 0) {
    }
  }

  String discardLinesUntilNonBlank() throws Exception {
    while (readLine() != null && line.trim().length() == 0) {
    }
    return line;
  }

  void checkLineForScript() {
    if (line.endsWith("#noautobond")) {
      line = line.substring(0, line.lastIndexOf('#')).trim();
      atomSetCollection.setAtomSetCollectionProperty("noautobond", "true");
    }
    int pt = line.indexOf("#jmolscript:");
    if (pt >= 0) {
      String script = line.substring(pt + 12, line.length());
      if (script.indexOf("#") >= 0) {
        script = script.substring(0, script.indexOf("#"));
      }
      String previousScript = atomSetCollection
          .getAtomSetCollectionProperty("jmolscript");
      if (previousScript == null)
        previousScript = "";
      else
        previousScript += ";";
      atomSetCollection.setAtomSetCollectionProperty("jmolscript",
          previousScript + script);
      line = line.substring(0, pt).trim();
    }
  }

  String readLine() throws Exception {
    line = reader.readLine();
    return line;
  }

  String readLineTrimmed() throws Exception {
    readLine();
    if (line == null)
      line = "";
    return line = line.trim();
  }
  
  String[] getStrings(String sinfo, int nFields, int width) {
    String[] fields = new String[nFields];
    int pt = 0;
    for (int i = 0; i < nFields; i++)
      fields[i] = sinfo.substring(pt, width);
    return fields;
  }
  
  
}
