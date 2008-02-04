/*
 *   Copyright (C) 2006  The Concord Consortium, Inc.,
 *   25 Love Lane, Concord, MA 01742
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * END LICENSE */

package org.concord.mw2d.models;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.concord.modeler.util.FloatQueue;

/**
 * Type-safe object defining a reaction and its parameters.
 * 
 * @author Charles Xie
 */

public abstract class Reaction implements Serializable {

	private transient PropertyChangeSupport pcs;

	String nameA = "A";
	String nameB = "B";
	String nameC = "C";
	String nameD = "D";
	int frequency = 10;
	Map<String, Double> parameters;
	Map<String, String> equations;
	List<String> well, hill;

	public Reaction() {
		parameters = new HashMap<String, Double>();
		equations = new HashMap<String, String>();
		well = new ArrayList<String>();
		hill = new ArrayList<String>();
		pcs = new PropertyChangeSupport(this);
	}

	public void init(int n, FloatQueue q) {
	}

	public String toHTML() {
		return toString();
	}

	public void setFrequency(int i) {
		frequency = i;
	}

	public int getFrequency() {
		return frequency;
	}

	public void setNameA(String s) {
		nameA = s;
	}

	public void setNameB(String s) {
		nameB = s;
	}

	public void setNameC(String s) {
		nameC = s;
	}

	public void setNameD(String s) {
		nameD = s;
	}

	public String getNameA() {
		return nameA;
	}

	public String getNameB() {
		return nameB;
	}

	public String getNameC() {
		return nameC;
	}

	public String getNameD() {
		return nameD;
	}

	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(pcl);
	}

	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		pcs.removePropertyChangeListener(pcl);
	}

	public void setParameters(Map<String, Double> hm) {
		parameters = hm;
	}

	public Map<String, Double> getParameters() {
		return parameters;
	}

	public List<String> getWells() {
		return well;
	}

	public List<String> getHills() {
		return hill;
	}

	public Double getParameter(String key) {
		return parameters.get(key);
	}

	public Double putParameter(String key, Double value) {
		Double oldValue = parameters.put(key, value);
		pcs.firePropertyChange(key, oldValue, value);
		return oldValue;
	}

	public static class nA__An extends Reaction {

		final static String VAA = "VAA";
		final static String VBB = "VBB";
		final static String VCC = "VCC";
		final static String VDD = "VDD";
		final static String VAB = "VAB";
		final static String VAC = "VAC";
		final static String VAD = "VAD";
		final static String VBC = "VBC";
		final static String VBD = "VBD";
		final static String VCD = "VCD";

		public final static int CHAIN_GROWTH = 0;
		public final static int STEP_GROWTH = 1;

		private int type = STEP_GROWTH;

		public nA__An() {
			putParameter(VAA, .2);
			putParameter(VBB, .2);
			putParameter(VCC, .2);
			putParameter(VDD, .2);
			putParameter(VAB, .2);
			putParameter(VAC, .2);
			putParameter(VAD, .2);
			putParameter(VBC, .2);
			putParameter(VBD, .2);
			putParameter(VCD, .2);
		}

		public void init(int n, FloatQueue q) {
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public String toString() {
			return "n" + nameA + " <--> " + nameA + "n";
		}

		public String toHTML() {
			return "n" + nameA + " &#8660; " + nameA + "<sub>n</sub>";
		}

	}

	public static class A2_B2__2AB extends Reaction {

		public final static String VAA = "VAA";
		public final static String VBB = "VBB";
		public final static String VAB = "VAB";
		public final static String VAB2 = "VAB2";
		public final static String VA2B = "VA2B";

		private FloatQueue moleFractionA2;
		private FloatQueue moleFractionB2;
		private FloatQueue moleFractionAB;
		private FloatQueue numberOfA2;
		private FloatQueue numberOfB2;
		private FloatQueue numberOfAB;

		public A2_B2__2AB() {
			well.add(VAA);
			well.add(VBB);
			well.add(VAB);
			putParameter(VAA, 4.0);
			putParameter(VBB, 4.0);
			putParameter(VAB, 6.0);
			hill.add(VAB2);
			hill.add(VA2B);
			putParameter(VAB2, 0.02);
			putParameter(VA2B, 0.02);
		}

		public void setMoleFractionA2(FloatQueue q) {
			moleFractionA2 = q;
		}

		public FloatQueue moleFractionA2() {
			return moleFractionA2;
		}

		public void setMoleFractionB2(FloatQueue q) {
			moleFractionB2 = q;
		}

		public FloatQueue moleFractionB2() {
			return moleFractionB2;
		}

		public void setMoleFractionAB(FloatQueue q) {
			moleFractionAB = q;
		}

		public FloatQueue moleFractionAB() {
			return moleFractionAB;
		}

		public void setNumberOfA2(FloatQueue q) {
			numberOfA2 = q;
		}

		public FloatQueue numberOfA2() {
			return numberOfA2;
		}

		public void setNumberOfB2(FloatQueue q) {
			numberOfB2 = q;
		}

		public FloatQueue numberOfB2() {
			return numberOfB2;
		}

		public void setNumberOfAB(FloatQueue q) {
			numberOfAB = q;
		}

		public FloatQueue numberOfAB() {
			return numberOfAB;
		}

		public void init(int n, FloatQueue q) {
			if (moleFractionA2 == null) {
				moleFractionA2 = new FloatQueue("Mole Fraction A2(%)", n);
				moleFractionA2.setCoordinateQueue(q);
				moleFractionA2.setReferenceUpperBound(100);
				moleFractionA2.setReferenceLowerBound(0);
			}
			if (moleFractionB2 == null) {
				moleFractionB2 = new FloatQueue("Mole Fraction B2(%)", n);
				moleFractionB2.setCoordinateQueue(q);
				moleFractionB2.setReferenceUpperBound(100);
				moleFractionB2.setReferenceLowerBound(0);
			}
			if (moleFractionAB == null) {
				moleFractionAB = new FloatQueue("Mole Fraction AB(%)", n);
				moleFractionAB.setCoordinateQueue(q);
				moleFractionAB.setReferenceUpperBound(100);
				moleFractionAB.setReferenceLowerBound(0);
			}
			int ub = AtomicModel.getMaximumNumberOfAtoms() >> 1;
			if (numberOfA2 == null) {
				numberOfA2 = new FloatQueue("Number of A2", n);
				numberOfA2.setCoordinateQueue(q);
				numberOfA2.setReferenceUpperBound(ub);
				numberOfA2.setReferenceLowerBound(0);
			}
			if (numberOfB2 == null) {
				numberOfB2 = new FloatQueue("Number of B2", n);
				numberOfB2.setCoordinateQueue(q);
				numberOfB2.setReferenceUpperBound(ub);
				numberOfB2.setReferenceLowerBound(0);
			}
			if (numberOfAB == null) {
				numberOfAB = new FloatQueue("Number of AB", n);
				numberOfAB.setCoordinateQueue(q);
				numberOfAB.setReferenceUpperBound(ub);
				numberOfAB.setReferenceLowerBound(0);
			}
		}

		public int getIDA() {
			return Element.ID_PL;
		}

		public int getIDB() {
			return Element.ID_WS;
		}

		public String toString() {
			return nameA + "2 + " + nameB + "2 <--> 2" + nameA + nameB;
		}

		public String toHTML() {
			return nameA + "<sub>2</sub> + " + nameB + "<sub>2</sub> &#8660; 2" + nameA + nameB;
		}

	}

	public static class O2_2H2__2H2O extends Reaction {

		public final static String VHH = "VHH";
		public final static String VOO = "VOO";
		public final static String VHO = "VHO";
		public final static String VHO2 = "VHO2";
		public final static String VOH2 = "VOH2";

		private FloatQueue moleFractionH2;
		private FloatQueue moleFractionO2;
		private FloatQueue moleFractionH2O;
		private FloatQueue numberOfH2;
		private FloatQueue numberOfO2;
		private FloatQueue numberOfH2O;

		public O2_2H2__2H2O() {
			nameA = "H";
			nameB = "O";
			well.add(VHH);
			well.add(VOO);
			well.add(VHO);
			putParameter(VHH, 4.52);
			putParameter(VOO, 5.16);
			putParameter(VHO, 5.2);
			hill.add(VHO2);
			hill.add(VOH2);
			putParameter(VHO2, 0.01);
			putParameter(VOH2, 0.01);
		}

		public void setMoleFractionH2(FloatQueue q) {
			moleFractionH2 = q;
		}

		public FloatQueue moleFractionH2() {
			return moleFractionH2;
		}

		public void setMoleFractionO2(FloatQueue q) {
			moleFractionO2 = q;
		}

		public FloatQueue moleFractionO2() {
			return moleFractionO2;
		}

		public void setMoleFractionH2O(FloatQueue q) {
			moleFractionH2O = q;
		}

		public FloatQueue moleFractionH2O() {
			return moleFractionH2O;
		}

		public void setNumberOfH2(FloatQueue q) {
			numberOfH2 = q;
		}

		public FloatQueue numberOfH2() {
			return numberOfH2;
		}

		public void setNumberOfO2(FloatQueue q) {
			numberOfO2 = q;
		}

		public FloatQueue numberOfO2() {
			return numberOfO2;
		}

		public void setNumberOfH2O(FloatQueue q) {
			numberOfH2O = q;
		}

		public FloatQueue numberOfH2O() {
			return numberOfH2O;
		}

		public void init(int n, FloatQueue q) {
			if (moleFractionH2 == null) {
				moleFractionH2 = new FloatQueue("Mole Fraction H2(%)", n);
				moleFractionH2.setCoordinateQueue(q);
				moleFractionH2.setReferenceUpperBound(100);
				moleFractionH2.setReferenceLowerBound(0);
			}
			if (moleFractionO2 == null) {
				moleFractionO2 = new FloatQueue("Mole Fraction O2(%)", n);
				moleFractionO2.setCoordinateQueue(q);
				moleFractionO2.setReferenceUpperBound(100);
				moleFractionO2.setReferenceLowerBound(0);
			}
			if (moleFractionH2O == null) {
				moleFractionH2O = new FloatQueue("Mole Fraction H2O(%)", n);
				moleFractionH2O.setCoordinateQueue(q);
				moleFractionH2O.setReferenceUpperBound(100);
				moleFractionH2O.setReferenceLowerBound(0);
			}
			int ub = AtomicModel.getMaximumNumberOfAtoms() >> 1;
			if (numberOfH2 == null) {
				numberOfH2 = new FloatQueue("Number of H2", n);
				numberOfH2.setCoordinateQueue(q);
				numberOfH2.setReferenceUpperBound(ub);
				numberOfH2.setReferenceLowerBound(0);
			}
			if (numberOfO2 == null) {
				numberOfO2 = new FloatQueue("Number of O2", n);
				numberOfO2.setCoordinateQueue(q);
				numberOfO2.setReferenceUpperBound(ub);
				numberOfO2.setReferenceLowerBound(0);
			}
			if (numberOfH2O == null) {
				numberOfH2O = new FloatQueue("Number of H2O", n);
				numberOfH2O.setCoordinateQueue(q);
				numberOfH2O.setReferenceUpperBound(ub);
				numberOfH2O.setReferenceLowerBound(0);
			}
		}

		public int getIDH() {
			return Element.ID_PL;
		}

		public int getIDO() {
			return Element.ID_WS;
		}

		public String toString() {
			return "2" + nameA + "2 + " + nameB + "2 --> 2" + nameA + "2" + nameB;
		}

		public String toHTML() {
			return "2" + nameA + "<sub>2</sub> + " + nameB + "<sub>2</sub> &#8594; 2" + nameA + "<sub>2</sub>" + nameB;
		}

	}

	/** C pretends to be a catalyst. */
	public static class A2_B2_C__2AB_C extends Reaction {

		public final static String VAA = "VAA";
		public final static String VBB = "VBB";
		public final static String VCC = "VCC";
		public final static String VAB = "VAB";
		public final static String VAC = "VAC";
		public final static String VBC = "VBC";
		public final static String VAB2 = "VAB2";
		public final static String VBA2 = "VBA2";
		public final static String VCA2 = "VCA2";
		public final static String VCB2 = "VCB2";
		public final static String VABC = "VABC";
		public final static String VBAC = "VBAC";

		private FloatQueue moleFractionA2;
		private FloatQueue moleFractionB2;
		private FloatQueue moleFractionAB;
		private FloatQueue numberOfA2;
		private FloatQueue numberOfB2;
		private FloatQueue numberOfAB;
		private FloatQueue numberOfC;

		public A2_B2_C__2AB_C() {
			well.add(VAA);
			well.add(VBB);
			well.add(VAB);
			well.add(VAC);
			well.add(VBC);
			putParameter(VAA, 0.4);
			putParameter(VBB, 0.4);
			putParameter(VCC, 0.0);
			putParameter(VAB, 2.0);
			putParameter(VAC, 0.4);
			putParameter(VBC, 0.4);
			hill.add(VAB2);
			hill.add(VBA2);
			hill.add(VCA2);
			hill.add(VCB2);
			hill.add(VABC);
			hill.add(VBAC);
			putParameter(VAB2, 5.0);
			putParameter(VBA2, 5.0);
			putParameter(VCA2, 0.1);
			putParameter(VCB2, 0.1);
			putParameter(VABC, 0.1);
			putParameter(VBAC, 0.1);
		}

		public void setMoleFractionA2(FloatQueue q) {
			moleFractionA2 = q;
		}

		public FloatQueue moleFractionA2() {
			return moleFractionA2;
		}

		public void setMoleFractionB2(FloatQueue q) {
			moleFractionB2 = q;
		}

		public FloatQueue moleFractionB2() {
			return moleFractionB2;
		}

		public void setMoleFractionAB(FloatQueue q) {
			moleFractionAB = q;
		}

		public FloatQueue moleFractionAB() {
			return moleFractionAB;
		}

		public void setNumberOfA2(FloatQueue q) {
			numberOfA2 = q;
		}

		public FloatQueue numberOfA2() {
			return numberOfA2;
		}

		public void setNumberOfB2(FloatQueue q) {
			numberOfB2 = q;
		}

		public FloatQueue numberOfB2() {
			return numberOfB2;
		}

		public void setNumberOfAB(FloatQueue q) {
			numberOfAB = q;
		}

		public FloatQueue numberOfAB() {
			return numberOfAB;
		}

		public void setNumberOfC(FloatQueue q) {
			numberOfC = q;
		}

		public FloatQueue numberOfC() {
			return numberOfC;
		}

		public void init(int n, FloatQueue q) {
			if (moleFractionA2 == null) {
				moleFractionA2 = new FloatQueue("Mole Fraction A2(%)", n);
				moleFractionA2.setCoordinateQueue(q);
				moleFractionA2.setReferenceUpperBound(100);
				moleFractionA2.setReferenceLowerBound(0);
			}
			if (moleFractionB2 == null) {
				moleFractionB2 = new FloatQueue("Mole Fraction B2(%)", n);
				moleFractionB2.setCoordinateQueue(q);
				moleFractionB2.setReferenceUpperBound(100);
				moleFractionB2.setReferenceLowerBound(0);
			}
			if (moleFractionAB == null) {
				moleFractionAB = new FloatQueue("Mole Fraction AB(%)", n);
				moleFractionAB.setCoordinateQueue(q);
				moleFractionAB.setReferenceUpperBound(100);
				moleFractionAB.setReferenceLowerBound(0);
			}
			int ub = AtomicModel.getMaximumNumberOfAtoms() >> 1;
			if (numberOfA2 == null) {
				numberOfA2 = new FloatQueue("Number of A2", n);
				numberOfA2.setCoordinateQueue(q);
				numberOfA2.setReferenceUpperBound(ub);
				numberOfA2.setReferenceLowerBound(0);
			}
			if (numberOfB2 == null) {
				numberOfB2 = new FloatQueue("Number of B2", n);
				numberOfB2.setCoordinateQueue(q);
				numberOfB2.setReferenceUpperBound(ub);
				numberOfB2.setReferenceLowerBound(0);
			}
			if (numberOfAB == null) {
				numberOfAB = new FloatQueue("Number of AB", n);
				numberOfAB.setCoordinateQueue(q);
				numberOfAB.setReferenceUpperBound(ub);
				numberOfAB.setReferenceLowerBound(0);
			}
			if (numberOfC == null) {
				numberOfC = new FloatQueue("Number of C*", n);
				numberOfC.setCoordinateQueue(q);
				numberOfC.setReferenceUpperBound(AtomicModel.getMaximumNumberOfAtoms() >> 1);
				numberOfC.setReferenceLowerBound(0);
			}
		}

		public int getIDA() {
			return Element.ID_PL;
		}

		public int getIDB() {
			return Element.ID_WS;
		}

		public int getIDC() {
			return Element.ID_CK;
		}

		public String toString() {
			return nameA + "2 + " + nameB + "2 + " + nameC + " <--> 2" + nameA + nameB + " + " + nameC;
		}

		public String toHTML() {
			return nameA + "<sub>2</sub> + " + nameB + "<sub>2</sub> + " + nameC + " &#8660; 2" + nameA + nameB + " + "
					+ nameC;
		}

	}

	/** elementary reaction */
	public static class A_B2__AB_B extends Reaction {
		public String toString() {
			return nameA + "* + " + nameB + "2 <--> " + nameA + nameB + " + " + nameB + "*";
		}

		public String toHTML() {
			return nameA + "&#183; + " + nameB + "<sub>2</sub> &#8660; " + nameA + nameB + " + " + nameB + "&#183;";
		}
	}

	/** elementary reaction */
	public static class A2_B__A_AB extends Reaction {
		public String toString() {
			return nameA + "2 + " + nameB + "* <--> " + nameA + "* + " + nameA + nameB;
		}

		public String toHTML() {
			return nameA + "<sub>2</sub> + " + nameB + "&#183; &#8660; " + nameA + "&#183; + " + nameA + nameB;
		}
	}

	/** elementary reaction */
	public static class C_A2__AC_A extends Reaction {
		public String toString() {
			return nameC + " + " + nameA + "2 <-->" + nameA + nameC + " + " + nameA + "*";
		}

		public String toHTML() {
			return nameC + " + " + nameA + "<sub>2</sub> &#8660; " + nameA + nameC + " + " + nameA + "&#183;";
		}
	}

	/** elementary reaction */
	public static class C_B2__BC_B extends Reaction {
		public String toString() {
			return nameC + " + " + nameB + "2 <--> " + nameB + nameC + " + " + nameB + "*";
		}

		public String toHTML() {
			return nameC + " + " + nameB + "<sub>2</sub> &#8660; " + nameB + nameC + " + " + nameB + "&#183;";
		}
	}

	/** elementary reaction */
	public static class A_BC__AB_C extends Reaction {
		public String toString() {
			return nameA + "* + " + nameB + nameC + " <--> " + nameA + nameB + " + " + nameC;
		}

		public String toHTML() {
			return nameA + "&#183; + " + nameB + nameC + " &#8660; " + nameA + nameB + " + " + nameC;
		}
	}

	/** elementary reaction */
	public static class B_AC__AB_C extends Reaction {
		public String toString() {
			return nameB + "* + " + nameA + nameC + " <--> " + nameA + nameB + " + " + nameC;
		}

		public String toHTML() {
			return nameB + "&#183; + " + nameA + nameC + " &#8660; " + nameA + nameB + " + " + nameC;
		}
	}

	/** elementary reaction */
	public static class A2__2A extends Reaction {
		public String toString() {
			return nameA + "2 <--> 2" + nameA + "*";
		}

		public String toHTML() {
			return nameA + "<sub>2</sub> &#8660; 2" + nameA + "&#183;";
		}
	}

	/** elementary reaction */
	public static class B2__2B extends Reaction {
		public String toString() {
			return nameB + "2 --> 2" + nameB + "*";
		}

		public String toHTML() {
			return nameB + "<sub>2</sub> &#8660; 2" + nameB + "&#183;";
		}
	}

	/** elementary reaction */
	public static class AB__A_B extends Reaction {
		public String toString() {
			return nameA + nameB + " <--> " + nameA + "* + " + nameB + "*";
		}

		public String toHTML() {
			return nameA + nameB + " &#8660; " + nameA + "&#183; + " + nameB + "&#183;";
		}
	}

	/** elementary reaction */
	public static class AC__A_C extends Reaction {
		public String toString() {
			return nameA + nameC + " <--> " + nameA + "* + " + nameC;
		}

		public String toHTML() {
			return nameA + nameC + " &#8660; " + nameA + "&#183; + " + nameC;
		}
	}

	/** elementary reaction */
	public static class BC__B_C extends Reaction {
		public String toString() {
			return nameB + nameC + " <--> " + nameB + "* + " + nameC;
		}

		public String toHTML() {
			return nameB + nameC + " &#8660; " + nameB + "&#183; + " + nameC;
		}
	}

	/** elementary reaction */
	public static class OH_H2__H2O_H extends Reaction {
		public String toString() {
			return "OH* + H2 --> H2O + H*";
		}

		public String toHTML() {
			return "OH&#183; + H<sub>2</sub> &#8594; H<sub>2</sub>O + H&#183;";
		}
	}

}