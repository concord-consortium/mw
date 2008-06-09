/* $RCSfile: SpartanSmolReader.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:15 $
 * $Revision: 1.10 $
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
import java.util.Hashtable;

import org.myjmol.util.Logger;

/*
 * Spartan SMOL and .spartan compound document reader
 * 
 */

class SpartanSmolReader extends AtomSetCollectionReader {

  final boolean debugReader = false;
  boolean isCompoundDocument;

  String modelName = "Spartan file";
  int atomCount;

  Hashtable moData = new Hashtable();

  AtomSetCollection readAtomSetCollection(BufferedReader reader)
      throws Exception {
    this.reader = reader;
    readLine();
    isCompoundDocument = (line.indexOf("Compound Document") >= 0);
    atomSetCollection = new AtomSetCollection("spartan "
        + (isCompoundDocument ? "compound document file" : "smol"));

    String bondData = "";
    SpartanArchive spartanArchive = null;
    try {
      while (line != null) {
        //if (atomCount == 0)
          //Logger.debug(line);
        if (line.equals("HESSIAN") && bondData != null) {
          //cache for later if necessary -- this is from the INPUT section
          while (readLine() != null
              && line.indexOf("ENDHESS") < 0)
            bondData += line + " ";
          //Logger.debug("bonddata:" + bondData);
        }
        if (line.equals("BEGINARCHIVE")
            || line.equals("BEGIN Compound Document Entry: Archive")) {
          spartanArchive = new SpartanArchive(this, logger, atomSetCollection,
              moData, bondData);
          bondData = null;
          readArchiveHeader();
          atomCount = spartanArchive.readArchive(line, false);
          if (atomCount > 0) {
            atomSetCollection.setAtomSetName(modelName);
          }
        } else if (atomCount > 0 && line.indexOf("BEGINPROPARC") == 0
            || line.equals("BEGIN Compound Document Entry: PropertyArchive")) {
          spartanArchive.readProperties();
          if (!atomSetCollection
              .setAtomSetCollectionPartialCharges("MULCHARGES"))
            atomSetCollection.setAtomSetCollectionPartialCharges("Q1_CHARGES");
        }
        readLine();
      }
    } catch (Exception e) {
      Logger.error("Could not read file at line: " + line, e);
      //TODO: Why this?
      //new NullPointerException();
    }
    // info out of order -- still a chance, at least for first model
    if (atomCount > 0 && spartanArchive != null && bondData != null)
      spartanArchive.addBonds(bondData);
    if (atomSetCollection.atomCount == 0) {
      atomSetCollection.errorMessage = "No atoms in file";
    }
    return atomSetCollection;
  }

  void readArchiveHeader()
      throws Exception {
    String modelInfo = readLine();
    logger.log(modelInfo);
    atomSetCollection.setCollectionName(modelInfo);
    modelName = readLine();
    logger.log(modelName);
    //    5  17  11  18   0   1  17   0 RHF      3-21G(d)           NOOPT FREQ
    readLine();
  }

}
