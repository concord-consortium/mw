/* $RCSfile: XyzReader.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:15 $
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


import java.io.BufferedReader;

import org.jmol.viewer.JmolConstants;

/**
 * Minnesota SuperComputer Center XYZ file format
 * 
 * simple symmetry extension via load command:
 * 9/2006 hansonr@stolaf.edu
 * 
 *  setAtomCoord(atom)
 *  applySymmetry()
 *  
 *  extended to read XYZI files (Bob's invention -- allows isotope numbers)
 * 
 */

class XyzReader extends AtomSetCollectionReader {
    
  AtomSetCollection readAtomSetCollection(BufferedReader reader) throws Exception {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("xyz");
    boolean iHaveAtoms = false;

    try {
      int modelAtomCount;
      while ((modelAtomCount = readAtomCount()) > 0) {
        if (++modelNumber != desiredModelNumber && desiredModelNumber > 0) {
          if (iHaveAtoms)
            break;
          skipAtomSet(modelAtomCount);
          continue;
        }
        iHaveAtoms = true;
        readAtomSetName();
        readAtoms(modelAtomCount);
        applySymmetry();
      }
    } catch (Exception ex) {
      atomSetCollection.errorMessage = "Could not read file:" + ex;
    }
    return atomSetCollection;
  }
    
  void skipAtomSet(int modelAtomCount) throws Exception {
    readLine(); //comment
    for (int i = modelAtomCount; --i >=0; )
      readLine(); //atoms
  }
  
  int readAtomCount() throws Exception {
    readLine();
    if (line != null) {
      int atomCount = parseInt(line);
      if (atomCount > 0)
        return atomCount;
    }
    return 0;
  }

  void readAtomSetName() throws Exception {
    readLineTrimmed();
    checkLineForScript();
    newAtomSet(line);
  }

  final float[] chargeAndOrVector = new float[4];
  final boolean isNaN[] = new boolean[4];
  
  void readAtoms(int modelAtomCount) throws Exception {
    for (int i = 0; i < modelAtomCount; ++i) {
      readLine();
      Atom atom = atomSetCollection.addNewAtom();
      int isotope = parseInt(line);
      String str = parseToken(line);
      // xyzI
      if (isotope == Integer.MIN_VALUE) {
        atom.elementSymbol = str;
      } else {
        str = str.substring((""+isotope).length());
        atom.elementNumber = (short)((isotope << 7) + JmolConstants.elementNumberFromSymbol(str));
        atomSetCollection.setFileTypeName("xyzi");
      }
      atom.x = parseFloat(line, ichNextParse);
      atom.y = parseFloat(line, ichNextParse);
      atom.z = parseFloat(line, ichNextParse);
      if (Float.isNaN(atom.x) || Float.isNaN(atom.y) || Float.isNaN(atom.z)) {
        logger.log("line cannot be read for XYZ atom data: " + line);
        atom.x = 0;
        atom.y = 0;
        atom.z = 0;
      }
      setAtomCoord(atom);
      for (int j = 0; j < 4; ++j)
        isNaN[j] =
          Float.isNaN(chargeAndOrVector[j] = parseFloat(line, ichNextParse));
      if (isNaN[0])
        continue;
      if (isNaN[1]) {
        atom.formalCharge = (int)chargeAndOrVector[0];
        continue;
      }
      if (isNaN[3]) {
        atom.vectorX = chargeAndOrVector[0];
        atom.vectorY = chargeAndOrVector[1];
        atom.vectorZ = chargeAndOrVector[2];
        continue;
      }
      atom.formalCharge = (int)chargeAndOrVector[0];
      atom.vectorX = chargeAndOrVector[1];
      atom.vectorY = chargeAndOrVector[2];
      atom.vectorZ = chargeAndOrVector[3];
    }
  }
}
