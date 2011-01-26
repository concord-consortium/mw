/* $RCSfile: AminoPolymer.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:07 $
 * $Revision: 1.12 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

import org.myjmol.util.Logger;

import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

class AminoPolymer extends AlphaPolymer {

  // the primary offset within the same mainchain;
  short[] mainchainHbondOffsets;
  short[] min1Indexes;
  short[] min1Energies;
  short[] min2Indexes;
  short[] min2Energies;

  AminoPolymer(Monomer[] monomers) {
    super(monomers);
  }

  boolean hasWingPoints() { return true; }

  boolean hbondsAlreadyCalculated;

  boolean debugHbonds;

  void calcHydrogenBonds(BitSet bsA, BitSet bsB) {
    debugHbonds = Logger.isActiveLevel(Logger.LEVEL_DEBUG);
    initializeHbondDataStructures();
    //Frame frame = model.mmset.frame;
    //hbondMax2 = frame.hbondMax * frame.hbondMax;
    calcProteinMainchainHydrogenBonds(bsA, bsB);
    
    if (debugHbonds) {
      Logger.debug("calcHydrogenBonds");
      for (int i = 0; i < monomerCount; ++i) {
        Logger.debug("  min1Indexes=" + min1Indexes[i] +
                           "\nmin1Energies=" + min1Energies[i] +
                           "\nmin2Indexes=" + min2Indexes[i] +
                           "\nmin2Energies=" + min2Energies[i]);
      }
    }
  }

  void initializeHbondDataStructures() {
    if (mainchainHbondOffsets == null) {
      mainchainHbondOffsets = new short[monomerCount];
      min1Indexes = new short[monomerCount];
      min1Energies = new short[monomerCount];
      min2Indexes = new short[monomerCount];
      min2Energies = new short[monomerCount];
    } else {
      for (int i = monomerCount; --i >= 0; ) {
        mainchainHbondOffsets[i] = min1Energies[i] = min2Energies[i] = 0;
      }
    }
    for (int i = monomerCount; --i >= 0; )
      min1Indexes[i] = min2Indexes[i] = -1;
  }

  void freeHbondDataStructures() {
    mainchainHbondOffsets =
      min1Indexes = min1Energies = min2Indexes = min2Energies = null;
  }

  final Vector3f vectorPreviousOC = new Vector3f();
  final Point3f aminoHydrogenPoint = new Point3f();

  void calcProteinMainchainHydrogenBonds(BitSet bsA, BitSet bsB) {
    Point3f carbonPoint;
    Point3f oxygenPoint;
    
    for (int i = 0; i < monomerCount; ++i) {
      AminoMonomer residue = (AminoMonomer)monomers[i];
      mainchainHbondOffsets[i] = 0;
      /****************************************************************
       * This does not acount for the first nitrogen in the chain
       * is there some way to predict where it's hydrogen is?
       * mth 20031219
       ****************************************************************/
      if (i > 0 && residue.getGroupID() != JmolConstants.GROUPID_PROLINE) {
        Point3f nitrogenPoint = residue.getNitrogenAtomPoint();
        aminoHydrogenPoint.add(nitrogenPoint, vectorPreviousOC);
        bondAminoHydrogen(i, aminoHydrogenPoint, bsA, bsB);
      }
      carbonPoint = residue.getCarbonylCarbonAtomPoint();
      oxygenPoint = residue.getCarbonylOxygenAtomPoint();
      vectorPreviousOC.sub(carbonPoint, oxygenPoint);
      vectorPreviousOC.scale(1/vectorPreviousOC.length());
    }
  }


  private final static float maxHbondAlphaDistance = 9;
  private final static float maxHbondAlphaDistance2 =
    maxHbondAlphaDistance * maxHbondAlphaDistance;
  private final static float minimumHbondDistance2 = 0.5f; // note: RasMol is 1/2 this. RMH
  private final static double QConst = -332 * 0.42 * 0.2 * 1000;  

  void bondAminoHydrogen(int indexDonor, Point3f hydrogenPoint,
                         BitSet bsA, BitSet bsB) {
    AminoMonomer source = (AminoMonomer)monomers[indexDonor];
    Point3f sourceAlphaPoint = source.getLeadAtomPoint();
    Point3f sourceNitrogenPoint = source.getNitrogenAtomPoint();
    int energyMin1 = 0;
    int energyMin2 = 0;
    int indexMin1 = -1;
    int indexMin2 = -1;
    for (int i = monomerCount; --i >= 0; ) {
      if ((i == indexDonor || (i+1) == indexDonor) || (i-1) == indexDonor)
        continue;
      AminoMonomer target = (AminoMonomer)monomers[i];
      Point3f targetAlphaPoint = target.getLeadAtomPoint();
      float dist2 = sourceAlphaPoint.distanceSquared(targetAlphaPoint);
      if (dist2 > maxHbondAlphaDistance2)
        continue;
      int energy = calcHbondEnergy(source.getNitrogenAtom(), sourceNitrogenPoint, hydrogenPoint, target);
      if (energy < energyMin1) {
        energyMin2 = energyMin1;
        indexMin2 = indexMin1;
        energyMin1 = energy;
        indexMin1 = i;
      } else if (energy < energyMin2) {
        energyMin2 = energy;
        indexMin2 = i;
      }
    }
    if (indexMin1 >= 0) {
      mainchainHbondOffsets[indexDonor] = (short)(indexDonor - indexMin1);
      min1Indexes[indexDonor] = (short)indexMin1;
      min1Energies[indexDonor] = (short)energyMin1;
      createResidueHydrogenBond(indexDonor, indexMin1, bsA, bsB);
      if (indexMin2 >= 0) {
        createResidueHydrogenBond(indexDonor, indexMin2, bsA, bsB);
        min2Indexes[indexDonor] = (short)indexMin2;
        min2Energies[indexDonor] = (short)energyMin2;
      }
    }
  }

  int hPtr = 0;
  int calcHbondEnergy(Atom nitrogen, Point3f nitrogenPoint,
                      Point3f hydrogenPoint, AminoMonomer target) {
    Point3f targetOxygenPoint = target.getCarbonylOxygenAtomPoint();

    /*
     * the following were changed from "return -9900" to "return 0"
     * Bob Hanson 8/30/06
     */
    float distON2 = targetOxygenPoint.distanceSquared(nitrogenPoint);
    if (distON2 < minimumHbondDistance2)
      return 0;

    float distOH2 = targetOxygenPoint.distanceSquared(hydrogenPoint);
    if (distOH2 < minimumHbondDistance2)
      return 0;

    Point3f targetCarbonPoint = target.getCarbonylCarbonAtomPoint();
    float distCH2 = targetCarbonPoint.distanceSquared(hydrogenPoint);
    if (distCH2 < minimumHbondDistance2)
      return 0;

    float distCN2 = targetCarbonPoint.distanceSquared(nitrogenPoint);
    if (distCN2 < minimumHbondDistance2)
      return 0;
    
    /*
     * I'm adding these two because they just makes sense -- Bob Hanson
     */
    
    double distOH = Math.sqrt(distOH2);
    double distCH = Math.sqrt(distCH2);
    double distCN = Math.sqrt(distCN2);
    double distON = Math.sqrt(distON2);

    int energy = (int) ((QConst / distOH - QConst / distCH + QConst / distCN - QConst
        / distON));

    boolean isHbond = (distCN2 > distCH2 && distOH <= 3.0f && energy <= -500);
    if (debugHbonds)
      Logger.debug("draw calcHydrogen"+(++hPtr)+ " ("+nitrogen.getInfo()+") {" + hydrogenPoint.x + " "
          + hydrogenPoint.y + " " + hydrogenPoint.z + "} #" + isHbond + " "
          + nitrogen.getInfo() + " " + target.getLeadAtom().getInfo()
          + " distOH=" + distOH + " distCH=" + distCH + " distCN=" + distCN
          + " distON=" + distON + " energy=" + energy);

    return (!isHbond ? 0 : energy < -9900 ? -9900 : energy);
  }

  void createResidueHydrogenBond(int indexAminoGroup, int indexCarbonylGroup,
                                 BitSet bsA, BitSet bsB) {
    short order;
    int aminoBackboneHbondOffset = indexAminoGroup - indexCarbonylGroup;
    if (debugHbonds) 
      Logger.debug("aminoBackboneHbondOffset=" +
                         aminoBackboneHbondOffset +
                         " amino:" +
                         monomers[indexAminoGroup].getSeqcodeString() +
                         " carbonyl:" +
                         monomers[indexCarbonylGroup].getSeqcodeString());
    switch (aminoBackboneHbondOffset) {
    case 2:
      order = JmolConstants.BOND_H_PLUS_2;
      break;
    case 3:
      order = JmolConstants.BOND_H_PLUS_3;
      break;
    case 4:
      order = JmolConstants.BOND_H_PLUS_4;
      break;
    case 5:
      order = JmolConstants.BOND_H_PLUS_5;
      break;
    case -3:
      order = JmolConstants.BOND_H_MINUS_3;
      break;
    case -4:
      order = JmolConstants.BOND_H_MINUS_4;
      break;
    default:
      order = JmolConstants.BOND_H_REGULAR;
    }
    AminoMonomer donor = (AminoMonomer)monomers[indexAminoGroup];
    Atom nitrogen = donor.getNitrogenAtom();
    AminoMonomer recipient = (AminoMonomer)monomers[indexCarbonylGroup];
    Atom oxygen = recipient.getCarbonylOxygenAtom();
    model.mmset.frame.addHydrogenBond(nitrogen, oxygen, order, bsA, bsB);
  }

  /*
   * If someone wants to work on this code for secondary structure
   * recognition that would be great
   *
   * miguel 2004 06 16
   */

  /*
   * New code for assigning secondary structure based on 
   * phi-psi angles instead of hydrogen bond patterns.
   *
   * old code is commented below the new.
   *
   * molvisions 2005 10 12
   *
   */

  void calculateStructures() {
    //deprecated: calcHydrogenBonds();
    char[] structureTags = new char[monomerCount];

    for (int i = 0; i < monomerCount - 1; ++i) {
      AminoMonomer leadingResidue = (AminoMonomer) monomers[i];
      AminoMonomer trailingResidue = (AminoMonomer) monomers[i + 1];
      calcPhiPsiAngles(leadingResidue, trailingResidue);
      if (isHelix(leadingResidue.psi, trailingResidue.phi)) {
        //this next is just Bob's attempt to separate different helices
        //it is CONSERVATIVE -- it displays fewer helices than before
        //thus allowing more turns and (presumably) better rockets.

        structureTags[i] = (trailingResidue.phi < 0 && leadingResidue.psi < 25 ? '4' : '3');
      } else if (isSheet(leadingResidue.psi, trailingResidue.phi)) {
        structureTags[i] = 's';
      } else if (isTurn(leadingResidue.psi, trailingResidue.phi)) {
        structureTags[i] = 't';
      } else {
        structureTags[i] = 'n';
      }

      if (Logger.isActiveLevel(Logger.LEVEL_DEBUG))
        Logger.debug(this.monomers[0].chain.chainID + " aminopolymer:" + i
            + " " + trailingResidue.phi + "," + leadingResidue.psi + " " + structureTags[i]);
    }

    // build alpha helix stretches
    for (int start = 0; start < monomerCount; ++start) {
      if (structureTags[start] == '4') {
        int end;
        for (end = start + 1; end < monomerCount && structureTags[end] == '4'; ++end) {
        }
        end--;
        if (end >= start + 3) {
          addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_HELIX, start,
              end);
        }
        start = end;
      }
    }

    for (int start = 0; start < monomerCount; ++start) {
      if (structureTags[start] == '3') {
        int end;
        for (end = start + 1; end < monomerCount && structureTags[end] == '3'; ++end) {
        }
        end--;
        if (end >= start + 3) {
          addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_HELIX, start,
              end);
        }
        start = end;
      }
    }

    // build beta sheet stretches
    for (int start = 0; start < monomerCount; ++start) {
      if (structureTags[start] == 's') {
        int end;
        for (end = start + 1; end < monomerCount && structureTags[end] == 's'; ++end) {
        }
        end--;
        if (end >= start + 2) {
          addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_SHEET, start,
              end);
        }
        start = end;
      }
    }

    // build turns
    for (int start = 0; start < monomerCount; ++start) {
      if (structureTags[start] == 't') {
        int end;
        for (end = start + 1; end < monomerCount && structureTags[end] == 't'; ++end) {
        }
        end--;
        if (end >= start + 2) {
          addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_TURN, start,
              end);
        }
        start = end;
      }
    }

  }
  
  
  void calcPhiPsiAngles(AminoMonomer leadingResidue,
                        AminoMonomer trailingResidue) {
    Point3f nitrogen1 = leadingResidue.getNitrogenAtomPoint();
    Point3f alphacarbon1 = leadingResidue.getLeadAtomPoint();
    Point3f carbon1 = leadingResidue.getCarbonylCarbonAtomPoint();
    Point3f nitrogen2 = trailingResidue.getNitrogenAtomPoint();
    Point3f alphacarbon2 = trailingResidue.getLeadAtomPoint();
    Point3f carbon2 = trailingResidue.getCarbonylCarbonAtomPoint();

    trailingResidue.phi = Measurement.computeTorsion(carbon1, nitrogen2,
                                            alphacarbon2, carbon2);
    leadingResidue.psi = Measurement.computeTorsion(nitrogen1, alphacarbon1,
                                            carbon1, nitrogen2);
  }
  
  
  /**
   * 
   * @param psi N-C-CA-N torsion for NEXT group
   * @param phi C-CA-N-C torsion for THIS group
   * @return whether this corresponds to a helix
   */
  static boolean isHelix(float psi, float phi) {
    return (phi >= -160) && (phi <= 0) && (psi >= -100) && (psi <= 45);
  }

  static boolean isSheet(float psi, float phi) {
    return
      ( (phi >= -180) && (phi <= -10) && (psi >= 70) && (psi <= 180) ) || 
      ( (phi >= -180) && (phi <= -45) && (psi >= -180) && (psi <= -130) ) ||
      ( (phi >= 140) && (phi <= 180) && (psi >= 90) && (psi <= 180) );
  }

  static boolean isTurn(float psi, float phi) {
    return (phi >= 30) && (phi <= 90) && (psi >= -15) && (psi <= 95);
  }


  /* 
   * old code for assigning SS
   *

  void calculateStructures() {
    calcHydrogenBonds();
    char[] structureTags = new char[monomerCount];

    findHelixes(structureTags);
    for (int iStart = 0; iStart < monomerCount; ++iStart) {
      if (structureTags[iStart] != 'n') {
        int iMax;
        for (iMax = iStart + 1;
             iMax < monomerCount && structureTags[iMax] != 'n';
             ++iMax)
          {}
        int iLast = iMax - 1;
        addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_HELIX,
                              iStart, iLast);
        iStart = iLast;
      }
    }

    // reset structureTags
    // for some reason if these are not reset, all helices are classified
    // as sheets. - tim 2205 10 12
        for (int i = monomerCount; --i >= 0; )
          structureTags[i] = 'n';

    findSheets(structureTags);
    
    if (debugHbonds)
      for (int i = 0; i < monomerCount; ++i)
        Logger.debug("" + i + ":" + structureTags[i] +
                           " " + min1Indexes[i] + " " + min2Indexes[i]);
    for (int iStart = 0; iStart < monomerCount; ++iStart) {
      if (structureTags[iStart] != 'n') {
        int iMax;
        for (iMax = iStart + 1;
             iMax < monomerCount && structureTags[iMax] != 'n';
             ++iMax)
          {}
        int iLast = iMax - 1;
        addSecondaryStructure(JmolConstants.PROTEIN_STRUCTURE_SHEET,
                              iStart, iLast);
        iStart = iLast;
      }
    }
  }

  void findHelixes(char[] structureTags) {
    findPitch(3, 4, '4', structureTags);
  }

  void findPitch(int minRunLength, int pitch, char tag, char[] tags) {
    int runLength = 0;
    for (int i = 0; i < monomerCount; ++i) {
      if (mainchainHbondOffsets[i] == pitch) {
        ++runLength;
        if (runLength == minRunLength)
          for (int j = minRunLength; --j >= 0; )
            tags[i - j] = tag;
        else if (runLength > minRunLength)
          tags[i] = tag;
      } else {
        runLength = 0;
      }
    }
  }

  void findSheets(char[] structureTags) {
    if (debugHbonds)
      Logger.debug("findSheets(...)");
    for (int a = 0; a < monomerCount; ++a) {
      //if (structureTags[a] == '4')
       //continue;
      for (int b = 0; b < monomerCount; ++b) {
        //if (structureTags[b] == '4')
          //continue;
        // tim 2005 10 11
        // changed tests to reflect actual hbonding patterns in 
        // beta sheets.
        if ( ( isHbonded(a, b) && isHbonded(b+2, a) ) || 
           ( isHbonded(b, a) && isHbonded(a, b+2) ) )  {
          if (debugHbonds)
            Logger.debug("parallel found a=" + a + " b=" + b);
          structureTags[a] = structureTags[b] = 
          structureTags[a+1] = structureTags[b+1] = 
          structureTags[a+2] = structureTags[b+2] = 'p';
         } else if (isHbonded(a, b) && isHbonded(b, a)) {
          if (debugHbonds)
            Logger.debug("antiparallel found a=" + a + " b=" + b);
          structureTags[a] = structureTags[b] = 'a';
          // tim 2005 10 11
          // gap-filling feature: if n is sheet, and n+2 or n-2 are sheet, 
          // make n-1 and n+1 sheet as well.
          if ( (a+2 < monomerCount) && (b-2 > 0) && 
             (structureTags[a+2] == 'a') && (structureTags[b-2] == 'a') ) 
            structureTags[a+1] = structureTags[b-1] = 'a';
          if ( (b+2 < monomerCount) && (a-2 > 0) && 
             (structureTags[a-2] == 'a') && (structureTags[b+2] == 'a') ) 
            structureTags[a-1] = structureTags[b+1] = 'a';
        } 
        else if ( (isHbonded(a, b+1) && isHbonded(b, a+1) ) || 
                ( isHbonded(b+1, a) && isHbonded(a+1, b) ) ) {
          if (debugHbonds)
            Logger.debug("antiparallel found a=" + a + " b=" + b);
          structureTags[a] = structureTags[a+1] =
          structureTags[b] = structureTags[b+1] = 'A';
        }
      }
    }
  }
  
  *
  * end old code for assigning SS.
  */
  
  boolean isHbonded(int indexDonor, int indexAcceptor) {
    if (indexDonor < 0 || indexDonor >= monomerCount ||
        indexAcceptor < 0 || indexAcceptor >= monomerCount)
      return false;
    return ((min1Indexes[indexDonor] == indexAcceptor &&
             min1Energies[indexDonor] <= -500) ||
            (min2Indexes[indexDonor] == indexAcceptor &&
             min2Energies[indexDonor] <= -500));
  }

}
