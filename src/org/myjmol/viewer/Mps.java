/* $RCSfile: Mps.java,v $
 * $Author: qxie $
 * $Date: 2007-01-18 14:08:53 $
 * $Revision: 1.13 $

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

import java.util.BitSet;
import java.util.Hashtable;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.myjmol.g3d.Graphics3D;
import org.myjmol.util.Logger;
/****************************************************************
 * Mps stands for Model-Chain-Polymer-Shape
 ****************************************************************/
abstract class Mps extends Shape {

  Mmset mmset;
  Atom[] atoms;
  
  Mpsmodel[] mpsmodels;

  final void initShape() {
    mmset = frame.mmset;
    atoms = frame.atoms;
  }

  void setSize(int size, BitSet bsSelected) {
    short mad = (short) size;
    initialize();
    for (int m = mpsmodels.length; --m >= 0; )
      mpsmodels[m].setMad(mad, bsSelected);
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    initialize();
    if ("color" == propertyName) {
      byte pid = JmolConstants.pidOf(value);
      short colix = Graphics3D.getColix(value);
      for (int m = mpsmodels.length; --m >= 0; )
        mpsmodels[m].setColix(colix, pid, bs);
      return;
    }
    if ("translucency" == propertyName) {
      boolean isTranslucent = ("translucent" == value);
      for (int m = mpsmodels.length; --m >= 0; )
        mpsmodels[m].setTranslucent(isTranslucent, bs);
    }
  }

  String getShapeState() {
    Hashtable temp = new Hashtable();
    Hashtable temp2 = new Hashtable();
    for (int m = mpsmodels.length; --m >= 0; )
      mpsmodels[m].setShapeState(temp, temp2);
    return getShapeCommands(temp, temp2, frame.atomCount);
  }

  abstract MpsShape allocateMpspolymer(Polymer polymer);

  void initialize() {
    if (mpsmodels == null) {
      int modelCount = mmset == null ? 0 : mmset.getModelCount();
      Model[] models = mmset.getModels();
      mpsmodels = new Mpsmodel[modelCount];
      for (int i = modelCount; --i >= 0; )
        mpsmodels[i] = new Mpsmodel(this, models[i]);
    }
  }

  int getMpsmodelCount() {
	  if(mpsmodels==null) return 0; // XIE
    return mpsmodels.length;
  }

  Mpsmodel getMpsmodel(int i) {
    return mpsmodels[i];
  }

  void findNearestAtomIndex(int xMouse, int yMouse, Closest closest) {
    for (int i = mpsmodels.length; --i >= 0; )
      mpsmodels[i].findNearestAtomIndex(xMouse, yMouse, closest);
  }

  void setModelClickability() {
	  if(mpsmodels == null) return;
    for (int i = mpsmodels.length; --i >= 0; ){
    	if(mpsmodels[i] != null) // XIE
           mpsmodels[i].setModelClickability();
    }
  }

  class Mpsmodel {
    MpsShape[] mpspolymers;
    int modelIndex;
    int modelVisibilityFlags = 0;
    
    Mpsmodel(Mps shape, Model model) {
      mpspolymers = new MpsShape[model.getPolymerCount()];
      this.modelIndex = model.modelIndex;
      for (int i = mpspolymers.length; --i >= 0;) {
        (mpspolymers[i] = allocateMpspolymer(model.getPolymer(i)))
            .setShape(shape);
      }
    }
    
    void setMad(short mad, BitSet bsSelected) {
      for (int i = mpspolymers.length; --i >= 0; ) {
        MpsShape polymer = mpspolymers[i];
        if (polymer.monomerCount > 0)
          polymer.setMad(mad, bsSelected);
      }
    }

    void setColix(short colix, byte pid, BitSet bsSelected) {
      for (int i = mpspolymers.length; --i >= 0; ) {
        MpsShape polymer = mpspolymers[i];
        if (polymer.monomerCount > 0)
          polymer.setColix(colix, pid, bsSelected);
      }
    }

    void setTranslucent(boolean isTranslucent, BitSet bsSelected) {
      for (int i = mpspolymers.length; --i >= 0; ) {
        MpsShape polymer = mpspolymers[i];
        if (polymer.monomerCount > 0)
          polymer.setTranslucent(isTranslucent, bsSelected);
      }
    }

    void setShapeState(Hashtable temp, Hashtable temp2) {
      for (int i = mpspolymers.length; --i >= 0; ) {
        MpsShape polymer = mpspolymers[i];
        if (polymer.monomerCount > 0)
          polymer.setShapeState(temp, temp2);
      }
    }

    int getMpspolymerCount() {
      return mpspolymers.length;
    }

    MpsShape getMpspolymer(int i) {
      return mpspolymers[i];
    }

    void findNearestAtomIndex(int xMouse, int yMouse, Closest closest) {
      for (int i = mpspolymers.length; --i >= 0; )
        mpspolymers[i].findNearestAtomIndex(xMouse, yMouse, closest);
    }

    void setModelClickability() {
      int displayModelIndex = viewer.getDisplayModelIndex();
      modelVisibilityFlags = 
        (displayModelIndex >= 0 && displayModelIndex != modelIndex
            ? 0 : myVisibilityFlag);
      for (int i = mpspolymers.length; --i >= 0; )
        mpspolymers[i].setModelClickability();
    }
    
  }

  
  abstract class MpsShape {
       
    Mps shape;
    
    Polymer polymer;
    
    Mesh[] meshes;
    boolean[] meshReady;

    short madOn;
    short madHelixSheet;
    short madTurnRandom;
    short madDnaRna;
    
    short[] mads;
    short[] colixes;
    byte[] paletteIDs;

    BitSet bsColixSet;
    BitSet bsSizeSet;
    boolean isActive;
    
    int monomerCount;
    Monomer[] monomers;

    Point3f[] leadMidpoints;
    Point3f[] leadPoints;
    Vector3f[] wingVectors;
    int[] leadAtomIndices;

    MpsShape(Polymer polymer, int madOn,
              int madHelixSheet, int madTurnRandom, int madDnaRna) {
      this.polymer = polymer;
      this.madOn = (short)madOn;
      this.madHelixSheet = (short)madHelixSheet;
      this.madTurnRandom = (short)madTurnRandom;
      this.madDnaRna = (short)madDnaRna;
      monomerCount = polymer.monomerCount;
      if (monomerCount > 0) {
        colixes = new short[monomerCount];
        paletteIDs = new byte[monomerCount];
        mads = new short[monomerCount + 1];
        monomers = polymer.monomers;
        meshReady = new boolean[monomerCount];
        meshes = new Mesh[monomerCount];
        leadPoints = polymer.getLeadPoints();
        leadMidpoints = polymer.getLeadMidpoints();
        wingVectors = polymer.getWingVectors();
        leadAtomIndices = polymer.getLeadAtomIndices();
        //Logger.debug("mps assigning wingVectors and leadMidpoints");
      }
    }

    void setShape(Mps shape) {
      this.shape = shape;
    }
    
    boolean hasBfactorRange = false;
    int bfactorMin, bfactorMax;
    int range;
    float floatRange;

    void calcBfactorRange() {
      bfactorMin = bfactorMax =
        monomers[0].getLeadAtom().getBfactor100();
      for (int i = monomerCount; --i > 0; ) {
        int bfactor =
          monomers[i].getLeadAtom().getBfactor100();
        if (bfactor < bfactorMin)
          bfactorMin = bfactor;
        else if (bfactor > bfactorMax)
          bfactorMax = bfactor;
      }
      range = bfactorMax - bfactorMin;
      floatRange = range;
      hasBfactorRange = true;
    }

    private final static double eightPiSquared100 = 8 * Math.PI * Math.PI * 100;
    /**
     * Calculates the mean positional displacement in milliAngstroms.
     * <p>
     * <a href='http://www.rcsb.org/pdb/lists/pdb-l/200303/000609.html'>
     * http://www.rcsb.org/pdb/lists/pdb-l/200303/000609.html
     * </a>
     * <code>
     * > -----Original Message-----
     * > From: pdb-l-admin@sdsc.edu [mailto:pdb-l-admin@sdsc.edu] On 
     * > Behalf Of Philipp Heuser
     * > Sent: Thursday, March 27, 2003 6:05 AM
     * > To: pdb-l@sdsc.edu
     * > Subject: pdb-l: temperature factor; occupancy
     * > 
     * > 
     * > Hi all!
     * > 
     * > Does anyone know where to find proper definitions for the 
     * > temperature factors 
     * > and the values for occupancy?
     * > 
     * > Alright I do know, that the atoms with high temperature 
     * > factors are more 
     * > disordered than others, but what does a temperature factor of 
     * > a specific 
     * > value mean exactly.
     * > 
     * > 
     * > Thanks in advance!
     * > 
     * > Philipp
     * > 
     * pdb-l: temperature factor; occupancy
     * Bernhard Rupp br@llnl.gov
     * Thu, 27 Mar 2003 08:01:29 -0800
     * 
     * * Previous message: pdb-l: temperature factor; occupancy
     * * Next message: pdb-l: Structural alignment?
     * * Messages sorted by: [ date ] [ thread ] [ subject ] [ author ]
     * 
     * Isotropic B is defined as 8*pi**2<u**2>.
     * 
     * Meaning: eight pi squared =79
     * 
     * so B=79*mean square displacement (from rest position) of the atom.
     * 
     * as u is in Angstrom, B must be in Angstrom squared.
     * 
     * example: B=79A**2
     * 
     * thus, u=sqrt([79/79]) = 1 A mean positional displacement for atom.
     * 
     * 
     * See also 
     * 
     * http://www-structure.llnl.gov/Xray/comp/comp_scat_fac.htm#Atomic
     * 
     * for more examples.
     * 
     * BR
     *</code>
     *
     * @param bFactor100
     * @return ?
     */
    short calcMeanPositionalDisplacement(int bFactor100) {
      return (short)(Math.sqrt(bFactor100/eightPiSquared100) * 1000);
    }

    void findNearestAtomIndex(int xMouse, int yMouse, Closest closest) {
      polymer.findNearestAtomIndex(xMouse, yMouse, closest, mads, myVisibilityFlag);
    }

    void setMad(short mad, BitSet bsSelected) {
      isActive = true;
      if (bsSizeSet == null)
        bsSizeSet = new BitSet();
      for (int i = monomerCount; --i >= 0; ) {
        int leadAtomIndex = leadAtomIndices[i];
        if (bsSelected.get(leadAtomIndex)) { 
          mads[i] = mad >= 0 ? mad : getMadSpecial(mad, i);
          boolean isVisible = (mads[i] > 0);
          bsSizeSet.set(i, isVisible);
          monomers[i].setShapeVisibility(myVisibilityFlag, isVisible);
          atoms[leadAtomIndex].setShapeVisibility(myVisibilityFlag,isVisible);
          falsifyMesh(i, true);
        }
      }
      if (monomerCount > 1)
        mads[monomerCount] = mads[monomerCount - 1];
    }

    short getMadSpecial(short mad, int groupIndex) {
      //undocumented
      switch (mad) {
      case -1: // trace on
        if (madOn >= 0)
          return madOn;
        if (madOn != -2) {
          Logger.error("not supported?");
          return 0;
        }
        // fall into;
      case -2: // trace structure
        switch (monomers[groupIndex].getProteinStructureType()) {
        case JmolConstants.PROTEIN_STRUCTURE_SHEET:
        case JmolConstants.PROTEIN_STRUCTURE_HELIX:
          return madHelixSheet;
        case JmolConstants.PROTEIN_STRUCTURE_DNA:
        case JmolConstants.PROTEIN_STRUCTURE_RNA:
          return madDnaRna;
        default:
          return madTurnRandom;
        }
      case -3: // trace temperature
        {
          if (! hasBfactorRange)
            calcBfactorRange();
          Atom atom = monomers[groupIndex].getLeadAtom();
          int bfactor100 = atom.getBfactor100(); // scaled by 1000
          int scaled = bfactor100 - bfactorMin;
          if (range == 0)
            return (short)0;
          float percentile = scaled / floatRange;
          if (percentile < 0 || percentile > 1)
            Logger.error("Que ha ocurrido? " + percentile);
          return (short)((1750 * percentile) + 250);
        }
      case -4: // trace displacement
        {
          Atom atom = monomers[groupIndex].getLeadAtom();
          return // double it ... we are returning a diameter
            (short)(2 * calcMeanPositionalDisplacement(atom.getBfactor100()));
        }
      }
      Logger.error("unrecognized Mps.getSpecial(" + mad + ")");
      return 0;
    }

    void falsifyMesh(int index, boolean andNearby) {
      if (meshReady == null)
        return;
      meshReady[index] = false;
      if (!andNearby)
        return;
      if (index > 0)
        meshReady[index - 1] = false;
      if (index < monomerCount - 1)
        meshReady[index + 1] = false;
    }    

    void setColix(short colix, byte pid, BitSet bsSelected) {
      isActive = true;
      if (bsColixSet == null)
        bsColixSet = new BitSet();
      for (int i = monomerCount; --i >= 0;) {
        int atomIndex = leadAtomIndices[i];
        if (bsSelected.get(atomIndex)) {
          colixes[i] = shape.setColix(colix, pid, atomIndex);
          paletteIDs[i] = pid;
          bsColixSet.set(i, colixes[i] != Graphics3D.INHERIT);
        }
      }
    }
    
    void setTranslucent(boolean isTranslucent, BitSet bsSelected) {
      isActive = true;
      if (bsColixSet == null)
        bsColixSet = new BitSet();
      for (int i = monomerCount; --i >= 0; )
        if (bsSelected.get(leadAtomIndices[i])) {
          colixes[i] = Graphics3D.getColixTranslucent(colixes[i], isTranslucent);
          bsColixSet.set(i, colixes[i] != Graphics3D.INHERIT);
      }
    }

    void setShapeState(Hashtable temp, Hashtable temp2) {
      if (!isActive)
        return;
      String type = JmolConstants.shapeClassBases[shapeID];
      for (int i = 0; i < monomerCount; i++) {
        int atomIndex1 = monomers[i].firstAtomIndex;
        int atomIndex2 = monomers[i].lastAtomIndex;
        if (!bsSizeSet.get(i)) //shapes MUST have been set with a size
          continue;
        setStateInfo(temp, atomIndex1, atomIndex2, type + " "
            + (mads[i] / 2000f));
        if (bsColixSet != null && bsColixSet.get(i))
          setStateInfo(temp2, atomIndex1, atomIndex2, getColorCommand(type, 
              paletteIDs[i], colixes[i]));
      }
    }  

    void setModelClickability() {
      if (!isActive || wingVectors == null)
        return;
      boolean isNucleicPolymer = polymer instanceof NucleicPolymer;
      if (!isNucleicPolymer)
        return;
      for (int i = monomerCount; --i >= 0;) {
        if (mads[i] <= 0)
          continue;
        NucleicMonomer group = (NucleicMonomer) monomers[i];
        if (frame.bsHidden.get(leadAtomIndices[i]))
          continue;
        group.setModelClickability();
      }
    }
  }  
}
