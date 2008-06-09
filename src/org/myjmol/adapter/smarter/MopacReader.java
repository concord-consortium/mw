/* $RCSfile: MopacReader.java,v $
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
package org.myjmol.adapter.smarter;


import java.io.BufferedReader;
import java.util.Vector;
import java.util.Hashtable;

/**
 * Reads Mopac 93, 97 or 2002 output files, but was tested only
 * for Mopac 93 files yet. (Miguel tweaked it to handle 2002 files,
 * but did not test extensively.)
 *
 * @author Egon Willighagen <egonw@jmol.org>
 */
class MopacReader extends AtomSetCollectionReader {
    
  String frameInfo;
  int baseAtomIndex;
  
  private boolean chargesFound = false;

  AtomSetCollection readAtomSetCollection(BufferedReader reader) throws Exception {
    
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("mopac");
    frameInfo = null;

    while (readLine() != null && ! line.startsWith(" ---")) {
      if (line.indexOf("MOLECULAR POINT GROUP") >= 0) {
          // hasSymmetry = true;
      } else if (line.trim().equals("CARTESIAN COORDINATES")) {
          processCoordinates();
          atomSetCollection.setAtomSetName("Input Structure");
      }
    }

    while (readLine() != null) {
      if (line.indexOf("TOTAL ENERGY") >= 0)
        processTotalEnergy();
      else if (line.indexOf("ATOMIC CHARGES") >= 0)
        processAtomicCharges();
      else if (line.trim().equals("CARTESIAN COORDINATES"))
        processCoordinates();
      else if (line.indexOf("ORIENTATION OF MOLECULE IN FORCE") >= 0) {
        processCoordinates();
        atomSetCollection.setAtomSetName("Orientation in Force Field");
      } else if (line.indexOf("NORMAL COORDINATE ANALYSIS") >= 0)
        readFrequencies();
    }
    return atomSetCollection;
  }
    
  void processTotalEnergy() {
    frameInfo = line.trim();
  }

  /**
   * Reads the section in MOPAC files with atomic charges.
   * These sections look like:
   * <pre>
   *               NET ATOMIC CHARGES AND DIPOLE CONTRIBUTIONS
   * 
   *          ATOM NO.   TYPE          CHARGE        ATOM  ELECTRON DENSITY
   *            1          C          -0.077432        4.0774
   *            2          C          -0.111917        4.1119
   *            3          C           0.092081        3.9079
   * </pre>
   * They are expected to be found in the file <i>before</i> the 
   * cartesian coordinate section.
   * 
   * @throws Exception
   */
void processAtomicCharges() throws Exception {
    discardLines(2);
    atomSetCollection.newAtomSet(); // charges before coords, see JavaDoc
    baseAtomIndex = atomSetCollection.atomCount;
    int expectedAtomNumber = 0;
    while (readLine() != null) {
      int atomNumber = parseInt(line);
      if (atomNumber == Integer.MIN_VALUE) // a blank line
        break;
      ++expectedAtomNumber;
      if (atomNumber != expectedAtomNumber)
        throw new Exception("unexpected atom number in atomic charges");
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = parseToken(line, ichNextParse);
      atom.partialCharge = parseFloat(line, ichNextParse);
    }
    chargesFound = true;
  }
    
  /**
   * Reads the section in MOPAC files with cartesian coordinates.
   * These sections look like:
   * <pre>
   *           CARTESIAN COORDINATES
   * 
   *     NO.       ATOM         X         Y         Z
   * 
   *      1         C        0.0000    0.0000    0.0000
   *      2         C        1.3952    0.0000    0.0000
   *      3         C        2.0927    1.2078    0.0000
   * </pre>
   * In a MOPAC2002 file the columns are different:
   * <pre>
   *          CARTESIAN COORDINATES
   *
   * NO.       ATOM           X             Y             Z
   *
   *  1         H        0.00000000    0.00000000    0.00000000
   *  2         O        0.95094500    0.00000000    0.00000000
   *  3         H        1.23995160    0.90598439    0.00000000
   * </pre>
   * 
   * @throws Exception
   */
  void processCoordinates() throws Exception {
    discardLines(3);
    int expectedAtomNumber = 0;
    if (!chargesFound) {
      atomSetCollection.newAtomSet();
      baseAtomIndex = atomSetCollection.atomCount;
    } else {
      chargesFound = false;
    }

    while (readLine() != null) {
      int atomNumber = parseInt(line);
      if (atomNumber == Integer.MIN_VALUE) // blank line
        break;
      ++expectedAtomNumber;
      if (atomNumber != expectedAtomNumber)
        throw new Exception("unexpected atom number in coordinates");
      String elementSymbol = parseToken(line, ichNextParse);

      Atom atom = atomSetCollection.atoms[baseAtomIndex + atomNumber - 1];
      if (atom == null) {
          atom = atomSetCollection.addNewAtom(); // if no charges were found first
      }
      atom.atomSerial = atomNumber;
      atom.x = parseFloat(line, ichNextParse);
      atom.y = parseFloat(line, ichNextParse);
      atom.z = parseFloat(line, ichNextParse);
      int atno = parseInt(elementSymbol); 
      if (atno != Integer.MIN_VALUE)
        elementSymbol = getElementSymbol(atno);
      atom.elementSymbol = elementSymbol;
    }
  }
  /**
   * Interprets the Harmonic frequencies section.
   * 
   * <pre>
   *     THE LAST 6 VIBRATIONS ARE THE TRANSLATION AND ROTATION MODES
   *    THE FIRST THREE OF THESE BEING TRANSLATIONS IN X, Y, AND Z, RESPECTIVELY
   *              NORMAL COORDINATE ANALYSIS
   *   
   *       ROOT NO.    1           2           3           4           5           6
   *   
   *              370.51248   370.82204   618.03031   647.68700   647.74806   744.32662
   *     
   *            1   0.00002     0.00001    -0.00002    -0.05890     0.07204    -0.00002
   *            2   0.00001    -0.00006    -0.00001     0.01860     0.13517     0.00000
   *            3   0.00421    -0.11112     0.06838    -0.00002    -0.00003    -0.02449
   *   
   *            4   0.00002     0.00001    -0.00002    -0.04779     0.07977    -0.00001
   *            5  -0.00002     0.00002     0.00001     0.13405    -0.02908     0.00004
   *            6  -0.10448     0.05212    -0.06842    -0.00005    -0.00002    -0.02447
   * </pre>
   * 
   * <p>
   * The vectors are added to a clone of the last read AtomSet. Only the
   * Frequencies are set as properties for each of the frequency type AtomSet
   * generated.
   * 
   * @throws Exception
   *             If an I/O error occurs
   */
  @SuppressWarnings("unchecked")
private void readFrequencies() throws Exception {
    Vector freqs = new Vector();
    Vector vibrations = new Vector();
    String[][] data;
    int nAtoms = atomSetCollection.getLastAtomSetAtomCount();
    while (readLine() != null
        && line.indexOf("DESCRIPTION") < 0)
      if (line.indexOf("ROOT") >= 0) {
        int frequencyCount = getTokens(line).length - 2;
        data = new String[nAtoms * 3 + 1][];
        fillDataBlock(data);
        for (int i = 0; i < frequencyCount; ++i) {
          float freq = parseFloat(data[0][i]);
          Hashtable info = new Hashtable();
          info.put("freq", new Float(freq));
          info.put("label", "");
          freqs.add(info);
          baseAtomIndex = atomSetCollection.atomCount;
          atomSetCollection.cloneLastAtomSet();
          Atom[] atoms = atomSetCollection.atoms;
          atomSetCollection.setAtomSetName(freq + " cm^-1");
          atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,
              "Frequencies");
          Vector vib = new Vector();
          for (int iatom = 0, dataPt = 1; iatom < nAtoms; ++iatom) {
            float dx = parseFloat(data[dataPt++][i + 1]);
            float dy = parseFloat(data[dataPt++][i + 1]);
            float dz = parseFloat(data[dataPt++][i + 1]);
            atoms[baseAtomIndex + iatom].addVibrationVector(dx, dy, dz);
            Vector vibatom = new Vector();
            vibatom.add(new Float(dx));
            vibatom.add(new Float(dy));
            vibatom.add(new Float(dz));
            vib.add(vibatom);
          }
          vibrations.add(vib);
        }
      }
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("VibFreqs", freqs);
    atomSetCollection
        .setAtomSetCollectionAuxiliaryInfo("vibration", vibrations);
  }
}
