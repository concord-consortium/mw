/* $RCSfile: GamessReader.java,v $
 * $Author: qxie $
 * $Date: 2007-03-27 18:22:43 $
 * $Revision: 1.12 $
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
import java.util.Hashtable;
import java.util.Vector;

import org.jmol.util.Logger;

class GamessReader extends AtomSetCollectionReader {

  final static float angstromsPerBohr = 0.529177f;

  int atomCount = 0;
  int moCount = 0;
  int shellCount = 0;
  int gaussianCount = 0;
  String calculationType = "?";
  Hashtable moData = new Hashtable();
  Vector orbitals = new Vector();

  @SuppressWarnings("unchecked")
AtomSetCollection readAtomSetCollection(BufferedReader reader)
      throws Exception {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("gamess");

    readLine();
    boolean iHaveAtoms = false;

    try {
      while (line != null) {
        if (line.indexOf("COORDINATES (BOHR)") >= 0 || line.indexOf("COORDINATES OF ALL ATOMS ARE (ANGS)") >= 0) {
          if (++modelNumber != desiredModelNumber && desiredModelNumber > 0) {
            if (iHaveAtoms)
              break;
            readLine();
            continue;
          }
          if (line.indexOf("COORDINATES (BOHR)") >= 0)
            readAtomsInBohrCoordinates();
          else
            readAtomsInAngstromCoordinates();
          iHaveAtoms = true;
        } else if (iHaveAtoms && line.indexOf("FREQUENCIES IN CM") >= 0) {
          readFrequencies();
        } else if (iHaveAtoms && line.indexOf("ATOMIC BASIS SET") >= 0) {
          readGaussianBasis();
          moData.put("calculationType", calculationType);
          atomSetCollection.setAtomSetAuxiliaryInfo("moData", moData);
          continue;
        } else if (iHaveAtoms && line.indexOf("EIGENVECTORS") >= 0) {
          readMolecularOrbitals();
          moData.put("mos", orbitals);
          atomSetCollection.setAtomSetAuxiliaryInfo("moData", moData);
          continue;
        }
        readLine();
      }
    } catch (Exception e) {
      logger.log(e + " at line: " + line);
      Logger.error(null, e);
      //TODO: Why this ?
      //new NullPointerException();
    }
    if (atomSetCollection.atomCount == 0) {
      atomSetCollection.errorMessage = "No atoms in file";
    }
    return atomSetCollection;
  }
  
  void readAtomsInBohrCoordinates() throws Exception {
/*
 ATOM      ATOMIC                      COORDINATES (BOHR)
           CHARGE         X                   Y                   Z
 C           6.0     3.9770911639       -2.7036584676       -0.3453920672

0         1         2         3         4         5         6         7    
01234567890123456789012345678901234567890123456789012345678901234567890123456789

*/    

    readLine(); // discard one line
    String atomName;
    atomSetCollection.newAtomSet();
    int n = 0;
    while (readLine() != null
        && (atomName = parseToken(line, 1, 6)) != null) {
      float x = parseFloat(line, 17, 37);
      float y = parseFloat(line, 37, 57);
      float z = parseFloat(line, 57, 77);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        break;
      Atom atom = atomSetCollection.addNewAtom();
      atom.atomName = atomName + (++n);
      atom.x = x * angstromsPerBohr;
      atom.y = y * angstromsPerBohr;
      atom.z = z * angstromsPerBohr;
    }
  }

  void readAtomsInAngstromCoordinates() throws Exception {
    readLine(); 
    readLine(); // discard two lines
    String atomName;
    atomSetCollection.newAtomSet();
/*    
       COORDINATES OF ALL ATOMS ARE (ANGS)
   ATOM   CHARGE       X              Y              Z
 ------------------------------------------------------------
 C           6.0   2.1045861621  -1.4307145508  -0.1827736240

0         1         2         3         4         5         6    
0123456789012345678901234567890123456789012345678901234567890

*/
    int n = 0;
    while (readLine() != null
        && (atomName = parseToken(line, 1, 6)) != null) {
      float x = parseFloat(line, 16, 31);
      float y = parseFloat(line, 31, 46);
      float z = parseFloat(line, 46, 61);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        break;
      Atom atom = atomSetCollection.addNewAtom();
      atom.atomName = atomName + (++n);
      atom.x = x;
      atom.y = y;
      atom.z = z;
    }
  }
  /*
   * 
   ATOMIC BASIS SET
   ----------------
   THE CONTRACTED PRIMITIVE FUNCTIONS HAVE BEEN UNNORMALIZED
   THE CONTRACTED BASIS FUNCTIONS ARE NOW NORMALIZED TO UNITY

   SHELL TYPE PRIMITIVE    EXPONENT          CONTRACTION COEFFICIENTS

   C         


   1   S    1           172.2560000       .061766907377
   1   S    2            25.9109000       .358794042852
   1   S    3             5.5333500       .700713083689

   2   L    4             3.6649800      -.395895162119       .236459946619
   2   L    5              .7705450      1.215834355681       .860618805716


   */
  @SuppressWarnings("unchecked")
void readGaussianBasis() throws Exception {
    Vector sdata = new Vector();
    Vector gdata = new Vector();
    atomCount = 0;
    gaussianCount = 0;
    int nGaussians = 0;
    shellCount = 0;
    String thisShell = "0";
    String[] tokens;
    discardLinesUntilContains("SHELL TYPE PRIMITIVE");
    readLine();
    Hashtable slater = null;
    while (readLine() != null && line.indexOf("TOTAL") < 0) {
      tokens = getTokens(line);
      switch (tokens.length) {
      case 1:
        atomCount++;
      case 0:
        break;
      default:
        if (!tokens[0].equals(thisShell)) {
          if (slater != null) {
            slater.put("nGaussians", new Integer(nGaussians));
            sdata.add(slater);
          }
          thisShell = tokens[0];
          shellCount++;
          slater = new Hashtable();
          slater.put("atomIndex", new Integer(atomCount - 1));
          slater.put("basisType", tokens[1]);
          slater.put("gaussianPtr", new Integer(gaussianCount)); // or parseInt(tokens[2]) - 1
          nGaussians = 0;
        }
        ++nGaussians;
        ++gaussianCount;
        gdata.add(tokens);
      }
    }
    if (slater != null) {
      slater.put("nGaussians", new Integer(nGaussians));
      sdata.add(slater);
    }
    float[][] garray = new float[gaussianCount][];
    for (int i = 0; i < gaussianCount; i++) {
      tokens = (String[]) gdata.get(i);
      garray[i] = new float[tokens.length - 3];
      for (int j = 3; j < tokens.length; j++)
        garray[i][j - 3] = parseFloat(tokens[j]);
    }
    moData.put("shells", sdata);
    moData.put("gaussians", garray);
    logger.log(shellCount + " slater shells read");
    logger.log(gaussianCount + " gaussian primitives read");
  }

  /*
   ------------------
   MOLECULAR ORBITALS
   ------------------

   1          2          3          4          5
   -20.4836   -11.2809    -1.4178     -.8698     -.6993
   A          A          A          A          A   
   1  C  1  S    -.000189    .986390   -.119709   -.171964    .000000
   2  C  1  S    -.000594    .093565    .132951    .200942    .000000
   3  C  1  X     .000000    .000000    .000000    .000000    .356539

   */

  @SuppressWarnings("unchecked")
void readMolecularOrbitals() throws Exception {
    Hashtable[] mos = new Hashtable[5];
    Vector[] data = new Vector[5];
    readLine(); // -------
    int nThisLine = 0;
    while (readLine() != null
        && line.indexOf("--") < 0 && line.indexOf(".....") < 0) {
      String[] tokens = getTokens(line);
      //Logger.debug(tokens.length + line);
      if (line.length() == 0) {
        for (int i = 0; i < nThisLine; i++) {
          float[] coefs = new float[data[i].size()];
          for (int j = coefs.length; --j >= 0;)
            coefs[j] = parseFloat((String) data[i].get(j));
          mos[i].put("coefficients", coefs);
          orbitals.add(mos[i]);
        }
        nThisLine = 0;
        continue;
      }
      if (nThisLine == 0) {
        nThisLine = tokens.length;
        tokens = getTokens(readLine());
        for (int i = 0; i < nThisLine; i++) {
          mos[i] = new Hashtable();
          data[i] = new Vector();
          mos[i].put("energy", new Float(tokens[i]));
        }
        tokens = getTokens(readLine());
        for (int i = 0; i < nThisLine; i++)
          mos[i].put("symmetry", tokens[i]);
        continue;
      }
      for (int i = 0; i < nThisLine; i++)
        data[i].add(tokens[i + 4]);
    }
    logger.log(orbitals.size() + " molecular orbitals read in model " + modelNumber);
  }

  void readFrequencies() throws Exception {
    int totalFrequencyCount = 0;
    int atomCountInFirstModel = atomSetCollection.atomCount;
    float[] xComponents = new float[5];
    float[] yComponents = new float[5];
    float[] zComponents = new float[5];

    discardLinesUntilContains("FREQUENCY:");
    while (line != null && line.indexOf("FREQUENCY:") >= 0) {
      int lineBaseFreqCount = totalFrequencyCount;
      ichNextParse = 17;
      int lineFreqCount;
      for (lineFreqCount = 0; lineFreqCount < 5; ++lineFreqCount) {
        float frequency = parseFloat(line, ichNextParse);
        //Logger.debug("frequency=" + frequency);
        if (Float.isNaN(frequency))
          break;
        ++totalFrequencyCount;
        if (totalFrequencyCount > 1)
          atomSetCollection.cloneFirstAtomSet();
      }
      Atom[] atoms = atomSetCollection.atoms;
      discardLinesUntilBlank();
      for (int i = 0; i < atomCountInFirstModel; ++i) {
        readLine();
        readComponents(lineFreqCount, xComponents);
        readLine();
        readComponents(lineFreqCount, yComponents);
        readLine();
        readComponents(lineFreqCount, zComponents);
        for (int j = 0; j < lineFreqCount; ++j) {
          int atomIndex = (lineBaseFreqCount + j) * atomCountInFirstModel + i;
          Atom atom = atoms[atomIndex];
          atom.vectorX = xComponents[j];
          atom.vectorY = yComponents[j];
          atom.vectorZ = zComponents[j];
        }
      }
      discardLines(12);
      readLine();
    }
  }

  void readComponents(int count, float[] components) {
    for (int i = 0, start = 20; i < count; ++i, start += 12)
      components[i] = parseFloat(line, start, start + 12);
  }
}
