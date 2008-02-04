/* $RCSfile: SmilesBond.java,v $
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
 * Bond in a SmilesMolecule
 */
public class SmilesBond {

  // Bond orders
  public final static int TYPE_UNKOWN = -1;
  public final static int TYPE_NONE = 0;
  public final static int TYPE_SINGLE = 1;
  public final static int TYPE_DOUBLE = 2;
  public final static int TYPE_TRIPLE = 3;
  public final static int TYPE_AROMATIC = 4;
  public final static int TYPE_DIRECTIONAL_1 = 5;
  public final static int TYPE_DIRECTIONAL_2 = 6;

  // Bond expressions
  public final static char CODE_NONE = '.';
  public final static char CODE_SINGLE = '-';
  public final static char CODE_DOUBLE = '=';
  public final static char CODE_TRIPLE = '#';
  public final static char CODE_AROMATIC = ':';
  public final static char CODE_DIRECTIONAL_1 = '/';
  public final static char CODE_DIRECTIONAL_2 = '\\';

  private SmilesAtom atom1;
  private SmilesAtom atom2;
  private int bondType;
  
  /**
   * SmilesBond constructor
   * 
   * @param atom1 First atom
   * @param atom2 Second atom
   * @param bondType Bond type
   */
  public SmilesBond(SmilesAtom atom1, SmilesAtom atom2, int bondType) {
    this.atom1 = atom1;
    this.atom2 = atom2;
    this.bondType = bondType;
  }

  /**
   * @param code Bond code
   * @return Bond type
   */
  public static int getBondTypeFromCode(char code) {
    switch (code) {
    case CODE_NONE:
      return TYPE_NONE;
    case CODE_SINGLE:
      return TYPE_SINGLE;
    case CODE_DOUBLE:
      return TYPE_DOUBLE;
    case CODE_TRIPLE:
      return TYPE_TRIPLE;
    case CODE_AROMATIC:
      return TYPE_AROMATIC;
    case CODE_DIRECTIONAL_1:
      return TYPE_DIRECTIONAL_1;
    case CODE_DIRECTIONAL_2:
      return TYPE_DIRECTIONAL_2;
    }
    return TYPE_UNKOWN;
  }

  public SmilesAtom getAtom1() {
    return atom1;
  }

  public void setAtom1(SmilesAtom atom) {
    this.atom1 = atom;
  }

  public SmilesAtom getAtom2() {
    return atom2;
  }

  public void setAtom2(SmilesAtom atom) {
    this.atom2 = atom;
  }

  public int getBondType() {
    return bondType;
  }

  public void setBondType(int bondType) {
    this.bondType = bondType;
  }
}
