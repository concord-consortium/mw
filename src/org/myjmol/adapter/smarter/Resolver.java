/* $RCSfile: Resolver.java,v $
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

package org.myjmol.adapter.smarter;


import java.io.BufferedReader;
import java.util.StringTokenizer;

import org.myjmol.api.JmolAdapter;
import org.myjmol.util.Logger;

class Resolver {

  static Object resolve(String name, BufferedReader bufferedReader,
                        JmolAdapter.Logger logger) throws Exception {
    return resolve(name, bufferedReader, logger, null);
  }

  static Object resolve(String name, BufferedReader bufferedReader,
                        JmolAdapter.Logger logger, int[] params) throws Exception {
    AtomSetCollectionReader atomSetCollectionReader;
    String atomSetCollectionReaderName =
      determineAtomSetCollectionReader(bufferedReader, logger);
    logger.log("The Resolver thinks", atomSetCollectionReaderName);
    String className =
      "org.jmol.adapter.smarter." + atomSetCollectionReaderName + "Reader";

    if (atomSetCollectionReaderName == null)
      return "unrecognized file format for file " + name;

    try {
      Class atomSetCollectionReaderClass = Class.forName(className);
      atomSetCollectionReader =
        (AtomSetCollectionReader)atomSetCollectionReaderClass.newInstance();
    } catch (Exception e) {
      String err = "Could not instantiate:" + className;
      logger.log(err);
      return err;
    }
    atomSetCollectionReader.setLogger(logger);
    atomSetCollectionReader.initialize(params);
    AtomSetCollection atomSetCollection =
      atomSetCollectionReader.readAtomSetCollection(bufferedReader);
    bufferedReader.close();
    return finalize(atomSetCollection, "file " + name);
  }

  /* XIE
  static Object DOMResolve(Object DOMNode, JmolAdapter.Logger logger) throws Exception {
    AtomSetCollectionReader atomSetCollectionReader = new XmlReader();

    atomSetCollectionReader.setLogger(logger);
    atomSetCollectionReader.initialize();

    AtomSetCollection atomSetCollection =
      atomSetCollectionReader.readAtomSetCollectionFromDOM(DOMNode);
    return finalize(atomSetCollection, "DOM node");
  }
  */

  static Object finalize(AtomSetCollection atomSetCollection, String filename) {
    atomSetCollection.freeze();
    if (atomSetCollection.errorMessage != null)
      return atomSetCollection.errorMessage + " for " + filename + " of type " + atomSetCollection.fileTypeName;
    if (atomSetCollection.atomCount == 0)
      return "No atoms found for " + filename  + " of type " + atomSetCollection.fileTypeName;
    return atomSetCollection;
  }

  static String determineAtomSetCollectionReader(BufferedReader bufferedReader,
                                                 JmolAdapter.Logger logger)
    throws Exception {
    String[] lines = new String[16];
    LimitedLineReader llr = new LimitedLineReader(bufferedReader, 16384);
    for (int i = 0; i < lines.length; ++i)
      lines[i] = llr.readLineWithNewline();
    if (checkV3000(lines))
      return "V3000";
    if (checkMol(lines))
      return "Mol";
    if (checkXyz(lines))
      return "Xyz";
    if (checkFoldingXyz(lines))
      return "FoldingXyz";
    if (checkCube(lines))
      return "Cube";
    if (checkOdyssey(lines))
      return "Odyssey";

    // run these loops forward ... easier for people to understand
    //file starts with added 4/26 to ensure no issue with NWChem files
    for (int i = 0; i < fileStartsWithRecords.length; ++i) {
      String[] recordTags = fileStartsWithRecords[i];
      for (int j = 0; j < recordTags.length; ++j) {
        String recordTag = recordTags[j];
        if (lines[0].startsWith(recordTag))
            return fileStartsWithFormats[i];
      }
    }
    for (int i = 0; i < lineStartsWithRecords.length; ++i) {
      String[] recordTags = lineStartsWithRecords[i];
      for (int j = 0; j < recordTags.length; ++j) {
        String recordTag = recordTags[j];
        for (int k = 0; k < lines.length; ++k) {
          if (lines[k].startsWith(recordTag))
            return lineStartsWithFormats[i];
        }
      }
    }
    
    String header = llr.getHeader();
    for (int i = 0; i < containsRecords.length; ++i) {
      String[] recordTags = containsRecords[i];
      for (int j = 0; j < recordTags.length; ++j) {
        String recordTag = recordTags[j];
        if (header.indexOf(recordTag) != -1)
          return containsFormats[i];
      }
    }

    if (lines[1] == null || lines[1].trim().length() == 0)
      return "Jme"; // this is really quite broken :-)
    
    for (int i = 0; i < lines.length; ++i)
      lines[i] = llr.readLineWithNewline();

    return null;
  }

  ////////////////////////////////////////////////////////////////
  // file types that need special treatment
  ////////////////////////////////////////////////////////////////

  static boolean checkOdyssey(String[] lines) {
    int i;
    for (i = 0; i < lines.length; i++)
      if (!lines[i].startsWith("C "))
        break;
    return (i + 2 < lines.length 
        && lines[i].charAt(0) == ' ' 
        && lines[i + 2].equals("0 1\n"));
  }
  
  static boolean checkV3000(String[] lines) {
    if (lines[3].length() >= 6) {
      String line4trimmed = lines[3].trim();
      if (line4trimmed.endsWith("V3000"))
        return true;
    }
    return false;
  }

  static boolean checkMol(String[] lines) {
    if (lines[3].length() >= 6) {
      String line4trimmed = lines[3].trim();
      if (line4trimmed.endsWith("V2000") ||
          line4trimmed.endsWith("v2000"))
        return true;
      try {
        Integer.parseInt(lines[3].substring(0, 3).trim());
        Integer.parseInt(lines[3].substring(3, 6).trim());
        return true;
      } catch (NumberFormatException nfe) {
      }
    }
    return false;
  }

  static boolean checkXyz(String[] lines) {
    try {
      Integer.parseInt(lines[0].trim());
      return true;
    } catch (NumberFormatException nfe) {
    }
    return false;
  }

  static boolean checkFoldingXyz(String[] lines) {
    try {
      StringTokenizer tokens = new StringTokenizer(lines[0].trim(), " \t");
      if ((tokens != null) && (tokens.countTokens() >= 2)) {
        Integer.parseInt(tokens.nextToken().trim());
        return true;
      }
    } catch (NumberFormatException nfe) {
    }
    return false;
  }

  static boolean checkCube(String[] lines) {
    try {
      StringTokenizer tokens2 = new StringTokenizer(lines[2]);
      if (tokens2.countTokens() != 4)
        return false;
      Integer.parseInt(tokens2.nextToken());
      for (int i = 3; --i >= 0; )
        new Float(tokens2.nextToken());
      StringTokenizer tokens3 = new StringTokenizer(lines[3]);
      if (tokens3.countTokens() != 4)
        return false;
      Integer.parseInt(tokens3.nextToken());
      for (int i = 3; --i >= 0; )
        if ((new Float(tokens3.nextToken())).floatValue() < 0)
          return false;
      return true;
    } catch (NumberFormatException nfe) {
    }
    return false;
  }

  void dumpLines(String[] lines) {
      for (int i = 0; i < lines.length; i++) {
        Logger.info("\nLine "+i + " len " + lines[i].length());
        for (int j = 0; j < lines[i].length(); j++)
          Logger.info("\t"+(int)lines[i].charAt(j));
      }
      Logger.info("");
  }

  ////////////////////////////////////////////////////////////////
  // these test files that startWith one of these strings
  ////////////////////////////////////////////////////////////////

  final static String[] nwchemRecords =
  {" argument  1 = "};

  final static String[] cubeRecords =
  {"JVXL"};

  final static String[] mol2Records =
  {"@<TRIPOS>"};

  final static String[] webmoRecords =
  {"[HEADER]"};

  final static String[][] fileStartsWithRecords =
  { nwchemRecords, cubeRecords, mol2Records, webmoRecords};

  final static String[] fileStartsWithFormats =
  { "NWChem", "Cube", "Mol2", "WebMO"};

  ////////////////////////////////////////////////////////////////
  // these test lines that startWith one of these strings
  ////////////////////////////////////////////////////////////////

  final static String[] pdbRecords = {
    "HEADER", "OBSLTE", "TITLE ", "CAVEAT", "COMPND", "SOURCE", "KEYWDS",
    "EXPDTA", "AUTHOR", "REVDAT", "SPRSDE", "JRNL  ", "REMARK",
    "DBREF ", "SEQADV", "SEQRES", "MODRES", 
    "HELIX ", "SHEET ", "TURN  ",
    "CRYST1", "ORIGX1", "ORIGX2", "ORIGX3", "SCALE1", "SCALE2", "SCALE3",
    "ATOM  ", "HETATM", "MODEL ",
  };

  final static String[] shelxRecords =
  { "TITL ", "ZERR ", "LATT ", "SYMM ", "CELL " };

  final static String[] cifRecords =
  { "data_", "_publ" };

  final static String[] ghemicalMMRecords =
  { "!Header mm1gp", "!Header gpr" };

  final static String[] jaguarRecords =
  { "  |  Jaguar version", };

  final static String[] hinRecords = 
  {"mol "};

  final static String[] mdlRecords = 
  {"$MDL "};

  final static String[] spartanSmolRecords =
  {"INPUT="};

  final static String[] csfRecords =
  {"local_transform"};
  
  final static String[][] lineStartsWithRecords =
  { pdbRecords, shelxRecords, cifRecords, ghemicalMMRecords,
    jaguarRecords, hinRecords , mdlRecords, 
    spartanSmolRecords, csfRecords};

  final static String[] lineStartsWithFormats =
  { "Pdb", "Shelx", "Cif", "GhemicalMM",
    "Jaguar", "Hin", "Mol", "SpartanSmol", "Csf"};

  ////////////////////////////////////////////////////////////////
  // contains formats
  ////////////////////////////////////////////////////////////////
  
  final static String[] xmlRecords =
  { "<?xml", "<atom", "<molecule", "<reaction", "<cml", "<bond", ".dtd\"",
    "<list>", "<entry", "<identifier", "http://www.xml-cml.org/schema/cml2/core" };

  final static String[] gaussianRecords =
  { "Entering Gaussian System", "Entering Link 1", "1998 Gaussian, Inc." };

  final static String[] mopacRecords =
  { "MOPAC 93 (c) Fujitsu", "MOPAC2002 (c) Fujitsu",
    "MOPAC FOR LINUX (PUBLIC DOMAIN VERSION)"};

  final static String[] qchemRecords = 
  { "Welcome to Q-Chem", "A Quantum Leap Into The Future Of Chemistry" };

  final static String[] gamessRecords =
  { "GAMESS" };

  final static String[] spartanRecords =
  { "Spartan" };

  final static String[] spartanBinaryRecords =
  { "|PropertyArchive" };

  final static String[] adfRecords =
  { "Amsterdam Density Functional" };
  
  final static String[][] containsRecords =
  { xmlRecords, gaussianRecords, mopacRecords, qchemRecords, gamessRecords,
    spartanBinaryRecords, spartanRecords, mol2Records, adfRecords, 
  };

  final static String[] containsFormats =
  { "Xml", "Gaussian", "Mopac", "Qchem", "Gamess", "SpartanSmol", "Spartan" , "Mol2", "Adf"};
}

class LimitedLineReader {
  int readLimit;
  char[] buf;
  int cchBuf;
  int ichCurrent;

  LimitedLineReader(BufferedReader bufferedReader, int readLimit)
    throws Exception {
    this.readLimit = readLimit;
    bufferedReader.mark(readLimit);
    buf = new char[readLimit];
    cchBuf = bufferedReader.read(buf);
    ichCurrent = 0;
    bufferedReader.reset();
  }

  String getHeader() {
    return new String(buf);  
  }
  
  String readLineWithNewline() {
    // mth 2004 10 17
    // for now, I am going to put in a hack here
    // we have some CIF files with many lines of '#' comments
    // I believe that for all formats we can flush if the first
    // char of the line is a #
    // if this becomes a problem then we will need to adjust
    while (ichCurrent < cchBuf) {
      int ichBeginningOfLine = ichCurrent;
      char ch = 0;
      while (ichCurrent < cchBuf &&
             (ch = buf[ichCurrent++]) != '\r' && ch != '\n') {
      }
      if (ch == '\r' && ichCurrent < cchBuf && buf[ichCurrent] == '\n')
        ++ichCurrent;
      int cchLine = ichCurrent - ichBeginningOfLine;
      if (buf[ichBeginningOfLine] == '#') // flush comment lines;
        continue;
      StringBuffer sb = new StringBuffer(cchLine);
      sb.append(buf, ichBeginningOfLine, cchLine);
      return "" + sb;
    }
    //Logger.debug("org.jmol.adapter.smarter.Resolver short input buffer");
    // miguel 2005 01 26
    // for now, just return the empty string.
    // it will only affect the Resolver code
    // it will be easier to handle because then everyone does not
    // need to check for the null pointer
    //
    // If it becomes a problem, then change this to null and modify
    // all the code above to make sure that it tests for null before
    // attempting to invoke methods on the strings. 
    return "";
  }
}
