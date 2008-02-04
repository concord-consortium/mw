/* $RCSfile: SymmetryOperation.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:13 $
 * $Revision: 1.1 $
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

package org.jmol.symmetry;

import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Matrix4f;
import org.jmol.util.Logger;

/*
 * Bob Hanson 4/2006
 * 
 * references: International Tables for Crystallography Vol. A. (2002) 
 *
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Ispace_group_symop_operation_xyz.html
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Isymmetry_equiv_pos_as_xyz.html
 *
 * LATT : http://macxray.chem.upenn.edu/LATT.pdf thank you, Patrick Carroll
 * 
 */

public class SymmetryOperation extends Matrix4f {
  String xyzOriginal;
  String xyz;
  boolean doNormalize = true;

  public SymmetryOperation() {
  }

  public SymmetryOperation(boolean doNormalize) {
    this.doNormalize = doNormalize;
  }

  public SymmetryOperation(SymmetryOperation op, Point3f[] atoms,
                           int atomIndex, int count, boolean doNormalize) {
    /*
     * externalizes and transforms an operation for use in atom reader
     * 
     */
    this.doNormalize = doNormalize;
    xyzOriginal = op.xyzOriginal;
    xyz = op.xyz;
    set(op); // sets the underlying Matrix4f
    m03 /= 12f;
    m13 /= 12f;
    m23 /= 12f;
    if (doNormalize)
      setOffset(atoms, atomIndex, count);
  }
  
  public String getXyz() {
    return xyz;
  }

  public String getXyzOriginal() {
    return xyzOriginal;
  }

  public void newPoint(Point3f atom1, Point3f atom2,
                       int transX, int transY, int transZ) {
    Point4f temp = new Point4f(atom1.x, atom1.y, atom1.z, 1);
    transform(temp, temp);
    atom2.x = temp.x + transX;
    atom2.y = temp.y + transY;
    atom2.z = temp.z + transZ;
  }

  String dumpInfo() {
    return "\n" + xyz + "\ninternal matrix representation:\n"
        + ((Matrix4f) this).toString();
  }

  final static String dumpSeitz(Matrix4f s) {
    return "{\t" + (int) s.m00 + "\t" + (int) s.m01 + "\t" + (int) s.m02 + "\t"
        + twelfthsOf(s.m03) + "\t}\n" + "{\t" + (int) s.m10 + "\t"
        + (int) s.m11 + "\t" + (int) s.m12 + "\t" + twelfthsOf(s.m13) + "\t}\n"
        + "{\t" + (int) s.m20 + "\t" + (int) s.m21 + "\t" + (int) s.m22 + "\t"
        + twelfthsOf(s.m23) + "\t}\n" + "{\t0\t0\t0\t1\t}\n";
  }
  
  boolean setMatrixFromXYZ(String xyz) {
    /*
     * sets symmetry based on an operator string "x,-y,z+1/2", for example
     * 
     */
    if (xyz == null)
      return false;
    this.xyzOriginal = xyz;
    xyz = xyz.toLowerCase();
    float[] temp = new float[16];
    boolean isDenominator = false;
    boolean isDecimal = false;
    boolean isNegative = false;
    char ch;
    int x = 0;
    int y = 0;
    int z = 0;
    float iValue = 0;
    String strOut = "";
    String strT;
    int rowPt = -1;
    temp[15] = 1;
    float decimalMultiplier = 1f;
    xyz += ",";
    //Logger.debug(xyz.length() + " " + xyz);
    for (int i = 0; i < xyz.length(); i++) {
      ch = xyz.charAt(i);
      //Logger.debug("char = " + ch + isDecimal);
      switch (ch) {
      case '\'':
      case ' ':
        continue;
      case '-':
        isNegative = true;
        continue;
      case '+':
        isNegative = false;
        continue;
      case '/':
        isDenominator = true;
        continue;
      case 'X':
      case 'x':
        x = (isNegative ? -1 : 1);
        break;
      case 'Y':
      case 'y':
        y = (isNegative ? -1 : 1);
        break;
      case 'Z':
      case 'z':
        z = (isNegative ? -1 : 1);
        break;
      case ',':
        if (++rowPt > 2) {
          Logger.warn("Symmetry Operation? " + xyz);
          return false;
        }
        int tpt = rowPt * 4;
        // put translation into 12ths
        iValue = iValue * 12f;
        if (doNormalize) {
          while (iValue > 6)
            iValue -= 12;
          while (iValue <= -6)
            iValue += 12;
        }
        temp[tpt++] = x;
        temp[tpt++] = y;
        temp[tpt++] = z;
        temp[tpt] = iValue;
        strT = "";
        strT += (x == 0 ? "" : x < 0 ? "-x" : strT.length() == 0 ? "x" : "+x");
        strT += (y == 0 ? "" : y < 0 ? "-y" : strT.length() == 0 ? "y" : "+y");
        strT += (z == 0 ? "" : z < 0 ? "-z" : strT.length() == 0 ? "z" : "+z");
        strT += xyzFraction(iValue, false);
        strOut += (strOut == "" ? "" : ",") + strT;
        //note: when ptLatt[3] = -1, ptLatt[rowPt] MUST be 0.
        if (rowPt == 2) {
          set(temp);
          this.xyz = strOut;
          rowPt = 0;
          return true;
        }
        x = y = z = 0;
        iValue = 0;
        break;
      case '.':
        isDecimal = true;
        decimalMultiplier = 1f;
        continue;
      case '0':
        if (!isDecimal)
          continue;
      //allow to pass through
      default:
        //Logger.debug(isDecimal + " " + ch + " " + iValue);
        int ich = ch - '0';
        if (isDecimal && ich >= 0 && ich <= 9) {
          decimalMultiplier /= 10f;
          if (iValue < 0)
            isNegative = true;
          iValue += decimalMultiplier * ich * (isNegative ? -1 : 1);
          continue;
        }
        if (ich >= 1 && ich <= 9) {
          if (isDenominator) {
            iValue /= ich;
          } else {
            iValue = (isNegative ? -1f : 1f) * ich;
          }
        } else {
          Logger.warn("symmetry character?" + ch);
        }
      }
      isDecimal = isDenominator = isNegative = false;
    }
    return false;
  }

  final static String getXYZFromMatrix(Matrix4f mat, boolean allPositive) {
    String str = "";
    float[] row = new float[4];
    for (int i = 0; i < 3; i++) {
      mat.getRow(i, row);
      String term = "";
      if (row[0] != 0)
        term += (row[0] < 0 ? "-" : "+") + "x";
      if (row[1] != 0)
        term += (row[1] < 0 ? "-" : "+") + "y";
      if (row[2] != 0)
        term += (row[2] < 0 ? "-" : "+") + "z";
      term += xyzFraction(row[3], allPositive);
      if (term.length() > 0 && term.charAt(0) == '+')
        term = term.substring(1);
      str += "," + term;
    }
    return str.substring(1);
  }

  private final static String twelfthsOf(float n12ths) {
    String str = "";
    if (n12ths < 0) {
      str = "-";
      n12ths = -n12ths;
    }
    return str + twelfths[((int) n12ths) % 12];  
  }
  
  private final static String[] twelfths = { "0", "1/12", "1/6", "1/4", "1/3",
      "5/12", "1/2", "7/12", "2/3", "3/4", "5/6", "11/12" };

  private final static String xyzFraction(float n12ths, boolean allPositive) {
    if (allPositive) {
      while (n12ths < 0)
        n12ths += 12f;
    } else if (n12ths > 6f) {
      n12ths -= 12f;
    }
    String s = twelfthsOf(n12ths);
    return (s.charAt(0) == '0' ? "" : n12ths > 0 ? "+" + s : s);
  }

  Point3f atomTest = new Point3f();

  private void setOffset(Point3f[] atoms, int atomIndex, int count) {
    /*
     * the center of mass of the full set of atoms is moved into the cell with this
     *  
     */
    int i1 = atomIndex;
    int i2 = i1 + count;
    float x = 0;
    float y = 0;
    float z = 0;
    for (int i = i1; i < i2; i++) {
      newPoint(atoms[i], atomTest, 0, 0, 0);
      x += atomTest.x;
      y += atomTest.y;
      z += atomTest.z;
    }
    
    //System.out.println("SymmetryOperation.setOffset " + xyz + " " + (x/count) + (y/count) + (z/count));
    
    while (x < -0.001 || x >= count + 0.001) {
      m03 += (x < 0 ? 1 : -1);
      x += (x < 0 ? count : -count);
    }
    while (y < -0.001 || y >= count + 0.001) {
      m13 += (y < 0 ? 1 : -1);
      y += (y < 0 ? count : -count);
    }
    while (z < -0.001 || z >= count + 0.001) {
      m23 += (z < 0 ? 1 : -1);
      z += (z < 0 ? count : -count);
    }
    //System.out.println((Matrix4f)this);
  }
}
