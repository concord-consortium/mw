/* $RCSfile: SmilesAtom.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:14 $
 * $Revision: 1.9 $
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.smiles;

/**
 * This class represents an atom in a <code>SmilesMolecule</code>.
 */
public class SmilesAtom {

  private int number;
  private String symbol;
  private int atomicMass;
  private int charge;
  private int hydrogenCount;
  private int matchingAtom;
  private String chiralClass;
  private int chiralOrder;

  private SmilesBond[] bonds;
  private int bondsCount;

  private final static int INITIAL_BONDS = 4;

  
  /**
   * Constant used for default chirality.
   */
  public final static String DEFAULT_CHIRALITY = "";
  /**
   * Constant used for Allene chirality.
   */
  public final static String CHIRALITY_ALLENE = "AL";
  /**
   * Constant used for Octahedral chirality.
   */
  public final static String CHIRALITY_OCTAHEDRAL = "OH";
  /**
   * Constant used for Square Planar chirality.
   */
  public final static String CHIRALITY_SQUARE_PLANAR = "SP";
  /**
   * Constant used for Tetrahedral chirality.
   */
  public final static String CHIRALITY_TETRAHEDRAL = "TH";
  /**
   * Constant used for Trigonal Bipyramidal chirality.
   */
  public final static String CHIRALITY_TRIGONAL_BIPYRAMIDAL = "TB";

  /**
   * Constructs a <code>SmilesAtom</code>.
   * 
   * @param number Atom number in the molecule. 
   */
  public SmilesAtom(int number) {
    this.number = number;
    this.symbol = null;
    this.atomicMass = Integer.MIN_VALUE;
    this.charge = 0;
    this.hydrogenCount = Integer.MIN_VALUE;
    this.matchingAtom = -1;
    this.chiralClass = null;
    this.chiralOrder = Integer.MIN_VALUE;
    bonds = new SmilesBond[INITIAL_BONDS];
    bondsCount = 0;
  }

  /**
   * Creates missing hydrogen atoms in a <code>SmilesMolecule</code>.
   * 
   * @param molecule Molecule containing the atom.
   */
  public void createMissingHydrogen(SmilesMolecule molecule) {
  	// Determing max count
  	int count = 0;
  	if (hydrogenCount == Integer.MIN_VALUE) {
      if (symbol != null) {
        if (symbol == "B") {
          count = 3;
        } else if (symbol == "Br") {
          count = 1;
        } else if (symbol == "C") {
          count = 4;
        } else if (symbol == "Cl") {
          count = 1;
        } else if (symbol == "F") {
          count = 1;
        } else if (symbol == "I") {
          count = 1;
        } else if (symbol == "N") {
          count = 3;
        } else if (symbol == "O") {
          count = 2;
        } else if (symbol == "P") {
          count = 3;
        } else if (symbol == "S") {
          count = 2;
        }
      }
      for (int i = 0; i < bondsCount; i++) {
        SmilesBond bond = bonds[i];
        switch (bond.getBondType()) {
        case SmilesBond.TYPE_SINGLE:
        case SmilesBond.TYPE_DIRECTIONAL_1:
        case SmilesBond.TYPE_DIRECTIONAL_2:
          count -= 1;
          break;
        case SmilesBond.TYPE_DOUBLE:
          count -= 2;
          break;
        case SmilesBond.TYPE_TRIPLE:
          count -= 3;
          break;
        }
      }
  	} else {
  	  count = hydrogenCount;
  	}

    // Adding hydrogens
    for (int i = 0; i < count; i++) {
      SmilesAtom hydrogen = molecule.createAtom();
      molecule.createBond(this, hydrogen, SmilesBond.TYPE_SINGLE);
      hydrogen.setSymbol("H");
    }
  }

  /**
   * Returns the atom number of the atom.
   * 
   * @return Atom number.
   */
  public int getNumber() {
    return number;
  }

  /**
   * Returns the symbol of the atom.
   * 
   * @return Atom symbol.
   */
  public String getSymbol() {
    return symbol;
  }

  /**
   * Sets the symbol of the atm.
   * 
   * @param symbol Atom symbol.
   */
  public void setSymbol(String symbol) {
    this.symbol = (symbol != null) ? symbol.intern() : null;
  }

  /**
   * Returns the atomic mass of the atom.
   * 
   * @return Atomic mass.
   */
  public int getAtomicMass() {
    return atomicMass;
  }

  /**
   * Sets the atomic mass of the atom.
   * 
   * @param mass Atomic mass.
   */
  public void setAtomicMass(int mass) {
    this.atomicMass = mass;
  }
  
  /**
   * Returns the charge of the atom.
   * 
   * @return Charge.
   */
  public int getCharge() {
    return charge;
  }

  /**
   * Sets the charge of the atom.
   * 
   * @param charge Charge.
   */
  public void setCharge(int charge) {
    this.charge = charge;
  }

  /**
   * Returns the number of a matching atom in a molecule.
   * This value is temporary, it is used during the pattern matching algorithm.
   * 
   * @return matching atom.
   */
  public int getMatchingAtom() {
    return matchingAtom;
  }

  /**
   * Sets the number of a matching atom in a molecule.
   * This value is temporary, it is used during the pattern matching algorithm.
   * 
   * @param atom Temporary: number of a matching atom in a molecule.
   */
  public void setMatchingAtom(int atom) {
    this.matchingAtom = atom;
  }

  /**
   * Returns the chiral class of the atom.
   * (see <code>CHIRALITY_...</code> constants)
   * 
   * @return Chiral class.
   */
  public String getChiralClass() {
    return chiralClass;
  }

  /**
   * Sets the chiral class of the atom.
   * (see <code>CHIRALITY_...</code> constants)
   * 
   * @param chiralClass Chiral class.
   */
  public void setChiralClass(String chiralClass) {
    this.chiralClass = (chiralClass != null) ? chiralClass.intern() : null;
  }

  /**
   * Returns the chiral order of the atom.
   * 
   * @return Chiral order.
   */
  public int getChiralOrder() {
    return chiralOrder;
  }

  /**
   * Sets the chiral order of the atom.
   * 
   * @param chiralOrder Chiral order.
   */
  public void setChiralOrder(int chiralOrder) {
    this.chiralOrder = chiralOrder;
  }

  /**
   * Returns the number of hydrogen atoms bonded with this atom.
   * 
   * @return Number of hydrogen atoms.
   */
  public int getHydrogenCount() {
    return hydrogenCount;
  }

  /**
   * Sets the number of hydrogen atoms bonded with this atom.
   * 
   * @param count Number of hydrogen atoms.
   */
  public void setHydrogenCount(int count) {
    this.hydrogenCount = count;
  }

  /**
   * Returns the number of bonds of this atom.
   * 
   * @return Number of bonds.
   */
  public int getBondsCount() {
    return bondsCount;
  }

  /**
   * Returns the bond at index <code>number</code>.
   * 
   * @param number Bond number.
   * @return Bond.
   */
  public SmilesBond getBond(int number) {
    if ((number >= 0) && (number < bondsCount)) {
      return bonds[number];
    }
    return null;
  }
  
  /**
   * Add a bond to the atom.
   * 
   * @param bond Bond to add.
   */
  public void addBond(SmilesBond bond) {
    if (bondsCount >= bonds.length) {
      SmilesBond[] tmp = new SmilesBond[bonds.length * 2];
      System.arraycopy(bonds, 0, tmp, 0, bonds.length);
      bonds = tmp;
    }
    bonds[bondsCount] = bond;
    bondsCount++;
  }
}
